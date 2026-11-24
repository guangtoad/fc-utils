//package com.toad.fc.example;
//
//import com.aliyun.fc.runtime.Context;
//import com.aliyun.fc.runtime.PojoRequestHandler;
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.Map;
//
//// 实现 PojoRequestHandler 接口，处理 HTTP 事件
//public class PojoHandler implements PojoRequestHandler<HTTPTriggerEvent, HTTPTriggerResponse> {
//    @Override
//    public HTTPTriggerResponse handleRequest(HTTPTriggerEvent request, Context context) {
//        context.getLogger().info("收到 HTTP 请求: " + request.toString());
//
//        // 获取请求体，处理 Base64 编码
//        String requestBody = request.getBody();
//        if (request.isIsBase64Encoded()) {
//            requestBody = new String(Base64.getDecoder().decode(request.getBody()), StandardCharsets.UTF_8);
//        }
//
//        String message = "HTTP 请求体内容: " + requestBody;
//        context.getLogger().info(message);
//
//        // 构建 HTTP 响应
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Content-Type", "application/json");
//
//        return HTTPTriggerResponse.builder()
//                .withStatusCode(200)
//                .withHeaders(headers)
//                .withBody("{\"status\": \"success\", \"message\": \"" + message + "\"}")
//                .build();
//    }
//}