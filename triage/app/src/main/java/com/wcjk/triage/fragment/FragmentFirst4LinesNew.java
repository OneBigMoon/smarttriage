package com.wcjk.triage.fragment;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
 * Created by hyc on 2019/4/22
 */
public class FragmentFirst4LinesNew extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());
    private TextView tv_call_text;

    private String doctorname = "doctorname";
    private String officename = "officename";

    @Override
    public int setLayoutId() {
        return R.layout.first1_new;
    }

    @Override
    protected int setShowArray() {
        return R.array.primarytriage;
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
    public void initView() {
        tv_call_text = view.findViewById(R.id.tv_call_text);
        if (tv_call_text != null) {
            tv_call_text.setVisibility(View.VISIBLE);
        }
        lv = (RecyclerView) view.findViewById(R.id.lv);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), setNum_play());
        gridLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        lv.setLayoutManager(gridLayoutManager);
        lv.addItemDecoration(new GridSpacingItemDecoration(setNum_play(), 2, false));
        lv.addItemDecoration(new SpacesItemDecoration(3));
        mAdapter = new QuickAdapter<Map<String, String>>(list) {
            @Override
            public int getItemViewType(int position) {
                if (position % 2 == 0) {
                    return 0;
                } else {
                    return 1;
                }
            }

            @Override
            public int getLayoutId(int viewType) {
                if (viewType == 0) {
                    return R.layout.first1_item_light_new;
                } else {
                    return R.layout.first1_item_dark_new;
                }
            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                for (String name : data.keySet()) {
                    if (data.get(name).equals("null")) {
                        holder.setText(name,"");
                    } else {
                        if ("waitno".equals(name)) {
                            String waitnoStr = data.get(name);
                            if (waitnoStr != null && waitnoStr.length() > 0) {
                                if (waitnoStr.contains("  ")) {
                                    StringBuffer sb = new StringBuffer();
                                    //"0009 朱思游游运  0010 陈思游  0018 黄培花  0014 李焕文  0011 陈文进  0016 刘永炜  0015 吕自福  0008 黄永惠  0012 肖淑娥";
                                    String[] waitNoArr = waitnoStr.split("  ");
                                    for (int i = 0; i < waitNoArr.length; i++) {
                                        String waitNo = waitNoArr[i];
                                        String[] waitArr = waitNo.split(" ");
                                        android.util.Log.i("convert", "waitNo:" + waitNo);
                                        android.util.Log.i("convert", "waitArr:" + waitArr.length);
                                        if (waitArr.length == 2) {
                                            sb.append(waitArr[0]);
                                            sb.append(" ");
                                            sb.append(convertName(waitArr[1]));

                                        } else {
                                            sb.append(waitNo);
                                        }
                                        if (i < waitNoArr.length - 1) {
                                            sb.append("  ");
                                        }
                                    }

                                    holder.setText(name, sb.toString());

                                }else {
                                    String[] waitNoArr = waitnoStr.split(" ");
                                    if (waitNoArr.length == 1) {
                                        holder.setText(name, waitnoStr);
                                    } else {
                                        StringBuffer sb = new StringBuffer();
                                        for (int i = 0; i < waitNoArr.length; i++) {
                                            android.util.Log.i("convert", "waitNoArr:" + waitNoArr[i]);
                                            if (i % 2 == 1) {
                                                sb.append(convertName(waitNoArr[i]));
                                                if (i < waitNoArr.length - 1) {
                                                    sb.append("  ");
                                                }
                                            } else {
                                                sb.append(waitNoArr[i]);
                                                sb.append(" ");
                                            }
                                        }
                                        holder.setText(name, sb.toString());
                                    }
                                }
                            }else{
                                holder.setText(name, "");
                            }
                        } else {
                            holder.setText(name, data.get(name));
                        }
                        log.i("convert: " + name + ":" + data.get(name));
                    }

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
        String text = even.getCallText();
        if (text != null && tv_call_text != null) {
            tv_call_text.setText(text);
        }
    }

    public static String convertName(String name) {
        if (name == null || name.length() == 0) {
            return "";
        }

        if (name.length() == 1) {
            return name;
        } else if (name.length() == 2 || name.length() == 3) {
            StringBuffer sb = new StringBuffer(name);
            sb.replace(1, 2, "*");
            return sb.toString();
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append(name.substring(0, 1));

            for (int i = 0; i < name.length() - 2; i++) {
                sb.append("*");
            }
            sb.append(name.substring(name.length() - 1, name.length()));
            return sb.toString();
        }
    }
}
