package com.example.smartmask;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.iot.model.v20180120.*;
import com.aliyuncs.profile.DefaultProfile;



/**
 * GetDeviceStatus    查看指定设备的运行状态
 * QueryDevice    查询产品的设备列表
 * GetDeviceShadow    查询设备影子
 */
public class Client {

    // 产品Key
    private static String productKey = "a104fcohulX";

    static IAcsClient client = null;

    static void init() {
        client = initialization();
    }

    /**
     * GetDeviceShadow    查询设备影子
     */
    private static GetDeviceShadowResponse getDeviceShadow(IAcsClient client, String deviceName, String productKey) throws ClientException {
        GetDeviceShadowRequest request = new GetDeviceShadowRequest();
        System.out.println("------------------getDeviceShadow-------------------");
        // 要查询的设备所隶属的产品Key
        request.setProductKey(productKey);
        // 要查询的设备名称
        request.setDeviceName(deviceName);
        GetDeviceShadowResponse response = client.getAcsResponse(request);
        return response;
    }

    /**
     * GetDeviceStatus    查看指定设备的运行状态
     */
    public static GetDeviceStatusResponse getDeviceStatus(IAcsClient client, String deviceName) throws ClientException {
        GetDeviceStatusRequest request = new GetDeviceStatusRequest();
        System.out.println("------------------getDeviceStatus-------------------");
        // 要查看运行状态的设备ID
        request.setDeviceName(deviceName);
        request.setProductKey(productKey);
        GetDeviceStatusResponse response = client.getAcsResponse(request);
        return response;
    }

    /**
     * QueryDevice    查询产品的设备列表
     */
    private static QueryDeviceResponse queryDevice(IAcsClient client, String productKey) throws ClientException {
        QueryDeviceRequest request = new QueryDeviceRequest();
        System.out.println("------------------queryDevice-------------------");
        // 要查询的设备所隶属的产品Key
        request.setProductKey(productKey);
        // 分页条件
        request.setCurrentPage(1);
        request.setPageSize(20);
        QueryDeviceResponse response = client.getAcsResponse(request);
        return response;
    }

    /**
     * stopFan 调用物模型服务停止吹风
     */
    public static InvokeThingServiceResponse InvokeThingService_stopFan(IAcsClient client) throws ClientException {
        InvokeThingServiceRequest request = new InvokeThingServiceRequest();
        System.out.println("------------------调用物模型服务停止吹风-------------------");
        // 要调用的设备所隶属的产品Key
        request.setProductKey(productKey);
        request.setArgs("{\"Fan\":0}");
        request.setDeviceName("Smart_Mask");
        request.setIdentifier("StopFan");
        InvokeThingServiceResponse response = client.getAcsResponse(request);
        return response;
    }

    /**
     * stopShake 调用物模型服务停止震动
     */
    public static InvokeThingServiceResponse InvokeThingService_stopShake(IAcsClient client) throws ClientException {
        InvokeThingServiceRequest request = new InvokeThingServiceRequest();
        System.out.println("------------------调用物模型服务停止震动-------------------");
        // 要调用的设备所隶属的产品Key
        request.setProductKey(productKey);
        request.setArgs("{\"Motor\":0}");
        request.setDeviceName("Smart_Mask");
        request.setIdentifier("StopShake");
        InvokeThingServiceResponse response = client.getAcsResponse(request);
        return response;
    }

    /**
     * stopShake 查询设备多个属性
     */
    public static QueryDevicePropertyStatusResponse queryDevicePropertyStatus(IAcsClient client) throws ClientException {
        QueryDevicePropertyStatusRequest request = new QueryDevicePropertyStatusRequest();
        System.out.println("------------------查询设备多个属性-------------------");
        // 要调用的设备所隶属的产品Key
        request.setProductKey(productKey);
        request.setDeviceName("Smart_Mask");
        QueryDevicePropertyStatusResponse response = client.getAcsResponse(request);
        return response;
    }

    /**
     * stopShake 查询设备单个属性
     */
    public static QueryDevicePropertyDataResponse queryDevicePropertyData(IAcsClient client,String Identifier) throws ClientException {
        QueryDevicePropertyDataRequest request = new QueryDevicePropertyDataRequest();
        System.out.println("------------------查询设备单个属性-------------------");
        // 要调用的设备所隶属的产品Key
        request.setProductKey(productKey);
        request.setDeviceName("Smart_Mask");
        request.setIdentifier(Identifier);
        request.setStartTime(System.currentTimeMillis());
        request.setEndTime(System.currentTimeMillis()+1);
        request.setAsc(1);
        request.setPageSize(10);
        QueryDevicePropertyDataResponse response = client.getAcsResponse(request);
        return response;
    }

    /**
     * Initialization  初始化公共请求参数
     */
    private static IAcsClient initialization() {
        String regionId = "cn-shanghai";
        String accessKeyId = "LTAI4GKLcdN5K8WnvYbGxAxM";
        String secret = "JjWbeMTW63Fohgz1P4AVWmBWKvUe5A";
        DefaultProfile profile = DefaultProfile.getProfile(
                regionId, // 您的可用区ID
                accessKeyId, // 您的AccessKey ID
                secret); // 您的AccessKey Secret
        return new DefaultAcsClient(profile);
    }
}
