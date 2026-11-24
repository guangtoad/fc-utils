package com.toad.fc.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class OssHandler implements StreamRequestHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // 1. 读取输入流中的事件 JSON 数据
        String eventJson = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

        context.getLogger().info("收到 OSS 触发事件: " + eventJson);

        try {
            // 2. 解析 JSON 提取 OSS 事件信息
            JSONObject jsonObject = JSON.parseObject(eventJson);
            JSONArray events = jsonObject.getJSONArray("events");

            if (events != null && !events.isEmpty()) {
                JSONObject firstEvent = events.getJSONObject(0);
                String eventName = firstEvent.getString("eventName");

                JSONObject oss = firstEvent.getJSONObject("oss");
                JSONObject bucket = oss.getJSONObject("bucket");
                JSONObject object = oss.getJSONObject("object");

                String bucketName = bucket.getString("name");
                String objectKey = object.getString("key");
                Long size = object.getLong("size");

                // 3. 执行你的业务逻辑（这里以打印日志为例）
                context.getLogger().info(String.format("检测到文件操作！\n事件类型: %s\nBucket: %s\n文件路径: %s\n文件大小: %d 字节",
                        eventName, bucketName, objectKey, size));

                // 模拟业务处理耗时
                Thread.sleep(500);

                // 4. 返回处理结果
                String response = "处理成功: " + objectKey;
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            context.getLogger().error("处理 OSS 事件失败");
            throw new RuntimeException(e);
        }
    }
}