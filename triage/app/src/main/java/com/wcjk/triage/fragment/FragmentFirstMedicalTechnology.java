package com.wcjk.triage.fragment;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.wcjk.triage.R;
import com.wcjk.triage.common.recyclerview.GridSpacingItemDecoration;
import com.wcjk.triage.common.recyclerview.QuickAdapter;
import com.wcjk.triage.common.recyclerview.SpacesItemDecoration;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.event.DataEven;
import com.wcjk.triage.event.ShowCallEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by hyc on 2020/04/12
 * 超声
 */
public class FragmentFirstMedicalTechnology extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());
    private TextView tv_call_text;

    private String status = "status";
    private String ticket = "no";
    //    private String brxmfull = "brxmfull";
    private String brxm = "name";

    @Override
    public int setLayoutId() {
        return R.layout.fragment_first_medical_technology;
    }

    @Override
    protected int setShowArray() {
        return R.array.primarymedicaltechnologytriage;
    }

    @Override
    protected int setCallTimes() {
        return 2;
    }

    @Override
    protected int setNum_play() {
        return 4;
    }

    @Override
    public void initView(){
        tv_call_text = view.findViewById(R.id.tv_call_text);
        if (tv_call_text != null) {
            tv_call_text.setVisibility(View.VISIBLE);
        }

        lv = (RecyclerView) view.findViewById(R.id.lv);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),setNum_play());
        gridLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        lv.setLayoutManager(gridLayoutManager);
        lv.addItemDecoration(new GridSpacingItemDecoration(setNum_play(), 2, false));
        lv.addItemDecoration(new SpacesItemDecoration(3));
        mAdapter = new QuickAdapter<Map<String,String>>(list) {
            @Override
            public int getItemViewType(int position) {
                if (position % 2 == 0){
                    return 0;
                }else {
                    return 1;
                }
            }

            @Override
            public int getLayoutId(int viewType) {
                if (viewType == 0) {
                    return R.layout.item_first_medical_technology_light;
                }else{
                    return R.layout.item_first_medical_technology_dark;
                }
            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                for (String name: data.keySet()){
                    if (data.get(name).equals("null")){
                        holder.setText(name,"");
                        log.i("convert: " + name + ":" + data.get(name));
                    }else{
                        holder.setText(name,data.get(name));
                        log.i("convert: " + name + ":" + data.get(name));
                    }

                }
            }
        };

        lv.setAdapter(mAdapter);

    }

    public void analysis(JSONArray jsonArray){
        if (jsonArray != null && jsonArray.length() > 0) {
            try {
//                list.clear();
                List<Map<String,String>> list_temp = new ArrayList<>();
                for (int i = 0;i <jsonArray.length();i++) {
                    JSONObject object =  jsonArray.getJSONObject(i);
                    Iterator it = object.keys();
                    Map<String,String> map = new HashMap<>();
                    while (it.hasNext()) {
                        String name = (String) it.next();
                        if (name.equals("patients")) {
                            JSONArray array = object.getJSONArray(name);
                            analysisPatient(array,map);
                        }else {
                            String value = object.getString(name);
                            map.put(name, value);
                        }
                    }
                    list_temp.add(map);
                }

                if (list_temp != null && list_temp.size() > 0) {
                    sendCall(list_temp);
                }else{
                    if (tv_call_text != null) {
                        tv_call_text.setText("");
                    }
                }
                setNewList(list_temp);

                log.i("列表长度: " +list.size());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            setNewList(null);
        }

    }

    private void analysisPatient(JSONArray patient,Map map){
        try {
            boolean isFind = false;
            List<String> list_ticket = new ArrayList<>();
            String wait = "";
            int wait_count = 0;
            for (int i = 0; i < patient.length(); i++) {
                JSONObject row = patient.getJSONObject(i);
                if (row == null) continue;
                //状态为2为诊结 不显示，过滤掉
                if (row.getInt(status) == 2) continue;
                String value_ticket = row.getString(ticket);
                if (value_ticket != null && list_ticket.contains(value_ticket)) continue;
                if (!isFind && row.getInt(status) == 1) {
                    map.put("call",row.getString("no") + " " + row.getString("name"));
                    map.put(calltime,row.getString(calltime));
                    map.put(callmessage,row.getString(callmessage));
                    map.put(callvoice,row.getString(callvoice));
                    isFind = true;
                    list_ticket.add(value_ticket);
                } else if (wait_count < 2){
                    String wait_temp = row.getString("no") + " " + row.getString("name");
                    if (TextUtils.isEmpty(wait)){
                        wait = wait_temp;
                    }else{
                        wait += " " + wait_temp;
                    }
                    list_ticket.add(value_ticket);
                    wait_count++;
                }
            }

            map.put("waitno",wait);

            if(!map.containsKey("call")){
                map.put("call","");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @Override
    @Subscribe(threadMode = ThreadMode.MAIN,sticky=true)
    public void  update(DataEven even){
        JSONObject jsonObject = even.getData();
        if (jsonObject != null){
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

    @Subscribe(threadMode = ThreadMode.MAIN,sticky=true)
    public void  showCallText(ShowCallEvent even){
        String text = even.getCallText();
        if (text != null && tv_call_text!= null){
            tv_call_text.setText(text);
        }
    }
}
