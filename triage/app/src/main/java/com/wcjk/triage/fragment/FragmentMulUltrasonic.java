package com.wcjk.triage.fragment;

import android.graphics.Color;
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
 * Created by hyc on 2018/7/27
 * 超声/CT/核磁共振多源
 * callno带有急诊那行需要标红显示
 * 翻页时间为12s
 * 一页显示8间诊室
 */
public class FragmentMulUltrasonic extends FragmentBase {
    public int index_wait_play = 0;
    public int index_pass_play = 0;
    protected QuickAdapter mwaitAdapter;
    protected QuickAdapter passAdapter;
    protected Map<String, List<Map<String, String>>> waitMap = new HashMap<String, List<Map<String, String>>>();
    protected Map<String, List<Map<String, String>>> queueMap = new HashMap<String, List<Map<String, String>>>();
    protected Map<String, List<Map<String, String>>> passMap = new HashMap<String, List<Map<String, String>>>();
    protected List<Map<String, String>> waitlist = new ArrayList<>();
    protected List<Map<String, String>> passlist = new ArrayList<>();

    private Log log = Log.getLogger(this.getClass());
    private TextView tv_call_text;
    private RecyclerView lv2;
    private RecyclerView passRv;
    private String callno = "callno";
    private String officename = "officename";
    @Override
    protected void handlerNextPage() {
        super.handlerNextPage();
        handerWaitNextPage();
        handerPassNextPage();
    }

    private void handerWaitNextPage() {
        if (waitlist.size() <= 0) return;
        index_wait_play += setNum_play_wait();
        if (index_wait_play >= waitlist.size()) {
            index_wait_play = 0;
        }
        if (lv2 != null) {
            if (index_wait_play == 0) {
                lv2.scrollToPosition(0);
            } else if (index_wait_play + setNum_play_wait() - 1 >= waitlist.size()) {
                lv2.scrollToPosition(waitlist.size() - 1);
            } else {
                lv2.scrollToPosition(index_wait_play + setNum_play_wait() - 1);
            }
        }
        log.i("等候队列长度: " + waitlist.size() + "，当前等候队列播放位置: " + index_wait_play);
    }

    private void handerPassNextPage() {
        if (passlist.size() <= 0) return;
        index_pass_play += setNum_play_pass();
        if (index_pass_play >= passlist.size()) {
            index_pass_play = 0;
        }
        if (passRv != null) {
            if (index_pass_play == 0) {
                passRv.scrollToPosition(0);
            } else if (index_pass_play + setNum_play_pass() - 1 >= passlist.size()) {
                passRv.scrollToPosition(passlist.size() - 1);
            } else {
                passRv.scrollToPosition(index_pass_play + setNum_play_pass() - 1);
            }
        }
        log.i("过号队列长度: " + passlist.size() + "，当前过号队列播放位置: " + index_pass_play);

    }

    @Override
    public int setLayoutId() {
        return R.layout.mul_ultrasonic;
    }

    @Override
    protected int setShowArray() {
        return R.array.fristarytriageultrasonic;
    }

    @Override
    protected int setCallTimes() {
        return 2;
    }

    @Override
    protected int setNum_play() {
        return 8;
    }

    protected int setNum_play_wait() {
        return 2 * setNum_play();
    }

    protected int setNum_play_pass() {
        return 2 * setNum_play();
    }

    @Override
    public void initView() {
        delay = 12 * 1000;
        tv_call_text = view.findViewById(R.id.tv_call_text);
        if (tv_call_text != null) {
            tv_call_text.setVisibility(View.VISIBLE);
        }

        initCallAdapter();
        initWaitAdapter();
        initPassAdapter();
    }

