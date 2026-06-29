package com.wcjk.triage.fragment;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wcjk.triage.R;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.event.DataEven;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by hyc on 2019/11/3
 * 超声
 */
public class FragmentSecondSplit extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());
    private LinearLayout ll;

    private String status = "status";
    private String ticket = "ticket";
    private String brxmfull = "brxmfull";


    @Override
    public int setLayoutId() {
        return R.layout.second_bc;
    }

    @Override
    protected int setCallTimes() {
        return 0;
    }

    @Override
    protected int setShowArray() {
        return R.array.secondarytriagesplit;
    }

    @Override
    protected int setNum_play() {
        return 2;
    }

    @Override
    protected void initView() {
        ll = (LinearLayout) view.findViewById(R.id.ll);
        loadView(1);
    }

    private void loadView(int num) {
        if (ll == null) return;
        if (num == 1) {
            View item = View.inflate(getActivity(), R.layout.second_split_item1, null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ll.removeAllViews();
            ll.addView(item, lp);
        } else if (num == 2) {
            View item2_1 = View.inflate(getActivity(), R.layout.second_split_item2, null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.weight = 1;
            ll.removeAllViews();
            ll.addView(item2_1, lp);
            View item2_2 = View.inflate(getActivity(), R.layout.second_split_item2, null);
            ll.addView(item2_2, lp);
        } else {
            ll.removeAllViews();
        }
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void update(DataEven even) {
        try {
            switch (even.getType()) {
                case DataEven.TYPE_OTHER:
                    log.i("fragment recv date");
                    JSONObject object = even.getData();
                    JSONArray queue = object.getJSONArray("queues");
                    if (queue != null && ll != null) {
                        if (queue.length() != ll.getChildCount()) {
                            loadView(queue.length());
                        } else {
                            bindClear();
                        }
                        bind(queue);
                    }

                    break;

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void bindClear() {
        if (ll.getChildCount() <= 0) return;
        for (int j = 0; j < ll.getChildCount(); j++) {
            View queue_view = ll.getChildAt(j);
            TextView tv = (TextView) queue_view.findViewWithTag("clinicname");
            if (tv != null) {
                tv.setText("");
            }
//            tv = (TextView) queue_view.findViewWithTag("ticket1");
//            if (tv != null) {
//                tv.setText("");
//            }
            tv = (TextView) queue_view.findViewWithTag("ticket2");
            if (tv != null) {
                tv.setText("");
            }
            tv = (TextView) queue_view.findViewWithTag("ticket3");
            if (tv != null) {
                tv.setText("");
            }
//            tv = (TextView) queue_view.findViewWithTag("brxmfull1");
//            if (tv != null) {
//                tv.setText("");
//            }
            tv = (TextView) queue_view.findViewWithTag("brxmfull2");
            if (tv != null) {
                tv.setText("");
            }
            tv = (TextView) queue_view.findViewWithTag("brxmfull3");
            if (tv != null) {
                tv.setText("");
            }
        }
    }


    private void bind(JSONArray queue) {
        if (queue == null && queue.length() <= 0 && ll.getChildCount() <= 0) return;

        for (int j = 0; j < ll.getChildCount(); j++) {
            JSONObject item = null;
            try {
                View queue_view = ll.getChildAt(j);
                item = queue.getJSONObject(j);
                if (item == null) break;
                Iterator it = item.keys();
                while (it.hasNext()) {
                    String name = (String) it.next();
                    if (name.equals("patients")) {
                        JSONArray array = item.getJSONArray(name);
                        boolean isFind = false;
                        int index = 2;
                        Map map = new HashMap();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject row = array.getJSONObject(i);
                            if (row == null) continue;
                            String value_ticket = row.getString(ticket);
                            if (value_ticket != null && map.containsKey(value_ticket)) continue;
                            if (!isFind && row.getInt(status) == 1) {
                                bindDate(queue_view, row, "ticket1", ticket);
                                bindDate(queue_view, row, "brxmfull1", brxmfull);
                                isFind = true;
                                map.put(value_ticket, status);

                            } else if (index < 4) {
                                bindDate(queue_view, row, ticket + index, ticket);
                                bindDate(queue_view, row, brxmfull + index, brxmfull);
                                index++;
                                map.put(value_ticket, status);
                            }

                        }
                    } else if (name.equals("offtime")) {
//                        String value = item.getString(name);
//                        value = value == null ? "" : value;
//                        EventBus.getDefault().post(new Event(Event.TYPE_CLEAR,value));
                    } else {
                        bindDate(queue_view, item, name);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean bindDate(View item, JSONObject object, String name) {
        if (item == null || object == null || name == null) return false;
        try {
            String value = object.getString(name);
            if (name.equals("clinicname")) {
                String name_doctor = object.getString("doctorname");
                if (!TextUtils.isEmpty(name_doctor)) {
                    value = value + name_doctor;
                }
            }
            log.i("bindDate: " + name + ":" + value);
            TextView tv = (TextView) item.findViewWithTag(name);
            if (tv != null) {
                tv.setText(value);
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean bindDate(View item, JSONObject object, String tag, String name) {
        if (item == null || object == null || name == null) return false;

        try {
            String value = object.getString(name);
            if (value == null) return false;
            if (name.equals(brxmfull)) {
                if (value.length() >= 2) {
                    value = value.replaceAll("([\\d\\D]{1})([\\d\\D]{1})(.*)", "$1*$3");//value.replace("/^(.).+(.)$/g", "$1*$2");
                }
            }
            log.i("bindDate: tag is " + tag + "," + name + ":" + value);
            TextView tv = (TextView) item.findViewWithTag(tag);
            if (tv != null) {
                tv.setText(value);
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
