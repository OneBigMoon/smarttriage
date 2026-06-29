package com.wcjk.triage.common.utils;

import android.content.Context;

import com.zcapi;

import java.util.Calendar;
import java.util.Locale;

public class ZcDeviceControl {
    private static final String DEFAULT_MAC = "02:00:00:00:00:00";
    private static final ZcDeviceControl INSTANCE = new ZcDeviceControl();

    private zcapi zcApi;
    private Log log = Log.getLogger(this.getClass());

    private ZcDeviceControl() {
    }

    public static ZcDeviceControl getInstance() {
        return INSTANCE;
    }

    public synchronized DeviceControlResult init(Context context) {
        if (context == null) {
            return DeviceControlResult.failure("Context 为空");
        }
        try {
            if (zcApi == null) {
                zcApi = new zcapi();
                zcApi.getContext(context.getApplicationContext());
                log.w("ZCAPI init success");
            }
            return DeviceControlResult.success("ZCAPI 已初始化");
        } catch (Throwable e) {
            zcApi = null;
            String msg = getThrowableMessage(e);
            log.e("ZCAPI init failed:" + msg);
            return DeviceControlResult.failure("ZCAPI 初始化失败：" + msg);
        }
    }

    public DeviceControlResult reboot(Context context) {
        try {
            getApi(context).reboot();
            return DeviceControlResult.success("ZCAPI 已调用整机重启");
        } catch (Throwable e) {
            return failure("ZCAPI 重启失败", e);
        }
    }

    public DeviceControlResult shutdown(Context context) {
        try {
            getApi(context).shutDown();
            return DeviceControlResult.success("ZCAPI 已调用整机关机");
        } catch (Throwable e) {
            return failure("ZCAPI 关机失败", e);
        }
    }

    public DeviceControlResult setBacklight(Context context, boolean on) {
        try {
            zcapi api = getApi(context);
            api.setLcdOnOff(on);
            api.setLcdOnOff(on, -1);
            return DeviceControlResult.success(on ? "ZCAPI 已调用 LCD/HDMI 亮屏" : "ZCAPI 已调用 LCD/HDMI 息屏");
        } catch (Throwable e) {
            return failure(on ? "ZCAPI 亮屏失败" : "ZCAPI 息屏失败", e);
        }
    }

