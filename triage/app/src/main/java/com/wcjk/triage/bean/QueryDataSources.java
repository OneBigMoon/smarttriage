package com.wcjk.triage.bean;

import java.util.List;

/**
 * Created by hyc on 2018/8/17
 */
public class QueryDataSources {
    /**
     * errcode : 0
     * result : [{"_id":"5b6ec0c6faaad51c8993d5fc","name":"数据源1"},{"_id":"5b6ec0cffaaad51c8993d602","name":"数据源2"},{"_id":"5b6ec3c9faaad51c8993d7eb","name":"数据源3"},{"_id":"5b6ec3ccfaaad51c8993d7ee","name":"数据源4"}]
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
         * _id : 5b6ec0c6faaad51c8993d5fc
         * name : 数据源1
         */

        private String _id;
        private String name;

        public String get_id() {
            return _id;
        }

        public void set_id(String _id) {
            this._id = _id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
