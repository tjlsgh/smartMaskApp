package com.example.smartmask;

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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.iot.model.v20180120.InvokeThingServiceResponse;
import com.aliyuncs.iot.model.v20180120.QueryDevicePropertyStatusResponse;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;

public class DeviceStateActivity extends AppCompatActivity {

    Button back_btn;
    TextView messageArrived_tv;
    Button refresh_btn;
    Gson gson = new Gson();
    TextView temperature_tv,fan_tv,motor_tv,usingTime_tv,alcohol_tv,airpressure_tv,aqi_tv;

    private String  temperature,fan,motor,usingTime,alcohol,airpressure,aqi;

    final static int REFRESH_SUCCESS = 1000;//刷新成功
    final static int REFRESH_ERROR = 1001;//刷新失败
    final static int REFRESH_STATE = 1002;//刷新失败


    private  class MHandler extends Handler {
        private WeakReference<Activity> mActivity;

        MHandler(Activity activity) {
            mActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Activity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case REFRESH_SUCCESS:
                        temperature_tv.setText(temperature+"ºC ");
                        aqi_tv.setText(aqi);
                        airpressure_tv.setText(airpressure+"Pa");
                        alcohol_tv.setText(alcohol);
                        usingTime_tv.setText(usingTime+"min");
                        if(fan.equals("1")){
                            fan_tv.setText("开");
                        }else
                            fan_tv.setText("关");
                        if(motor.equals("1")){
                            motor_tv.setText("开");
                        }else
                            motor_tv.setText("关");
                        break;
                    case REFRESH_ERROR:
                        break;
                    case REFRESH_STATE:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                refresh();
                            }
                        }).start();
                        break;
                }

            }
        }
    }

    private final Handler mHandler = new DeviceStateActivity.MHandler(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_state);

        back_btn = findViewById(R.id.Back);
        messageArrived_tv = findViewById(R.id.messageArrivedTextView);
        refresh_btn = findViewById(R.id.refresh);
        aqi_tv = findViewById(R.id.aqi_tv);
        airpressure_tv = findViewById(R.id.airpressure_tv);
        alcohol_tv = findViewById(R.id.alcohol_tv);
        usingTime_tv = findViewById(R.id.usingTime_tv);
        motor_tv = findViewById(R.id.motor_tv);
        fan_tv = findViewById(R.id.fan_tv);
        temperature_tv = findViewById(R.id.temperature_tv);



        back_btn.setOnClickListener(new OnClick());
        refresh_btn.setOnClickListener(new OnClick());
        new Thread(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        }).start();
    }
    class OnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.Back:
                    Intent intent = new Intent(DeviceStateActivity.this, MainActivity.class);
                    startActivity(intent);
                    break;
                case R.id.refresh:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(true){
                                try{
                                    Thread.sleep(2000);
                                    Message message = new Message();
                                    message.what=REFRESH_STATE;
                                    mHandler.sendMessage(message);
                                }catch (InterruptedException e){
                                    System.out.println("查询设备状态错误");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                    break;
            }
        }
    }
    void refresh(){
        if(Client.client != null){
            try{
                QueryDevicePropertyStatusResponse response = Client.queryDevicePropertyStatus(Client.client);
                System.out.println(gson.toJson(response));
                JSONObject json = JSON.parseObject(gson.toJson(response));//将json字符串转换成jsonObject对象
                aqi = json.getJSONObject("data").getJSONArray("list").getJSONObject(0).getString("value");
                airpressure = json.getJSONObject("data").getJSONArray("list").getJSONObject(1).getString("value");
                alcohol = json.getJSONObject("data").getJSONArray("list").getJSONObject(2).getString("value");
                fan = json.getJSONObject("data").getJSONArray("list").getJSONObject(3).getString("value");
                motor = json.getJSONObject("data").getJSONArray("list").getJSONObject(4).getString("value");
                temperature = json.getJSONObject("data").getJSONArray("list").getJSONObject(5).getString("value");
                usingTime = json.getJSONObject("data").getJSONArray("list").getJSONObject(6).getString("value");
                Message msg = new Message();
                msg.what = REFRESH_SUCCESS;
                mHandler.sendMessage(msg);
            } catch (ServerException e) {
                System.out.println("服务端异常！！" );
                e.printStackTrace();
            } catch (ClientException e) {
                System.out.println("ErrCode:" + e.getErrCode());
                System.out.println("ErrMsg:" + e.getErrMsg());
                System.out.println("RequestId:" + e.getRequestId());
            }
        }else{
            showToast("请先初始化！");
        }
    }
    void showToast(String msg){//toast显示
        Looper.prepare();
        Toast.makeText(DeviceStateActivity.this, msg, Toast.LENGTH_SHORT).show();
        Looper.loop();
    }
}
