package com.wcjk.triage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.wcjk.triage.common.utils.Log;

/**
 * Created by hyc on 2019/3/30
 */

public class UpdateRestartReceiver extends BroadcastReceiver {
    private Log log = Log.getLogger(this.getClass());

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        Uri data = intent.getData();
        log.w("UpdateRestartReceiver receive:" + action + ", data:" + (data == null ? "" : data.toString()));
        if (Intent.ACTION_PACKAGE_REPLACED.equals(action)
                && (data == null || !context.getPackageName().equals(data.getSchemeSpecificPart()))) {
            return;
        }
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            Toast.makeText(context, "已升级到新版本", Toast.LENGTH_SHORT).show();
            Intent intent2 = new Intent(context, MainActivity.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent2);
            log.w("UpdateRestartReceiver start MainActivity");
        }
    }

}
