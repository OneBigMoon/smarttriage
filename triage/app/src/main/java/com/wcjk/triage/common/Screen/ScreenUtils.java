package com.wcjk.triage.common.Screen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.wcjk.triage.App;
import com.wcjk.triage.common.utils.DeviceControlResult;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.common.utils.ZcDeviceControl;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;

/**
 * Created by hyc on 2018/8/15
 */
public class ScreenUtils {
    private static ScreenUtils screenUtils;
    private DevicePolicyManager policyManager;
    private ComponentName adminReceiver;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private String lastOperationMessage = "";
    private Log log = Log.getLogger(this.getClass());

    public static ScreenUtils instance(){
        if(screenUtils == null){
            screenUtils = new ScreenUtils();
            screenUtils.init();
        }
        return screenUtils;
    }

    public void init(){
        screenUtils.adminReceiver = new ComponentName(App.getAppContext(), ScreenOffAdminReceiver.class);
        screenUtils.mPowerManager = (PowerManager) App.getAppContext().getSystemService(POWER_SERVICE);
        screenUtils.policyManager = (DevicePolicyManager) App.getAppContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            KeyguardManager km = (KeyguardManager) App.getAppContext().getSystemService(KEYGUARD_SERVICE);
            if (km != null) {
                KeyguardManager.KeyguardLock kl = km.newKeyguardLock("name");
                kl.disableKeyguard();
            }
        } catch (Exception e) {
            log.e("disable keyguard failed:" + e.getMessage());
        }
    }

    /**
     * 检测并激活设备管理器权限
     */
    public Intent getIntent(Activity activity) {
        adminReceiver = new ComponentName(activity, ScreenOffAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "开启后就可以使用锁屏功能了...");
        return intent;
    }

    public boolean isOpen() {
        return policyManager != null && adminReceiver != null && policyManager.isAdminActive(adminReceiver);
    }

    /**
     * 检测屏幕状态
     */
    public boolean getScreenState() {
        PowerManager pm = (PowerManager) App.getAppContext().getSystemService(Context.POWER_SERVICE);
        boolean screenOn = pm.isScreenOn();
        if (screenOn) {
            //亮屏
            return true;
        } else {
            //熄屏
            return false;
        }
    }

    /**
     * 亮屏
     */
    @SuppressLint("InvalidWakeLockTag")
    public boolean setScreenOn() {
//        if(!getScreenState()){
//
//        }
        //            EventBus.getDefault().post(new Event(Event.TYPE_RESTART));
        DeviceControlResult zcResult = ZcDeviceControl.getInstance().setBacklight(App.getAppContext(), true);
        logDeviceControlResult(zcResult);
        String wakeMessage = "";
        try {
            if (mPowerManager == null) {
                throw new IllegalStateException("PowerManager 不可用");
            }
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "triage:screen_on");
            mWakeLock.acquire(3000);
            wakeMessage = "WakeLock 已请求亮屏";
            log.w("setScreenOn");
        } catch (Exception e) {
            wakeMessage = "WakeLock 亮屏失败：" + e.getMessage();
            log.e(wakeMessage);
        }
        lastOperationMessage = joinMessages(zcResult.getMessage(), wakeMessage);
        log.w(lastOperationMessage);
        return zcResult.isSuccess();
    }

    /**
     *  熄屏
     */
    public boolean setScreenOff() {
        DeviceControlResult zcResult = ZcDeviceControl.getInstance().setBacklight(App.getAppContext(), false);
        boolean zcSuccess = zcResult.isSuccess();
        logDeviceControlResult(zcResult);
        DeviceControlResult adminResult = lockByDeviceAdmin();
        logDeviceControlResult(adminResult);
        lastOperationMessage = joinMessages(zcResult.getMessage(), adminResult.getMessage());
        log.w("setScreenOff");
        return zcSuccess || adminResult.isSuccess();
    }

    public String getLastOperationMessage() {
        return lastOperationMessage;
    }

    private void logDeviceControlResult(DeviceControlResult result) {
        if (result == null) {
            log.e("设备控制无返回结果");
            return;
        }
        if (result.isSuccess()) {
            log.w(result.getMessage());
        } else {
            log.e(result.getMessage());
        }
    }

    private String joinMessages(String first, String second) {
        if (first == null || first.length() == 0) {
            return second == null ? "" : second;
        }
        if (second == null || second.length() == 0) {
            return first;
        }
        return first + "；" + second;
    }

    private DeviceControlResult lockByDeviceAdmin() {
        try {
            if (isOpen()) {
                policyManager.lockNow();
                log.w("设备管理员已调用 lockNow");
                return DeviceControlResult.success("设备管理员已调用锁屏");
            }
            log.w("设备管理员未激活，跳过 lockNow");
            return DeviceControlResult.failure("设备管理员未激活");
        } catch (Exception e) {
            String message = "设备管理员锁屏失败：" + e.getMessage();
            log.e(message);
            return DeviceControlResult.failure(message);
        }
    }

}
