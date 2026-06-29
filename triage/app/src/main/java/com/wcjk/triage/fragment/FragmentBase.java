package com.wcjk.triage.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wcjk.triage.common.Config;
import com.wcjk.triage.common.recyclerview.QuickAdapter;
import com.wcjk.triage.common.ttsiflytek.TtsHelper;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.event.ClearEvent;
import com.wcjk.triage.event.DataEven;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.wcjk.triage.global.Global.Test;
import static com.wcjk.triage.global.Global.TestKey;

/**
 * Created by hyc on 2018/7/27
 */
public abstract class FragmentBase extends Fragment {
    private Date lastDate = new Date();
    private static Set<String> callCashSet = new HashSet<>();
    private Log log = Log.getLogger(this.getClass());
    private Unbinder unbinder;
    protected View view;
    public CountDownTimer countDownTimer;

    protected String callvoice = "callvoice";
    protected String callmessage = "callmessage";
    protected String calltime = "calltime";
    protected boolean isFirst = true;

    //设置布局
    protected abstract int setLayoutId();

    //初始化view
    protected abstract void initView();

    //设置演示数据
    protected abstract int setShowArray();

    //设置呼叫次数
    protected abstract int setCallTimes();

    //设置一页显示个数
    protected abstract int setNum_play();

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    protected abstract void update(DataEven even);

    //翻页特别处理
    protected void handlerNextPage(){

    }


    public int index_play = 0;
    public int delay = 5 * 1000;
    public final int what_play_next_page = 1;
    protected List<Map<String, String>> list = new ArrayList<>();

