package com.wcjk.triage.common.utils;

public class DeviceControlResult {
    private final boolean success;
    private final String message;

    private DeviceControlResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static DeviceControlResult success(String message) {
        return new DeviceControlResult(true, message);
    }

    public static DeviceControlResult failure(String message) {
        return new DeviceControlResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }
}
