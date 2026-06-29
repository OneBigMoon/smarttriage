package com.wcjk.triage.common.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppUtils {

    public static String getUrl(String ip, String path) {
        String url;
        if (ip.contains("http://")) {
            url = ip + path;
        } else {
            url = "http://" + ip + path;
        }
        return url;
    }

    public static String getUrl(String ip, String port, String path) {
        if (TextUtils.isEmpty(port)) {
            return getUrl(ip, path);
        }
        String url;
        if (ip.contains("http://")) {
            url = ip + ":" + port + path;
        } else {
            url = "http://" + ip + ":" + port + path;
        }
        return url;
    }

    public static String getIp(String url) {
        String ip = "";
        if (TextUtils.isEmpty(url)) {
            return ip;
        }
        String[] temp = url.split("//");
        if (temp.length >= 1) {
            String[] temp2 = temp[temp.length - 1].split(":");
            if (temp2.length >= 1) {
                ip = temp2[0];
            }
        }

        return ip;
    }

    public static String md5sum(String str) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        byte[] m = md5.digest();
//        StringBuilder sb = new StringBuilder();
//        for (byte aM : m) {
//            sb.append(aM);
//        }
//        return sb.toString();
        StringBuilder hex = new StringBuilder(m.length * 2);
        for (byte b : m) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    /**
     * 获取一个文件的md5值(可处理大文件)	 * @return md5 value
     */
    public static String getMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return new String(Hex.encodeHex(MD5.digest()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取软件版本号
     *
     * @param context
     * @return
     */
    public static int getVerCode(Context context) {
        int verCode = -1;
        try {
            //注意："com.example.try_downloadfile_progress"对应AndroidManifest.xml里的package="……"部分
            verCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return verCode;
    }

    /**
     * 获取版本名称
     *
     * @param context
     * @return
     */
    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return verName;
    }

    public static int compareVersion(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return -2;
        }
        int[] ver1 = getVersion(version1);
        int[] ver2 = getVersion(version2);
        if (ver1 == null || ver2 == null) {
            return -2;
        }
        int versonLength = Math.min(ver1.length, ver2.length);
        for (int i = 0; i < versonLength; i++) {
            if (ver1[i] < ver2[i]) {
                return -1;
            } else if (ver1[i] > ver2[i]) {
                return 1;
            }
        }
        return 0;
    }

    private static int[] getVersion(String version) {
        String[] strVer = version.split("\\.");
        if (strVer == null || strVer.length <= 0) {
            System.out.println("version error: " + version);
            return null;
        }
        int[] ver = new int[strVer.length];
        for (int i = 0; i < strVer.length; i++) {
            try {
                ver[i] = Integer.parseInt(strVer[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return ver;
    }

    public static String getStringByJSONObject(JSONObject object, String key) {
        String value = "";
        if (object != null && key != null) {
            try {
                value = object.getString(key);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                return value;
            }
        }
        return value;
    }


    /*
     * pm命令可以通过adb在shell中执行，同样，我们可以通过代码来执行
     */
    public static String execCommand(String... command) {

        Process process = null;
        InputStream errIs = null;
        InputStream inIs = null;
        String result = "";

        try {
            process = new ProcessBuilder().command(command).start();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = -1;

            errIs = process.getErrorStream();
            while ((read = errIs.read()) != -1) {
                baos.write(read);
            }

            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }

            result = new String(baos.toByteArray());
            inIs.close();
            errIs.close();
            process.destroy();
        } catch (IOException e) {
            result = e.getMessage();
        }
        return result;
    }
}
