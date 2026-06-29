package com.wcjk.triage.socketio;

import android.text.TextUtils;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.wcjk.triage.App;
import com.wcjk.triage.BuildConfig;
import com.wcjk.triage.bean.ServerResponse;
import com.wcjk.triage.common.Config;
import com.wcjk.triage.common.utils.AppUtils;
import com.wcjk.triage.common.utils.DateUtils;
import com.wcjk.triage.common.utils.FileUtil;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.common.utils.RemoteCommandUtils;
import com.wcjk.triage.common.utils.SystemDateUtils;
import com.wcjk.triage.common.utils.Utils;
import com.wcjk.triage.common.utils.ZipUtils;
import com.wcjk.triage.event.ClearEvent;
import com.wcjk.triage.event.DataEven;
import com.wcjk.triage.event.Event;
import com.wcjk.triage.global.Global;
import com.wcjk.triage.http.ApiManager;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.wcjk.triage.global.Global.ClientNo;
import static com.wcjk.triage.global.Global.ClientNoKey;


public class SocketIo {
    private final String socketio1 = "/socketio?no=$no$&ip=$ip$&mac=$mac$&model=$model$&appversion=$appversion$";
    private final String socketio2 = "/socketio?ip=$ip$&mac=$mac$&model=$model$&appversion=$appversion$";

    private final String apiv1_message = "apiv1_message";
    private final String apiv1_heartbeat = "apiv1_heartbeat";
    private static final long HEARTBEAT_INTERVAL_MS = 9000L;
    private static final long RECONNECT_DELAY_MS = 15000L;

    public static Long timeDiff=0l;

    private String type_config = "CONFIG";
    private String type_command = "COMMAND";
    private String type_data = "DATA";
    private String type_error = "ERROR";

    private static SocketIo socket = null;

    public synchronized boolean isState() {
        return state;
    }

    public synchronized void setState(boolean state) {
        if (state != this.state) {
            this.state = state;
//            EventBus.getDefault().post(new CallEvent("测试结束"));
        }
    }

    public  boolean state = false;

