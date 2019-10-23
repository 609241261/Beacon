package com.bluetooth.beacon;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.Identifier;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class MyAdapter extends BaseAdapter {
    public List<Beacon> mlist;
    private LayoutInflater mInflater;

    public MyAdapter(Context context , List<Beacon> list){
        mlist = list;
        mInflater = LayoutInflater.from(context);
    }

    //获取传入的数组大小
    @Override
    public int getCount() {
        return mlist.size();
    }

    //获取第N条数据
    @Override
    public Object getItem(int i) {
        return mlist.get(i);
    }

    //获取item id
    @Override
    public long getItemId(int i) {
        return i;
    }

    //主要方法
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = new ViewHolder();
        if(view == null){
            //首先为view绑定布局
            view = mInflater.inflate(R.layout.devices_item , null);
            viewHolder.name = (TextView) view.findViewById(R.id.bluetoothname);
            viewHolder.NamespaceID = (TextView) view.findViewById(R.id.NamespaceID);
            viewHolder.instanceId = (TextView) view.findViewById(R.id.instanceId);

            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) view.getTag();
        }
        Beacon beacon = mlist.get(i);
        viewHolder.name.setText("BeaconName:"+beacon.getBluetoothName());
        viewHolder.NamespaceID.setText("NamespaceID:"+beacon.getId1().toHexString());
        viewHolder.instanceId.setText("InstanceID:"+beacon.getId2().toHexString());

        return view;
    }

    class ViewHolder{
        private TextView name , NamespaceID , instanceId;
    }
}