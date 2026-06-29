package com.wcjk.triage.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wcjk.triage.MainActivity;
import com.wcjk.triage.common.utils.Log;

/**
 * Created by hyc on 2018/8/16
 */

public class StartReceiver extends BroadcastReceiver {
    private Log log = Log.getLogger(this.getClass());

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        log.w("StartReceiver receive:" + action);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(i);
            log.w("StartReceiver start MainActivity");
        }
    }
}
