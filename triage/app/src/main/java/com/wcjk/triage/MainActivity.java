package com.wcjk.triage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.bumptech.glide.Glide;
import com.wcjk.triage.bean.ResponseUpgrade;
import com.wcjk.triage.common.AutoScrollTextView;
import com.wcjk.triage.common.Config;
import com.wcjk.triage.common.Screen.ScreenUtils;
import com.wcjk.triage.common.ttsiflytek.TtsHelper;
import com.wcjk.triage.common.utils.AppUtils;
import com.wcjk.triage.common.utils.DateUtils;
import com.wcjk.triage.common.utils.DeviceControlResult;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.common.utils.RemoteCommandUtils;
import com.wcjk.triage.common.utils.Utils;
import com.wcjk.triage.common.utils.ZcDeviceControl;
import com.wcjk.triage.event.DataEven;
import com.wcjk.triage.event.Event;
import com.wcjk.triage.fragment.FragmentDrugFirst;
import com.wcjk.triage.fragment.FragmentDrugSecond;
import com.wcjk.triage.fragment.FragmentDrugSecondLine1;
import com.wcjk.triage.fragment.FragmentFirst;
import com.wcjk.triage.fragment.FragmentFirst4Lines;
import com.wcjk.triage.fragment.FragmentFirst4LinesNew;
import com.wcjk.triage.fragment.FragmentFirstMedicalTechnology;
import com.wcjk.triage.fragment.FragmentMedicalTechnology;
import com.wcjk.triage.fragment.FragmentMulUltrasonic;
import com.wcjk.triage.fragment.FragmentSecond;
import com.wcjk.triage.fragment.FragmentSecondBc;
import com.wcjk.triage.fragment.FragmentSecondFuer;
import com.wcjk.triage.fragment.FragmentSecondSplit;
import com.wcjk.triage.fragment.FragmentTestFirst;
import com.wcjk.triage.global.Global;
import com.wcjk.triage.http.ApiManager;
import com.wcjk.triage.http.ServerApi;
import com.wcjk.triage.http.download.DownloadUtils;
import com.wcjk.triage.http.download.JsDownloadListener;
import com.wcjk.triage.socketio.SocketIo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.media.AudioManager.FLAG_SHOW_UI;

public class MainActivity extends BaseActivity {
    private static final String TEMPLATE_KIND_NATIVE = "native";
    private Log log = Log.getLogger(this.getClass());

    private ImageView iv_logo;
    private ImageView iv_state;
    private TextView tv_date;
    private TextView tv_weekday;
    private TextView tv_time;
    private AutoScrollTextView tv_run;
    private TextView tv_title;
    private Timer timer = new Timer();
    private int count = 0;

    private final int what_update_time = 0;
    private final int what_net_cloud_off = 1;
    private final int what_net_disconnect = 2;
    private final int what_net_ok = 3;
    private final int what_connect_server = 4;
    private final int what_upgrade = 5;
    private final int what_toasth = 6;

