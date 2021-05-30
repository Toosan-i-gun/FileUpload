package com.itcc.layout.activity;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class HeadSetReciever extends BroadcastReceiver {

    private HeadSetCallBack callBack;
    private int headSetConnectedState = 0;

    public HeadSetReciever(HeadSetCallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
            //有线耳机
            if (intent.hasExtra("state")) {
                if (0 == intent.getIntExtra("state", 0)) {
                    Toast.makeText(context, "有线耳机已断开", Toast.LENGTH_LONG).show();
                    headSetConnectedState = 1;
                } else if (1 == intent.getIntExtra("state", 0)
                            || 2 == intent.getIntExtra("state", 0)) {
                    Toast.makeText(context, "有线耳机已连接", Toast.LENGTH_LONG).show();
                    headSetConnectedState = 2;
                }
            }
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothClass bluetoothClass = device.getBluetoothClass();
            int deviceId = bluetoothClass.getMajorDeviceClass();
            if (BluetoothClass.Device.Major.AUDIO_VIDEO == deviceId) {
                Toast.makeText(context, "蓝牙耳机已连接", Toast.LENGTH_LONG).show();
                headSetConnectedState = 2;
            }
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothClass bluetoothClass = device.getBluetoothClass();
            int deviceId = bluetoothClass.getMajorDeviceClass();
            if (BluetoothClass.Device.Major.AUDIO_VIDEO == deviceId) {
                Toast.makeText(context, "蓝牙耳机已断开", Toast.LENGTH_LONG).show();
                headSetConnectedState = 1;
            }
        }

        if (null != callBack) {
            callBack.headSetConnectedStateCallBack(headSetConnectedState);
        }
    }

    public interface HeadSetCallBack {
        void headSetConnectedStateCallBack(int state);
    }
}
