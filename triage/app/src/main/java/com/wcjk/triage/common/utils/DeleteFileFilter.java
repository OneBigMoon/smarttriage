package com.wcjk.triage.common.utils;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by hyc on 2019/4/20
 */

public class DeleteFileFilter implements FilenameFilter {

    private boolean isPrefix;
    private String mRegEx;// 前缀或后缀规则

    public DeleteFileFilter(boolean isPrefix, @NonNull String regEx) {
        this.isPrefix = isPrefix;
        this.mRegEx = regEx;
    }

    @Override
    public boolean accept(File file, String s) {
        return isPrefix ? s.startsWith(mRegEx) : s.endsWith(mRegEx);
    }
}

