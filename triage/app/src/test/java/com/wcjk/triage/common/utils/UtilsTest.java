package com.wcjk.triage.common.utils;

import org.junit.Test;

import java.net.ServerSocket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {
    @Test
    public void isTcpPortOpenReturnsTrueForListeningPort() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        try {
            assertTrue(Utils.isTcpPortOpen("127.0.0.1",
                    String.valueOf(serverSocket.getLocalPort()), 1000));
        } finally {
            serverSocket.close();
        }
    }

    @Test
    public void isTcpPortOpenReturnsFalseForBlankHostOrPort() {
        assertFalse(Utils.isTcpPortOpen("", "7016", 1000));
        assertFalse(Utils.isTcpPortOpen("127.0.0.1", "", 1000));
    }
}