    private void initCallAdapter() {
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
                    return R.layout.mul_ultrasonic_item_light;
                } else {
                    return R.layout.mul_ultrasonic_item_dark;
                }
            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                String callnoValue = data.get("callno");
                boolean isEmergency = false;
                if (callnoValue != null && callnoValue.contains("急诊")) {
                    isEmergency = true;
                }
                for (String name : data.keySet()) {
                    if (data.get(name) == null || data.get(name).equals("null")) {
                        holder.setText(name, "");
                        log.i("convert: " + name + ":" + data.get(name));
                    } else {
                        TextView textView = holder.getView(name);
                        if (textView != null) {
                            if (isEmergency) {
                                textView.setTextColor(Color.parseColor("#FF0000"));
                            } else {
                                textView.setTextColor(Color.parseColor("#006085"));
                            }
                            textView.setText(data.get(name));
                        }
                        log.i("convert: " + name + ":" + data.get(name));
                    }

                }
            }
        };

        lv.setAdapter(mAdapter);
    }

    private void initWaitAdapter() {
        lv2 = (RecyclerView) view.findViewById(R.id.lv2);
        mwaitAdapter = new QuickAdapter<Map<String, String>>(waitlist) {
            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getLayoutId(int viewType) {
                return R.layout.mul_ultrasonic_wait_item;

            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                for (String name : data.keySet()) {
                    if (data.get(name).equals("null")) {
                        holder.setText(name, "");
                        log.i("convert: " + name + ":" + data.get(name));
                    } else {
                        holder.setText(name, data.get(name));
                        log.i("convert: " + name + ":" + data.get(name));
                    }

                }
            }
        };
        GridLayoutManager gridLayoutManager2 = new GridLayoutManager(getActivity(), setNum_play_wait());
        gridLayoutManager2.setOrientation(LinearLayoutManager.HORIZONTAL);
        lv2.setLayoutManager(gridLayoutManager2);

        lv2.setAdapter(mwaitAdapter);
    }

    private void initPassAdapter() {
        passRv = (RecyclerView) view.findViewById(R.id.rv_pass);
        passAdapter = new QuickAdapter<Map<String, String>>(passlist) {
            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getLayoutId(int viewType) {
                return R.layout.mul_ultrasonic_pass_item;

            }

            @Override
            public void convert(VH holder, Map<String, String> data, int position) {
                for (String name : data.keySet()) {
                    if (data.get(name).equals("null")) {
                        holder.setText(name, "");
                        log.i("convert: " + name + ":" + data.get(name));
                    } else {
                        holder.setText(name, data.get(name));
                        log.i("convert: " + name + ":" + data.get(name));
                    }

                }
            }
        };
        GridLayoutManager gridLayoutManager2 = new GridLayoutManager(getActivity(), setNum_play_pass());
        gridLayoutManager2.setOrientation(LinearLayoutManager.HORIZONTAL);
        passRv.setLayoutManager(gridLayoutManager2);

        passRv.setAdapter(passAdapter);
    }

    public void analysis(JSONArray jsonArray) {
        if (jsonArray != null && jsonArray.length() > 0) {
            try {
//                list.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject queque = jsonArray.getJSONObject(i);
                    JSONArray patients = queque.getJSONArray("patients");
                    String queuename = queque.getString("queuename");
                    List<Map<String, String>> list_call = new ArrayList<>();
                    List<Map<String, String>> list_wait = new ArrayList<>();
                    List<Map<String, String>> list_pass = new ArrayList<>();

                    for (int j = 0; j < patients.length(); j++) {
                        JSONObject patient = patients.getJSONObject(j);
                        Iterator it = patient.keys();
                        Map<String, String> map = new HashMap<>();
                        while (it.hasNext()) {
                            String name = (String) it.next();
                            String value = patient.getString(name);
                            map.put(name, value == null ? "" : value);
                        }
                        if (map.get("status").equals("4")) {
                            //正在就诊
                            map.put("call", map.get("callno") + "-" + map.get("patientname"));
                            map.put(officename, queuename + "-" + map.get(officename));
                            list_call.add(map);
                        } else {
                            //等候就诊
                            String callTimeValue = map.get(calltime);
                            log.i("callTimeValue: " + callTimeValue);

                            if (callTimeValue == null || callTimeValue.equals("") || callTimeValue.equals("null")) {
                                map.put("call", queuename + "-" + map.get("callno") + "-" + map.get("patientname"));
                                list_wait.add(map);
                                log.i("callTimeValue: " + callTimeValue);

                            } else {
                                //过号的病人
                                map.put("call", map.get("callno") + "-" + map.get("patientname"));
                                list_pass.add(map);
                                log.i("callTimeValue: " + callTimeValue);

                            }

                        }
                    }

                    Collections.sort(list_call, new Comparator<Map<String, String>>() {
                        @Override
                        public int compare(Map<String, String> o1, Map<String, String> o2) {
                            int result = Collator.getInstance(Locale.CHINESE).compare(o1.get(officename), o2.get(officename));
                            return result;
                        }
                    });
                    queueMap.put(queuename, list_call);
                    waitMap.put(queuename, list_wait);
                    passMap.put(queuename, list_pass);
                }

                List<Map<String, String>> list_call_all = new ArrayList<>();
                for (List<Map<String, String>> patients : queueMap.values()) {
                    list_call_all.addAll(patients);
                }
                sendCall(list_call_all);
                setNewList(list_call_all);
                mHandler.removeMessages(what_play_next_page);
                setNewWaitList();
                setNewPassList();
                mHandler.sendEmptyMessageDelayed(what_play_next_page, delay);

                log.i("正在就诊列表长度: " + list.size()
                        + " 等候就诊人数为：" + waitlist.size()
                        + " 过号人数为：" + passlist.size());
            } catch (JSONException e) {
                e.printStackTrace();
                log.e("解析数据错误：" + e.getMessage());
            }
        } else {
            queueMap.clear();
            waitMap.clear();
            passMap.clear();
            setNewList(null);
            setNewWaitList();
            setNewPassList();
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
                        log.e("解析数据错误：" + e.getMessage());
                    }
                    analysis(array);
                } else {
                    bindDate(jsonObject, name);
                }
            }
        }
        isFirst = false;
    }

    protected void setNewWaitList() {
        List<Map<String, String>> list_wait_temp = new ArrayList<>();
        for (List<Map<String, String>> patients : waitMap.values()) {
            list_wait_temp.addAll(patients);
        }
        Collections.sort(list_wait_temp, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                String callno1 = o1.get(callno);
                String callno2 = o2.get(callno);
                if (callno1 == null || callno2 == null) return 0;
                if (callno1.length() < callno2.length()) {
                    return -1;
                } else if (callno1.length() > callno2.length()) {
                    return 1;
                } else {
                    return callno1.compareTo(callno2);
                }
            }
        });

        index_wait_play = 0;
        waitlist.clear();

        if (list_wait_temp.size() > 0) {
            for (int i = 0; i < list_wait_temp.size(); i += 2) {
                String call_left = list_wait_temp.get(i).get("call");
                String call_right = i + 1 < list_wait_temp.size() ? list_wait_temp.get(i + 1).get("call") : "";
                Map<String, String> item = new HashMap<String, String>();
                item.put("call_left", call_left + "  " + call_right);
//                item.put("call_right", call_right);
                waitlist.add(item);
            }
        }
        if (mwaitAdapter == null) return;
        mwaitAdapter.notifyDataSetChanged();

        if (waitlist.size() > 0) {
            lv2.scrollToPosition(0);
        }
    }

    protected void setNewPassList() {
        List<Map<String, String>> list_pass_temp = new ArrayList<>();
        for (List<Map<String, String>> patients : passMap.values()) {
            list_pass_temp.addAll(patients);
        }
        index_pass_play = 0;
        passlist.clear();
        passlist.addAll(list_pass_temp);

        if (passAdapter == null) return;
        passAdapter.notifyDataSetChanged();

        if (passlist.size() > 0) {
            passRv.scrollToPosition(0);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void showCallText(ShowCallEvent even) {
        String text = even.getCallText();
        if (text != null && tv_call_text != null) {
            tv_call_text.setText(text);
        }
    }
}
