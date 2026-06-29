package com.wcjk.triage;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.wcjk.triage.bean.QueryDataSources;
import com.wcjk.triage.bean.QueryStyles;
import com.wcjk.triage.common.BaseDialog;
import com.wcjk.triage.common.CommonDialog;
import com.wcjk.triage.common.Config;
import com.wcjk.triage.common.PickerView;
import com.wcjk.triage.common.popwindow.CommonPopupWindow;
import com.wcjk.triage.common.recyclerview.QuickAdapter;
import com.wcjk.triage.common.usb.USBBroadCastReceiver;
import com.wcjk.triage.common.usb.UsbHelper;
import com.wcjk.triage.common.utils.AppUtils;
import com.wcjk.triage.common.utils.DateUtils;
import com.wcjk.triage.common.utils.DeviceControlResult;
import com.wcjk.triage.common.utils.FileUtil;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.common.utils.Utils;
import com.wcjk.triage.common.utils.ZcDeviceControl;
import com.wcjk.triage.common.utils.ZipUtils;
import com.wcjk.triage.event.Event;
import com.wcjk.triage.global.Global;
import com.wcjk.triage.http.ApiManager;
import com.wcjk.triage.http.NetCallBack;
import com.wcjk.triage.socketio.SocketIo;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

import static com.wcjk.triage.global.Global.ClientNo;
import static com.wcjk.triage.global.Global.ClientNoKey;
import static com.wcjk.triage.global.Global.Test;
import static com.wcjk.triage.global.Global.TestKey;

/**
 * Created by hyc on 2018/8/6
 */
