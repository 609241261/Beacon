package com.bluetooth.beacon;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;

public class devicemdm {
    //必须设置为系统app，才可以强制更改定位服务和移动数据服务

    //判定定位服务是否开启
    public static boolean GPSstates(final Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
// 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）  
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
// 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）  
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }
    //开启定位服务
    public static void openGPS(Context context) {
        Intent GPSIntent = new Intent();
        GPSIntent.setClassName("com.android.settings",
                "com.android.settings.widget.SettingsAppWidgetProvider");
        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
        GPSIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(context, 0, GPSIntent, 0).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            System.out.println("设置错误: " + e.toString());
        }
    }
    //关闭定位服务
    public static void closeGPS(Context context) {
        Intent GPSIntent = new Intent();
        GPSIntent.setClassName("com.android.settings","com.android.settings.widget.SettingsAppWidgetProvider");
        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
        GPSIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(context, 0, GPSIntent, 1).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            System.out.println("设置错误: " + e.toString());
        }
    }


    //判定移动数据服务状态
    public boolean getMobileDataState(Context context) {
        TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method getDataEnabled = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
            if (null != getDataEnabled) {
                return (Boolean) getDataEnabled.invoke(telephonyService);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
   //设置移动数据服务是否开启
    public void setMobileDataState(Context context, boolean enabled) {
        TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled",boolean.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyService, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("移动数据设置错误: " + e.toString());
        }
    }



    //获取wifi开关状态
    public static int WiFistatus(WifiManager wifiManager){
        int status = wifiManager.getWifiState();
        //  status == WifiManager.WIFI_STATE_ENABLED  开启状态
        return status;
    }
    //wifiManager.setWifiEnabled(true);开启wifi
    //wifiManager.setWifiEnabled(false);关闭wifi

    //usb管控，需要系统权限
    public static void UsbSetting(boolean enable) {
        String TAG = "UsbSetting";
        String allow_com = "setprop persist.sys.usb.config mtp,adb";
        String disallow_com = "setprop persist.sys.usb.config none";
        try {
            if(enable == true){
                Log.i(TAG, "Command : " + allow_com);
                Runtime.getRuntime().exec(allow_com);
            }else {
                Log.i(TAG, "Command : " + disallow_com);
                Runtime.getRuntime().exec(disallow_com);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //麦克风管控
    public static void MicrophoneSetting(boolean enable, Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        try{
            if(enable == true){
                audioManager.setMicrophoneMute(true);
            }else {
                audioManager.setMicrophoneMute(false);
            }
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //

}
