package com.wcjk.triage.fragment.fjykfsdeyy;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.google.zxing.common.StringUtils;
import com.wcjk.triage.R;
import com.wcjk.triage.bean.fjykfsdeyy.KeShiInfo;
import com.wcjk.triage.bean.fjykfsdeyy.WaitPatientInfo;
import com.wcjk.triage.common.recyclerview.GridSpacingItemDecoration;
import com.wcjk.triage.common.recyclerview.SpacesItemDecoration;
import com.wcjk.triage.common.recyclerview.fjykfsdeyy.KeShiAdapter;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.event.DataEven;
import com.wcjk.triage.event.ShowCallEvent;
import com.wcjk.triage.fragment.FragmentBase;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by hyc on 2018/7/27
 */
public class FragmentFirst extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());
    private TextView tv_call_text;
    private KeShiAdapter keShiAdapter;
    private List<KeShiInfo> keShiInfoList = new ArrayList<>();

    private String doctorname = "doctorname";
    private String officename = "officename";

    @Override
    public int setLayoutId() {
        return R.layout.first;
    }

    @Override
    protected int setShowArray() {
        return R.array.leveldepart;
    }

    @Override
    protected int setCallTimes() {
        return 2;
    }

    @Override
    protected int setNum_play() {
        return 5;
    }

    @Override
    public void initView() {
        delay = 3 * delay;
//        keShiInfoList.add(new KeShiInfo());
//        keShiInfoList.add(new KeShiInfo());
//        keShiInfoList.add(new KeShiInfo());
//        keShiInfoList.add(new KeShiInfo());
//        keShiInfoList.add(new KeShiInfo());
//        int i = 1;
//        for (KeShiInfo keShiInfo : keShiInfoList) {
//            keShiInfo.setOfficename("儿科诊室" + i);
//            keShiInfo.setDoctorname("医生:张晓明");
//            keShiInfo.setWaitPatientInfoList(new ArrayList<WaitPatientInfo>());
//            for (int j = 0; j < 7; j++) {
//                keShiInfo.getWaitPatientInfoList().add(new WaitPatientInfo(85200002 + j + 1 + "", "朱必休"));
//            }
//            i++;
//        }
        lv = (RecyclerView) view.findViewById(R.id.lv);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), setNum_play());
        gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        lv.setLayoutManager(gridLayoutManager);
        lv.addItemDecoration(new GridSpacingItemDecoration(setNum_play(), 10, false));
        lv.addItemDecoration(new SpacesItemDecoration(3));
        keShiAdapter = new KeShiAdapter(keShiInfoList, this.getActivity());
        lv.setAdapter(keShiAdapter);
    }

    public void analysis(JSONArray jsonArray) {
        if (jsonArray != null && jsonArray.length() > 0) {
            //                list.clear();
            String jsonStr = jsonArray.toString();
            com.alibaba.fastjson.JSONArray obj = com.alibaba.fastjson.JSONArray.parseArray(jsonStr);
            List<KeShiInfo> keShiInfos = obj.toJavaList(KeShiInfo.class);
            if (keShiInfos != null && keShiInfos.size() > 0) {
                for (KeShiInfo keShiInfo : keShiInfos) {
                    keShiInfo.setWaitPatientInfoList(new ArrayList<WaitPatientInfo>());
                    if (keShiInfo.getWaitno() != null && keShiInfo.getWaitno().length() > 0) {
                        String[] s = keShiInfo.getWaitno().split("  ");
                        for (int i = 0; i < s.length; i++) {
                            if (s[i].length() > 0) {
                                String[] s1 = s[i].split(" ");
                                keShiInfo.getWaitPatientInfoList().add(new WaitPatientInfo(s1[0], changeName(s1[1])));
                            }

                        }
                        Collections.sort(keShiInfo.getWaitPatientInfoList(), new Comparator<WaitPatientInfo>() {
                            @Override
                            public int compare(WaitPatientInfo o1, WaitPatientInfo o2) {
                                return o1.getCallno().compareTo(o2.getCallno());
                            }
                        });
                    }
                    if (keShiInfo.getWaitPatientInfoList().size() > 1) {

                        Collections.sort(keShiInfo.getWaitPatientInfoList(), new Comparator<WaitPatientInfo>() {
                            @Override
                            public int compare(WaitPatientInfo o1, WaitPatientInfo o2) {
                                int a1 = Integer.parseInt(Pattern.compile("[^0-9]").matcher(o1.getCallno()).replaceAll("").trim());
                                int a2 = Integer.parseInt(Pattern.compile("[^0-9]").matcher(o2.getCallno()).replaceAll("").trim());
                                return a1 - a2;
                            }
                        });
                    }
                    for (int i = keShiInfo.getWaitPatientInfoList().size() - 1; i > 5; i--) {
                        keShiInfo.getWaitPatientInfoList().remove(i);
                    }
                }
            }
            mHandler.removeMessages(what_play_next_page);
            index_play = 0;
            keShiAdapter.updateList(keShiInfos);
            try {
//                list.clear();
                List<Map<String, String>> list_temp = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject object = jsonArray.getJSONObject(i);
                    Iterator it = object.keys();
                    Map<String, String> map = new HashMap<>();
                    while (it.hasNext()) {
                        String name = (String) it.next();
                        String value = object.getString(name);
                        map.put(name, value);
                    }
                    map.put("call", map.get("callno") + " " + map.get("patientname"));
                    list_temp.add(map);
                }
                if (list_temp != null && list_temp.size() > 0) {
                    Collections.sort(list_temp, new Comparator<Map<String, String>>() {
                        @Override
                        public int compare(Map<String, String> o1, Map<String, String> o2) {
                            int result = Collator.getInstance(Locale.CHINESE).compare(o1.get(officename), o2.get(officename));
                            if (result == 0) {
                                return Collator.getInstance(Locale.CHINESE).compare(o1.get(doctorname), o2.get(doctorname));
                            } else {
                                return result;
                            }
                        }
                    });
                    sendCall(list_temp);
                } else {
                    if (tv_call_text != null) {
                        tv_call_text.setText("");
                    }
                }
                setNewList1(list_temp);
                log.i("列表长度: " + list.size());
            } catch (JSONException e) {
                e.printStackTrace();
                log.e(e);
            }
        }

