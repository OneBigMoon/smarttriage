package com.wcjk.triage.fragment;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.wcjk.triage.BuildConfig;
import com.wcjk.triage.R;
import com.wcjk.triage.common.recyclerview.GridSpacingItemDecoration;
import com.wcjk.triage.common.recyclerview.QuickAdapter;
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
public class FragmentDrugSecond extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());

    private int span = 2;

    public int getLine() {
        return BuildConfig.DrugSecondLineNum;
    }

    @Override
    public int setLayoutId() {
        return R.layout.f_drug;
    }

    @Override
    protected int setCallTimes() {
        return 0;
    }

    @Override
    protected int setShowArray() {
        return R.array.secondarypharmacytriage;
    }

    @Override
    protected int setNum_play() {
        return span * getLine();
    }

    @Override
    public void initView(){
        lv = (RecyclerView) view.findViewById(R.id.lv);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),span);
        gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        lv.setLayoutManager(gridLayoutManager);
        lv.addItemDecoration(new GridSpacingItemDecoration(span, 2, false));

        mAdapter = new QuickAdapter<Map<String,String>>(list) {
            @Override
            public int getLayoutId(int viewType) {
                return R.layout.item_drug_second;
            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                LinearLayout ll = holder.getView(R.id.ll);

                ViewGroup.LayoutParams params = ll.getLayoutParams();
                params.height = lv.getHeight () /getLine();
                ll.setLayoutParams(params);
                for (String name: data.keySet()){
                    holder.setText(name,data.get(name));
                    log.i("convert: " + name + ":" + data.get(name));
                }

                if (getLine() == 1){
                    if (position % 2 == 0){
                        ll.setBackgroundResource(R.color.item_drug_second_light);
                    }else{
                        ll.setBackgroundResource(R.color.item_drug_second_dark);
                    }
                }else {
                    if (position % 4 == 1 || position % 4 == 2) {
                        ll.setBackgroundResource(R.color.item_drug_second_light);
                    } else {
                        ll.setBackgroundResource(R.color.item_drug_second_dark);
                    }
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
                    list_temp.add(map);
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
    }

}
