package com.example.smartmask;


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class BleControlActivity extends AppCompatActivity {
    private UUID[] uuid;
    private BluetoothAdapter mBluetoothAdapter;
    private Button bleEnable_btn, bleBack_btn, scanLeDevice_btn,bleConnect_btn,bleDisconnect_btn,stop_fanByBle_btn,stop_motorByBle_btn;
    private ListView mListView;
    private TextView bleStatus,msgTextView_ble;
    private boolean mScanning = true;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothDevice targetDevice;
    private BluetoothGatt mBluetoothGatt;
    List<BluetoothGattService> serviceList;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_DISCONNECTED = 3;
    private static final int WRITE_SUCCESS = 4;
    private static final int REQUEST_ENABLE_BT = 1000;
    private static final long SCAN_PERIOD = 10000;

    private static boolean SCAN = true;

    private BluetoothGattCharacteristic notifyCharacteristic,writeCharacteristic;

    private class MHandler extends Handler {
        private WeakReference<Activity> mActivity;

        MHandler(Activity activity) {
            mActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REQUEST_ENABLE_BT:
                    bleEnable_btn.setText("蓝牙已开启");
                    bleEnable_btn.setClickable(false);
                    break;
                case STATE_CONNECTED:
                    bleEnable_btn.setText("蓝牙服务已连接");
                    break;
                case STATE_DISCONNECTED:
                    bleEnable_btn.setText("蓝牙服务已断开");
                    break;
                case WRITE_SUCCESS:
                    bleEnable_btn.setText("写入指令成功");
                    break;
            }
        }
    }


    private final Handler mHandler = new BleControlActivity.MHandler(BleControlActivity.this);

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK){
                    Toast.makeText(this, "蓝牙已经开启", Toast.LENGTH_SHORT).show();
                    bleEnable_btn.setClickable(false);
                    bleEnable_btn.setText("蓝牙已开启");
                    bleStatus.setText("蓝牙已开启");
                }
                else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "没有蓝牙权限", Toast.LENGTH_SHORT).show();
                    finish();
                } break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_control);
        bleEnable_btn = findViewById(R.id.bleEnable);
        bleBack_btn = findViewById(R.id.bleBack_btn);
        scanLeDevice_btn = findViewById(R.id.scanLeDevice);
        bleConnect_btn = findViewById(R.id.bleConnect);
        bleDisconnect_btn = findViewById(R.id.bleDisconnect);
        stop_fanByBle_btn = findViewById(R.id.stop_fanByBle);
        stop_motorByBle_btn = findViewById(R.id.stop_motorByBle);
        mListView = findViewById(R.id.mListView);
        bleStatus = findViewById(R.id.bleStatus);
        msgTextView_ble = findViewById(R.id.msgTextView_ble);
        //初始化
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mListView.setAdapter(mLeDeviceListAdapter);

        bleEnable_btn.setOnClickListener(new OnClick());
        bleBack_btn.setOnClickListener(new OnClick());
        scanLeDevice_btn.setOnClickListener(new OnClick());
        bleConnect_btn.setOnClickListener(new OnClick());
        bleDisconnect_btn.setOnClickListener(new OnClick());
        stop_fanByBle_btn.setOnClickListener(new OnClick());
        stop_motorByBle_btn.setOnClickListener(new OnClick());

        //请求权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
        //判断蓝牙是否开启
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if(mBluetoothAdapter.isEnabled()) {
                        bleEnable_btn.setClickable(false);
                        bleEnable_btn.setText("蓝牙已开启");
                        bleStatus.setText("蓝牙已开启");
                    }
                }catch (Exception e){
                    System.out.println("------------------蓝牙未开启------------------");
                }
            }
        }).start();
    }




    class OnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                    //开启蓝牙
                case R.id.bleEnable:
                    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                    break;
                    //返回
                case R.id.bleBack_btn:
                    Intent intent = new Intent(BleControlActivity.this, MainActivity.class);
                    startActivity(intent);
                    break;
                    //开始扫描
                case R.id.scanLeDevice:
                    scanLeDevice(SCAN);
                    if(mScanning){
                        scanLeDevice_btn.setText("停止扫描");
                        SCAN = false;
                    }else{
                        scanLeDevice_btn.setText("开始扫描");
                        mScanning = true;
                        SCAN = true;
                    }
                    break;
                case R.id.bleConnect:
                    bleConnect(targetDevice);
                    break;
                case R.id.bleDisconnect:
                    bleDisconnect();
                    break;
                case R.id.stop_motorByBle:
                    sendMessageBle("stopMotor");
                    break;
                case R.id.stop_fanByBle:
                    sendMessageBle("stopFan");
                    break;
                default:break;
            }
        }
    }

    //扫描设备
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //延迟关闭扫描
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(leScanCallback);
                    bleStatus.setText("已停止扫描");
                    scanLeDevice_btn.setText("开始扫描");
                    SCAN = true;
                }
            }, SCAN_PERIOD);

            mScanning = true;
            System.out.println("开始扫描设备");
            mBluetoothAdapter.startLeScan(leScanCallback);
            bleStatus.setText("正在扫描……");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
        //蓝牙连接
    private void bleConnect(BluetoothDevice device){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //先等待停止搜索蓝牙
                try{
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                if(targetDevice != null){
                    //连接设备
                    mBluetoothGatt = targetDevice.connectGatt(BleControlActivity.this,false,gattCallback);
                    //开启扫描服务
                    mBluetoothGatt.discoverServices();
                    BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString("FFE0"));
                    notifyCharacteristic = service.getCharacteristic(UUID.fromString("FFE1"));
                    writeCharacteristic =  service.getCharacteristic(UUID.fromString("FFE2"));
                    //开启监听
                    mBluetoothGatt.setCharacteristicNotification(notifyCharacteristic, true);
                    BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                }else{
                    Toast.makeText(BleControlActivity.this,"未连接",Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }
        //断开连接
    private  void bleDisconnect(){
        if(targetDevice != null){
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }else{
            Toast.makeText(BleControlActivity.this,"未连接",Toast.LENGTH_SHORT).show();
        }
    }
    void sendMessageBle(String value){
        if(writeCharacteristic != null){
            writeCharacteristic.setValue(value);
            mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            System.out.println("--------蓝牙发送数据: "+value);
        }
    }

    // 回调 Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(BleControlActivity.this,"找到蓝牙",Toast.LENGTH_SHORT).show();
                            msgTextView_ble.setText("找到蓝牙！" + device.getName());
                            if(device.getName().equals("目标蓝牙名称")){
                                targetDevice = device;
                                mBluetoothAdapter.stopLeScan(leScanCallback);
                            }
                        }
                    });
                }
            };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothGatt.STATE_CONNECTED){
                System.out.println("------------------设备连接上 开始扫描服务------------------");
                mBluetoothGatt.discoverServices();
                //
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = STATE_CONNECTED;
                        mHandler.sendMessage(message);
                    }
                }).start();
            }
            if(newState == BluetoothGatt.STATE_DISCONNECTED){
                System.out.println("------------------设备断开------------------");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = STATE_DISCONNECTED;
                        mHandler.sendMessage(message);
                    }
                }).start();
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {

            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("-----回调函数——发送数据成功");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = WRITE_SUCCESS;
                        mHandler.sendMessage(message);
                    }
                }).start();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //开启监听成功，可以向设备写入命令了
                System.out.println("------------------开启监听成功------------------");
            }
        };

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
             serviceList = gatt.getServices();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            System.out.println("---收到数据----");
        }
    };


    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        private LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = BleControlActivity.this.getLayoutInflater();
        }
        private void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        public void clear() {
            mLeDevices.clear();
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}

