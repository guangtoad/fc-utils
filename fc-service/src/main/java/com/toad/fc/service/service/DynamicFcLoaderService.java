package com.toad.fc.service.service;


import com.aliyun.fc.runtime.Context;
import com.toad.fc.service.handler.FcFunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Service
public class DynamicFcLoaderService {

    private static final Logger log = LoggerFactory.getLogger(DynamicFcLoaderService.class);

    // 1. 从配置文件中读取插件路径
    @Value("${fc.plugin.path}")
    private String pluginDirPath;

    private final ApplicationContext applicationContext;

    // 2. 维护一个已加载的 FC 插件清单（BeanName -> 类名）
    private final Map<String, String> loadedPlugins = new LinkedHashMap<>();

    public DynamicFcLoaderService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 应用启动后自动加载插件
     */
    @PostConstruct
    public void init() {
        try {
            loadFcPlugins();
        } catch (Exception e) {
            log.error("初始化加载 FC 插件失败", e);
        }
    }

    /**
     * 动态加载指定目录下的 FC 插件 JAR 包
     */
    public void loadFcPlugins() throws Exception {
        File dir = new File(pluginDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("插件目录不存在: {}", dir.getAbsolutePath());
            return;
        }

        File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return;

        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)
                ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

        for (File jarFile : jarFiles) {
            log.info("开始加载 FC 插件: {}", jarFile.getName());

            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    this.getClass().getClassLoader()
            );

            try (JarFile jar = new JarFile(jarFile)) {
                log.debug("加载jar:" + jar.getName());
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    log.debug("加载 Entry:" + name);
                    if (name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace("/", ".").substring(0, name.length() - 6);
                        log.debug("加载 class:" + name);
                        try {
                            Class<?> clazz = classLoader.loadClass(className);

                            if (com.aliyun.fc.runtime.HttpRequestHandler.class.isAssignableFrom(clazz) && !clazz.isInterface()) {

                                String beanName = clazz.getSimpleName();

                                // 3. 幂等性检查：防止重复注册
                                //    如果容器中已经存在该名称的 Bean 定义，则跳过，避免覆盖或冲突
                                if (!beanFactory.containsBeanDefinition(beanName)) {

                                    // 4. 构建 Bean 定义
                                    //    genericBeanDefinition(clazz): 创建一个基于指定类的通用 Bean 定义对象
                                    BeanDefinitionBuilder builder = BeanDefinitionBuilder
                                            .genericBeanDefinition(clazz)

                                            // 5. 设置自动装配模式：AUTOWIRE_BY_TYPE (值为 2)
                                            //    含义：Spring 在创建该 Bean 时，会自动查找容器中与该 Bean 属性类型匹配的其它 Bean 进行注入。
                                            //    注意：这等同于 XML 配置中的 <bean autowire="byType" />。
                                            //    作用：无需显式使用 @Autowired 注解，只要类型匹配且容器中只有一个候选者，就会自动注入。
                                            .setAutowireMode(2); // AUTOWIRE_BY_TYPE

                                    // 6. 注册 Bean 定义到容器
                                    //    将构建好的 BeanDefinition 正式注册到 BeanFactory 中，此时 Bean 尚未实例化，只是元数据注册
                                    beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());

                                    // 7. 记录已加载的插件映射关系
                                    //    用于后续管理、查询或卸载插件时快速定位类名
                                    loadedPlugins.put(beanName, className);

                                    // 8. 记录日志
                                    //    输出成功注册的 Bean 名称，便于运维排查和监控
                                    log.info("成功注册 FC 插件 Bean: {}", beanName);
                                }
                            }

                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            log.debug("跳过类: {}, 原因: {}", className, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取所有已加载的 FC 插件清单
     */
    public Map<String, String> getLoadedPlugins() {
        return Collections.unmodifiableMap(loadedPlugins);
    }

    /**
     * 调用已加载的 FC 函数
     */
//    public Object invokeFcFunction(String beanName, Map<String, Object> event) {
//        if (applicationContext.containsBean(beanName)) {
//            com.aliyun.fc.runtime.HttpRequestHandler handler = (com.aliyun.fc.runtime.HttpRequestHandler) applicationContext.getBean(beanName);
//            return handler.handleRequest(HttpServletRequest request, HttpServletResponse response, Context context);
//        }
//        throw new RuntimeException("未找到对应的 FC 插件 Bean: " + beanName);
//    }

    public Object invokeFcFunction( String beanName, Map<String, Object> event, HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        if (applicationContext.containsBean(beanName)) {
            com.aliyun.fc.runtime.HttpRequestHandler handler = (com.aliyun.fc.runtime.HttpRequestHandler) applicationContext.getBean(beanName);
            ThreadLocal<Context> contextHolder = new ThreadLocal<>();
            Context context = contextHolder.get();
            handler.handleRequest(request, response, context);
            return null;
        }
        throw new RuntimeException("未找到对应的 FC 插件 Bean: " + beanName);
    }
}