public class SettingDialog extends BaseDialog {
    private Log log = Log.getLogger(this.getClass());
    private List<QueryStyles.ResultBean> list_style = new ArrayList<>();
    private List<QueryDataSources.ResultBean> list_source = new ArrayList<>();
    private TextView content4StyleText;
    private TextView content4SourceText;
    private boolean styleListLoadFailed = false;
    private boolean sourceListLoadFailed = false;
    private boolean isTest = false;
    private UsbHelper usbHelper ;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        usbHelper = new UsbHelper(App.getAppContext(), usbBroadCastReceiver);
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        if (usbHelper != null){
            usbHelper.destory();
        }
        super.onDestroy();
    }

    @Override
    public int setUpLayoutId() {
        return R.layout.dialog_setting;
    }

    @Override
    public void convertView(ViewHolder holder, final BaseDialog dialog) {
        holder.setOnClickListener(R.id.iv_close, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        final LinearLayout ll_content = holder.getView(R.id.ll_content);
        RadioGroup radioGroup = holder.getView(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                View view = null;
                switch (checkedId) {
                    case R.id.radio1:
                        view = LayoutInflater.from(getActivity()).inflate(R.layout.setting_content1, null);
                        initContent1(view);
                        break;
                    case R.id.radio2:
                        view = LayoutInflater.from(getActivity()).inflate(R.layout.setting_content2, null);
                        initContent2(view);
                        break;
                    case R.id.radio3:
                        view = LayoutInflater.from(getActivity()).inflate(R.layout.setting_content3, null);
                        initContent3(view);
                        break;
                    case R.id.radio4:
                        view = LayoutInflater.from(getActivity()).inflate(R.layout.setting_content4, null);
                        initContent4(view);
                        break;
                    case R.id.radio5:
                        view = LayoutInflater.from(getActivity()).inflate(R.layout.setting_content5, null);
                        initContent5(view);
                        break;
                    default:
                        break;
                }
                ll_content.removeAllViews();
                ll_content.addView(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            }
        });
        radioGroup.check(R.id.radio1);
        try {
            getData();
        }catch (Exception e){
            log.e(e);
        }


    }


    private void getData(){
        ApiManager.getInstance().getStyles(new NetCallBack<QueryStyles>() {
            @Override
            public void onSucess(Response<QueryStyles> response) {
                list_style = new ArrayList<>();
                if (response == null || response.body() == null || response.body().getResult() == null) {
                    log.e("样式列表返回空");
                    styleListLoadFailed = true;
                }else{
                    list_style = response.body().getResult();
                    styleListLoadFailed = false;
                }
                refreshContent4();
            }
            @Override
            public void onFailed(String msg) {
                log.e(TextUtils.isEmpty(msg) ? "" : msg);
                list_style = new ArrayList<>();
                styleListLoadFailed = true;
                refreshContent4();
            }
        });

        ApiManager.getInstance().getDataSources(new NetCallBack<QueryDataSources>() {
            @Override
            public void onSucess(Response<QueryDataSources> response) {
                list_source = new ArrayList<>();
                if (response == null || response.body() == null || response.body().getResult() == null) {
                    log.e("数据源列表返回空");
                    sourceListLoadFailed = true;
                }else{
                    list_source = response.body().getResult();
                    sourceListLoadFailed = false;
                }
                refreshContent4();
            }
            @Override
            public void onFailed(String msg) {
                log.e(TextUtils.isEmpty(msg) ? "" : msg);
                list_source = new ArrayList<>();
                sourceListLoadFailed = true;
                refreshContent4();
            }
        });
    }

    private void initContent1(View view) {
        TextView tv1 = (TextView) view.findViewById(R.id.tv1);
        tv1.setText(Config.getConfig(Global.NameKey,Global.Name));
        TextView tv2 = (TextView)view.findViewById(R.id.tv2);
        tv2.setText(Config.getConfig(Global.ClientNoKey,Global.ClientNo));
        TextView tv3 = (TextView)view.findViewById(R.id.tv3);
        if (SocketIo.getInstance().isState()){
            tv3.setText("连接");
        }else{
            tv3.setText("未连接");
        }
        TextView tv6 = (TextView)view.findViewById(R.id.tv6);
        String version = AppUtils.getVerName(getActivity());
        if (version != null){
            tv6.setText(version);
        }
        AppCompatCheckBox checkBox = (AppCompatCheckBox)view.findViewById(R.id.checkbox);
        if ("true".equals(Config.getConfig(TestKey,Test))){
            checkBox.setChecked(true);
            isTest = true;
        }else{
            checkBox.setChecked(false);
            isTest = false;
        }
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Config.setConfig(TestKey,"true");
                    isTest = true;
                }else {
                    Config.setConfig(TestKey,"false");
                    isTest =false;
                }
            }
        });

    }

    private void initContent2(View view) {
//        final TextView et1 = (TextView) view.findViewById(R.id.et1);
        final TextView tv1 = (TextView) view.findViewById(R.id.tv1);
        final TextView tv2 = (TextView) view.findViewById(R.id.tv2);
        final TextView tv3 = (TextView) view.findViewById(R.id.tv3);
        final TextView tv4 = (TextView) view.findViewById(R.id.tv4);
        Button btn_ok = (Button) view.findViewById(R.id.btn_ok);

        final String client_no_old = Config.getConfig(Global.ClientNoKey, Global.ClientNo);
        final String power_on_old = Config.getConfig(Global.PowerOnKey,Global.PowerOn);
        final String power_off_old = Config.getConfig(Global.PowerOffKey,Global.PowerOff);
        final String rotate_old = Config.getConfig(Global.RotateKey,Global.Rotate);
        final String volume_old = Config.getConfig(Global.VolumeKey,Global.Volume);

//        et1.setText(client_no_old);
        tv1.setText(power_on_old);
        tv2.setText(power_off_old);
        tv3.setText(rotate_old);
        tv4.setText(volume_old + "");

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                String client_no = et1.getText().toString();
                String power_on = tv1.getText().toString();
                String power_off = tv2.getText().toString();
                String rotate = tv3.getText().toString();
                String volume = tv4.getText().toString();
                JSONObject content = new JSONObject();
//                if (!client_no.equals(client_no_old)){
//                    Config.setConfig(Global.ClientNoKey, client_no);
//                    try {
//                        content.put("no",client_no);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
                if (!power_on.equals(power_on_old)){
//                    Config.setConfig(Global.PowerOnKey, power_time[0]);
//                    Config.setConfig(Global.PowerOffKey,power_time[1]);
                    try {
                        content.put("powerontime", power_on);
                        } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (!power_off.equals(power_off_old)){
                    try {
                        content.put("powerofftime", power_off);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    EventBus.getDefault().post(new Event(Event.TYPE_SETTING_POWERON));
                }
                if (!rotate.equals(rotate_old)){
//                    Config.setConfig(Global.RotateKey, rotate);
                    try {
                        content.put("rotation",rotate);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    EventBus.getDefault().post(new Event(Event.TYPE_SETTING_ROTATE));
                }
                if (!volume.equals( volume_old)){
//                    Config.setConfig(Global.VolumeKey, volume);
                    try {
                        content.put("volume",volume);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    EventBus.getDefault().post(new Event(Event.TYPE_SETTING_VOLUMN));
                }

                if (isTest){
                    Config.setConfig(Global.PowerOnKey, power_on);
                    Config.setConfig(Global.PowerOffKey,power_off);
                    Config.setConfig(Global.RotateKey,rotate);
                    Config.setConfig(Global.VolumeKey,volume);
                    if (!rotate.equals(rotate_old)) {
                        DeviceControlResult rotateResult = ZcDeviceControl.getInstance().applyRotation(getActivity(), rotate);
                        log.w(rotateResult.getMessage());
                        EventBus.getDefault().post(new Event(Event.TYPE_RESTART));
                    }
                    EventBus.getDefault().post(new Event(Event.TYPE_SETTING_VOLUMN));
                }else{
                    SocketIo.getInstance().sendConfig(content);
                }
//                if (!rotate.equals(rotate_old)){
//                    dismiss();
//                }
//                toast("保存成功");

            }
        });

        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog(getActivity(),android.R.style.Theme_DeviceDefault_Light_Dialog,tv1,0,0);
//                initTimePicker(tv1);
            }
        });

        tv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog(getActivity(),android.R.style.Theme_DeviceDefault_Light_Dialog,tv2,0,0);
