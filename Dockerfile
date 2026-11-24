# 指定基础镜像为 Eclipse Temurin 发行版的 Java 17 JRE，基于 Ubuntu Jammy (22.04) 系统
FROM eclipse-temurin:17-jre-jammy

# 定义构建参数 APP_HOME，用于指定应用的工作目录，默认值为空字符串
ARG APP_HOME=""

# 定义构建参数 TZ，用于指定容器的时区，默认值为亚洲/上海 (东八区)
ARG TZ=Asia/Shanghai

# 定义构建参数 SERVER_PORT，用于指定应用的服务端口，默认值为 80
ARG SERVER_PORT=80

# 将构建参数 APP_HOME 的值赋给环境变量 APP_HOME，以便在容器运行时使用
ENV APP_HOME=${APP_HOME}

# 将构建参数 TZ 的值赋给环境变量 TZ，用于配置容器的系统时区
ENV TZ=${TZ}

# 将构建参数 SERVER_PORT 的值赋给环境变量 SERVER_PORT，以便在容器运行时读取
ENV SERVER_PORT=${SERVER_PORT}

# 设置环境变量 JAVA_OPTS，包含 JVM 运行参数：使用 G1 垃圾回收器、最大堆内存占容器内存的 75%、指定随机数生成器源以加快启动速度
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# 设置环境变量 SPRING_PROFILES_ACTIVE，指定 Spring Boot 应用激活的配置环境为 prod (生产环境)
ENV SPRING_PROFILES_ACTIVE=prod

# 设置环境变量 FC_SERVER_PORT，优先读取环境变量 FC_SERVICE_HOST 的值，若不存在则回退使用 SERVER_PORT 的值
ENV FC_SERVER_PORT=${FC_SERVICE_HOST:-${SERVER_PORT}}

# 更新 apt 软件包索引，为安装 tzdata 做准备
RUN apt-get update

# 安装 tzdata 软件包（不安装推荐依赖），用于提供时区数据支持
RUN apt-get install -y --no-install-recommends tzdata

# 创建指向指定时区 (/usr/share/zoneinfo/${TZ}) 的软链接到 /etc/localtime，以设置系统时区
RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime

# 将指定的时区字符串写入 /etc/timezone 文件，作为系统时区的补充配置
RUN echo ${TZ} > /etc/timezone

# 在 APP_HOME 目录下创建 logs 子目录，用于存放应用运行日志
RUN mkdir -p ${APP_HOME}/logs

# 创建一个名为 appuser 的系统用户组，并创建一个同名系统用户，将其加入该组，设置无登录 shell 以提高安全性
RUN groupadd -r appuser && useradd -r -g appuser -d ${APP_HOME} -s /sbin/nologin appuser

# 清理 apt 缓存和不再需要的软件包，以减小最终生成的 Docker 镜像体积
RUN apt-get autoremove -y && apt-get clean

# 删除 apt 的包列表缓存文件，进一步减小镜像体积
RUN rm -rf /var/lib/apt/lists/*

# 打印当前工作目录路径（通常用于构建时的调试和确认）
RUN pwd

# 设置后续指令的工作目录为 APP_HOME 环境变量指定的路径
WORKDIR ${APP_HOME}

# 将本地构建产物目录 fc-service/target/ 下的 jar 包复制到容器的 APP_HOME 目录下，并重命名为 app.jar
COPY fc-service/target/*.jar ${APP_HOME}/app.jar

# 递归修改 APP_HOME 目录及其下所有文件的所有者为 appuser，确保应用用户有权限读写
RUN chown -R appuser:appuser ${APP_HOME}

# 切换当前运行用户为 appuser，后续指令及容器启动后均以该非 root 用户身份运行，提升安全性
USER appuser

# 声明容器对外暴露的端口号为 FC_SERVER_PORT 环境变量的值（仅作声明，不实际映射）
EXPOSE ${FC_SERVER_PORT}

# 声明一个挂载点，将容器内的 APP_HOME/logs 目录标记为可挂载卷，便于日志持久化
VOLUME ["${APP_HOME}/logs"]

# 配置健康检查指令：每 30 秒执行一次，超时时间 5 秒，启动宽限期 60 秒，连续失败 3 次判定为不健康
# 检查方式为通过 curl 请求 Actuator 的健康端点，若返回非 200 状态码则退出并标记为不健康
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${FC_SERVER_PORT}/actuator/health || exit 1

# 设置容器启动时的入口命令：通过 sh -c 执行 java 命令
# 传入 JAVA_OPTS 环境变量中的 JVM 参数、通过系统属性指定服务端口和激活的 Spring 配置文件，最后运行 app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} \
    -Dserver.port=${FC_SERVER_PORT} \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
    -jar ${APP_HOME}/app.jar"]