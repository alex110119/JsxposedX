package com.jsxposed.x.hook.compat

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 纯 Java 反射工具，替代旧版 `XposedHelpers`。
 *
 * 方法签名刻意与 `XposedHelpers` 保持一致，业务层迁移时只需把 `XposedHelpers.` 换成 `ReflectX.`。
 * API 102 不提供任何反射辅助能力，因此这部分必须在模块内自实现。
 */
object ReflectX {

    private val PRIMITIVES = mapOf(
        "boolean" to java.lang.Boolean.TYPE,
        "byte" to java.lang.Byte.TYPE,
        "char" to java.lang.Character.TYPE,
        "short" to java.lang.Short.TYPE,
        "int" to java.lang.Integer.TYPE,
        "long" to java.lang.Long.TYPE,
        "float" to java.lang.Float.TYPE,
        "double" to java.lang.Double.TYPE,
        "void" to java.lang.Void.TYPE,
    )

    private val WRAPPERS = mapOf<Class<*>, Class<*>>(
        java.lang.Boolean.TYPE to java.lang.Boolean::class.java,
        java.lang.Byte.TYPE to java.lang.Byte::class.java,
        java.lang.Character.TYPE to java.lang.Character::class.java,
        java.lang.Short.TYPE to java.lang.Short::class.java,
        java.lang.Integer.TYPE to java.lang.Integer::class.java,
        java.lang.Long.TYPE to java.lang.Long::class.java,
        java.lang.Float.TYPE to java.lang.Float::class.java,
        java.lang.Double.TYPE to java.lang.Double::class.java,
    )

    // ── 类查找 ──

    fun findClass(className: String, classLoader: ClassLoader?): Class<*> {
        PRIMITIVES[className]?.let { return it }

        if (className.endsWith("[]")) {
            val component = findClass(className.substring(0, className.length - 2), classLoader)
            return java.lang.reflect.Array.newInstance(component, 0).javaClass
        }

        if (classLoader != null) {
            return Class.forName(className, false, classLoader)
        }
        return Class.forName(className, false, ReflectX::class.java.classLoader)
    }

    fun findClassIfExists(className: String, classLoader: ClassLoader?): Class<*>? =
        try {
            findClass(className, classLoader)
        } catch (_: Throwable) {
            null
        }

    // ── 字段查找 ──

    fun findField(clazz: Class<*>?, fieldName: String): Field {
        requireNotNull(clazz) { "class 为 null，无法查找字段 $fieldName" }
        return walkHierarchy(clazz).firstNotNullOfOrNull { type ->
            try {
                type.getDeclaredField(fieldName).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                null
            }
        } ?: throw NoSuchFieldError("No field $fieldName in class ${clazz.name}")
    }

