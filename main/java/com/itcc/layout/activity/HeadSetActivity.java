package com.itcc.layout.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class HeadSetActivity extends AppCompatActivity implements HeadSetReciever.HeadSetCallBack {

    private static final String TAG = HeadSetActivity.class.getSimpleName();

    private HeadSetReciever headSetReciever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerHeadSetReceiver();
    }

    private void registerHeadSetReceiver() {
        IntentFilter headSetFilter = new IntentFilter();

        //android.media.AudioManager.ACTION_HEADSET_PLUG 有线耳机
        headSetFilter.addAction(Intent.ACTION_HEADSET_PLUG);

        // 之前用过BluetoothHeadSet,但是对于蓝牙耳机的连接状态不准
        //BluetoothDevice 下面的是对蓝牙设备的连接断开做监听
        headSetFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        headSetFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        headSetReciever = new HeadSetReciever(this);
        registerReceiver(headSetReciever, headSetFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterHeadSetReceiver();
    }

    private void unregisterHeadSetReceiver() {
        unregisterReceiver(headSetReciever);
    }

    @Override
    public void headSetConnectedStateCallBack(int state) {
        if (1 == state) {
            //耳机断开
        } else if (2 == state) {
            //耳机连上
        }
    }
}