    private NotificationManager mNotifyManager;
    private Notification mNotification;
    private MyTimerTask myTimerTask;
//    private List<String> list_time_clear;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case what_update_time:
                    updateTime();
                    break;
                case what_net_cloud_off:
                    if (iv_state != null && iv_state.getVisibility() != View.VISIBLE) {
                        iv_state.setVisibility(View.VISIBLE);
                    }
                    if (iv_state != null) {
                        iv_state.setImageResource(R.drawable.ic_cloud_off_24dp);
                    }
                    break;
                case what_net_disconnect:
                    if (iv_state != null && iv_state.getVisibility() != View.VISIBLE) {
                        iv_state.setVisibility(View.VISIBLE);
                    }
                    if (iv_state != null) {
                        iv_state.setImageResource(R.drawable.ic_disconnect);
                    }
                    break;
                case what_net_ok:
                    if (iv_state != null && iv_state.getVisibility() == View.VISIBLE) {
                        iv_state.setVisibility(View.INVISIBLE);
                    }
                    break;
                case what_connect_server:
//                    JSONObject jsonObject = null;
//                    try {
//                        jsonObject = new JSONObject("{\"queues\":[{\"patients\":[{\"ticket\":\"B002\",\"brxmfull\":\"曾清霞\",\"status\":\"0\"},{\"ticket\":\"B002\",\"brxmfull\":\"曾清霞\",\"status\":\"0\"}],\"queuename\":\"超声2外\"}]}");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    DataEven dataEven = new DataEven(DataEven.TYPE_OTHER, jsonObject);
//                    EventBus.getDefault().postSticky(dataEven);
                    SocketIo.getInstance().connect();
                    break;
                case what_upgrade:
                    getServerApkVersion();
                    break;
                case what_toasth:
                    showToast((String) msg.obj);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log.w("onCreate");
        super.onCreate(savedInstanceState);
        changeRotate();
        requestPermissions();
        String style = Config.getConfig(Global.StyleKKey, Global.StyleK);
        if (BuildConfig.FLAVOR.equals("fzpfy3128") && (style.equals("secondarytriage") || style.equals("secondarytriageultrasonic") || style.equals("secondarypharmacytriage") || style.equals("secondarypharmacytriagetwo") || style.equals("secondarytriagesplit"))) {
            setContentView(R.layout.activity_main_fzpfy);
        } else {
            setContentView(R.layout.activity_main);
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        ZcDeviceControl.getInstance().setStatusBarVisible(this, false);
        initView();
        changeContent();
        startTimerAndConnectServer();
        mHandler.sendEmptyMessageDelayed(what_upgrade, 2000);
        setVolumn();
        applyTemplateBranding();

    }


    @Override
    protected void onResume() {
        log.w("onResume");
        super.onResume();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        log.w("onConfigurationChanged");
        super.onConfigurationChanged(newConfig); // 检测配置改动后执行相关操作
        String style = Config.getConfig(Global.StyleKKey, Global.StyleK);
        if (BuildConfig.FLAVOR.equals("fzpfy3128") && (style.equals("secondarytriage") || style.equals("secondarytriageultrasonic") || style.equals("secondarypharmacytriage") || style.equals("secondarypharmacytriagetwo") || style.equals("secondarytriagesplit"))) {
            setContentView(R.layout.activity_main_fzpfy);
        } else {
            setContentView(R.layout.activity_main);
        }
        initView();
        changeContent();
        applyTemplateBranding();
    }

    @Override
    protected void onStart() {
        log.w("onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        log.w("onStop");
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        log.w("onDestroy");
        super.onDestroy();

        SocketIo.getInstance().socketDisconn();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
//        if (tv_run != null){
//            tv_run.stopScroll();
//        }
        mHandler.removeMessages(what_update_time);
        mHandler.removeMessages(what_net_cloud_off);
        mHandler.removeMessages(what_net_disconnect);
        mHandler.removeMessages(what_net_ok);
        mHandler.removeMessages(what_connect_server);
        mHandler.removeMessages(what_upgrade);
        mHandler.removeMessages(what_toasth);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            String time = DateUtils.getCurrentTime();
            if (time.compareTo(Config.getConfig(Global.PowerOnKey, Global.PowerOn)) == 0) {
                EventBus.getDefault().post(new Event(Event.TYPE_SCREENON));
                log.w("is time to setScreenOn");
            } else if (time.compareTo(Config.getConfig(Global.PowerOffKey, Global.PowerOff)) == 0) {
                EventBus.getDefault().post(new Event(Event.TYPE_SCREENOFF));
                log.w("is time to setScreenOff");
            }

//            if (list_time_clear != null &&list_time_clear.size() > 0 ) {
//                String detailTime = DateUtils.getCurrentDate("yyyy-MM-dd HH:mm");
//                for (int i = 0;i<list_time_clear.size();i++){
//                    if (detailTime.equals(list_time_clear.get(i))){
//                        list_time_clear.remove(i);
//                        EventBus.getDefault().post(new ClearEvent());
//                        log.w("is time to clear screen:"+detailTime);
//
//                        JSONObject content_on = new JSONObject();
//                        try {
//                            content_on.put("cmd", "cleardata");
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                        SocketIo.getInstance().sendCmd(content_on);
//                        break;
//                    }
//                }
//            }

            mHandler.sendEmptyMessage(what_update_time);
            if (count % 20 == 4) {
                checkNet();
            }
            count++;
        }
    }

