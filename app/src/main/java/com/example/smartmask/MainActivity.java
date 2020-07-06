package com.example.smartmask;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.iot.model.v20180120.GetDeviceStatusResponse;
import com.aliyuncs.iot.model.v20180120.InvokeThingServiceResponse;
import com.google.gson.Gson;
import com.alibaba.fastjson.JSON;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    Button init_button, stopFan_button, stopMotor_button, deviceState_button,
            wifiControl_button,device_state_btn,bleControl_button;
    TextView state_tv,msgTextView;
    Gson gson = new Gson();
    Button deviceFgm_btn, myFgm_btn;
    String online = "在线",offline = "不在线",
            initSuccess = "初始化成功",initError = "初始化失败";
    String status,msgText;


    final static int INIT_SUCCESS = 1000;//连接成功
    final static int INIT_ERROR = 1001;//未连接
    final static int ONLINE = 1002;//在线
    final static int OFFLINE = 1003;//不在线
    final static int CHECK_STATE = 1004;
    final static int STOP_FAN = 1005;
    final static int STOP_MOTOR = 1006;

    private class MHandler extends Handler {
        private WeakReference<Activity> mActivity;

        MHandler(Activity activity) {
            mActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Activity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case INIT_SUCCESS:
                        msgTextView.setText(initSuccess);
                        status = online;
                        break;
                    case INIT_ERROR:
                        msgTextView.setText(initError);
                        status = offline;
                        break;
                    case ONLINE:
                        state_tv.setText(online);;
                        break;
                    case OFFLINE:
                        state_tv.setText(offline);;
                        break;
                    case CHECK_STATE:
                        //线程执行查询设备状态任务
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getDeviceState();
                            }
                        }).start();
                        break;
                }

            }
        }
    }

    private final Handler mHandler = new MainActivity.MHandler(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_button = findViewById(R.id.init_button);
        stopFan_button = findViewById(R.id.stop_fan);
        stopMotor_button = findViewById(R.id.stop_motor);
        msgTextView = findViewById(R.id.msgTextView);
        deviceState_button = findViewById(R.id.check_state);
        state_tv = findViewById(R.id.state_tv);
        wifiControl_button = findViewById(R.id.wifi_control);
        device_state_btn = findViewById(R.id.device_state_btn);
        bleControl_button = findViewById(R.id.ble_control);


        //设置监听器
        init_button.setOnClickListener(new OnClick());
        stopFan_button.setOnClickListener(new OnClick());
        stopMotor_button.setOnClickListener(new OnClick());
        deviceState_button.setOnClickListener(new OnClick());
        wifiControl_button.setOnClickListener(new OnClick());
        device_state_btn.setOnClickListener(new OnClick());
        bleControl_button.setOnClickListener(new OnClick());


    }
    private class OnClick implements View.OnClickListener{
        Intent intent;
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.init_button:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            init();
                        }
                    }).start();
                    break;
                case R.id.device_state_btn:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(true){
                                try{
                                    //定时器
                                    Message message = new Message();
                                    message.what=CHECK_STATE;
                                    mHandler.sendMessage(message);
                                    Thread.sleep(10000);
                                }catch (InterruptedException e){
                                    System.out.println("------------------查询设备状态错误------------------");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                    break;
                case R.id.stop_fan:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            invokeService(STOP_FAN);
                        }
                    }).start();
                    break;
                case R.id.stop_motor:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            invokeService(STOP_MOTOR);
                        }
                    }).start();
                    break;
                case R.id.check_state:
                    intent = new Intent(MainActivity.this,DeviceStateActivity.class);
                    startActivity(intent);
                    break;
                case R.id.wifi_control:
                    intent = new Intent(MainActivity.this,WifiControlActivity.class);
                    startActivity(intent);
                    break;

                case R.id.ble_control:
                    intent = new Intent(MainActivity.this,BleControlActivity.class);
                    startActivity(intent);
                    break;
                default:
                    break;
            }

        }
    }


    //初始化client
    void init(){
        if(Client.client == null){
            Client.init();
        }else
            showToast("已初始化");
        if(Client.client != null){
            getDeviceState();
            Message msg = new Message();
            msg.what = INIT_SUCCESS;
            mHandler.sendMessage(msg);
            System.out.println("------------------初始化client成功------------------");
            showToast("初始化成功");

        }else {
            showToast("初始化失败");
        }
    }

    //查询设备状态
    void  getDeviceState(){
        String status = "OFFLINE";
        if(Client.client != null){
            try{
                GetDeviceStatusResponse deviceStatusResponse = Client.getDeviceStatus(Client.client,"Smart_Mask");
                JSONObject json = JSON.parseObject(gson.toJson(deviceStatusResponse));//将json字符串转换成jsonObject对象
                status = json.getJSONObject("data").getString("status");//获得data下的对象再取得status对应的值
            } catch (ServerException e) {
                System.out.println("------------------服务端异常！！------------------" );
                e.printStackTrace();
            } catch (ClientException e) {
                System.out.println("ErrCode:" + e.getErrCode());
                System.out.println("ErrMsg:" + e.getErrMsg());
                System.out.println("RequestId:" + e.getRequestId());
            }
        }else{
            System.out.println("------------------client为空------------------");
            showToast("请先初始化");
        }
        Message msg = new Message();
        if(status.equals("OFFLINE")){//不在线
            msg.what = OFFLINE;
            mHandler.sendMessage(msg);
        }else{
            msg.what = ONLINE;//在线
            mHandler.sendMessage(msg);
        }
    }

    //调用服务
    void invokeService(int service){
        InvokeThingServiceResponse invokeThingServiceResponse;
        try{
            if(Client.client != null){
                if(service == STOP_FAN){
                    invokeThingServiceResponse = Client.InvokeThingService_stopFan(Client.client);
                    System.out.println(gson.toJson(invokeThingServiceResponse));
                    msgTextView.setText("发送指令：停止吹风");
                }else if(service == STOP_MOTOR){
                    invokeThingServiceResponse = Client.InvokeThingService_stopShake(Client.client);
                    System.out.println(gson.toJson(invokeThingServiceResponse));
                    msgTextView.setText("发送指令：停止震动");
                }
                showToast("已发送数据");
            }else{
                showToast("请先初始化");
            }
        } catch (ServerException e) {
            System.out.println("------------------服务端异常！！------------------" );
            e.printStackTrace();
        } catch (ClientException e) {
            System.out.println("ErrCode:" + e.getErrCode());
            System.out.println("ErrMsg:" + e.getErrMsg());
            System.out.println("RequestId:" + e.getRequestId());
        }
    }


    void showToast(String msg){//toast显示
        Looper.prepare();
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        Looper.loop();
    }



    @Override
    protected void onResume() {
        super.onResume();
        //再次查询设备状态
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        Message message = new Message();
                        message.what=CHECK_STATE;
                        mHandler.sendMessage(message);
                        Thread.sleep(40000);
                    }catch (InterruptedException e){
                        System.out.println("------------------查询设备状态错误------------------");
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
