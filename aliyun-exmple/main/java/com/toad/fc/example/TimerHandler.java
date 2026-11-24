package com.toad.fc.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.StreamRequestHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TimerHandler implements StreamRequestHandler {
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // 读取定时触发器传递的事件数据
        String eventJson = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

        context.getLogger().info("收到定时触发事件: " + eventJson);

        // 解析定时触发器的事件参数
        JSONObject event = JSON.parseObject(eventJson);
        String triggerTime = event.getString("triggerTime");
        String triggerName = event.getString("triggerName");
        String payload = event.getString("payload"); // 你在控制台配置的自定义参数

        context.getLogger().info(String.format("定时任务执行！触发时间: %s, 触发器名称: %s, 自定义参数: %s",
                triggerTime, triggerName, payload));

        // 执行你的定时业务逻辑（如清理数据库、生成报表等）
        // ...

        outputStream.write("定时任务执行成功".getBytes(StandardCharsets.UTF_8));
    }
}