    public DeviceControlResult setSystemTime(Context context, long timeMillis) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeMillis);
            getApi(context).setSystemTime(toZcDateTime(calendar, true));
            return DeviceControlResult.success("ZCAPI 已设置系统时间");
        } catch (Throwable e) {
            return failure("ZCAPI 设置系统时间失败", e);
        }
    }

    public DeviceControlResult installApk(Context context, String apkPath, boolean silent) {
        if (apkPath == null || apkPath.trim().length() == 0) {
            return DeviceControlResult.failure("APK 路径为空");
        }
        try {
            getApi(context).InstallApk(apkPath, silent);
            return DeviceControlResult.success("ZCAPI 已调用安装 APK");
        } catch (Throwable e) {
            return failure("ZCAPI 安装 APK 失败", e);
        }
    }

    public DeviceControlResult applyRotation(Context context, String rotate) {
        int rotation = toZcRotation(rotate);
        if (rotation < 0) {
            return DeviceControlResult.failure("自动旋转无需写入 ZCAPI");
        }
        try {
            getApi(context).setScreenRotation(rotation);
            return DeviceControlResult.success("ZCAPI 已设置屏幕旋转：" + rotation);
        } catch (Throwable e) {
            return failure("ZCAPI 设置屏幕旋转失败", e);
        }
    }

    public DeviceControlResult setStatusBarVisible(Context context, boolean visible) {
        try {
            zcapi api = getApi(context);
            api.setStatusBar(visible);
            api.setGestureStatusBar(visible);
            return DeviceControlResult.success(visible ? "ZCAPI 已显示系统栏" : "ZCAPI 已隐藏系统栏");
        } catch (Throwable e) {
            return failure(visible ? "ZCAPI 显示系统栏失败" : "ZCAPI 隐藏系统栏失败", e);
        }
    }

    public DeviceControlResult setPowerOnOffTime(Context context, String powerOn, String powerOff) {
        PowerSchedule schedule = buildNextPowerSchedule(System.currentTimeMillis(), powerOn, powerOff);
        if (!schedule.enabled) {
            return DeviceControlResult.failure(schedule.reason);
        }
        try {
            getApi(context).setPowetOnOffTime(true, schedule.powerOnTime, schedule.powerOffTime);
            return DeviceControlResult.success("ZCAPI 已设置下次开关机时间");
        } catch (Throwable e) {
            return failure("ZCAPI 设置开关机时间失败", e);
        }
    }

    public String getDeviceMac(Context context) {
        try {
            zcapi api = getApi(context);
            String mac = firstUsableMac(
                    callEthMac(api, "eth0"),
                    callEthMac(api, "eth1"),
                    callDefaultEthMac(api),
                    callWifiMac(api));
            if (isUsableMacAddress(mac)) {
                return normalizeMacAddress(mac);
            }
        } catch (Throwable e) {
            log.e("ZCAPI get mac failed:" + getThrowableMessage(e));
        }
        return "";
    }

    public String getDeviceInfo(Context context) {
        try {
            zcapi api = getApi(context);
            return "ZCAPI " + safeString(api.getZcapiVersion()) + " / " + safeString(api.getZckjID());
        } catch (Throwable e) {
            return "";
        }
    }

    public static String normalizeMacAddress(String mac) {
        if (mac == null) {
            return "";
        }
        return mac.trim().toUpperCase(Locale.ENGLISH);
    }

    public static boolean isUsableMacAddress(String mac) {
        String normalized = normalizeMacAddress(mac);
        return normalized.length() > 0 && !DEFAULT_MAC.equals(normalized);
    }

    public static int toZcRotation(String rotate) {
        if (rotate == null) {
            return -1;
        }
        try {
            int value = Integer.parseInt(rotate.trim());
            if (value == 0 || value == 90 || value == 180 || value == 270) {
                return value;
            }
        } catch (NumberFormatException ignore) {
        }
        return -1;
    }

    static PowerSchedule buildNextPowerSchedule(long nowMillis, String powerOn, String powerOff) {
        int[] onParts = parseClock(powerOn);
        int[] offParts = parseClock(powerOff);
        if (onParts == null || offParts == null) {
            return PowerSchedule.disabled("开关机时间格式错误");
        }
        if ("00:00:00".equals(powerOn) && "23:59:59".equals(powerOff)) {
            return PowerSchedule.disabled("未配置有效开关机时间");
        }

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);

        Calendar offTime = nextClock(now, offParts);
        Calendar onTime = (Calendar) offTime.clone();
        onTime.set(Calendar.HOUR_OF_DAY, onParts[0]);
        onTime.set(Calendar.MINUTE, onParts[1]);
        onTime.set(Calendar.SECOND, onParts[2]);
        onTime.set(Calendar.MILLISECOND, 0);
        if (!onTime.after(offTime)) {
            onTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        return PowerSchedule.enabled(toZcDateTime(onTime, false), toZcDateTime(offTime, false));
    }

    private synchronized zcapi getApi(Context context) {
        DeviceControlResult result = init(context);
        if (!result.isSuccess() || zcApi == null) {
            throw new IllegalStateException(result.getMessage());
        }
        return zcApi;
    }

    private DeviceControlResult failure(String prefix, Throwable e) {
        String msg = prefix + "：" + getThrowableMessage(e);
        log.e(msg);
        return DeviceControlResult.failure(msg);
    }

    private static int[] parseClock(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.trim().split(":");
        if (parts.length != 3) {
            return null;
        }
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int second = Integer.parseInt(parts[2]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                return null;
            }
            return new int[]{hour, minute, second};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Calendar nextClock(Calendar now, int[] clock) {
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, clock[0]);
        target.set(Calendar.MINUTE, clock[1]);
        target.set(Calendar.SECOND, clock[2]);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }
        return target;
    }

    private static int[] toZcDateTime(Calendar calendar, boolean includeSecond) {
        if (includeSecond) {
            return new int[]{
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND)
            };
        }
        return new int[]{
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
        };
    }

    private static String firstUsableMac(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (isUsableMacAddress(value)) {
                return value;
            }
        }
        return "";
    }

    private static String callEthMac(zcapi api, String name) {
        try {
            return api.getEthMacAddress(name);
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static String callDefaultEthMac(zcapi api) {
        try {
            return api.getEthMacAddress();
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static String callWifiMac(zcapi api) {
        try {
            return api.getWifiMacAddress();
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String getThrowableMessage(Throwable e) {
        if (e == null) {
            return "未知错误";
        }
        String msg = e.getMessage();
        return msg == null || msg.length() == 0 ? e.getClass().getSimpleName() : msg;
    }

    static class PowerSchedule {
        final boolean enabled;
        final int[] powerOnTime;
        final int[] powerOffTime;
        final String reason;

        private PowerSchedule(boolean enabled, int[] powerOnTime, int[] powerOffTime, String reason) {
            this.enabled = enabled;
            this.powerOnTime = powerOnTime;
            this.powerOffTime = powerOffTime;
            this.reason = reason;
        }

        static PowerSchedule enabled(int[] powerOnTime, int[] powerOffTime) {
            return new PowerSchedule(true, powerOnTime, powerOffTime, "");
        }

        static PowerSchedule disabled(String reason) {
            return new PowerSchedule(false, null, null, reason);
        }
    }
}
