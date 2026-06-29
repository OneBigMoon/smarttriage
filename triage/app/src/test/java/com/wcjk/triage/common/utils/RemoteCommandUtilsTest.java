package com.wcjk.triage.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoteCommandUtilsTest {
    @Test
    public void getReceiveMessageNamesKnownCommands() {
        assertEquals("收到远程命令：重启",
                RemoteCommandUtils.getReceiveMessage(RemoteCommandUtils.CMD_RESTART));
        assertEquals("收到远程命令：开机",
                RemoteCommandUtils.getReceiveMessage(RemoteCommandUtils.CMD_ON));
        assertEquals("收到远程命令：关机",
                RemoteCommandUtils.getReceiveMessage(RemoteCommandUtils.CMD_OFF));
    }

    @Test
    public void getResultMessageIncludesFailureReason() {
        assertEquals("远程命令关机执行失败：ZCAPI 息屏失败",
                RemoteCommandUtils.getResultMessage(RemoteCommandUtils.CMD_OFF,
                        false, "ZCAPI 息屏失败"));
    }

    @Test
    public void getResultMessageIncludesSuccessReason() {
        assertEquals("远程命令开机执行成功：ZCAPI 已调用 LCD/HDMI 亮屏",
                RemoteCommandUtils.getResultMessage(RemoteCommandUtils.CMD_ON,
                        true, "ZCAPI 已调用 LCD/HDMI 亮屏"));
    }

    @Test
    public void getCommandNameFallsBackToRawCommand() {
        assertEquals("收到远程命令：unknown",
                RemoteCommandUtils.getReceiveMessage("unknown"));
    }
}
