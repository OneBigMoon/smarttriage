package com.wcjk.triage.fragment;

import android.text.TextUtils;
import android.widget.TextView;

import com.wcjk.triage.BuildConfig;
import com.wcjk.triage.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
public class FragmentSecondFuer extends FragmentSecond {

    @Override
    public int setLayoutId() {
        return R.layout.second_fuer;
    }

    @Override
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


    protected boolean bindDate(Map<String,String> map, String tag, String name) {
        if (map == null) return false;
        String value = map.get(name);
        value = value == null ? "" : value;
        log.i("bindDate: tag is " + tag + "," + name + ":" + value);
        TextView tv = (TextView) view.findViewWithTag(tag);
        if (tv != null) {
            tv.setText(value);
            return true;
        }
        return false;
    }

}
