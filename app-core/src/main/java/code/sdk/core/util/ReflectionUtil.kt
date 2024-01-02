package code.sdk.core.util

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * reflection util
 */
object ReflectionUtil {
    private val TAG = ReflectionUtil::class.java.simpleName
    private var sForNameMethod: Method? = null
    private var sGetDeclaredMethod: Method? = null
    private var sGetFieldMethod: Method? = null

    /**
     * init reflection and cache it
     */
    init {
        try {
            sForNameMethod = Class::class.java.getDeclaredMethod("forName", String::class.java)
            sGetDeclaredMethod = Class::class.java
                .getDeclaredMethod(
                    "getDeclaredMethod",
                    String::class.java,
                    arrayOf<Class<*>>().javaClass
                )
            sGetFieldMethod =
                Class::class.java.getDeclaredMethod("getDeclaredField", String::class.java)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    /**
     * get filed
     *
     * @param clzName   the class name
     * @param filedName the filed name
     * @return the class filed
     */
    fun getFiled(clzName: String, filedName: String): Field? {
        var field: Field? = null
        if (canReflection()) {
            try {
                val clz = sForNameMethod!!.invoke(null, clzName) as Class<*>
                field = sGetFieldMethod!!.invoke(clz, filedName) as Field
                field.isAccessible = true
            } catch (_: Throwable) {
            }
        }
        return field
    }

    /**
     * set filed to the instance
     *
     * @param src       the instance
     * @param clzName   the class name
     * @param filedName the filed name
     * @param tarObj    target object
     */
    fun setFiled(
        src: Any, clzName: String,
        filedName: String, tarObj: Any?
    ) {
        try {
            val field = getFiled(clzName, filedName)
            field?.set(src, tarObj)
        } catch (_: Throwable) {
        }
    }

    /**
     * get method
     *
     * @param clzName    the class name
     * @param methodName the method name
     * @param clzArgs    method params
     * @return method
     */
    fun getMethod(
        clzName: String, methodName: String,
        clzArgs: Array<Class<*>?>?
    ): Method? {
        var method: Method? = null
        if (canReflection()) {
            try {
                val clz = sForNameMethod!!.invoke(null, clzName) as Class<*>
                method = sGetDeclaredMethod!!.invoke(clz, methodName, clzArgs) as Method
                method.isAccessible = true
            } catch (_: Throwable) {
            }
        }
        return method
    }

    /**
     * invoke method
     *
     * @param src        the instance
     * @param clzName    the class name
     * @param methodName the method name
     * @param clzArgs    args class array
     * @param objArgs    args
     * @return obj
     */
    fun invokeMethod(
        src: Any, clzName: String,
        methodName: String, clzArgs: Array<Class<*>?>?, vararg objArgs: Any?
    ): Any? {
        var result: Any? = null
        try {
            val method = getMethod(clzName, methodName, clzArgs)
            if (method != null) {
                result = method.invoke(src, *objArgs)
            }
        } catch (_: Throwable) {
        }
        return result
    }

    fun newInstance(clzName: String?, clzArgs: Array<Class<*>?>?, vararg objArgs: Any?): Any? {
        var instance: Any? = null
        if (canReflection()) {
            try {
                val clz = Class.forName(clzName)
                if (clzArgs != null) {
                    val constructor = clz.getDeclaredConstructor(*clzArgs)
                    //设置私有构造可以访问
                    constructor.isAccessible = true
                    instance = constructor.newInstance(*objArgs)
                } else {
                    instance = clz.newInstance()
                }
            } catch (t: Throwable) {
            }
        }
        return instance
    }

    /**
     * check can reflation
     *
     * @return can use reflection or no
     */
    private fun canReflection(): Boolean {
        var canReflection = true
        if (sForNameMethod == null || sGetDeclaredMethod == null || sGetFieldMethod == null) {
            canReflection = false
        }
        return canReflection
    }
}