//        if (jsonArray != null && jsonArray.length() > 0) {
//            try {
////                list.clear();
//                List<Map<String,String>> list_temp = new ArrayList<>();
//                for (int i = 0;i <jsonArray.length();i++) {
//                    JSONObject object =  jsonArray.getJSONObject(i);
//                    Iterator it = object.keys();
//                    Map<String,String> map = new HashMap<>();
//                    while (it.hasNext()) {
//                        String name = (String) it.next();
//                        String value = object.getString(name);
//                        map.put(name,value);
//                    }
//                    map.put("call",map.get("callno") + " " + map.get("patientname"));
//                    list_temp.add(map);
//                }
//                if (list_temp != null && list_temp.size() > 0) {
//                    Collections.sort(list_temp, new Comparator<Map<String, String>>() {
//                        @Override
//                        public int compare(Map<String, String> o1, Map<String, String> o2) {
//                            int result = Collator.getInstance(Locale.CHINESE).compare(o1.get(officename),o2.get(officename));
//                            if (result == 0){
//                                return Collator.getInstance(Locale.CHINESE).compare(o1.get(doctorname),o2.get(doctorname));
//                            }else{
//                                return result;
//                            }
//                        }
//                    });
//                    sendCall(list_temp);
//                }else{
//                    if (tv_call_text != null) {
//                        tv_call_text.setText("");
//                    }
//                }
//                setNewList(list_temp);
//
//                log.i("列表长度: " +list.size());
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }else{
//            setNewList(null);
//        }

    }


    public static String changeName(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        if (s.length() < 3) {
            return s.substring(0, 1) + "*";
        } else {
            return s.substring(0, 1) + "*" + s.substring(s.length() - 1, s.length());
        }
    }


    @Override
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void update(DataEven even) {
        JSONObject jsonObject = even.getData();
        if (jsonObject != null) {
            Iterator it = jsonObject.keys();
            while (it.hasNext()) {
                String name = (String) it.next();
                if (name.equals("queues")) {
                    JSONArray array = null;
                    try {
                        array = jsonObject.getJSONArray(name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    analysis(array);
                } else {
                    bindDate(jsonObject, name);
                }
            }
        }
        isFirst = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void showCallText(ShowCallEvent even) {
//        String text = even.getCallText();
//        if (text != null && tv_call_text!= null){
//            tv_call_text.setText(text);
//        }
    }
}
