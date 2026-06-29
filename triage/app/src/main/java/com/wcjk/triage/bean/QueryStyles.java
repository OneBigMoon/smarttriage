package com.wcjk.triage.bean;

import java.util.List;

/**
 * Created by hyc on 2018/8/17
 */
public class QueryStyles {

    /**
     * errcode : 0
     * result : [{"key":"primarytriage","name":"诊室一级分诊"},{"key":"secondarytriage","name":"诊室二级分诊"},{"key":"secondarytriagevertical","name":"诊室二级分诊（竖）"},{"key":"primarytriagedetailed","name":"诊室一级分诊详细"}]
     */

    private int errcode;
    private List<ResultBean> result;

    public int getErrcode() {
        return errcode;
    }

    public void setErrcode(int errcode) {
        this.errcode = errcode;
    }

    public List<ResultBean> getResult() {
        return result;
    }

    public void setResult(List<ResultBean> result) {
        this.result = result;
    }

    public static class ResultBean {
        /**
         * key : primarytriage
         * name : 诊室一级分诊
         */

        private String key;
        private String name;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
