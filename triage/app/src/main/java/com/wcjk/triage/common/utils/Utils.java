package com.wcjk.triage.common.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class Utils {

    private static Log log = Log.getLogger(Utils.class);

    public static String getLocalIpAddress(Context cxt) {
        WifiManager wifiManager = (WifiManager) cxt.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            int ipAddress = wifiInfo.getIpAddress();
            if (ipAddress != 0) {
                return int2ip(ipAddress);
            }
        }
        try {
            String ipv4;
            ArrayList<NetworkInterface> nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : nilist) {
                ArrayList<InetAddress> ialist = Collections.list(ni.getInetAddresses());
                for (InetAddress address : ialist) {
                    if (!address.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4 = address.getHostAddress())) {
                        return ipv4;
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected())
            {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED)
                {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNetworkOnline(String url) {
        if (TextUtils.isEmpty(url)) return false;
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("ping -c 3 " + url);
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isTcpPortOpen(String host, String port, int timeoutMillis) {
        if (host == null || host.trim().length() == 0 || port == null || port.trim().length() == 0) {
            return false;
        }
        Socket socket = null;
        try {
            int portNum = Integer.parseInt(port.trim());
            if (portNum <= 0 || portNum > 65535) {
                return false;
            }
            socket = new Socket();
            socket.connect(new InetSocketAddress(host.trim(), portNum), timeoutMillis);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static String int2ip(long ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    public static boolean isTopActivy(Context context, String cmdName) {
        if (context == null || cmdName == null)
            return false;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
        String cmpNameTemp = null;
        if (null != runningTaskInfos) {
            cmpNameTemp = (runningTaskInfos.get(0).topActivity).getClassName();
        }
        if (null == cmpNameTemp)
            return false;
        return cmpNameTemp.endsWith(cmdName);
    }

    /**
     * 读取文本文件内容
     *
     * @param path
     *          文件路径
     * @return 文件内容
     */
    public static String readFileContent(String path) {
        File file = new File(path);
        if (file.isFile() == false)
            return null;
        try {
            String content = readInputStream2(new FileInputStream(file));
            return content;
        } catch (Exception e) {
            log.e(e);
            return null;
        }
    }

    // 上一个函数按字节流每次读取1024字节，有可能导致中文乱码，使用这个函数按字符流读取
    public static String readInputStream2(InputStream in) {
        try {
            StringBuffer stringBuffer = new StringBuffer();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"), 1024);
            char[] buffer = new char[1024];
            int len;
            while ((len = br.read(buffer)) != -1) {
                stringBuffer.append(buffer,0,len);
            }
            in.close();
            return stringBuffer.toString();
        } catch (Exception e) {
            log.e(e);
            return null;
        }
    }

    public static String getExtPath() {
        String ExtPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        return ExtPath;
    }

    //文件存储根目录
    public static String getFileRoot(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File external = context.getExternalFilesDir(null);
            if (external != null) {
                return external.getAbsolutePath();
            }
        }
        return context.getFilesDir().getAbsolutePath();
    }

    public static String getAppPath(){
        String dpsPath =getExtPath() + File.separator + "triage";
        File file = new File(dpsPath);
        if (file.isFile())
            file.delete();
        if (!file.exists())
            file.mkdirs();
        return dpsPath;
    }

    public static String getAppPath(String name) {
        String dpsPath = getAppPath() + File.separator + name;
        return dpsPath;
    }

    public static String getCfgPath() {
        String logPath = getAppPath() + File.separator + "cfg";
        File file = new File(logPath);
        if (file.isFile())
            file.delete();
        if (!file.exists())
            file.mkdirs();
        return logPath;
    }

    public static String getCfgPath(String name) {
        String logPath = getCfgPath() + File.separator + name;
        return logPath;
    }

    public static String getLogPath() {
        String logPath = getAppPath() + File.separator + "log";
        File file = new File(logPath);
        if (file.isFile())
            file.delete();
        if (!file.exists())
            file.mkdirs();
        return logPath;
    }

    public static String getLogPath(String name) {
        String logPath = getLogPath() + File.separator + name;
        return logPath;
    }

    /**
     * 获取MAC地址
     *
     * @param context
     * @return
     */
    public static String getMacAddress(Context context) {
        String zcMac = ZcDeviceControl.getInstance().getDeviceMac(context);
        if (ZcDeviceControl.isUsableMacAddress(zcMac)) {
            return ZcDeviceControl.normalizeMacAddress(zcMac);
        }
        String mac = "02:00:00:00:00:00";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mac = getMacDefault(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mac = getMacAddress();
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            mac = getMacFromHardware();
        }
        return mac;
    }

    /**
     * Android  6.0 之前（不包括6.0）
     * 必须的权限  <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
     * @param context
     * @return
     */
    private static String getMacDefault(Context context) {
        String mac = "02:00:00:00:00:00";
        if (context == null) {
            return mac;
        }

        WifiManager wifi = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
            return mac;
        }
        WifiInfo info = null;
        try {
            info = wifi.getConnectionInfo();
        } catch (Exception e) {
        }
        if (info == null) {
            return null;
        }
        mac = info.getMacAddress();
        if (!TextUtils.isEmpty(mac)) {
            mac = mac.toUpperCase(Locale.ENGLISH);
        }
        return mac;
    }

    /**
     * Android 6.0（包括） - Android 7.0（不包括）
     * @return
     */
    private static String getMacAddress() {
        String WifiAddress = "02:00:00:00:00:00";
        try {
            WifiAddress = new BufferedReader(new FileReader(new File("/sys/class/net/wlan0/address"))).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return WifiAddress;
    }

    /**
     * 遍历循环所有的网络接口，找到接口是 wlan0
     * 必须的权限 <uses-permission android:name="android.permission.INTERNET" />
     * @return
     */
    private static String getMacFromHardware() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }
}
