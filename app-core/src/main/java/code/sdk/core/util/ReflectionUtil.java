package code.sdk.core.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * reflection util
 */
public class ReflectionUtil {
    public static final String TAG = ReflectionUtil.class.getSimpleName();

    private static Method sForNameMethod;
    private static Method sGetDeclaredMethod;
    private static Method sGetFieldMethod;

    /**
     * init reflection and cache it
     */
    static {
        try {
            sForNameMethod = Class.class.getDeclaredMethod("forName", String.class);
            sGetDeclaredMethod = Class.class
                    .getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
            sGetFieldMethod = Class.class.getDeclaredMethod("getDeclaredField", String.class);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * get filed
     *
     * @param clzName   the class name
     * @param filedName the filed name
     * @return the class filed
     */
    @Nullable
    public static Field getFiled(@NonNull String clzName, @NonNull String filedName) {
        Field field = null;
        if (canReflection()) {
            try {
                Class<?> clz = (Class<?>) sForNameMethod.invoke(null, clzName);
                field = (Field) sGetFieldMethod.invoke(clz, filedName);
                field.setAccessible(true);
            } catch (Throwable t) {
            }
        }

        return field;
    }

    /**
     * set filed to the instance
     *
     * @param src       the instance
     * @param clzName   the class name
     * @param filedName the filed name
     * @param tarObj    target object
     */
    public static void setFiled(@NonNull Object src, @NonNull String clzName,
                                @NonNull String filedName, Object tarObj) {
        try {
            Field field = getFiled(clzName, filedName);
            if (field != null) {
                field.set(src, tarObj);
            }
        } catch (Throwable t) {
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
    @Nullable
    public static Method getMethod(@NonNull String clzName, @NonNull String methodName,
                                   Class[] clzArgs) {
        Method method = null;
        if (canReflection()) {
            try {
                Class<?> clz = (Class<?>) sForNameMethod.invoke(null, clzName);
                method = (Method) sGetDeclaredMethod.invoke(clz, methodName, clzArgs);
                method.setAccessible(true);
            } catch (Throwable t) {
            }
        }
        return method;
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
    public static Object invokeMethod(@NonNull Object src, @NonNull String clzName,
                                      @NonNull String methodName, Class[] clzArgs, Object... objArgs) {
        Object result = null;
        try {
            Method method = getMethod(clzName, methodName, clzArgs);
            if (method != null) {
                result = method.invoke(src, objArgs);
            }
        } catch (Throwable t) {
        }
        return result;
    }

    public static Object newInstance(String clzName, Class[] clzArgs, Object... objArgs) {
        Object instance = null;
        if (canReflection()) {
            try {
                Class<?> clz = Class.forName(clzName);
                if (clzArgs != null) {
                    Constructor constructor = clz.getDeclaredConstructor(clzArgs);
                    //设置私有构造可以访问
                    constructor.setAccessible(true);
                    instance = constructor.newInstance(objArgs);
                } else {
                    instance = clz.newInstance();
                }
            } catch (Throwable t) {
            }
        }
        return instance;
    }

    /**
     * check can reflation
     *
     * @return can use reflection or no
     */
    private static boolean canReflection() {
        boolean canReflection = true;
        if (sForNameMethod == null || sGetDeclaredMethod == null || sGetFieldMethod == null) {
            canReflection = false;
        }
        return canReflection;
    }

    public static Object invokeMethod(String className, String methodName, Object obj) {

        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName);
            return method.invoke(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}