    protected RecyclerView lv;
    protected QuickAdapter mAdapter;

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case what_play_next_page:
                    mHandler.removeMessages(what_play_next_page);
                    mHandler.sendEmptyMessageDelayed(what_play_next_page, delay);
                    handlerNextPage();
                    if (list == null) return;
                    index_play += setNum_play();
                    if (index_play >= list.size()) {
                        index_play = 0;
                    }
                    if (lv != null) {
                        if (index_play == 0) {
                            lv.scrollToPosition(0);
                        } else if (index_play + setNum_play() - 1 >= list.size()) {
                            lv.scrollToPosition(list.size() - 1);
                        } else {
                            lv.scrollToPosition(index_play + setNum_play() - 1);
                        }
                    }
                    log.i("正在就诊队列长度: " + list.size() + "，当前正在就诊队列播放位置: " + index_play);
                    break;
            }
        }
    };

    protected void setNewList(List<Map<String, String>> list_new) {
        mHandler.removeMessages(what_play_next_page);
        index_play = 0;
        list.removeAll(list);

        if (list_new != null && list_new.size() > 0) {
            list.addAll(list_new);
        }
        if (mAdapter == null) return;
        mAdapter.notifyDataSetChanged();
        if (list.size() > 0) {
            lv.scrollToPosition(0);
        }
        if (list.size() > setNum_play()) {
            mHandler.sendEmptyMessageDelayed(what_play_next_page, delay);
        }
    }
    protected void setNewList1(List<Map<String, String>> list_new) {
        mHandler.removeMessages(what_play_next_page);
        index_play = 0;
        list.removeAll(list);

        if (list_new != null && list_new.size() > 0) {
            list.addAll(list_new);
        }
        if (list.size() > 0) {
            lv.scrollToPosition(0);
        }
        if (list.size() > setNum_play()) {
            mHandler.sendEmptyMessageDelayed(what_play_next_page, delay);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.i("onCreate ");

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        log.i("onCreateView ");
        view = inflater.inflate(setLayoutId(), container, false);
        unbinder = ButterKnife.bind(this, view);
        initView();
        return view;
    }


    @Override
    public void onStart() {
        log.i("onStart ");
        super.onStart();
    }

    @Override
    public void onStop() {
        log.i("onStop ");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        log.i("onDestroyView ");
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        log.i("onDestroy ");
        if (mHandler != null) {
            mHandler.removeMessages(what_play_next_page);
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        log.i("onResume ");
        super.onResume();
        EventBus.getDefault().register(this);
        if (Config.getConfig(TestKey, Test).equals("true")) {
            initCountDownTimeer();
            countDownTimer.start();
        } else {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
        }
    }

    @Override
    public void onPause() {
        log.i("onPause ");
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    private int count_send_data = 0;

    public void initCountDownTimeer() {
        if (countDownTimer == null) {
            final String[] show_array = getResources().getStringArray(setShowArray());
            countDownTimer = new CountDownTimer(2000 * 1000, 20 * 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    try {
                        if (show_array != null && show_array.length > 0) {

                            JSONObject jsonObject = new JSONObject(show_array[count_send_data % show_array.length]);
                            DataEven dataEven = new DataEven(DataEven.TYPE_OTHER, jsonObject);
                            EventBus.getDefault().removeStickyEvent(DataEven.class);
                            EventBus.getDefault().postSticky(dataEven);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    count_send_data++;
                }

                @Override
                public void onFinish() {
                }
            };
        }
    }

    protected boolean bindDate(JSONObject object, String name) {
        try {
            String value = object.getString(name);
            value = value == null ? "" : value;
            log.i("bindDate: " + name + ":" + value);
            TextView tv = (TextView) view.findViewWithTag(name);
            if (tv != null) {
                tv.setText(value);
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected void sendCall(List<Map<String, String>> list_temp) {
        Date now = new Date();
        if (now.getDay() - lastDate.getDay() >0) {
            log.i("时间过了一天，清空: callCashSet" );
            lastDate = now;
            callCashSet.clear();
        }
        if (setCallTimes() <= 0) {
            log.i("呼叫次数设置为: " + setCallTimes());
            return;
        }

        if (list_temp == null || list_temp.size() <= 0) return;
        if (list_temp.size() > 0) {
         /*   String callText = "";
            for (Map<String,String> map_temp:list_temp){
                String callvoice_new = map_temp.get(callvoice);
                if (!TextUtils.isEmpty(callvoice_new)){
                    String calltime_new = map_temp.get(calltime);
                    for (Map<String,String> map:list){
                        String callvoice_last = map.get(callvoice);
                        String calltime_last = map.get(calltime);
                        if (callvoice_new.equals(callvoice_last)){
                            if (
                                (TextUtils.isEmpty(calltime_new ) && TextUtils.isEmpty(calltime_last )) ||
                                    (!TextUtils.isEmpty(calltime_new) && !TextUtils.isEmpty(calltime_last ) && calltime_new.compareTo(calltime_last) <= 0)){
                                log.i("已经叫过: " +callvoice_new+"，叫号时间：" +(calltime_new==null?"":calltime_new));
                                callvoice_new = "";
                                break;
                            }else{
                                log.i("重叫: " +callvoice_new + ",叫号时间：" +(calltime_new==null?"":calltime_new)+",上一次叫号时间：" +(calltime_last==null?"":calltime_last));
                            }
                        }
                    }

                    if (!isFirst && !TextUtils.isEmpty(callvoice_new)) {
                        log.i("添加到叫号队列: " +callvoice_new + ",呼叫次数为：" +  setCallTimes());
                        for (int i = 0;i < setCallTimes();i++) {
                            String callmessage_new = map_temp.get(callmessage);
                            TtsHelper.getInstance().ttsSpeak(callvoice_new,callmessage_new);

                        }
                    }
                }

            }*/

            for (Map<String, String> map_temp : list_temp) {
                String callvoice_new = map_temp.get(callvoice);
                String calltime_new = map_temp.get(calltime);
                if (!TextUtils.isEmpty(callvoice_new) && !TextUtils.isEmpty(calltime_new)) {
                    if (!isFirst && !callCashSet.contains(callvoice_new + "_" + calltime_new)) {
                        callCashSet.add(callvoice_new + "_" + calltime_new);
                        log.i("添加到叫号队列: " + callvoice_new + ",呼叫次数为：" + setCallTimes());
                        for (int i = 0; i < setCallTimes(); i++) {
                            String callmessage_new = map_temp.get(callmessage);
                            TtsHelper.getInstance().ttsSpeak(callvoice_new, callmessage_new);
                        }
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void clear(ClearEvent even) {
        final String[] show_array = getResources().getStringArray(setShowArray());
        try {
            if (show_array != null && show_array.length > 0) {
                JSONObject jsonObject = new JSONObject(show_array[show_array.length - 1]);
                jsonObject.put("clearData", true);
                DataEven dataEven = new DataEven(DataEven.TYPE_OTHER, jsonObject);
                EventBus.getDefault().removeStickyEvent(DataEven.class);
                EventBus.getDefault().postSticky(dataEven);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


}
