package com.example.smartmask;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import java.io.IOException;
import java.io.InputStream;

import java.io.PrintWriter;
import java.lang.ref.WeakReference;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class WifiControlActivity extends AppCompatActivity {
    private EditText wifiIp_et, wifiPort_et;
    private Button stop_motorByWifi_btn,stop_fanByWifi_btn, wifiConnect_btn, wifiDisconnect_btn,wifiBack_bt,clearText_wifi;
    private TextView msgTextView_wifi;
    private int PORT;
    private String HOST;
    private Socket client,socket;
    private ServerSocket server;
    private static Map<Socket,String> clientList = new HashMap<>();

    final static int CONNECTED_RESPONSE = 1000;//连接成功
    final static int DISCONNECTED_RESPONSE = 1001;//连接成功
    final static int RESPONSE_TIMEOUT = 1002;//未连接
    final static int SEND_FAIl = 1003;//发送失败
    final static int RECEIVER_DATA = 1004;//接收
    final static int CLEAR_TEXT = 1005;//接收

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
                case CONNECTED_RESPONSE:
                    wifiConnect_btn.setTextColor(Color.parseColor("#216F02"));
                    wifiConnect_btn.setText("已连接");
                    wifiConnect_btn.setClickable(false);
                    wifiDisconnect_btn.setTextColor(Color.BLACK);
                    wifiDisconnect_btn.setClickable(true);
                    wifiDisconnect_btn.setText("断开连接");
                    break;
                case DISCONNECTED_RESPONSE:
                    wifiDisconnect_btn.setTextColor(Color.parseColor("#216F02"));
                    wifiDisconnect_btn.setText("已断开连接");
                    wifiDisconnect_btn.setClickable(false);
                    wifiConnect_btn.setTextColor(Color.BLACK);
                    wifiConnect_btn.setText("连接");
                    wifiConnect_btn.setClickable(true);
                    break;
                case RESPONSE_TIMEOUT:
                    msgTextView_wifi.append("连接超时！");
                    break;
                case RECEIVER_DATA:
                    String s = msg.obj.toString();
                    msgTextView_wifi.append(s+ "\n");
                    break;
                case SEND_FAIl:
                    msgTextView_wifi.append("发送指令失败"+"\n");
                    break;
                case CLEAR_TEXT:
                    msgTextView_wifi.setText(" ");
                    break;
                default:
                    break;
                }

            }
        }
    }

    private final Handler mHandler = new WifiControlActivity.MHandler(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        System.out.println("WifiControlActivity: onCreate :");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_control);
        wifiPort_et = findViewById(R.id.wifiPort);
        wifiIp_et = findViewById(R.id.wifiIp);
        stop_motorByWifi_btn = findViewById(R.id.stop_motorByWifi);
        stop_fanByWifi_btn = findViewById(R.id.stop_fanByWifi);
        wifiConnect_btn = findViewById(R.id.wifiConnect);
        wifiDisconnect_btn = findViewById(R.id.wifiDisconnect);
        wifiBack_bt = findViewById(R.id.wifiBack_bt);
        msgTextView_wifi = findViewById(R.id.msgTextView_wifi);
        msgTextView_wifi.setMovementMethod(ScrollingMovementMethod.getInstance());
        clearText_wifi = findViewById(R.id.clearText_wifi);
        wifiConnect_btn.setOnClickListener(new OnClick());
        wifiDisconnect_btn.setOnClickListener(new OnClick());
        stop_motorByWifi_btn.setOnClickListener(new OnClick());
        stop_fanByWifi_btn.setOnClickListener(new OnClick());
        wifiBack_bt.setOnClickListener(new OnClick());
        clearText_wifi.setOnClickListener(new OnClick());

        //服务端线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if(server == null){
                        server = new ServerSocket(8087);
                    }
                    while(true){
                        socket = server.accept();
                        System.out.println("有客户端连接");
                        final String address = socket.getRemoteSocketAddress().toString();
                        System.out.println("连接成功，连接地址为：" + address);
                        ServerSocketThread serverSocketThread = new ServerSocketThread(socket,address);
                        serverSocketThread.start();
                    }
                }catch (IOException e){
                    System.out.println("server……");
                    e.printStackTrace();
                    try{
                        if(socket != null ){
                            socket.close();
                        }
                    }catch (IOException e1){
                        System.out.println("1socket.close……");
                        e1.printStackTrace();
                    }
                }finally {
                    try{
                        if(socket != null ){
                            socket.close();
                        }
                    }catch (IOException e1){
                        System.out.println("2socket.close……");
                        e1.printStackTrace();
                    }
                }

            }
        }).start();
    }
    private class OnClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.wifiConnect:
                    connect();
                    break;
                case R.id.wifiDisconnect:
                    diconnect();
                    break;
                case R.id.stop_motorByWifi:
                    TCPSend("MotorOff");
                        break;
                case R.id.stop_fanByWifi:
                    TCPSend("FanOff");
                        break;
                case R.id.wifiBack_bt:
                    Intent intent = new Intent(WifiControlActivity.this,MainActivity.class);
                    startActivity(intent);
                    break;
                case R.id.clearText_wifi:
                    clearText();
                    break;
            }
        }
    }

    private void connect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(client == null){
                    //判断输入是否为空
                    if (!TextUtils.isEmpty(wifiIp_et.getText())  && !TextUtils.isEmpty(wifiPort_et.getText())){
                        //获取编辑框值
                        HOST = wifiIp_et.getText().toString();
                        PORT = Integer.valueOf(wifiPort_et.getText().toString());
                        try{
                            client = new Socket(HOST,PORT);
//                        out = new PrintStream(mSocket.getOutputStream());
                            //如果不为空发送连接成功
                            if (client != null) {
                                System.out.println("------------------socket连接成功------------------");
                                Message message = new Message();
                                message.what = CONNECTED_RESPONSE;
                                mHandler.sendMessage(message);
                            }
                        }catch (IOException ex) {
                            showToast("连接失败！");
                            System.out.println("------------------连接超时------------------");
                            ex.printStackTrace();
                            Message message = new Message();
                            message.what = RESPONSE_TIMEOUT;
                            mHandler.sendMessage(message);
                        }
                    }else{
                        showToast("请输入WiFi名称及密码");
                    }
                }else{
                    showToast("已连接！");
                }
            }
        }).start();
    }
    private void diconnect(){
        if(client != null) {
            try {
                client.close();
                client = null;
                System.out.println("------------------已断开连接------------------");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Message message = new Message();
            message.what = DISCONNECTED_RESPONSE;
            mHandler.sendMessage(message);
        }else{
            Toast.makeText(WifiControlActivity.this,"请先连接！",Toast.LENGTH_SHORT).show();
        }
    }
    private void TCPSend(final String data) {
        // 开启线程来发起网络请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(client == null)
                {
                    showToast("请先连接！");
                }else {
                    try {
                        PrintWriter writer = new PrintWriter(client.getOutputStream());
                        writer.println(data);
                        writer.flush();
                        showToast("已发送指令");
                        System.out.println("------------------TCPSend发送数据成功------------------");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        System.out.println("------------------TCPSend发送数据失败------------------");
                        System.out.println("------------------发送数据成功------------------");
                        Message message = new Message();
                        message.what = SEND_FAIl;
                        // 将服务器返回的结果存放到Message中
                        mHandler.sendMessage(message);
                    }
                }
            }
        }).start();
    }

    //创建server线程
    private class ServerSocketThread extends Thread {
        Socket socket;
        String address;

        ServerSocketThread(Socket socket, String address) {
            this.socket = socket;
            this.address = address;
            System.out.println("------------------接收到socket------------------");
        }

        @Override
        public void run() {
            super.run();

            try {
                synchronized (this) {
                    clientList.put(socket, address);
                }
                System.out.println("------------------准备接收客户端信息------------------");
                InputStream inputStream = socket.getInputStream();

                System.out.println("------------------创建输入流------------------");
                byte[] buffer = new byte[1024];
                int len;

                while ((len = inputStream.read(buffer)) != -1) {
                    String text = new String(buffer, 0, len);
                    if(text.equals("end"))
                        break;
                    Message message = new Message();
                    message.what = RECEIVER_DATA;
                    message.obj = text;
                    mHandler.sendMessage(message);
                    System.out.println("收到的数据为：" + text);
                }

                inputStream.close();
                socket.close();
                System.out.print("------------------socket,in已关闭------------------");
            } catch (Exception e) {
                System.out.println("------------------接收客户端信息错误------------------");
                e.printStackTrace();
            } finally {
                synchronized (this) {
                    System.out.println("关闭连接：" + address);
                    clientList.remove(address);
                }
            }
        }
    }
    //清空接收区
    private void clearText(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = CLEAR_TEXT;
                mHandler.sendMessage(message);
                showToast("清空接收区");
            }
        }).start();
    }

    void showToast(String msg){//toast显示
        Looper.prepare();
        Toast.makeText(WifiControlActivity.this, msg, Toast.LENGTH_SHORT).show();
        Looper.loop();
    }


    @Override
    protected void onStop() {
        super.onStop();
        try{
            if(server != null){
                server.close();
            }
            if(socket != null)
                socket.close();
            System.out.println("------------------WifiControlActivity:onStop------------------");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("------------------退出WifiContActivity------------------");
        try{
            server.close();
            server = null;
            System.out.println("------------------server closed------------------");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
