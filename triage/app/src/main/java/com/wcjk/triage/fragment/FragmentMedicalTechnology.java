package com.wcjk.triage.fragment;

import android.widget.TextView;

import com.wcjk.triage.R;
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
 * Created by hyc on 2018/7/27
 * 超声
 */
public class FragmentMedicalTechnology extends FragmentBase {
    private Log log = Log.getLogger(this.getClass());

    private String status = "status";
    private String ticket = "no";
//    private String brxmfull = "brxmfull";
    private String brxm = "name";

    @Override
    public int setLayoutId() {
        return R.layout.fragment_medicaltechnologytriage;
    }

    @Override
    protected int setCallTimes() {
        return 2;
    }

    @Override
    protected int setShowArray() {
        return R.array.medicaltechnologytriage;
    }

    @Override
    protected int setNum_play() {
        return 2;
    }

    @Override
    protected void initView() {

    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void update(DataEven even) {
        switch (even.getType()) {
            case DataEven.TYPE_OTHER:
                log.i("fragment recv date");
                JSONObject object = even.getData();
                bindClear();
                bind(object);
                break;

        }

        isFirst = false;
    }

    private void bindClear() {
        if (view == null) return;
        TextView tv = (TextView) view.findViewWithTag("patientcount");
        if (tv != null) {
            tv.setText("");
        }
        for (int i = 1; i <= 4; i++) {
            tv = (TextView) view.findViewWithTag(ticket + i);
            if (tv != null) {
                tv.setText("");
            }
            tv = (TextView) view.findViewWithTag(brxm + i);
            if (tv != null) {
                tv.setText("");
            }
        }

    }

    private void bind(JSONObject queue) {
        Iterator it = queue.keys();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.equals("patients")) {
                JSONArray array = null;
                try {
                    array = queue.getJSONArray(name);
                    boolean isFind = false;
                    int index = 2;
                    Map map = new HashMap();
                    List<String> list_ticket = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject row = array.getJSONObject(i);
                        if (row == null) continue;
                        //状态为2为诊结 不显示，过滤掉
                        if (row.getInt(status) == 2) continue;
                        String value_ticket = row.getString(ticket);
                        if (value_ticket != null && list_ticket.contains(value_ticket)) continue;
                        if (!isFind && row.getInt(status) == 1) {
                            bindDate(row, ticket+"1", ticket);
//                            bindDate(row, "brxmfull1", brxmfull);
                            bindDate(row, brxm+"1", brxm);
                            isFind = true;
                            list_ticket.add(value_ticket);

                            List<Map<String,String>> list_temp = new ArrayList<>();
                            Iterator it_row = row.keys();
                            while (it_row.hasNext()) {
                                String name_row = (String) it_row.next();
                                String value = row.getString(name_row);
                                map.put(name_row,value);
                            }
                            list_temp.add(map);
                            sendCall(list_temp);
                            list = list_temp;
                        } else if ( index < 5) {
                            list_ticket.add(value_ticket);
                            bindDate(row, ticket + index, ticket);
//                            bindDate(row, "brxmfull" + index, brxmfull);
                            bindDate(row, brxm + index, brxm);
                            index++;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                bindDate(queue, name);
            }
        }
    }

    private boolean bindDate(JSONObject object, String tag, String name) {
        try {
            String value = object.getString(name);
            value = value == null ? "" : value;
            log.i("bindDate: tag is " + tag + "," + name + ":" + value);
            TextView tv = (TextView) view.findViewWithTag(tag);
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
