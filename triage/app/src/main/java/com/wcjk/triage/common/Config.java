package com.wcjk.triage.common;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 16-4-11.
 */
public class Config {
    private static Context mContext;

    public static void setContext(Context context) {
        mContext = context;
    }

    private static SharedPreferences getPref() {
        return mContext.getSharedPreferences("Config", mContext.MODE_PRIVATE);
    }

    public static synchronized String getConfig(String key, String defaultValue) {
        SharedPreferences sp = getPref();
        return sp.getString(key, defaultValue);
    }

    public static synchronized int getConfig(String key, int defaultValue) {
        SharedPreferences sp = getPref();
        return sp.getInt(key, defaultValue);
    }

    public static synchronized void setConfig(String key, String value) {
        SharedPreferences sp = getPref();
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static synchronized boolean getConfig(String key, boolean defaultValue) {
        SharedPreferences sp = getPref();
        return sp.getBoolean(key, defaultValue);
    }

    public static synchronized void setConfig(String key, int value) {
        SharedPreferences sp = getPref();
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static synchronized void setConfig(String key, boolean value) {
        SharedPreferences sp = getPref();
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static synchronized void setConfig(String key, List<Map<String, String>> datas) {
        JSONArray mJsonArray = new JSONArray();
        for (int i = 0; i < datas.size(); i++) {
            Map<String, String> itemMap = datas.get(i);
            Iterator<Map.Entry<String, String>> iterator = itemMap.entrySet().iterator();
            JSONObject object = new JSONObject();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                try {
                    object.put(entry.getKey(), entry.getValue());
                } catch (JSONException e) {

                }
            }
            mJsonArray.put(object);
        }

        SharedPreferences sp = getPref();
        SharedPreferences.Editor editor = sp.edit();;
        editor.putString(key, mJsonArray.toString());
        editor.commit();
    }
    public static synchronized List<Map<String, String>> getConfig(String key) {
        List<Map<String, String>> datas = new ArrayList<Map<String, String>>();
        SharedPreferences sp = getPref();
        String result = sp.getString(key, "");
        try {
            JSONArray array = new JSONArray(result);
            for (int i = 0; i < array.length(); i++) {
                JSONObject itemObject = array.getJSONObject(i);
                Map<String, String> itemMap = new HashMap<String, String>();
                JSONArray names = itemObject.names();
                if (names != null) {
                    for (int j = 0; j < names.length(); j++) {
                        String name = names.getString(j);
                        String value = itemObject.getString(name);
                        itemMap.put(name, value);
                    }
                }
                datas.add(itemMap);
            }
        } catch (JSONException e) {

        }
        return datas;
    }

    public static synchronized void setConfigHashMap(String key, HashMap<String,String> datas) {
        Iterator<HashMap.Entry<String, String>> iterator = datas.entrySet().iterator();
        JSONObject object = new JSONObject();
        while (iterator.hasNext()) {
            HashMap.Entry<String, String> entry = iterator.next();
            try {
                object.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {

            }
        }

        SharedPreferences sp = getPref();
        SharedPreferences.Editor editor = sp.edit();;
        editor.putString(key, object.toString());
        editor.commit();
    }
    public static synchronized HashMap<String, String> getConfigHashMap(String key) {
        HashMap<String, String> datas = new HashMap<String, String>();
        SharedPreferences sp = getPref();
        String result = sp.getString(key, "");
        try {
            JSONObject itemObject = new JSONObject(result);
            JSONArray names = itemObject.names();
            if (names != null) {
                for (int j = 0; j < names.length(); j++) {
                    String name = names.getString(j);
                    String value = itemObject.getString(name);
                    datas.put(name, value);
                }
            }
        } catch (JSONException e) {

        }
        return datas;
    }
}