//                initTimePicker(tv1);
            }
        });

        tv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List list = new ArrayList();
                final String[] array = getActivity().getResources().getStringArray(R.array.rotate);
                for (String rotate : array){
                    list.add(rotate);
                }
                showSelectDialog(tv3,list);
            }
        });
        tv4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List list = new ArrayList();
                for (int i= 0; i <10 ;i++){
                    list.add(i +"");
                }
                showSelectDialog(tv4,list);
            }
        });

        Button btn_restart = (Button) view.findViewById(R.id.btn_restart);
        btn_restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(new Event(Event.TYPE_RESTART));
            }
        });
    }


    private void initContent3(View view) {
        final EditText et1 = (EditText) view.findViewById(R.id.et1);
        final EditText et2 = (EditText) view.findViewById(R.id.et2);

        final TextView et4 = (TextView) view.findViewById(R.id.et4);
        final Button button1 = (Button) view.findViewById(R.id.btn_cancle);
        final Button button2 = (Button) view.findViewById(R.id.btn_ok);
        et1.setText(Config.getConfig(Global.ServerIpKey, Global.ServerIp));
        et2.setText(Config.getConfig(Global.ServerPortKey, Global.ServerPort));
        et4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String serverip = et1.getText().toString().trim();
                final String serverport = et2.getText().toString().trim();
                if (TextUtils.isEmpty(serverip)) {
                    toast("服务器地址为空");
                    return;
                }
                if (TextUtils.isEmpty(serverport)) {
                    toast("服务器端口为空");
                    return;
                }
                if (hasPortInServerIp(serverip)) {
                    toast("服务器地址和端口请分开填写");
                    return;
                }
                if (!isValidPort(serverport)) {
                    toast("服务器端口错误");
                    return;
                }
                et4.setText("测试中");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final boolean isOnline = Utils.isTcpPortOpen(AppUtils.getIp(serverip), serverport, 3000);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (isOnline){
                                        et4.setText("端口连接成功");
                                    }else {
                                        et4.setText("端口连接失败");
                                    }
                                }
                            });
                    }
                }).start();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serverip = et1.getText().toString().trim();
                String serverport = et2.getText().toString().trim();
                if (TextUtils.isEmpty(serverip)) {
                    toast("服务器地址为空");
                } else if (TextUtils.isEmpty(serverport)) {
                    toast("服务器端口为空");
                } else if (hasPortInServerIp(serverip)) {
                    toast("服务器地址和端口请分开填写");
                } else if (!isValidPort(serverport)) {
                    toast("服务器端口错误");
                } else {
                    String serverip_old = Config.getConfig(Global.ServerIpKey,Global.ServerIp);
                    String serverport_old = Config.getConfig(Global.ServerPortKey,Global.ServerPort);
                    boolean changed = !serverip.equals(serverip_old) || !serverport.equals(serverport_old);
                    Config.setConfig(Global.ServerIpKey, serverip);
                    Config.setConfig(Global.ServerPortKey, serverport);
                    if (changed || !SocketIo.getInstance().isState()) {
                        ApiManager.getInstance().resetApi();
                        try {
                            SocketIo.getInstance().connect();
                            toast((changed ? "配置已保存，正在连接 " : "正在重新连接 ") + serverip + ":" + serverport);
                        } catch (RuntimeException e) {
                            toast("连接服务器失败：" + e.getMessage());
                        }
                    }else{
                        toast("设置未修改");
                    }
                }


            }
        });
    }

    private void refreshContent4() {
        if (content4StyleText == null) {
            return;
        }
        if (!isAdded() || getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (content4StyleText == null || content4SourceText == null) {
                    return;
                }
                String style_key_old = Config.getConfig(Global.StyleKKey, Global.StyleK);
                bindStyleText(content4StyleText, style_key_old, list_style);
                String source_key_old = Config.getConfig(Global.SourceIdKey, Global.SourceId);
                bindSourceText(content4SourceText, source_key_old, list_source);
            }
        });
    }

    private void bindStyleText(TextView tv, String style_key, List<QueryStyles.ResultBean> list) {
        if (tv == null) return;

        if (list == null || list.isEmpty()) {
            if (styleListLoadFailed) {
                tv.setText("样式列表加载失败");
            } else {
                tv.setText("样式列表加载中...");
            }
            return;
        }
        if (!TextUtils.isEmpty(style_key)) {
            for (QueryStyles.ResultBean item : list){
                if (item != null && style_key.equals(item.getKey())) {
                    tv.setText(item.getName());
                    return;
                }
            }
        }
        tv.setText(list.get(0) == null ? "" : list.get(0).getName());
    }

    private void bindSourceText(TextView tv, String source_key, List<QueryDataSources.ResultBean> list) {
        if (tv == null) return;

        if (list == null || list.isEmpty()) {
            if (sourceListLoadFailed) {
                tv.setText("数据源列表加载失败");
            } else {
                tv.setText("数据源列表加载中...");
            }
            return;
        }
        if (!TextUtils.isEmpty(source_key)) {
            for (QueryDataSources.ResultBean item : list){
                if (item != null && source_key.equals(item.get_id())) {
                    tv.setText(item.getName());
                    return;
                }
                if (item != null && source_key.equals(item.getName())) {
                    tv.setText(item.getName());
                    return;
                }
            }
        }
        tv.setText(list.get(0) == null ? "" : list.get(0).getName());
    }

    private void initContent4(View view) {
        final TextView tv1 = (TextView) view.findViewById(R.id.tv1);
        final TextView tv2 = (TextView) view.findViewById(R.id.tv2);
        content4StyleText = tv1;
        content4SourceText = tv2;

        Button btn_cancle = (Button) view.findViewById(R.id.btn_cancle);
        Button btn_ok = (Button) view.findViewById(R.id.btn_ok);

        final String style_key_old = Config.getConfig(Global.StyleKKey,Global.StyleK);
        bindStyleText(tv1, style_key_old, list_style);

        final String source_key_old = Config.getConfig(Global.SourceIdKey,Global.SourceId);
        bindSourceText(tv2, source_key_old, list_source);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String style_name = tv1.getText().toString();
                String source_name = tv2.getText().toString();
                JSONObject content = new JSONObject();
                boolean changed = false;

                if (list_style != null ){
                    for (QueryStyles.ResultBean style :list_style){
                        if (style == null || style.getName() == null || style_name == null){
                            continue;
                        }
                        if (style.getName().equals(style_name) && !style.getKey().equals(style_key_old)){
//                            Config.setConfig(Global.StyleKKey, style.getKey());
                            try {
                                content.put("style",style.getKey());
                                changed = true;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//                            EventBus.getDefault().post(new Event(Event.TYPE_SETTING_STYLE));
                            if (isTest){
                                Config.setConfig(Global.StyleKKey,style.getKey());
                                EventBus.getDefault().post(new Event(Event.TYPE_SETTING_STYLE));
                                EventBus.getDefault().post(new Event(Event.TYPE_RESTART));
                            }
                            break;
                        }
                    }
                }

                if (list_source != null ){
                    for (QueryDataSources.ResultBean source :list_source){
                        if (source == null || source.getName() == null || source_name == null){
                            continue;
                        }
                        if (source.getName().equals(source_name) && !source.get_id().equals(source_key_old)){
//                            Config.setConfig(Global.SourceIdKey, source.get_id());
                            try {
                                content.put("datasource",source.get_id());
                                changed = true;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//                            EventBus.getDefault().post(new Event(Event.TYPE_SETTING_SOURCE));
                            break;
                        }
                    }

                }
                if (!changed){
                    toast("未检测到配置变更");
                    return;
                }
                SocketIo.getInstance().sendConfig(content);

            }
        });

        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (list_style == null || list_style.size() == 0) {
                    toast("样式列表还未加载完成");
                    return;
                }
                showStyleSelectDialog(tv1, list_style);
            }
        });

        tv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (list_source == null || list_source.size() == 0) {
                    toast("数据源列表还未加载完成");
                    return;
                }
                showSourceSelectDialog(tv2,list_source);
            }
        });
    }

    private void showStyleSelectDialog(final TextView btn, final List<QueryStyles.ResultBean> list) {
        if (btn == null || list == null ||list.size() <= 0) return;

        final int width =btn.getWidth();
        final int item_height = btn.getHeight();
        final CommonPopupWindow popupWindow = new CommonPopupWindow(getActivity(),R.layout.setting_type_select,width) {
            RecyclerView rv;
            @Override
            protected void initView() {
                rv = (RecyclerView) contentView.findViewById(R.id.lv) ;
                rv.setLayoutManager(new LinearLayoutManager(getActivity()));

                QuickAdapter<QueryStyles.ResultBean> adapter = new QuickAdapter<QueryStyles.ResultBean>(list) {
                    @Override
                    public int getLayoutId(int viewType) {
                        return R.layout.setting_type_select_item;
                    }

                    @Override
                    public void convert(VH holder, QueryStyles.ResultBean data, int position) {
                        TextView tv = holder.getView(R.id.tv);
                        ViewGroup.LayoutParams params = tv.getLayoutParams();
                        params.height = item_height;
                        params.width = width;
                        tv.setLayoutParams(params);
                        holder.setText(R.id.tv, data == null ? "" : data.getName());
                        if (data != null && data.getName() != null && data.getName().equals(btn.getText().toString())) {
                            holder.setBackGround(R.id.tv, R.color.color_4DA9C2);
                        }
                    }

                };
                adapter.setOnItemClickListener(new QuickAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(int position) {
                        QueryStyles.ResultBean bean = list.get(position);
                        btn.setText(bean == null || bean.getName() == null ? "" : bean.getName());
                        dismiss();
                    }

                    @Override
                    public void onLongClick(int position) {

                    }
                });
                rv.setAdapter(adapter);

            }
            @Override
            protected void initEvent() {

            }
        };
        popupWindow.showAsDropDown(btn, 0,0);
    }

    private void showSourceSelectDialog(final TextView btn, final List<QueryDataSources.ResultBean> list) {
        if (btn == null || list == null || list.size() <= 0) return;

        final int width =btn.getWidth();
        final int item_height = btn.getHeight();
        final CommonPopupWindow popupWindow = new CommonPopupWindow(getActivity(),R.layout.setting_type_select,width) {
            RecyclerView rv;
            @Override
            protected void initView() {
                rv = (RecyclerView) contentView.findViewById(R.id.lv) ;
                rv.setLayoutManager(new LinearLayoutManager(getActivity()));

                QuickAdapter<QueryDataSources.ResultBean> adapter = new QuickAdapter<QueryDataSources.ResultBean>(list) {
                    @Override
                    public int getLayoutId(int viewType) {
                        return R.layout.setting_type_select_item;
                    }

                    @Override
                    public void convert(VH holder, QueryDataSources.ResultBean data, int position) {
                        TextView tv = holder.getView(R.id.tv);
                        ViewGroup.LayoutParams params = tv.getLayoutParams();
                        params.height = item_height;
                        params.width = width;
                        tv.setLayoutParams(params);
                        holder.setText(R.id.tv, data == null ? "" : data.getName());
                        if (data != null && data.getName() != null && data.getName().equals(btn.getText().toString())) {
                            holder.setBackGround(R.id.tv, R.color.color_4DA9C2);
                        }
                    }

                };
                adapter.setOnItemClickListener(new QuickAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(int position) {
                        QueryDataSources.ResultBean bean = list.get(position);
                        btn.setText(bean == null || bean.getName() == null ? "" : bean.getName());
                        dismiss();
                    }

                    @Override
                    public void onLongClick(int position) {

                    }
                });
                rv.setAdapter(adapter);

            }
            @Override
            protected void initEvent() {

            }
        };
        popupWindow.showAsDropDown(btn, 0,0);
    }

    private void showSelectDialog(final TextView btn, final List<String> list) {
        if (btn == null || list.size() <= 0) return;
        final int width =btn.getWidth();
        final int item_height = btn.getHeight();
        final CommonPopupWindow popupWindow = new CommonPopupWindow(getActivity(),R.layout.setting_type_select,width) {
            private RecyclerView rv;
            @Override
            protected void initView() {
                rv = (RecyclerView) contentView.findViewById(R.id.lv) ;
                rv.setLayoutManager(new LinearLayoutManager(getActivity()));

                QuickAdapter<String> adapter = new QuickAdapter<String>(list) {
                    @Override
                    public int getLayoutId(int viewType) {
                        return R.layout.setting_type_select_item;
                    }

                    @Override
                    public void convert(VH holder, String data, int position) {
                        TextView tv = holder.getView(R.id.tv);
                        ViewGroup.LayoutParams params = tv.getLayoutParams();
                        params.height = item_height;
                        params.width = width;
                        tv.setLayoutParams(params);
                        holder.setText(R.id.tv, data);
                        if (data.equals(btn.getText().toString())) {
                            holder.setBackGround(R.id.tv, R.color.color_4DA9C2);
                        }
                    }

                };
                adapter.setOnItemClickListener(new QuickAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(int position) {
                        btn.setText(list.get(position));
                        dismiss();
                    }

                    @Override
                    public void onLongClick(int position) {

                    }
                });
                rv.setAdapter(adapter);

            }
            @Override
            protected void initEvent() {

            }
        };
        popupWindow.showAsDropDown(btn, 0,0);
    }

    public void showTimePickerDialog(Activity activity, int themeResId, final TextView tv,int hour_d,int minute_d) {
        new TimePickerDialog( activity,themeResId,
                // 绑定监听器
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String time = String.format("%02d:%02d:00",hourOfDay,minute);
                        tv.setText(time);
                    }
                }
                // 设置初始时间
                , hour_d
                , minute_d
                // true表示采用24小时制
                ,true).show();
    }


    private String time_splite = ":"; //时分间隔符
    private String time2_splite = " - "; //开始时间结束时间间隔符

    private void initTimePicker(final TextView tv) {
        CommonDialog.newInstance()
                .setLayoutId(R.layout.select_time)
                .setConvertListener(new CommonDialog.ViewConvertListener() {
                    @Override
                    public void convertView(BaseDialog.ViewHolder holder, final BaseDialog dialog) {
                        final PickerView pickerView_start = holder.getView(R.id.pickerview_start);
                        final PickerView pickerView_end = holder.getView(R.id.pickerview_end);
                        String src[] = tv.getText().toString().split( time2_splite);

                        ArrayList<String> mDatas_start = getTimeList(src[1],true);
                        ArrayList<String> mDatas_end = getTimeList(src[0],false);

                        pickerView_start.setData(mDatas_start);
                        pickerView_end.setData(mDatas_end);

                        pickerView_start.setSelected(src[0]);
                        pickerView_end.setSelected(src[1]);
                        pickerView_start.setOnSelectListener(new PickerView.onSelectListener() {
                            @Override
                            public void onSelect(String text) {
                                String select = pickerView_end.getSelect();
                                pickerView_end.setData(getTimeList(text,false));
                                pickerView_end.setSelected(select);
                            }
                        });

                        pickerView_end.setOnSelectListener(new PickerView.onSelectListener() {
                            @Override
                            public void onSelect(String text) {
                                String select = pickerView_start.getSelect();
                                pickerView_start.setData(getTimeList(text,true));
                                pickerView_start.setSelected(select);
                            }
                        });
                        holder.setOnClickListener(R.id.btn_select_times, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String select_start = pickerView_start.getSelect();
                                String select_end = pickerView_end.getSelect();
                                tv.setText(select_start+time2_splite+select_end);
                                dialog.dismiss();
                            }
                        });
                    }
                })
