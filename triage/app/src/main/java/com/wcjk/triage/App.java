package com.wcjk.triage;

import android.app.Application;
import android.content.Context;

import com.wcjk.triage.common.Config;
import com.wcjk.triage.common.ttsiflytek.TtsHelper;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.common.utils.ZcDeviceControl;

/**
 * Created by cpp on 2016/8/9.
 */
public class App extends Application {
    private static Context context;
    private Log log = Log.getLogger(this.getClass());
    @Override
    public void onCreate() {
        super.onCreate();
//        DpsLog.Configure(BuildConfig.LOG_FILE_NAME);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
        context = getApplicationContext();
        Config.setContext(App.getAppContext());
        ZcDeviceControl.getInstance().init(App.getAppContext());
        try {
            Log.Configure("triage");
            log = Log.getLogger(this.getClass());
            log.i("App onCreate");
        }catch (Exception e){
            log.e(e.toString());
        }


        try {
            TtsHelper.getInstance().init(App.getAppContext());
        } catch (Exception e) {
            log.e("TTS init failed:" + e.toString());
            log.e(e);
        }

//        SocketIo.getInstance().connect();
    }


    @Override
    public void onTerminate() {
        log.d("onTerminate");
        super.onTerminate();
    }

    public static Context getAppContext() {
        return context;
    }

    final class CrashHandler implements Thread.UncaughtExceptionHandler {
        private Log log = Log.getLogger(CrashHandler.class);

        @Override
        public void uncaughtException(Thread arg0, Throwable arg1) {
            log.e(arg1);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }
}
