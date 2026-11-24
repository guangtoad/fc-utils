package com.toad.fc.service.handler.plugin;


import com.toad.fc.service.handler.FcFunctionHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

// 注意：这里即使加了 @Component，主应用启动时也不会扫描到，需要后续动态注册
public class OssProcessPlugin implements FcFunctionHandler {

    // 演示：动态注册后，Spring 能够成功注入此依赖
    // @Autowired
    // private SomeMainService mainService;

    @Override
    public Object execute(Map<String, Object> event) {
        String bucket = (String) event.get("bucket");
        String objectKey = (String) event.get("key");

        System.out.println("[插件执行] 正在处理 OSS 文件: " + bucket + "/" + objectKey);

        // 模拟业务逻辑
        return "处理成功: " + objectKey;
    }
}