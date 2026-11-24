package com.toad.fc.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class MnsHandler implements StreamRequestHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        String eventJson = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

        context.getLogger().info("收到 MNS 消息事件: " + eventJson);

        // 解析 MNS 消息（可能是批量消息）
        JSONObject jsonObject = JSON.parseObject(eventJson);
        JSONArray events = jsonObject.getJSONArray("events");

        for (int i = 0; i < events.size(); i++) {
            JSONObject event = events.getJSONObject(i);
            String messageBody = event.getString("messageBody");
            String messageId = event.getString("messageId");

            context.getLogger().info(String.format("处理 MNS 消息 - ID: %s, 内容: %s", messageId, messageBody));

        }

        outputStream.write("MNS 消息处理成功".getBytes(StandardCharsets.UTF_8));
    }
}