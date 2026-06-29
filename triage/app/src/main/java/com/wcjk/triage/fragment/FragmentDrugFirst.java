package com.wcjk.triage.fragment;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.wcjk.triage.BuildConfig;
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
 * Created by hyc on 2019/1/25
 */
public class FragmentDrugFirst extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());
    private TextView tv_call_text;

    @Override
    public int setLayoutId() {
        return R.layout.f_drug_first;
    }

    @Override
    protected int setCallTimes() {
        return 1;
    }

    @Override
    protected int setNum_play() {
        return 5;
    }

    @Override
    protected int setShowArray() {
        return R.array.primarypharmacytriage;
    }

    @Override
    public void initView() {
        tv_call_text = view.findViewById(R.id.tv_call_text);
        tv_call_text.setVisibility(View.VISIBLE);
        lv = (RecyclerView) view.findViewById(R.id.lv);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), setNum_play());
        gridLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        lv.setLayoutManager(gridLayoutManager);
        lv.addItemDecoration(new GridSpacingItemDecoration(setNum_play(), 2, false));
        lv.addItemDecoration(new SpacesItemDecoration(3));


        mAdapter = new QuickAdapter<Map<String, String>>(list) {

            @Override
            public int getLayoutId(int viewType) {
                return R.layout.item_drug_first;
            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                for (String name : data.keySet()) {
                    log.i("convert: " + name + ":" + data.get(name));
                    holder.setText(name, data.get(name));
                }

                holder.setText("action", "  号窗口取药");

//                if (position == 0){
//                    holder.setBackGround(R.id.cl,R.color.item_drug_first_dark);
//                    holder.setBackGround(R.id.no_name,R.drawable.shape_gradient_light);
//                    holder.setBackGround(R.id.winno,R.drawable.shape_gradient_light);
//                }else
                if (position % 2 == 1) {
                    holder.setBackGround(R.id.cl, R.color.item_drug_first_light);
                    holder.setBackGround(R.id.no_name, R.drawable.bg_drag_corner_light);
                    holder.setBackGround(R.id.winno, R.drawable.bg_drag_corner_light);
                } else {
                    holder.setBackGround(R.id.cl, R.color.item_drug_first_dark);
                    holder.setBackGround(R.id.no_name, R.drawable.bg_drag_corner_dark);
                    holder.setBackGround(R.id.winno, R.drawable.bg_drag_corner_dark);
                }

            }
        };

        lv.setAdapter(mAdapter);

    }

    public void analysis(JSONArray jsonArray) {
        if (jsonArray != null) {
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
                    map.put("no_name", map.get("no") + "号  " + map.get("name"));
                    map.put("winname", map.get("winno"));
                    String no_namefull = map.get("no") + "号" +
                            (map.get("namefull") == null ? map.get("name") : map.get("namefull"));
                    map.put(callvoice, "请" + no_namefull + "到" + map.get("winname") + "号窗口取药");
                    map.put(callmessage, "请" + map.get("no_name") + "到" + map.get("winname") + "号窗口取药");
                    list_temp.add(map);
                }

                if (list_temp != null && list_temp.size() > 0) {
                    sendCall(list_temp);
                } else {
                    if (tv_call_text != null) {
                        tv_call_text.setText("");
                    }
                }
                setNewList(list_temp);
                log.i("列表长度: " + list.size());
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void showCallText(ShowCallEvent even) {
        String text = even.getCallText();
        if (text != null && tv_call_text != null) {
            tv_call_text.setText(text);
        }
    }

}