    private fun walkHierarchy(clazz: Class<*>): Sequence<Class<*>> = sequence {
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            yield(current)
            current.interfaces.forEach { yield(it) }
            current = current.superclass
        }
    }

    // ── 方法 / 构造器查找 ──

    fun findMethodExact(clazz: Class<*>, methodName: String, paramTypes: Array<Class<*>>): Method =
        findMethodExactIfExists(clazz, methodName, paramTypes)
            ?: throw NoSuchMethodError("No method $methodName in class ${clazz.name}")

    fun findMethodExactIfExists(clazz: Class<*>, methodName: String, paramTypes: Array<Class<*>>): Method? {
        walkHierarchy(clazz).forEach { type ->
            try {
                return type.getDeclaredMethod(methodName, *paramTypes).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                // 继续向上找
            }
        }
        return null
    }

    fun findConstructorExact(clazz: Class<*>, paramTypes: Array<Class<*>>): Constructor<*> {
        val ctor = clazz.getDeclaredConstructor(*paramTypes)
        ctor.isAccessible = true
        return ctor
    }

    private fun findMethodBestMatch(clazz: Class<*>, methodName: String, args: Array<out Any?>): Method {
        val candidates = walkHierarchy(clazz)
            .flatMap { it.declaredMethods.asSequence() }
            .filter { it.name == methodName && it.parameterTypes.size == args.size }
            .toList()
            .ifEmpty { throw NoSuchMethodError("No method $methodName in class ${clazz.name}") }

        candidates.firstOrNull { isExactMatch(it.parameterTypes, args) }
            ?.let { it.isAccessible = true; return it }

        candidates.firstOrNull { isAssignable(it.parameterTypes, args) }
            ?.let { it.isAccessible = true; return it }

        throw NoSuchMethodError("No matching method $methodName(${args.joinToString(",") { it?.javaClass?.name ?: "null" }}) in ${clazz.name}")
    }

    private fun findConstructorBestMatch(clazz: Class<*>, args: Array<out Any?>): Constructor<*> {
        val candidates = clazz.declaredConstructors
            .filter { it.parameterTypes.size == args.size }

        candidates.firstOrNull { isExactMatch(it.parameterTypes, args) }
            ?.let { it.isAccessible = true; return it }

        candidates.firstOrNull { isAssignable(it.parameterTypes, args) }
            ?.let { it.isAccessible = true; return it }

        throw NoSuchMethodError("No matching constructor in ${clazz.name}")
    }

    private fun isExactMatch(paramTypes: Array<Class<*>>, args: Array<out Any?>): Boolean =
        paramTypes.indices.all { index ->
            val expected = WRAPPERS[paramTypes[index]] ?: paramTypes[index]
            val actual = args[index]?.javaClass ?: return@all false
            expected == actual
        }

    private fun isAssignable(paramTypes: Array<Class<*>>, args: Array<out Any?>): Boolean =
        paramTypes.indices.all { index ->
            val paramType = WRAPPERS[paramTypes[index]] ?: paramTypes[index]
            val arg = args[index] ?: return@all !paramTypes[index].isPrimitive
            paramType.isAssignableFrom(arg.javaClass)
        }

    // ── 调用 ──

    fun callMethod(obj: Any?, methodName: String, vararg args: Any?): Any? {
        requireNotNull(obj) { "对象为 null，无法调用 $methodName" }
        val method = findMethodBestMatch(obj.javaClass, methodName, args)
        return try {
            method.invoke(obj, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    fun callMethod(obj: Any?, methodName: String, paramTypes: Array<Class<*>>, vararg args: Any?): Any? {
        requireNotNull(obj) { "对象为 null，无法调用 $methodName" }
        val method = findMethodExact(obj.javaClass, methodName, paramTypes)
        return try {
            method.invoke(obj, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        val method = findMethodBestMatch(clazz, methodName, args)
        return try {
            method.invoke(null, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    fun callStaticMethod(clazz: Class<*>, methodName: String, paramTypes: Array<Class<*>>, vararg args: Any?): Any? {
        val method = findMethodExact(clazz, methodName, paramTypes)
        return try {
            method.invoke(null, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    fun newInstance(clazz: Class<*>, vararg args: Any?): Any {
        val ctor = findConstructorBestMatch(clazz, args)
        return try {
            requireNotNull(ctor.newInstance(*args)) { "构造器返回 null: ${clazz.name}" }
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    fun newInstance(clazz: Class<*>, paramTypes: Array<Class<*>>, vararg args: Any?): Any {
        val ctor = findConstructorExact(clazz, paramTypes)
        return try {
            requireNotNull(ctor.newInstance(*args)) { "构造器返回 null: ${clazz.name}" }
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    // ── 实例字段读写 ──

    fun getObjectField(obj: Any, fieldName: String): Any? = findField(obj.javaClass, fieldName).get(obj)

    fun setObjectField(obj: Any, fieldName: String, value: Any?) {
        findField(obj.javaClass, fieldName).set(obj, value)
    }

    fun getBooleanField(obj: Any, fieldName: String): Boolean = findField(obj.javaClass, fieldName).getBoolean(obj)

    fun setBooleanField(obj: Any, fieldName: String, value: Boolean) {
        findField(obj.javaClass, fieldName).setBoolean(obj, value)
    }

    fun getByteField(obj: Any, fieldName: String): Byte = findField(obj.javaClass, fieldName).getByte(obj)

    fun setByteField(obj: Any, fieldName: String, value: Byte) {
        findField(obj.javaClass, fieldName).setByte(obj, value)
    }

    fun getCharField(obj: Any, fieldName: String): Char = findField(obj.javaClass, fieldName).getChar(obj)

    fun setCharField(obj: Any, fieldName: String, value: Char) {
        findField(obj.javaClass, fieldName).setChar(obj, value)
    }

    fun getShortField(obj: Any, fieldName: String): Short = findField(obj.javaClass, fieldName).getShort(obj)

    fun setShortField(obj: Any, fieldName: String, value: Short) {
        findField(obj.javaClass, fieldName).setShort(obj, value)
    }

    fun getIntField(obj: Any, fieldName: String): Int = findField(obj.javaClass, fieldName).getInt(obj)

    fun setIntField(obj: Any, fieldName: String, value: Int) {
        findField(obj.javaClass, fieldName).setInt(obj, value)
    }

    fun getLongField(obj: Any, fieldName: String): Long = findField(obj.javaClass, fieldName).getLong(obj)

    fun setLongField(obj: Any, fieldName: String, value: Long) {
        findField(obj.javaClass, fieldName).setLong(obj, value)
    }

    fun getFloatField(obj: Any, fieldName: String): Float = findField(obj.javaClass, fieldName).getFloat(obj)

    fun setFloatField(obj: Any, fieldName: String, value: Float) {
        findField(obj.javaClass, fieldName).setFloat(obj, value)
    }

    fun getDoubleField(obj: Any, fieldName: String): Double = findField(obj.javaClass, fieldName).getDouble(obj)

    fun setDoubleField(obj: Any, fieldName: String, value: Double) {
        findField(obj.javaClass, fieldName).setDouble(obj, value)
    }

    // ── 静态字段读写 ──

    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? = findField(clazz, fieldName).get(null)

    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        findField(clazz, fieldName).set(null, value)
    }

    fun getStaticBooleanField(clazz: Class<*>, fieldName: String): Boolean = findField(clazz, fieldName).getBoolean(null)

    fun setStaticBooleanField(clazz: Class<*>, fieldName: String, value: Boolean) {
        findField(clazz, fieldName).setBoolean(null, value)
    }

    fun getStaticByteField(clazz: Class<*>, fieldName: String): Byte = findField(clazz, fieldName).getByte(null)

    fun setStaticByteField(clazz: Class<*>, fieldName: String, value: Byte) {
        findField(clazz, fieldName).setByte(null, value)
    }

    fun getStaticCharField(clazz: Class<*>, fieldName: String): Char = findField(clazz, fieldName).getChar(null)

    fun setStaticCharField(clazz: Class<*>, fieldName: String, value: Char) {
        findField(clazz, fieldName).setChar(null, value)
    }

    fun getStaticShortField(clazz: Class<*>, fieldName: String): Short = findField(clazz, fieldName).getShort(null)

    fun setStaticShortField(clazz: Class<*>, fieldName: String, value: Short) {
        findField(clazz, fieldName).setShort(null, value)
    }

    fun getStaticIntField(clazz: Class<*>, fieldName: String): Int = findField(clazz, fieldName).getInt(null)

    fun setStaticIntField(clazz: Class<*>, fieldName: String, value: Int) {
        findField(clazz, fieldName).setInt(null, value)
    }

    fun getStaticLongField(clazz: Class<*>, fieldName: String): Long = findField(clazz, fieldName).getLong(null)

    fun setStaticLongField(clazz: Class<*>, fieldName: String, value: Long) {
        findField(clazz, fieldName).setLong(null, value)
    }

    fun getStaticFloatField(clazz: Class<*>, fieldName: String): Float = findField(clazz, fieldName).getFloat(null)

    fun setStaticFloatField(clazz: Class<*>, fieldName: String, value: Float) {
        findField(clazz, fieldName).setFloat(null, value)
    }

    fun getStaticDoubleField(clazz: Class<*>, fieldName: String): Double = findField(clazz, fieldName).getDouble(null)

    fun setStaticDoubleField(clazz: Class<*>, fieldName: String, value: Double) {
        findField(clazz, fieldName).setDouble(null, value)
    }

    // ── 附加实例字段（旧版 XposedHelpers 的附加存储） ──

    private val additionalFields: MutableMap<Any, MutableMap<Any, Any?>> =
        Collections.synchronizedMap(IdentityHashMap())

    fun setAdditionalInstanceField(obj: Any, key: Any, value: Any?): Any? {
        val store = additionalFields.getOrPut(obj) { Collections.synchronizedMap(HashMap()) }
        return store.put(key, value)
    }

    fun getAdditionalInstanceField(obj: Any, key: Any): Any? =
        additionalFields[obj]?.get(key)

    fun removeAdditionalInstanceField(obj: Any, key: Any): Any? {
        val store = additionalFields[obj] ?: return null
        val removed = store.remove(key)
        if (store.isEmpty()) additionalFields.remove(obj)
        return removed
    }

    /** 供 JS 侧判断字段是否存在。 */
    fun hasField(clazz: Class<*>, fieldName: String): Boolean =
        try {
            findField(clazz, fieldName)
            true
        } catch (_: Throwable) {
            false
        }

    internal fun isStatic(field: Field): Boolean = Modifier.isStatic(field.modifiers)
}
