package com.bluetooth.beacon;

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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LogWriter;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends Activity implements BeaconConsumer,RangeNotifier,View.OnClickListener {
    protected static final String TAG = "MonitoringActivity";


    private Button scan;
    private ListView list;

    BluetoothAdapter bluetoothAdapter;

    private BeaconManager beaconManager;
    List<Beacon> beaconList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        String mac;
        mac=getBluetoothMacAddress();
        String IMEI=getIMEI(this);
        String TelNum=getTelNumber(this);

        Log.d(TAG,"IMEI:"+IMEI+"   TelNum:"+TelNum+"    Mac:"+mac);

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
        list = (ListView) findViewById(R.id.list);

        scan.setOnClickListener(this);
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
        for (Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                Log.d(TAG, "I see a beacon transmitting namespace id: "+namespaceId+
                        " and instance id: "+instanceId+
                        " approximately "+beacon.getDistance()+" meters away.");
                //将设备加入列表数据中
                if (!beaconList.contains(beacon)) {
                    beaconList.add(beacon);
                    list.setAdapter(new MyAdapter(MainActivity.this, beaconList));
                }
            }
        }
        for (Beacon beacon:beaconList) {
            //将设备从列表数据中删除
            if(!beacons.contains(beacon)) {
                beaconList.remove(beacon);
                Log.d(TAG,"i delete a beacon.");
                list.setAdapter(new MyAdapter(MainActivity.this, beaconList));
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
    public Vector<beacon> differ(Vector<beacon> p0, Vector<beacon> p1){
        Vector<beacon> differ_p = p0;
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
    public Vector<area> classify(Vector<beacon> p){
        //以beacon1初始化一个区域r1
        area r1 = new area();
        r1.area_id = p.get(0).area_id;
        r1.beacon_set.add(p.get(0));

        //将r1加入区域集合R中
        Vector<area> R = new Vector<area>();
        R.add(r1);

        //遍历p中剩余beacon
        for(int i=1;i<p.size();i++){
            //新建临时region，并加入R中
            area temp_r = new area();
            temp_r.area_id = p.get(i).area_id;
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
    public boolean determine(area r,Vector<beacon> differ_p1,Vector<beacon> differ_p2,Vector<beacon> differ_p3){
        //若区域beacon小于3个，则认为不在该区域内
        if(r.beacon_set.capacity()<3){
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