    private Log log = Log.getLogger(this.getClass());
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private String currentServer = "";
    private long lastStateToastTime = 0L;
    private volatile boolean heartBeatStarted = false;

    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isState()) {
                connect();
            }
        }
    };

    private final Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!heartBeatStarted) {
                return;
            }
            if (mSocket != null) {
                try {
                    JSONObject object = new JSONObject();
                    object.put("type", "HEARTBEAT");
                    JSONObject content = new JSONObject();
                    String local = Utils.getLocalIpAddress(App.getAppContext());
                    content.put("ip", TextUtils.isEmpty(local) ? "" : local);
                    object.put("content", content);
                    mSocket.emit(apiv1_heartbeat, object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            mainHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };

    public static SocketIo getInstance(){
        if(socket == null){
            socket = new SocketIo();
        }
        return socket;
    }

    private Socket mSocket;
    public void connect(){
        try {
            log.i("connect");
            socketDisconn();
            String ip = AppUtils.getUrl(Config.getConfig(Global.ServerIpKey,Global.ServerIp),
                    Config.getConfig(Global.ServerPortKey,Global.ServerPort),"");
            currentServer = ip;

            String clientId = Config.getConfig(ClientNoKey, Global.ClientNo);
            String path = "" ;
            if (TextUtils.isEmpty(clientId)){
                path = socketio2;
            }else {
                path = socketio1;
                path =path.replace("$no$",clientId);
            }
            String localIp = Utils.getLocalIpAddress(App.getAppContext() );
            path = path.replace("$ip$",TextUtils.isEmpty(localIp) ? "" : localIp );
            String mac = Utils.getMacAddress(App.getAppContext());
            path = path.replace("$mac$",TextUtils.isEmpty(mac) ? "" : mac );
            path = path.replace("$model$", BuildConfig.FLAVOR);
            String version = AppUtils.getVerName(App.getAppContext());
            path = path.replace("$appversion$",TextUtils.isEmpty(version) ? "" : version );
            String url = AppUtils.getUrl(ip,path);
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket"};
            mSocket = IO.socket(url,options);
            socketConn();
            log.w("socketio connect to "+ url);
        } catch (URISyntaxException e) {
            log.e("socketio url invalid: " + e.getMessage());
            showStateToast("服务器连接参数异常：" + e.getMessage(), true);
        }
    }

    //连接到Server
    private void socketConn() {
        log.i("socketConn");
        if (mSocket != null) {
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
            mSocket.on(Socket.EVENT_CONNECT, onConnect);
            mSocket.on(apiv1_message, recv);
            mSocket.connect();
        }
    }

    public void socketDisconn(){

        stopHeartBeat();
        cancelReconnect();

        if (mSocket != null) {
            log.i("socketDisconn");
            mSocket.disconnect();
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
            mSocket.off(Socket.EVENT_CONNECT, onConnect);
            mSocket.off(apiv1_message, recv);
            mSocket = null;
            setState(false);
        }
    }

    private void startHeartBeat() {
        if (heartBeatStarted) {
            return;
        }

        heartBeatStarted = true;
        mainHandler.post(heartBeatRunnable);
    }

    private void stopHeartBeat() {
        if (!heartBeatStarted) {
            return;
        }

        heartBeatStarted = false;
        mainHandler.removeCallbacks(heartBeatRunnable);
    }

    private void scheduleReconnect() {
        cancelReconnect();
        mainHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS);
    }

    private void cancelReconnect() {
        mainHandler.removeCallbacks(reconnectRunnable);
    }

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            log.i("onConnectError");
            showStateToast("服务器连接失败 " + currentServer + getConnectError(args), true);
            socketDisconn();
            scheduleReconnect();
        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            log.i("onConnect");
            setState(true);
            showStateToast("服务器连接成功 " + currentServer, false);
            cancelReconnect();
            startHeartBeat();
        }
    };

    private String getConnectError(Object... args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }
        String msg = String.valueOf(args[0]);
        if (TextUtils.isEmpty(msg)) {
            return "";
        }
        return "：" + msg;
    }

    private void showStateToast(final String text, boolean throttle) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (throttle && now - lastStateToastTime < 10000) {
            return;
        }
        lastStateToastTime = now;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(App.getAppContext(), text, Toast.LENGTH_LONG).show();
                log.w("showStateToast :" + text);
            }
        });
    }

    private void showCommandToast(final String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(App.getAppContext(), text, Toast.LENGTH_LONG).show();
                log.w("showCommandToast :" + text);
            }
        });
    }


    /**
     * "content":  {
         "style":"secondarytriage",
         "datasource":"5b6ec0c6faaad51c8993d5fc",
         "volume":6, //0-9
         "powerontime":"08:00:00",
         "powerofftime":"18:00:00",
         "rotation":"auto", // 自动:"auto",横屏:"0",横屏反向:"180",竖屏:"270",竖屏反向:"90"
     * }
     */
    public void sendConfig(JSONObject content){
        try {
            if (content == null || mSocket == null ) return;
            JSONObject object = new JSONObject();
            object.put("type",type_config);
            object.put("content", content);
            log.i("sendConfig:" + object.toString());
                mSocket.emit(apiv1_message,object);
            }catch (JSONException e){
                e.printStackTrace();
            }
        }

    public void sendCmd(JSONObject content){
        try {
            if (content == null || mSocket == null ) return;
            JSONObject object = new JSONObject();
            object.put("type",type_command);
            object.put("content", content);
            log.i("sendCmd:" + object.toString());
            mSocket.emit(apiv1_message,object);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    private Emitter.Listener recv = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            log.i("recv");
            if (args == null || args.length == 0 || args[0] == null) {
                return;
            }

            Object raw = args[0];
            JSONObject data;
            if (raw instanceof JSONObject) {
                data = (JSONObject) raw;
            } else if (raw instanceof String) {
                try {
                    data = new JSONObject((String) raw);
                } catch (JSONException e) {
                    log.e(e.getMessage());
                    return;
                }
            } else {
                return;
            }

            String type = data.optString("type", "");
            Object contentObj = data.has("content") ? data.opt("content") : null;
            handleMessageFrame(type, contentObj);
        }

    };

    private void handleMessageFrame(String type, Object contentObj) {
        if (TextUtils.isEmpty(type)) {
            return;
        }

        if (apiv1_message.equals(type)) {
            Object inner = contentObj;
            if (inner == null) {
                return;
            }

            if (inner instanceof String) {
                try {
                    inner = new JSONObject((String) inner);
                } catch (JSONException e) {
                    return;
                }
            }

            if (!(inner instanceof JSONObject)) {
                return;
            }

            JSONObject innerObj = (JSONObject) inner;
            String innerType = innerObj.optString("type", "");
            Object innerContent = innerObj.has("content") ? innerObj.opt("content") : null;
            handleMessageFrame(innerType, innerContent);
            return;
        }

        JSONObject content = contentObj == null ? null : toJsonObject(contentObj);

        if (type.equals(type_config)) {
            dispatchConfigUpdate(content);
            return;
        }

        if (type.equals(type_command)) {
            if (content != null) {
                dispatchCommand(content);
            }
            return;
        }

        if (type.equals(type_data)) {
            if (content == null) {
                return;
            }
            EventBus.getDefault().postSticky(new DataEven(DataEven.TYPE_OTHER, content));
            try {
                log.i(content.toString());
            }catch (Exception e){
            }
            return;
        }

        if (type.equals(type_error) || "apiv1_error".equals(type)) {
            if (content == null) {
                return;
            }
            String code = content.optString("code", "");
            if (!TextUtils.isEmpty(code) && (code.equals("11002") ||  code.equals("11001"))){
                socketDisconn();
                EventBus.getDefault().post(new Event(Event.TYPE_SERVER_ERROR));
            }
        }
    }

    private JSONObject toJsonObject(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }

        if (value instanceof String) {
            try {
                return new JSONObject((String) value);
            } catch (JSONException e) {
                return null;
            }
        }

        return null;
    }

    private void dispatchConfigUpdate(JSONObject content) {
        if (content == null) {
            return;
        }

        log.i("type:" + type_config);
        log.i("content:" + content);
        setConfig(content, ClientNoKey,Global.ClientNo);
        setConfig(content,Global.NameKey,Global.Name);
        setConfig(content,Global.SourceIdKey,Global.SourceId);
        setConfig(content,Global.PowerOnKey,Global.PowerOn);
        setConfig(content,Global.PowerOffKey,Global.PowerOff);
        if (setConfig(content,Global.VolumeKey,Global.Volume)){
            EventBus.getDefault().post(new Event(Event.TYPE_SETTING_VOLUMN));
        }

        if (setConfig(content,Global.ParamsKey,Global.Params)){
            EventBus.getDefault().post(new DataEven(DataEven.TYPE_PARAMS,null));
        }

        if (setConfig(content,Global.HorselampKey,Global.Horselamp)){
            EventBus.getDefault().post(new Event(Event.TYPE_HORSELAMP));
        }

        try {
            long time = content.getLong(Global.TimeslampKey);
            if (time > 0) {
                SystemDateUtils.setSysDateAndTime(App.getAppContext(),time);
            }
        }catch (Exception e){
            log.e(e.getMessage());
        }

        if (setConfig(content,Global.TitleKey,Global.Title)){
            EventBus.getDefault().post(new Event(Event.TYPE_TITLE));
        }

        applyTemplateConfig(content);

        if (setConfig(content,Global.RotateKey,Global.Rotate) || setConfig(content,Global.StyleKKey,Global.StyleK)){
            EventBus.getDefault().post(new Event(Event.TYPE_RESTART));
        }
    }

    private void dispatchCommand(JSONObject content) {
        String cmd = content.optString("cmd", "");
        if (TextUtils.isEmpty(cmd)) {
            return;
        }

        showCommandToast(RemoteCommandUtils.getReceiveMessage(cmd));
        if (cmd.equals(RemoteCommandUtils.CMD_RESTART)) {
            EventBus.getDefault().post(new Event(Event.TYPE_RESTART));
        }else if (cmd.equals(RemoteCommandUtils.CMD_ON)) {
            EventBus.getDefault().post(new Event(Event.TYPE_SCREENON, cmd));
        }else if (cmd.equals(RemoteCommandUtils.CMD_OFF)) {
            EventBus.getDefault().post(new Event(Event.TYPE_SCREENOFF, cmd));
        }else if (cmd.equals(RemoteCommandUtils.CMD_UPGRADE)) {
            EventBus.getDefault().post(new Event(Event.TYPE_UPGRADE, cmd));
        }else if (cmd.equals(RemoteCommandUtils.CMD_UPLOAD_LOG)) {
            uploadLog();
        }else if (cmd.equals(RemoteCommandUtils.CMD_CLEAR_DATA)) {
            EventBus.getDefault().post(new ClearEvent());
        }
    }

    private boolean setConfig(JSONObject content,String key,String value_default){
        try {
            if (content == null || !content.has(key)) {
                return false;
            }

            Object raw = content.get(key);
            if (raw == null || JSONObject.NULL.equals(raw)) {
                return false;
            }

            String value;
            if (raw instanceof String) {
                value = (String) raw;
            } else {
                value = String.valueOf(raw);
            }

            String value_old = Config.getConfig(key, value_default);
            if (!value.equals(value_old)){
                Config.setConfig(key,value);
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void applyTemplateConfig(JSONObject content) {
        if (content == null) {
            return;
        }

        if (!content.has(Global.TemplateKey) || content.isNull(Global.TemplateKey)) {
            String existingTemplate = Config.getConfig(Global.TemplateConfig, "");
            if (!TextUtils.isEmpty(existingTemplate)) {
                clearTemplateConfig();
                EventBus.getDefault().post(new Event(Event.TYPE_SETTING_STYLE));
            }
            return;
        }

        Object templateObj = content.opt(Global.TemplateKey);
        if (templateObj == null || templateObj == JSONObject.NULL) {
            String existingTemplate = Config.getConfig(Global.TemplateConfig, "");
            if (!TextUtils.isEmpty(existingTemplate)) {
                clearTemplateConfig();
                EventBus.getDefault().post(new Event(Event.TYPE_SETTING_STYLE));
            }
            return;
        }

        JSONObject template;
        try {
            if (templateObj instanceof JSONObject) {
                template = (JSONObject) templateObj;
            } else {
                template = new JSONObject(String.valueOf(templateObj));
            }
        } catch (Exception e) {
            String existingTemplate = Config.getConfig(Global.TemplateConfig, "");
            if (!TextUtils.isEmpty(existingTemplate)) {
                clearTemplateConfig();
                EventBus.getDefault().post(new Event(Event.TYPE_SETTING_STYLE));
            }
            return;
        }

        String templateJson = template.toString();
        String existingTemplate = Config.getConfig(Global.TemplateConfig, "");
        if (!templateJson.equals(existingTemplate)) {
            Config.setConfig(Global.TemplateConfig, templateJson);

            Config.setConfig(Global.TemplateKeyStyle, template.optString("key", ""));
            Config.setConfig(Global.TemplateKindKey, template.optString("kind", "native"));
            Config.setConfig(Global.TemplateVersionKey, template.optString("version", ""));
            Config.setConfig(Global.TemplateLogoKey, template.optString("logo", ""));
            Config.setConfig(Global.TemplatePackageKey, template.optString("package", ""));

            if (template.has("manifest")) {
                Object manifest = template.opt("manifest");
                Config.setConfig(Global.TemplateManifestKey, manifest == null || manifest == JSONObject.NULL ? "" : manifest.toString());
            } else {
                Config.setConfig(Global.TemplateManifestKey, "");
            }

            EventBus.getDefault().post(new Event(Event.TYPE_SETTING_STYLE));
        }
    }

    private void clearTemplateConfig() {
        Config.setConfig(Global.TemplateConfig, "");
        Config.setConfig(Global.TemplateKeyStyle, "");
        Config.setConfig(Global.TemplateKindKey, "");
        Config.setConfig(Global.TemplateVersionKey, "");
        Config.setConfig(Global.TemplateLogoKey, "");
        Config.setConfig(Global.TemplatePackageKey, "");
        Config.setConfig(Global.TemplateManifestKey, "");
    }

    private boolean setConfig(JSONObject content,String name,String key,int value_default){
        try {
            int value = content.getInt(name);
            if (value != -1) {
                int value_old = Config.getConfig(key,value_default);
                if (value != value_old){
                    Config.setConfig(key,value);
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    private  void uploadLog(){
        String zipPath = Utils.getAppPath();
            FileUtil.deletFile(zipPath,false,".zip");
        List<String> list_log = new ArrayList<>();
//      String logFilePath = Utils.getLogPath( "triage.log");
        String logFilePath = Utils.getLogPath( );
        String log_name = "log-" + Config.getConfig(ClientNoKey,ClientNo) + "-"
                + DateUtils.getCurrentDate("yyyyMMddHHmmss") + ".zip";
        zipPath = Utils.getAppPath() + File.separator + log_name;
        try {
            ZipUtils.ZipFolder(logFilePath,zipPath);
            final File file = new File(zipPath);
            if (file == null || file.exists() == false){
                log.e( "上传日志失败：压缩失败");
                return;
            }
        } catch (Exception e) {
            log.e("上传日志失败:" + e.getMessage());
            return;
        }
        list_log.add(zipPath);
        ApiManager.getInstance().uploadLogs(Config.getConfig(ClientNoKey,ClientNo),list_log)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ServerResponse>() {
                               @Override
                               public void onSubscribe(Disposable d) {

                               }

                               @Override
                               public void onNext(ServerResponse responseBody) {
                                   if (responseBody != null ){
                                       try {
//                                                                String result = responseBody.string();
//                                                                log.w("上传日志：" + result);
//                                                                ServerResponse ret = JSON.parseObject(result, ServerResponse.class);
                                           if (responseBody != null && responseBody.getErrcode() == 0){
                                               log.w("上传日志成功");
                                               Toast.makeText(App.getAppContext(), "上传日志成功", Toast.LENGTH_SHORT).show();
                                           }else{
                                               log.w("上传日志失败");
                                               Toast.makeText(App.getAppContext(), "上传日志失败", Toast.LENGTH_SHORT).show();

                                           }
                                       } catch (Exception e) {
                                           e.printStackTrace();
                                       }

                                   }
                               }

                               @Override
                               public void onError(Throwable e) {
                                   log.w("上传日志失败"+e.getMessage());
                                   Toast.makeText(App.getAppContext(),"上传日志失败"+e.getMessage(), Toast.LENGTH_SHORT).show();
                               }

                               @Override
                               public void onComplete() {

                               }
                           }
                );
    }
}
