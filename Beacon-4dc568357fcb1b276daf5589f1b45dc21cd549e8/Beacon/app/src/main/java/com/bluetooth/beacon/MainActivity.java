package com.bluetooth.beacon;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.Identifier;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LogWriter;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.bluetooth.beacon.devicemdm;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends Activity implements BeaconConsumer,RangeNotifier,View.OnClickListener {
    protected static final String TAG = "MonitoringActivity";


    private Button scan;
    private Switch GPS;
    private Switch wifi;
    private Switch usb;
    private Switch Microphone;
    private ListView list;

    BluetoothAdapter bluetoothAdapter;

    private BeaconManager beaconManager;
    List<Beacon> beaconList = new ArrayList<>();

    Vector<my_beacon> t1 = new Vector<my_beacon>();
    Vector<my_beacon> t2 = new Vector<my_beacon>();
    Vector<my_beacon> t3 = new Vector<my_beacon>();
    Vector<my_beacon> t4 = new Vector<my_beacon>();

    public static final int DPM_ACTIVATION_REQUEST_CODE = 100;
    private ComponentName adminComponent;
    private DevicePolicyManager devicePolicyManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(getPackageName(),getPackageName() + ".DeviceAdministrator");

        if (!devicePolicyManager.isAdminActive(adminComponent)) {

            Intent activateDeviceAdmin = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            startActivityForResult(activateDeviceAdmin, DPM_ACTIVATION_REQUEST_CODE);

        }

        setContentView(R.layout.activity_main);
        initView();



        //获取手机基本信息
        String mac;
        mac=getBluetoothMacAddress();
        String IMEI=getIMEI(this);
        String TelNum=getTelNumber(this);

        Log.d(TAG,"IMEI:"+IMEI+"   TelNum:"+TelNum+"    Mac:"+mac);


        //获取蓝牙控制器
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        beaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
// Detect the main identifier (UID) frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
// Detect the telemetry (TLM) frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
// Detect the URL frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    private void initView() {
        scan = (Button) findViewById(R.id.scan);
        GPS = (Switch) findViewById(R.id.GPS);
        wifi = (Switch) findViewById(R.id.wifi);
        usb = (Switch) findViewById(R.id.usb);
        Microphone = (Switch) findViewById(R.id.Microphone);
        list = (ListView) findViewById(R.id.list);





        scan.setOnClickListener(this);
        //GPS管控
        GPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);  //定位控制器
                boolean gps_state = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);  //GPS状态

                if(isChecked){
                    Settings.Secure.putInt(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE, android.provider.Settings.Secure.LOCATION_MODE_OFF);
                }else {
                    Settings.Secure.putInt(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
                }
                Log.i(TAG, "gps :" + gps_state );
            }
        });

        //USB管控（控制USB调试功能）
        usb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Settings.Global.putInt(getApplicationContext().getContentResolver(),Settings.Global.ADB_ENABLED,1);
                }else {
                    Settings.Global.putInt(getApplicationContext().getContentResolver(),Settings.Global.ADB_ENABLED,0);
                }
            }
        });

        //WiFi管控
        wifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);  //WiFi控制器
                if(isChecked){
                    wifiManager.setWifiEnabled(true);
                }else {
                    wifiManager.setWifiEnabled(false);
                }
            }
        });

        //麦克风管控
        Microphone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                if(isChecked){
                    audioManager.setMicrophoneMute(true);
                }else {
                    audioManager.setMicrophoneMute(false);
                }
                Log.d(TAG,"MicrophoneMute is "+audioManager.isMicrophoneMute());
            }
        });
    }

    @Override
    public void onClick(View v) {
        //检测位置权限
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PERMISSION_GRANTED)
        {
            Toast.makeText(this, "系统检测到未开启位置权限,请开启", Toast.LENGTH_SHORT).show();
        }


        switch (v.getId()) {
            case R.id.scan:
                beaconManager.bind(this);

                break;
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }



        beaconManager.addRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

        t1 = (Vector<my_beacon>) t2.clone();       // 直接使用=不是值传递
        t2 = (Vector<my_beacon>) t3.clone();
        t3 = (Vector<my_beacon>) t4.clone();

        t4.clear();

        /*
        if(! (t1.isEmpty() | t2.isEmpty() | t3.isEmpty()) ){
            Log.d(TAG,"t1_beacon1_id:"+t1.get(0).beacon_id+"   t1_size:   " + t1.size()+
                    "   t2_beacon1_id:"+t2.get(0).beacon_id+"   t2_size:   " + t2.size()+
                    "   t3_beacon1_id:"+t3.get(0).beacon_id+"   t3_size:   " + t3.size());
        }
        */

        for (Beacon beacon: beacons) {
            try {
                if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                    // This is a Eddystone-UID frame
                    Identifier namespaceId = beacon.getId1();
                    Identifier instanceId = beacon.getId2();
                    double rssi = beacon.getRssi();
                /*Log.d(TAG, "I see a beacon transmitting namespace id: "+namespaceId+
                        " and instance id: "+instanceId+
                        " approximately "+beacon.getDistance()+" meters away.");*/
                    Log.d(TAG, "namespaceId: " + namespaceId + " instanceId: " + instanceId + " RSSI: " + rssi);
                    //将设备加入列表数据中
                    if (!beaconList.contains(beacon)) {
                        beaconList.add(beacon);
                        list.setAdapter(new MyAdapter(MainActivity.this, beaconList));

                        devicePolicyManager.setCameraDisabled(adminComponent, true);
                        Log.d(TAG, String.valueOf(devicePolicyManager.getCameraDisabled(adminComponent)));

                    }

                    //将设备加入t4中
                    my_beacon temp = new my_beacon();
                    temp.beacon_id = beacon.getId2().toHexString();
                    temp.area_id = beacon.getId2().toHexString(); //区域id待定

                    temp.rssi = beacon.getRssi();

                    t4.add(temp);
                }

            }catch (SecurityException securityException){
                Log.i("Device Administrator", "Error occurred while disabling/enabling camera - " + securityException.getMessage());
            }

        }


        /*for (Beacon beacon:beaconList) {
            //将设备从列表数据中删除
            if(!beacons.contains(beacon)) {
                beaconList.remove(beacon);
                Log.d(TAG,"i delete a beacon.");
                list.setAdapter(new MyAdapter(MainActivity.this, beaconList));
            }

        }*/

        Iterator<Beacon> iterator = beaconList.iterator();
        while (iterator.hasNext()){
            Beacon beacon = iterator.next();
            if (!beacons.contains(beacon)){
                iterator.remove();
            }
            list.setAdapter(new MyAdapter(MainActivity.this, beaconList));
        }

        if(beaconList.size() == 0){
            devicePolicyManager.setCameraDisabled(adminComponent, false);
            Log.d(TAG, String.valueOf(devicePolicyManager.getCameraDisabled(adminComponent)));
        }


        //计算差值
        Vector<my_beacon> differ_p1 = differ(t4,t1);
        Vector<my_beacon> differ_p2 = differ(t4,t2);
        Vector<my_beacon> differ_p3 = differ(t4,t3);

        //划分区域
        Vector<area> R = classify(t4);

        //分别判断当前位置是否属于各个区域
        if(R.size() == 0){
            Boolean A_empty = false;
            Log.d(TAG, "Can't distinguish" + A_empty);
        }else{
            Boolean[] A = new Boolean[R.size()];
            for(int i=0;i<R.size();i++){
                A[i] = determine(R.get(i),differ_p1,differ_p2,differ_p3);
                Log.d(TAG, i + " is " + A[i]);
            }
        }



    }

    //已过时
    private static String getNewMac(Context context) {
        try {
            String mMac =  Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return mMac;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static String getIMEI(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            String IMEI = tm.getImei();
            return IMEI;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static String getTelNumber(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            String TelNumber = tm.getLine1Number();
            return TelNumber;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //通过反射方法获取mac
    private String getBluetoothMacAddress() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothMacAddress = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            try {
                Field mServiceField = bluetoothAdapter.getClass().getDeclaredField("mService");
                mServiceField.setAccessible(true);

                Object btManagerService = mServiceField.get(bluetoothAdapter);

                if (btManagerService != null) {
                    Log.d(TAG,"000");
                    //Class btM = bluetoothAdapter.getClass();
                    //Method method = btM.getMethod("getAddress");
                    //bluetoothMacAddress = (String) method.invoke(bluetoothAdapter);
                    bluetoothMacAddress = (String) btManagerService.getClass().getMethod("getAddress").invoke(btManagerService);
                    Log.d(TAG,"ble:"+bluetoothMacAddress);
                }
            } catch (NoSuchFieldException e) {
                Log.d(TAG,"NoSuchFieldException");
            } catch (NoSuchMethodException e) {
                Log.d(TAG,"NoSuchMethodException");
            } catch (IllegalAccessException e) {
                Log.d(TAG,"IllegalAccessException");
            } catch (InvocationTargetException e) {
                Log.d(TAG,"InvocationTargetException");
            }
        } else {
            bluetoothMacAddress = bluetoothAdapter.getAddress();
        }
        return bluetoothMacAddress;
    }


    //计算模块
    public Vector<my_beacon> differ(Vector<my_beacon> p0, Vector<my_beacon> p1){
        Vector<my_beacon> differ_p = p0;
        for(int i=1;i<differ_p.size();i++){
            //与p1位置的所有beacond的rssi作差值。若某beacon在p1未出现，则认为其在p1的rssi为0
            int index = p1.indexOf(differ_p.get(i));
            if(index != -1){
                differ_p.get(i).rssi = differ_p.get(i).rssi - p1.get(index).rssi;
            }
        }
        //计算结束，返回differ-p
        return differ_p;
    }

    //分类模块
    public Vector<area> classify(Vector<my_beacon> p){

        if(p.isEmpty()){
            Vector<area> R_empty = new Vector<area>();
            return R_empty;
        }

        //以beacon1初始化一个区域r1
        area r1 = new area();
        r1.area_id = p.get(0).area_id;
        r1.beacon_set = new Vector<my_beacon>();
        r1.beacon_set.add(p.get(0));

        //将r1加入区域集合R中
        Vector<area> R = new Vector<area>();
        R.add(r1);

        //遍历p中剩余beacon
        for(int i=1;i<p.size();i++){
            //新建临时region，并加入R中
            area temp_r = new area();
            temp_r.area_id = p.get(i).area_id;
            temp_r.beacon_set = new Vector<my_beacon>();
            temp_r.beacon_set.add(p.get(i));
            R.add(temp_r);

            //判断临时region与加入之前的R是否重复
            for(int j=0;j<R.size()-1;j++){
                if(R.get(j).area_id == temp_r.area_id){
                    //如果重复，则将临时region中的beacon加入到已有的region中，并从R中删去刚刚加入到临时region;
                    //如果未重复，则保留刚刚加入的临时region
                    R.get(j).beacon_set.add(p.get(i));
                    R.remove(R.size());
                }
            }
        }
        //遍历完毕，输出分类结果R
        return R;
    }

    //符号函数
    public boolean sign (int num){
        if(num>0){
            return true;
        }else{
            return false;
        }
    }

    //判定模块
    public boolean determine(area r,Vector<my_beacon> differ_p1,Vector<my_beacon> differ_p2,Vector<my_beacon> differ_p3){
        //若区域beacon小于3个，则认为不在该区域内
        if(r.beacon_set.size()<3){
            return false;
        }

        int size = r.beacon_set.size();
        boolean[] X = new boolean[size];

        //对于区域内每一个beacon
        for(int i=0;i<size;i++){
            //每个路径对应的差值
            int index1 = differ_p1.indexOf(r.beacon_set.get(i));
            int index2 = differ_p2.indexOf(r.beacon_set.get(i));
            int index3 = differ_p3.indexOf(r.beacon_set.get(i));

            //该beacon的判定量
            X[i] = (sign(differ_p1.get(index1).rssi) ^ sign(differ_p2.get(index2).rssi))
                    |  (sign(differ_p3.get(index3).rssi) ^ sign(differ_p2.get(index2).rssi));
        }

        //判定
        boolean K = X[0];
        for(int i=1;i<X.length;i++){
            K = K & X[i];
        }

        if(K)
            return true;
        else
            return false;
    }
}
