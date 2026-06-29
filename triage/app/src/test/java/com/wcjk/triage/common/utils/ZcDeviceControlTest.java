package com.wcjk.triage.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZcDeviceControlTest {
    @Test
    public void normalizeMacAddressReturnsUpperCaseAddress() {
        assertEquals("AA:BB:CC:DD:EE:FF",
                ZcDeviceControl.normalizeMacAddress("aa:bb:cc:dd:ee:ff"));
    }

    @Test
    public void isUsableMacAddressRejectsEmptyAndAndroidDefault() {
        assertFalse(ZcDeviceControl.isUsableMacAddress(null));
        assertFalse(ZcDeviceControl.isUsableMacAddress(""));
        assertFalse(ZcDeviceControl.isUsableMacAddress("02:00:00:00:00:00"));
        assertTrue(ZcDeviceControl.isUsableMacAddress("AA:BB:CC:DD:EE:FF"));
    }

    @Test
    public void toZcRotationMapsConfiguredDegrees() {
        assertEquals(0, ZcDeviceControl.toZcRotation("0"));
        assertEquals(90, ZcDeviceControl.toZcRotation("90"));
        assertEquals(180, ZcDeviceControl.toZcRotation("180"));
        assertEquals(270, ZcDeviceControl.toZcRotation("270"));
        assertEquals(-1, ZcDeviceControl.toZcRotation("auto"));
    }
}
