package com.wcjk.triage.socketio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Demo {
    public static void main(String[] args) throws JSONException {
        JSONObject jsonObject = new  JSONObject("content:{\"patiens\":[{\"calltime\":\"09:17:09\",\"winno\":103,\"callvoice\":\"请3015号王德进到3\",\"winname\":\"3\",\"no\":\"3015号\",\"name\":\"王*进\"}]}");;
        if (jsonObject != null){
            Iterator it = jsonObject.keys();
            while (it.hasNext()) {
                String name = (String) it.next();
                if (name.equals("patiens")) {
                    JSONArray jsonArray = null;
                    try {
                        jsonArray = jsonObject.getJSONArray(name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (jsonArray != null ) {
                        try {
//                list.clear();
                            List<Map<String,String>> list_temp = new ArrayList<>();
                            for (int i = 0;i <jsonArray.length();i++) {
                                JSONObject object =  jsonArray.getJSONObject(i);
                                Iterator it2 = object.keys();
                                Map<String,String> map = new HashMap<>();
                                while (it2.hasNext()) {
                                    String name1 = (String) it2.next();
                                    String value = object.getString(name1);
                                    map.put(name1,value);
                                }
                                map.put("no_name",map.get("no") + "号  " + map.get("name"));
                                map.put("winname",map.get("winname").replace("窗口",""));
                                list_temp.add(map);
                            }



                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                }
            }
        }
    }
}
