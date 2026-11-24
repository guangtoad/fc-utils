package com.toad.fc.service.controller;

import com.toad.fc.service.service.DynamicFcLoaderService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/fc")
public class FcInvokeController {

    private final DynamicFcLoaderService loaderService;

    public FcInvokeController(DynamicFcLoaderService loaderService) {
        this.loaderService = loaderService;
    }

    /**
     * 获取所有已加载的 FC 插件清单
     */
    @GetMapping("/plugins")
    public Map<String, String> listPlugins() {
        return loaderService.getLoadedPlugins();
    }

    /**
     * 手动触发重新加载插件
     */
    @PostMapping("/load")
    public String loadPlugins() {
        try {
            loaderService.loadFcPlugins();
            return "插件加载完成";
        } catch (Exception e) {
            return "加载失败: " + e.getMessage();
        }
    }

    /**
     * 调用指定的 FC 函数
     */
    @PostMapping("/invoke/{beanName}")
    public Object invoke(@PathVariable String beanName, @RequestBody Map<String, Object> event, HttpServletRequest request,   // Spring 自动注入
                         HttpServletResponse response) throws ServletException, IOException {

        return loaderService.invokeFcFunction(beanName, event, request, response);
    }
}