    private void changeRotate() {
        String rotate = Config.getConfig(Global.RotateKey, Global.Rotate);
        String[] rotate_array = getResources().getStringArray(R.array.rotate);
        log.w("changeRotate:" + rotate);
        if (rotate.equals(rotate_array[0])) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else if (rotate.equals(rotate_array[1])) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (rotate.equals(rotate_array[2])) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (rotate.equals(rotate_array[3])) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else if (rotate.equals(rotate_array[4])) {
//            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
//            }
        }
        DeviceControlResult result = ZcDeviceControl.getInstance().applyRotation(this, rotate);
        if (result.isSuccess()) {
            log.w(result.getMessage());
        } else {
            log.w(result.getMessage());
        }
    }

    private void requestPermissions() {
        RxPermissions rxPermission = new RxPermissions(MainActivity.this);
        rxPermission.requestEach(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {
                            // 用户已经同意该权限
                            log.i(permission.name + " :用户同意该权限");
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                            log.i(permission.name + " :用户拒绝该权限");
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』
                            log.i(permission.name + ":用户拒绝该权限,并且下次不再询问");
                        }
                    }
                });

    }

    private void changeContent() {
        if (!isTemplateNative()) {
            log.w("template is non-native, fallback to builtin style");
            showFragment(0);
            return;
        }

        String style = Config.getConfig(Global.StyleKKey, Global.StyleK);
        String[] style_array = getResources().getStringArray(R.array.style);
        for (int i = 0; i < style_array.length; i++) {
            if (style_array[i].equals(style)) {
                showFragment(i);
                return;
            }
        }

        if (!TextUtils.isEmpty(style) && !style.equals(Global.StyleK)) {
            log.w("unsupported style: " + style + ", fallback to default style");
            Config.setConfig(Global.StyleKKey, Global.StyleK);
        }
        showFragment(0);
    }

    @SuppressLint("WrongViewCast")
    private void initView() {
        iv_logo = (ImageView) findViewById(R.id.iv_logo);
        iv_state = (ImageView) findViewById(R.id.iv_state);
        tv_date = (TextView) findViewById(R.id.tv_date);
        tv_weekday = (TextView) findViewById(R.id.tv_weekday);
        tv_time = (TextView) findViewById(R.id.tv_time);
        iv_logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingDialog dialog = new SettingDialog();
                dialog.setSize(998, 667)
                        .setGravity(Gravity.CENTER)
                        .show(getFragmentManager());
            }
        });

        tv_run = (AutoScrollTextView) findViewById(R.id.tv_run);
        if (tv_run != null) {
            tv_run.setText(Config.getConfig(Global.HorselampKey, Global.Horselamp));
        }
