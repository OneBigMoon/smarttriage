package com.wcjk.triage.global;


import android.os.Environment;

import com.wcjk.triage.App;
import com.wcjk.triage.BuildConfig;

public class Global {
    public static boolean debug = true;
    public static int RESULT_OK = 0;

    public static final String ServerIpKey = "ServerIp";
    public static final String ServerIp = BuildConfig.SERVER;
    public static final String ServerPortKey = "ServerPort";
    public static final String ServerPort = "";

    public static final String ClientNoKey = "no";
    public static final String ClientNo = "";
    public static final String NameKey = "name";
    public static final String Name = "";
    public static final String StyleKKey = "style";
    public static final String StyleK = "primarytriage";//"secondarytriageultrasonic";//"secondarytriage";
    public static final String SourceIdKey = "datasource";
    public static final String SourceId = "";
    public static final String RotateKey = "rotation";
    public static final String Rotate = "auto";

    public static final String VolumeKey = "volume";
    public static final String Volume = "9";
    public static final String PowerOnKey = "powerontime";
    public static final String PowerOn = "00:00:00";
    public static final String PowerOffKey = "powerofftime";
    public static final String PowerOff = "23:59:59";

    public static final String TestKey = "test";
    public static final String Test = "false";

    public static final String HorselampKey = "horselamp";
    public static final String Horselamp = "感谢您耐心等待。";
    public static final String TimeslampKey = "timestamp";
    public static final String TitleKey = "title";
    public static final String Title = "";
//    public static final String Title = "西药房80";

    public static final String TemplateKey = "template";
    public static final String TemplateConfig = "template_config";
    public static final String TemplateKeyStyle = "template_style_key";
    public static final String TemplateLogoKey = "template_logo";
    public static final String TemplatePackageKey = "template_package";
    public static final String TemplateManifestKey = "template_manifest";
    public static final String TemplateKindKey = "template_kind";
    public static final String TemplateVersionKey = "template_version";

    public static final String offtimeKey = "offtime";
    public static final String offtime = "。";

    public static final String ParamsKey = "Params";
    public static final String Params = "{\"tip\":\"1.流感期间少外出\n2.勤洗手\n3.\n4.\n5.\n6.\n7.\n8.n9\",\"tel\":\"121323232323(陈医生) 121323232323(陈医生)\",\"qr1\":\"中国人民医院\",\"qr2\":\"中国打发打发\"}";

    public final static String APP_ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + App.getAppContext().getPackageName();
    public final static String DOWNLOAD_DIR = "/downlaod/";

}
