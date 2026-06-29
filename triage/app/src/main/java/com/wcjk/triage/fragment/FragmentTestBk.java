package com.wcjk.triage.fragment;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.wcjk.triage.R;
import com.wcjk.triage.common.recyclerview.GridSpacingItemDecoration;
import com.wcjk.triage.common.recyclerview.QuickAdapter;
import com.wcjk.triage.common.recyclerview.SpacesItemDecoration;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.event.DataEven;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by hyc on 2019/1/25
 */
public class FragmentTestBk extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());

    private String wins = "wins";

    private String callno = "callno";
    private String winno = "winno";


    @Override
    public int setLayoutId() {
        return R.layout.f_test;
    }

    @Override
    protected int setShowArray() {
        return R.array.drawbloodtriage;
    }

    @Override
    protected int setCallTimes() {
        return 2;
    }
    @Override
    protected int setNum_play() {
        return 6;
    }

    @Override
    public void initView(){
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
                    return R.layout.test_item_light;
                }else{
                    return R.layout.test_item_dark;
                }
            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                String waitno = "";
                String patientname = "";
                for(int i = 0;i<= 4;i++){
                    holder.setText("no"+i,"");
                }
                for (String name: data.keySet()){
                    holder.setText(name,data.get(name));
                }
            }
        };

        lv.setAdapter(mAdapter);

    }

    public void analysis(JSONArray jsonArray){
        if (jsonArray != null ) {
            try {
//                list.clear();
                List<Map<String,String>> list_temp = new ArrayList<>();
                for (int i = 0;i <jsonArray.length();i++) {
                    Map<String,String> map = new HashMap<>();
                    JSONObject object =  jsonArray.getJSONObject(i);
                    String winno_value = object.getString("name");
                    if (!TextUtils.isEmpty(winno_value)){
                        winno_value = winno_value.replace("抽血窗口","");
                        map.put(winno,winno_value);
                    }
                    try {
                        String callno_value = object.getString(callno);
                        if (!TextUtils.isEmpty(callno_value)){
                            callno_value = callno_value.replace("号","");
                            map.put(callno,callno_value);
                        }
                    }catch (Exception e){
                        log.e("json error:" + e.getMessage());
                    }

                    try {
                        String callvoice_value = object.getString(callvoice);
                        if (!TextUtils.isEmpty(callvoice_value)){
                            map.put(callvoice,callvoice_value);
                        }
                    }catch (Exception e){
                        log.e("json error:" + e.getMessage());
                    }

                    try {
                        JSONArray patients = object.getJSONArray("patients");
                        if (patients != null) {
                            for (int j = 0; j < patients.length(); j++) {
                                JSONObject patient = patients.getJSONObject(j);
                                String no = patient.getString("no");
                                no = no.replace("号","");
                                map.put("no"+j,no);
                                if(map.get(callno) != null && map.get(callno).equals(no)){
                                    try {
                                        map.put(calltime,patient.getString("tickettime"));
                                    }catch (Exception e){
                                        log.e("json error:" + e.getMessage());
                                    }
                                }
                            }
                        }
                    }catch (Exception e){
                        log.e("json error:" + e.getMessage());
                    }

                    list_temp.add(map);
                }

                if (list_temp != null && list_temp.size() > 0) {
                    Collections.sort(list_temp, new Comparator<Map<String, String>>() {
                        @Override
                        public int compare(Map<String, String> o1, Map<String, String> o2) {
                            return Collator.getInstance(Locale.CHINESE).compare(o1.get(winno),o2.get(winno));
                        }
                    });
                    sendCall(list_temp);
                }

                setNewList(list_temp);
                log.i("列表长度: " +list.size());
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
                if (name.equals(wins)) {
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

}
