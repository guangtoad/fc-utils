package com.toad.fc.service.handler;

import java.util.Map;

/**
 * FC 函数统一调用接口
 */
public interface FcFunctionHandler {
    /**
     * 执行 FC 函数逻辑
     * @param event 事件参数（模拟 FC 的 event 入参）
     * @return 执行结果
     */
    Object execute(Map<String, Object> event);
}