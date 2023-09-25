package code.sdk.core.util;

import android.text.TextUtils;

import java.lang.reflect.Method;

public class ImitateChecker {
    public static boolean isImitate() {
        return mayOnEmulatorViaQEMU() || isEmulatorFromAbi();
    }

    private static boolean mayOnEmulatorViaQEMU() {
        String qemu = getProp("ro.kernel.qemu");
        return "1".equals(qemu);
    }

    private static boolean isEmulatorFromAbi() {
        String abi = getProp("ro.product.cpu.abi");
        if (abi == null) {
            return false;
        }
        return !TextUtils.isEmpty(abi) && abi.contains("x86");
    }


    private static String getProp(String property) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method method = systemProperties.getMethod("get", String.class);
            Object[] params = new Object[1];
            params[0] = property;
            return (String) method.invoke(systemProperties, params);
        } catch (Exception e) {
            return null;
        }
    }
}
