package com.wcjk.triage.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wcjk.triage.BuildConfig;
import com.wcjk.triage.R;
import com.wcjk.triage.bean.DoctorPhoto;
import com.wcjk.triage.common.Config;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.event.DataEven;
import com.wcjk.triage.global.Global;
import com.wcjk.triage.zxing.QRCodeUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by hyc on 2018/7/27
 */
public class FragmentSecond extends FragmentBase {
    protected Log log = Log.getLogger(this.getClass());

    protected String status = "status";
    protected String ticket = "ticket";
    protected String brxm = "brxm";
    protected String brxmfull = "brxmfull";
    protected String doctorphoto = "doctorphoto";

    //    private String doctorschedule = "doctorschedule" ;//排班
    @Override
    public int setLayoutId() {
        return R.layout.second;
    }

    @Override
    protected int setShowArray() {
        return R.array.secondarytriage;
    }

    @Override
    protected int setCallTimes() {
        return 2;
    }

    @Override
    protected int setNum_play() {
        return 1;
    }

    @Override
    protected void initView() {
//        updateParams();
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void update(DataEven even) {
        switch (even.getType()) {
            case DataEven.TYPE_OTHER:
                log.i("fragment recv date");
                JSONObject object = even.getData();
                if(object.optBoolean("clearData")){
                    bindClear(true);
                }else {
                    bindClear(false);
                }

                bind(object);
                break;
//            case DataEven.TYPE_PARAMS:
//                updateParams();
//                break;
        }

        isFirst = false;
    }

    private void bindClear(boolean isClearData) {
        if (view == null) return;
        TextView tv = (TextView) view.findViewWithTag("patientcount");
        if (tv != null) {
            tv.setText("");
        }
        if(isClearData){
            for (int i = 1; i <= 3; i++) {
                tv = (TextView) view.findViewWithTag("ticket" + i);
                if (tv != null) {
                    tv.setText("");
                }
                tv = (TextView) view.findViewWithTag("brxmfull" + i);
                if (tv != null) {
                    tv.setText("");
                }
                tv = (TextView) view.findViewWithTag("brxm" + i);
                if (tv != null) {
                    tv.setText("");
                }
            }
        }else {
            for (int i = 2; i <= 3; i++) {
                tv = (TextView) view.findViewWithTag("ticket" + i);
                if (tv != null) {
                    tv.setText("");
                }
                tv = (TextView) view.findViewWithTag("brxmfull" + i);
                if (tv != null) {
                    tv.setText("");
                }
                tv = (TextView) view.findViewWithTag("brxm" + i);
                if (tv != null) {
                    tv.setText("");
                }
            }
        }

    }
    protected void bind(JSONObject jsonObject) {
        Iterator it = jsonObject.keys();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.equals("patients")) {
                JSONArray array = null;
                try {
                    array = jsonObject.getJSONArray(name);
                    //叫号队列
                    List<Map<String,String>> list_call = new ArrayList<>();
                    List<Map<String,String>> list_wait = new ArrayList<>();
                    boolean isFind = false;
                    int index = 2;
                    Map map = new HashMap();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject row = array.getJSONObject(i);
                        if (row == null) continue;
                        String value_ticket = row.getString(ticket);
                        if (value_ticket != null && map.containsKey(value_ticket)) continue;
                        if (!isFind && row.getInt(status) == 1) {
                            bindDate(row, "ticket1", ticket);
                            bindDate(row, "brxmfull1", brxmfull);
                            bindDate(row, "brxm1", brxm);
                            isFind = true;
                            map.put(value_ticket, status);

                        } else{
                            Map<String,String> map_wait = new HashMap();
                            map_wait.put(ticket,row.getString(ticket));
                            map_wait.put(brxmfull,row.getString(brxmfull));
                            map_wait.put(brxm,row.getString(brxm));
                            map_wait.put(status, row.getInt(status) +"");
                            list_wait.add(map_wait);
                            index++;
                            map.put(value_ticket, status);
                        }

                        if (row.getInt(status) == 1 && BuildConfig.isSecondCall) {
                            //泉州附二新增二级分诊语音播报
                            //由于医院存在同时叫号，所以判断只要状态为1，则叫号。
                            Iterator it_row = row.keys();
                            Map map_call = new HashMap();
                            while (it_row.hasNext()) {
                                String name_row = (String) it_row.next();
                                String value = row.getString(name_row);
                                map_call.put(name_row,value);
                            }
                            list_call.add(map_call);
                        }
                    }

                    if (list_wait != null && list_wait.size() > 0) {
                        Collections.sort(list_wait, new Comparator<Map<String, String>>() {
                            @Override
                            public int compare(Map<String, String> o1, Map<String, String> o2) {
                                if ("1".equals(o2.get(status) ) ){
                                    return 1;
                                }else {
                                    String ticket1 = o1.get(ticket);
                                    String ticket2 = o2.get(ticket);
                                    if (TextUtils.isEmpty(ticket2)){
                                        return 1;
                                    }

                                    if (TextUtils.isEmpty(ticket1)){
                                        return -1;
                                    }

                                    return ticket1.substring(1).compareTo(ticket2.substring(1));
                                }
                            }
                        });

                        for (int i = 0;i<list_wait.size();i++){
                            bindDate(list_wait.get(i), "ticket"+(i+2), ticket);
                            bindDate(list_wait.get(i), "brxmfull"+(i+2), brxmfull);
                            bindDate(list_wait.get(i), "brxm"+(i+2), brxm);
                        }
                    }

                    sendCall(list_call);
                    list = list_call;

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                bindDate(jsonObject, name);
            }
        }
    }
    @Override
    protected boolean bindDate(JSONObject object, String name) {
        try {
            String value = object.getString(name);
            value = value == null ? "" : value;
            value = value.equals("null") ? "" : value;
            if(BuildConfig.FLAVOR.equals("qzgqyy") ||BuildConfig.FLAVOR.equals("mhey")){
                if(name.startsWith("ticket")){
                    value+=" "+ object.getString("brxm");
                }
            }
//            log.i("bindDate: " + name + ":" + value);
            if (name.equals("offtime")) {
//                EventBus.getDefault().post(new Event(Event.TYPE_CLEAR,value));
            } else if (name.equals(doctorphoto)) {
                ImageView iv_doctorphoto = (ImageView) view.findViewById(R.id.doctorphoto);
                if (TextUtils.isEmpty(value)) {
                    Glide.with(getActivity())
                            .load(R.mipmap.doctor_q)
                            .into(iv_doctorphoto);
//                    log.i("43434: " + name + ":" + value);
                } else {
                    String file_path = createImage(value);
                    if (!TextUtils.isEmpty(file_path)) {
                        Bitmap bitmap = getLoacalBitmap(file_path); //从本地取图片(
                        iv_doctorphoto.setImageBitmap(bitmap); //设置Bitmap
//
//                        File file = new File(file_path);
//                        if (file != null) {
//                            Glide.with(getActivity())
//                                    .load(file)
//                                    .into(iv_doctorphoto);
//                        }
                    }
                }
            } else {
                TextView tv = (TextView) view.findViewWithTag(name);
                if (tv != null) {
//                    if (name.equals(doctorschedule)){
//                        value = "坐诊时间:" + value;
//                    }
                    tv.setText(value);
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 加载本地图片
     *
     * @param url
     * @return
     */
    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);  ///把流转化为Bitmap图片
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    protected boolean bindDate(Map<String,String> map, String tag, String name) {
        if (map == null) return false;
        String value = map.get(name);
        value = value == null ? "" : value;
        if(BuildConfig.FLAVOR.equals("qzgqyy")||BuildConfig.FLAVOR.equals("mhey")){
            if(tag.startsWith("ticket")){
                value+=" "+ map.get("brxm");
            }
        }
        log.i("bindDate: tag is " + tag + "," + name + ":" + value);
        TextView tv = (TextView) view.findViewWithTag(tag);
        if (tv != null) {
            tv.setText(value);
            return true;
        }
        return false;
    }

    protected boolean bindDate(JSONObject object, String tag, String name) {
        try {
            String value = object.getString(name);
            value = value == null ? "" : value;
            if(BuildConfig.FLAVOR.equals("qzgqyy")||BuildConfig.FLAVOR.equals("mhey")){
                if(tag.startsWith("ticket")){
                    value+=" "+ object.getString("brxm");
                }
            }
            log.i("bindDate: tag is " + tag + "," + name + ":" + value);
            TextView tv = (TextView) view.findViewWithTag(tag);
            if (tv != null) {
                tv.setText(value);
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateParams() {
        String params = Config.getConfig(Global.ParamsKey, Global.Params);
        if (TextUtils.isEmpty(params)) return;
        try {
            JSONObject object = new JSONObject(params);
            Iterator it = object.keys();
            while (it.hasNext()) {
                String name = (String) it.next();
                if (name.equals("qr1") || name.equals("qr2")) {
                    log.i("updateParams: " + name);
                    ImageView iv = (ImageView) view.findViewWithTag(name);
                    if (iv != null) {
                        String value = object.getString(name);
                        Bitmap bitmap = QRCodeUtil.createQRImage(value, 200, 200, null);
                        if (bitmap != null) {
                            iv.setImageBitmap(bitmap);
                        }
                    }
                } else {
                    bindDate(object, name);
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected String createImage(String text) {
        if (text == null) return null;
        DoctorPhoto doctorPhoto = new Gson().fromJson(text, new TypeToken<DoctorPhoto>() {
        }.getType());
        if (doctorPhoto.getData() == null) return null;
        try {
            String file_path = getPath() + "/image.png";
            OutputStream os = new FileOutputStream(file_path);
            os.write(doctorPhoto.getData(), 0, doctorPhoto.getData().length);
            os.flush();
            os.close();
            return file_path;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getPath() {
        String directoryPath = "";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {//判断外部存储是否可用
            directoryPath = getActivity().getExternalFilesDir("image").getAbsolutePath();
        } else {//没外部存储就使用内部存储
            directoryPath = getActivity().getFilesDir() + File.separator + "image";
        }
        File file = new File(directoryPath);
        log.w("图片存储路径:" + directoryPath);
        if (!file.exists()) {//判断文件目录是否存在
            file.mkdirs();
        }
        return directoryPath;
    }
}
