package com.wcjk.triage.fragment;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by hyc on 2019/1/25
 */
public class FragmentTestFirst extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());

    @Override
    public int setLayoutId() {
        return R.layout.f_drug_first;
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
            public int getLayoutId(int viewType) {
                return R.layout.item_test_first;
            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                for (String name: data.keySet()){
                    holder.setText(name,data.get(name));
                    log.i("convert: " + name + ":" + data.get(name));
                }

                if (position == 0){
                    holder.setBackGround(R.id.cl,R.color.item_drug_first_dark);
                    holder.setBackGround(R.id.no_name,R.drawable.shape_gradient_light);
                    holder.setBackGround(R.id.winno,R.drawable.shape_gradient_light);
                }else if (position % 2 == 1){
                    holder.setBackGround(R.id.cl,R.color.item_drug_first_light);
                    holder.setBackGround(R.id.no_name,R.drawable.bg_drag_corner_light);
                    holder.setBackGround(R.id.winno,R.drawable.bg_drag_corner_light);
                }else{
                    holder.setBackGround(R.id.cl,R.color.item_drug_first_dark);
                    holder.setBackGround(R.id.no_name,R.drawable.bg_drag_corner_dark);
                    holder.setBackGround(R.id.winno,R.drawable.bg_drag_corner_dark);
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
                    JSONObject object =  jsonArray.getJSONObject(i);
                    Iterator it = object.keys();
                    Map<String,String> map = new HashMap<>();
                    while (it.hasNext()) {
                        String name = (String) it.next();
                        String value = object.getString(name);
                        map.put(name,value);
                    }
                    map.put("no_name",map.get("no") + "号  " + map.get("name"));
                    map.put("winname",map.get("winname").replace("窗口",""));
                    map.put(callmessage,"请"+map.get("no_name")+"到抽血窗口"+map.get("winname"));
                    list_temp.add(map);
                }

                if (list_temp != null && list_temp.size() > 0) {
                    sendCall(list_temp);
                }

                setNewList(list_temp);
                log.i("列表长度: " +list.size());
            } catch (JSONException e) {
                e.printStackTrace();
                log.e("json error:" + e.getMessage());
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
                if (name.equals("patiens")) {
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