//                .setDimAmout(0.3f)
                .setGravity(tv, new BaseDialog.LayoutGravity(BaseDialog.LayoutGravity.TO_BOTTOM),0,0)
                .setSize(800,600)
//                        .setAnimStyle(R.style.EnterExitAnimation)
                .show(getFragmentManager ());
    }

    private ArrayList<String> getTimeList(String time,boolean isBefor) {
        ArrayList<String> mDatas = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            int max = 60;
            int step = 30;

            for (int j = 0;j < max ;j += step) {
                String time_temp = String.format("%02d%s%02d%s%02d",i,time_splite,j,time_splite,0) ;
                if (isBefor) {
                    if (time_temp.compareTo(time) < 0) {
                        mDatas.add(time_temp);
                    } else {
                        break;
                    }
                } else {
                    if (time_temp.compareTo(time) > 0) {
                        mDatas.add(time_temp);
                    }
                }
            }

            if (!isBefor && i == 23){
                String time_temp = String.format("%02d%s%02d%s%02d",i,time_splite,59,time_splite,59) ;
                if (time_temp.compareTo(time) > 0) {
                    mDatas.add(time_temp);
                }
            }
        }
        return mDatas;
    }

    private void initContent5(View view){
        Button button2 = (Button)view.findViewById(R.id.btn_ok);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                final String log_path = Utils.getLogPath();
                copyLocalFile();
            }
        });
    }

    private void toast(String text){
        if (TextUtils.isEmpty(text)){
            return;
        }else{
            log.w(text);
            Toast.makeText(getActivity(),text,Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasPortInServerIp(String serverip) {
        if (TextUtils.isEmpty(serverip)) {
            return false;
        }
        String host = serverip.trim();
        int schemeIndex = host.indexOf("://");
        if (schemeIndex != -1) {
            host = host.substring(schemeIndex + 3);
        }
        return host.contains(":");
    }

    private boolean isValidPort(String serverport) {
        try {
            int port = Integer.parseInt(serverport);
            return port > 0 && port <= 65535;
        } catch (Exception e) {
            return false;
        }
    }

    private void copyLocalFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtil.deletFile(Utils.getAppPath(),false,".zip");
                String logFilePath = Utils.getLogPath( );
                String log_name = "log-" + Config.getConfig(ClientNoKey,ClientNo) + "-"
                        + DateUtils.getCurrentDate("yyyyMMddHHmmss") + ".zip";
                final String zipPath = Utils.getAppPath() + File.separator + log_name;
                log.i( "压缩日志：压缩目录：" + logFilePath +"，压缩包：" + zipPath);
                File file = null;
                try {
                    ZipUtils.ZipFolder(logFilePath,zipPath);
                    file = new File(zipPath);
                    if (file == null || file.exists() == false){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toast( "压缩日志失败");
                            }
                        });

                        return;
                    }
                } catch (final Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast("压缩日志失败:" + e.getMessage());
                        }
                    });
                    return;
                }

                usbHelper.getDeviceList();
                //复制结果
                final boolean result = usbHelper.saveFileDirToUsb(file, usbHelper.getCurrentFolder(), new UsbHelper.DownloadProgressListener() {
                    @Override
                    public void downloadProgress(final int progress) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String text = "From Local : " + zipPath
                                        + "\nTo Usb : " + usbHelper.getCurrentFolder().getName()
                                        + "\nProgress : " + progress;
                                toast(text);
                            }
                        });
                    }
                });

                //主线程更新UI
                 getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result) {
                            toast("拷贝成功");
                        } else {
                            toast("拷贝失败：请检查是否插入u盘");
                        }
                    }
                });
            }
        }).start();
    }

    private USBBroadCastReceiver.UsbListener usbBroadCastReceiver = new USBBroadCastReceiver.UsbListener() {
        @Override
        public void insertUsb(UsbDevice device_add) {
//            if (usbList.size() == 0) {
//                updateUsbFile(0);
//            }
            toast("插入u盘");
        }

        @Override
        public void removeUsb(UsbDevice device_remove) {
//            updateUsbFile(0);
            toast("拔出u盘");
        }

        @Override
        public void getReadUsbPermission(UsbDevice usbDevice) {

        }

        @Override
        public void failedReadUsb(UsbDevice usbDevice) {

        }
    };

    @Override
    public void dismiss() {
        content4StyleText = null;
        content4SourceText = null;
        super.dismiss();

    }

    public void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        Window window = getActivity().getWindow();
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = window.getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);

        }

    }
}
