package com.wcjk.triage.common.utils;

public class RemoteCommandUtils {
    public static final String CMD_RESTART = "restart";
    public static final String CMD_ON = "on";
    public static final String CMD_OFF = "off";
    public static final String CMD_UPGRADE = "upgrade";
    public static final String CMD_UPLOAD_LOG = "uploadlog";
    public static final String CMD_CLEAR_DATA = "cleardata";

    private RemoteCommandUtils() {
    }

    public static String getCommandName(String cmd) {
        if (isEmpty(cmd)) {
            return "未知";
        }
        if (CMD_RESTART.equals(cmd)) {
            return "重启";
        }
        if (CMD_ON.equals(cmd)) {
            return "开机";
        }
        if (CMD_OFF.equals(cmd)) {
            return "关机";
        }
        if (CMD_UPGRADE.equals(cmd)) {
            return "升级";
        }
        if (CMD_UPLOAD_LOG.equals(cmd)) {
            return "上传日志";
        }
        if (CMD_CLEAR_DATA.equals(cmd)) {
            return "清屏";
        }
        return cmd;
    }

    public static String getReceiveMessage(String cmd) {
        return "收到远程命令：" + getCommandName(cmd);
    }

    public static String getResultMessage(String cmd, boolean success, String reason) {
        String message = "远程命令" + getCommandName(cmd) + (success ? "执行成功" : "执行失败");
        if (!isEmpty(reason)) {
            message += "：" + reason;
        }
        return message;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }
}