//        tv_run.init(this);
//        tv_run.startScroll();
        EventBus.getDefault().post(new DataEven(Event.TYPE_HORSELAMP, null));
        tv_title = findViewById(R.id.tv_title);
        String title = Config.getConfig(Global.TitleKey, Global.Title);
        if (tv_title != null) {
            if (TextUtils.isEmpty(title)) {
                tv_title.setVisibility(View.GONE);
            } else {
                tv_title.setVisibility(View.VISIBLE);
                tv_title.setText(Config.getConfig(Global.TitleKey, Global.Title));
            }

        }

        applyTemplateBranding();
    }

    private void applyTemplateBranding() {
        if (iv_logo == null) {
            return;
        }

        String templateJson = Config.getConfig(Global.TemplateConfig, "");
        if (TextUtils.isEmpty(templateJson)) {
            iv_logo.setImageDrawable(null);
            return;
        }

        try {
            JSONObject template = new JSONObject(templateJson);
            String logo = template.optString("logo", "");
            if (TextUtils.isEmpty(logo)) {
                iv_logo.setImageDrawable(null);
                return;
            }

            String logoUrl = normalizeTemplateAssetUrl(logo);
            if (TextUtils.isEmpty(logoUrl)) {
                iv_logo.setImageDrawable(null);
                return;
            }

            Glide.with(this)
                    .load(logoUrl)
                    .into(iv_logo);
        } catch (JSONException e) {
            iv_logo.setImageDrawable(null);
            e.printStackTrace();
        }
    }

    private boolean isTemplateNative() {
        String templateJson = Config.getConfig(Global.TemplateConfig, "");
        if (TextUtils.isEmpty(templateJson)) {
            return true;
        }

        try {
            JSONObject template = new JSONObject(templateJson);
            return TEMPLATE_KIND_NATIVE.equalsIgnoreCase(template.optString("kind", TEMPLATE_KIND_NATIVE));
        } catch (JSONException e) {
            e.printStackTrace();
            return true;
        }
    }

    private String normalizeTemplateAssetUrl(String assetPathOrUrl) {
        if (TextUtils.isEmpty(assetPathOrUrl)) {
            return "";
        }

        if (assetPathOrUrl.startsWith("http://") || assetPathOrUrl.startsWith("https://")) {
            return assetPathOrUrl;
        }

        return AppUtils.getUrl(
                Config.getConfig(Global.ServerIpKey, Global.ServerIp),
                Config.getConfig(Global.ServerPortKey, Global.ServerPort),
                assetPathOrUrl
        );
    }

    Fragment fragment = null;

    private void showFragment(int type) {
        Fragment fragment = null;
        if (fragment == null) {
            switch (type) {
                case 0:
                    if (BuildConfig.FLAVOR.contains("qzfe3128")) {
                        fragment = new FragmentFirst4LinesNew();
                    } else {
                        fragment = new FragmentFirst();
                    }
                    break;
                case 1:
                    fragment = new FragmentFirst4Lines();
                    break;
                case 2:
                    if (BuildConfig.FLAVOR.contains("qzfe")) {
                        fragment = new FragmentSecondFuer();
                    } else {
                        fragment = new FragmentSecond();
                    }

                    break;
                case 3:
                    fragment = new FragmentSecondBc();
                    break;
                case 4:
                    fragment = new FragmentTestFirst();
                    break;
                case 5:
                    if (!BuildConfig.FLAVOR.contains("zznj")) {
                        fragment = new FragmentDrugFirst();
                    } else {
                        fragment = new com.wcjk.triage.fragment.zznj.FragmentDrugFirst();
                    }
                    break;
                case 6:
                    fragment = new FragmentDrugSecond();
                    break;
                case 7:
                    fragment = new FragmentSecondSplit();
                    break;
                case 8:
                    fragment = new FragmentDrugSecondLine1();
                    break;
                case 9:
                    fragment = new FragmentFirstMedicalTechnology();
                    break;
                case 10:
                    fragment = new FragmentMedicalTechnology();
                    break;
                case 11:
                    fragment = new com.wcjk.triage.fragment.fjykfsdeyy.FragmentFirst();
                    break;
                case 12:
                    fragment = new FragmentMulUltrasonic();
                    break;
            }
        }

        if (fragment != null) {
            getFragmentManager().beginTransaction().replace(R.id.content, fragment, fragment.getClass().getSimpleName()).commit();
            log.w("show style is " + fragment.getClass().getSimpleName());
        }
    }

    private void updateTime() {
        if (tv_date != null) {
            tv_date.setText(DateUtils.getCurrentDate("yyyy年MM月dd日"));
        }
        if (tv_weekday != null) {
            tv_weekday.setText(DateUtils.getCurrentWeek());
        }

        if (tv_time != null) {
            tv_time.setText(DateUtils.getCurrentTime());
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void update(Event event) {
        switch (event.getType()) {
            case Event.TYPE_RESTART:
                log.w("update restart,go to reboot");
                reboot();
                break;
            case Event.TYPE_SETTING_ROTATE:
                log.w("update rotate,go to reboot");
                break;
            case Event.TYPE_SETTING_STYLE:
                log.w("update style,go to reboot");
                applyTemplateBranding();
                changeContent();
                break;
            case Event.TYPE_SCREENON:
                log.w("update screen on");
                wakeActivityForScreenOn();
                boolean screenOnSuccess = ScreenUtils.instance().setScreenOn();
                if (isRemoteCommand(event, RemoteCommandUtils.CMD_ON)) {
                    showRemoteCommandResult(RemoteCommandUtils.CMD_ON, screenOnSuccess,
                            ScreenUtils.instance().getLastOperationMessage());
                }
                sendCommandAck(RemoteCommandUtils.CMD_ON);
                break;
            case Event.TYPE_SCREENOFF:
                log.w("update screen off");
                prepareActivityForScreenOff();
                boolean screenOffSuccess = ScreenUtils.instance().setScreenOff();
                if (isRemoteCommand(event, RemoteCommandUtils.CMD_OFF)) {
                    showRemoteCommandResult(RemoteCommandUtils.CMD_OFF, screenOffSuccess,
                            ScreenUtils.instance().getLastOperationMessage());
                }
                sendCommandAck(RemoteCommandUtils.CMD_OFF);
                break;
            case Event.TYPE_SETTING_VOLUMN:
                log.w("update volumn");
                setVolumn();
                break;
            case Event.TYPE_SETTING_SOURCE:
                log.w("update source");
                break;
            case Event.TYPE_SERVER_ERROR:
                checkNet();
                mHandler.removeMessages(what_connect_server);
                mHandler.sendEmptyMessageDelayed(what_connect_server, 20000);
                break;
            case Event.TYPE_HORSELAMP:
                if (tv_run != null) {
//                    tv_run.stopScroll();
                    tv_run.setText(Config.getConfig(Global.HorselampKey, Global.Horselamp));
//                    tv_run.init(this);
//                    tv_run.startScroll();
                }
                break;
            case Event.TYPE_UPGRADE:
                getServerApkVersion();
                break;
            case Event.TYPE_TITLE:
                if (tv_title != null) {
                    String title = Config.getConfig(Global.TitleKey, Global.Title);
                    if (TextUtils.isEmpty(title)) {
                        tv_title.setVisibility(View.GONE);
                    } else {
                        tv_title.setVisibility(View.VISIBLE);
                        tv_title.setText(Config.getConfig(Global.TitleKey, Global.Title));
                    }
                }
                break;
//            case  Event.TYPE_CLEAR:
//                String time_clear = event.getValue();
//                list_time_clear = null;
//                if (!TextUtils.isEmpty(time_clear)){
//                    String[] temp = time_clear.split(",");
//                    if (temp != null && temp.length >0){
//                        list_time_clear = new ArrayList<>();
//                        for (int i = 0;i<temp.length;i++){
//                            list_time_clear.add(temp[i]);
//                            log.i("添加清屏时间: " + temp[i]);
//                        }
//                    }
//                }
//                break;
        }
    }

//    @Subscribe(threadMode = ThreadMode.ASYNC)
//    public void update_tts(CallEvent event) {
////        if (event != null && !TextUtils.isEmpty(event.getData())) {
////            log.w(event.getData() == null ?"":event.getData());
////            TtsHelper.getInstance().ttsSpeak(event.getData());
////        }
//    }

    private void reboot() {
        log.w("reboot");
        showToast("正在调用系统重启(ZCAPI)");
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                log.w("call ZCAPI reboot");
                DeviceControlResult result = ZcDeviceControl.getInstance().reboot(MainActivity.this);
                log.w(result.getMessage());
                if (!result.isSuccess()) {
                    showToast(result.getMessage());
                }
                schedulePowerManagerRebootFallback();
            }
        }, 1500);
    }

    private void schedulePowerManagerRebootFallback() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                log.w("ZCAPI reboot did not stop app, try PowerManager reboot");
                showToast("ZCAPI未重启，尝试系统重启权限");
                try {
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (powerManager == null) {
                        throw new IllegalStateException("PowerManager 不可用");
                    }
                    powerManager.reboot(null);
                } catch (Throwable e) {
                    String msg = e.getMessage();
                    if (TextUtils.isEmpty(msg)) {
                        msg = e.getClass().getSimpleName();
                    }
                    log.e("PowerManager reboot failed:" + msg);
                    showToast("系统重启失败：" + msg);
                }
            }
        }, 3000);
    }

    private void sendCommandAck(String cmd) {
        JSONObject content = new JSONObject();
        try {
            content.put("cmd", cmd);
            SocketIo.getInstance().sendCmd(content);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean isRemoteCommand(Event event, String cmd) {
        return event != null && cmd.equals(event.getValue());
    }

    private void showRemoteCommandResult(String cmd, boolean success, String reason) {
        showToast(RemoteCommandUtils.getResultMessage(cmd, success, reason));
    }

    private void startTimerAndConnectServer() {
        if (timer != null) {
            if (myTimerTask != null) {
                myTimerTask.cancel();
            }
            myTimerTask = new MyTimerTask();
            timer.schedule(myTimerTask, 0, 1000);//0为延迟时间，1000为间隔时间,(单位：毫秒)
        }
        mHandler.removeMessages(what_connect_server);
        mHandler.sendEmptyMessageDelayed(what_connect_server, 2000);
    }

    private void wakeActivityForScreenOn() {
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard(this, null);
                }
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            log.e(e.getMessage());
        }
    }

    private void prepareActivityForScreenOff() {
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false);
                setTurnScreenOn(false);
            }
        } catch (Exception e) {
            log.e(e.getMessage());
        }
    }


    private void checkNet() {
        if (!SocketIo.getInstance().isState()) {
            if (Utils.isNetworkAvailable(this)) {
                mHandler.sendEmptyMessage(what_net_cloud_off);
            } else {
                mHandler.sendEmptyMessage(what_net_disconnect);
            }
        } else {
            mHandler.sendEmptyMessage(what_net_ok);
        }
    }

    private void showToast(String str) {
        if (TextUtils.isEmpty(str)) return;
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
        log.w("showToast :" + str);
    }

    private void getServerApkVersion() {
        log.w("获取远程版本");
        final String version = AppUtils.getVerName(MainActivity.this);
        ApiManager.getInstance().getUpgradeVersion(version)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseUpgrade>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResponseUpgrade responseUpgrade) {
                        if (responseUpgrade != null && !TextUtils.isEmpty(responseUpgrade.getName())) {
                            final String apkName = responseUpgrade.getName();
                            String url = "upgrade/download?name=" + apkName;
                            log.w("获取远程版本为：" + responseUpgrade.getAppversion());
                            downFile(url, responseUpgrade.getMd5());
                        } else {
                            Message message = new Message();
                            message.what = what_toasth;
                            message.obj = "当前版本已经是最高：" + version;
                            mHandler.sendMessage(message);
                            log.w("当前版本已经是最高：" + version);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.e("获取升级包版本信息失败：" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    public void downFile(String url, final String md5) {
        String path = AppUtils.getUrl(Config.getConfig(Global.ServerIpKey, Global.ServerIp),
                Config.getConfig(Global.ServerPortKey, Global.ServerPort), ServerApi.BASE_URL);
        log.w("开始下载升级包文件");
        log.w("基础路径为：" + path);
        final DownloadUtils downloadUtils = new DownloadUtils(path, new JsDownloadListener() {
            int count_k = 0;

            @Override
            public void onStartDownload(long length) {
                log.w("文件长度为：" + length);
            }

            @Override
            public void onProgress(int progress) {
                if (count_k + 1024 * 512 < progress) {
                    count_k = progress;
                    log.w("已下载：" + progress + "");
                    Message message = new Message();
                    message.what = what_toasth;
                    message.obj = "已下载：" + progress + "";
                    mHandler.sendMessage(message);
                }
            }

            @Override
            public void onFinishDownload() {

            }

            @Override
            public void onFail(String errorInfo) {

            }
        });

        File file = new File(getApkPath(), "triage.apk");
        log.w("请求地址为：" + url);
        log.w("升级包文件保存为：" + file.getPath());
        downloadUtils.download(url, file, new Observer() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Object o) {

            }

            @Override
            public void onError(Throwable e) {
                log.e("下载失败:" + e.getMessage());
                Message message = new Message();
                message.what = what_toasth;
                message.obj = "下载失败:" + e.getMessage();
                mHandler.sendMessage(message);
            }

            @Override
            public void onComplete() {
                File file_local = new File(getApkPath(), "triage.apk");
                String md5_local = AppUtils.getMD5(file_local);
                if (md5_local != null && md5 != null && md5_local.toLowerCase().equals(md5.toLowerCase())) {
                    log.w("本地MD5：" + md5_local);
                    log.w(" 远程MD5：" + md5);
                    log.w("下载升级包成功，准备安装");
                    Message message = new Message();
                    message.what = what_toasth;
                    message.obj = "下载升级包成功，准备安装";
                    mHandler.sendMessage(message);
                    installApp(getApkPath() + "/triage.apk");
//                    installApp(file_local);
                } else {
                    log.e("md5错误,无法升级");
                    log.e("本地MD5：" + md5_local);
                    log.e(" 远程MD5：" + md5);
//                    showToast("downFile：md5错误,无法升级");

                }
            }
        });
    }


    public String getApkPath() {
        String directoryPath = "";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {//判断外部存储是否可用
            directoryPath = getExternalFilesDir("apk").getAbsolutePath();
        } else {//没外部存储就使用内部存储
            directoryPath = getFilesDir() + File.separator + "apk";
        }
        File file = new File(directoryPath);
        log.w("升级包存储路径:" + directoryPath);
        if (!file.exists()) {//判断文件目录是否存在
            file.mkdirs();
        }
        return directoryPath;
    }


    private void installApp(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        startActivity(intent);
    }

    public String installApp(String apkAbsolutePath) {
        DeviceControlResult zcInstallResult = ZcDeviceControl.getInstance().installApk(this, apkAbsolutePath, true);
        if (zcInstallResult.isSuccess()) {
            String message = zcInstallResult.getMessage() + "，等待系统完成升级";
            log.w(message);
            showToast(message);
            return "zcapi_sent\n";
        }
        log.e(zcInstallResult.getMessage());
        String[] args = {"pm", "install", "-r", apkAbsolutePath};
        String result = "";
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        InputStream errIs = null;
        InputStream inIs = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = -1;
            process = processBuilder.start();
            errIs = process.getErrorStream();
            while ((read = errIs.read()) != -1) {
                baos.write(read);
            }
            baos.write("\n".getBytes());
            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }
            byte[] data = baos.toByteArray();
            result = new String(data);
            log.w("pm install result:" + result);
            if (result.equals("success\n")) {
                restartAfterUpgradeInstall();
            } else {
                showToast("安装升级包失败：" + result);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showToast("安装升级包异常：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showToast("安装升级包异常：" + e.getMessage());
        } finally {
            try {
                if (errIs != null) {
                    errIs.close();
                }
                if (inIs != null) {
                    inIs.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }

    private void restartAfterUpgradeInstall() {
        showToast("升级安装成功，正在重新打开应用");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void setVolumn() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        String volumn = Config.getConfig(Global.VolumeKey, Global.Volume);
        am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, Integer.valueOf(volumn) * max / 9, FLAG_SHOW_UI);
        if (Integer.valueOf(volumn) * max / 9 <= 0) {
            TtsHelper.getInstance().setVolumn(false);
        } else {
            TtsHelper.getInstance().setVolumn(true);
        }
        //        am.setStreamVolume(AudioManager.MODE_IN_COMMUNICATION, Integer.valueOf(volumn) * max / 9,0);

    }
}
