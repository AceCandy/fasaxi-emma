package cn.acecandy.fasaxi.emma;

import cn.hutool.v7.core.text.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;

/**
 * 应用入口
 *
 * @author AceCandy
 * @since 2025/01/03
 */
@Slf4j
// @EnableRetry
// @RestController
@EnableScheduling
@EnableFeignClients(basePackages = "cn.acecandy.fasaxi.emma.sao.client")
@ConfigurationPropertiesScan
@ServletComponentScan
@SpringBootApplication
public class Application {

    @SneakyThrows
    public static void main(String[] args) {
        // 显式创建 SpringApplication 实例
        SpringApplication app = new SpringApplication(Application.class);
        app.setBannerMode(Banner.Mode.OFF);
        // 启动容器并获取上下文
        ConfigurableApplicationContext context = app.run(args);
        init();
        Environment env = context.getEnvironment();
        String host = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "8802");
        String pathContext = env.getProperty("server.servlet.context-path", "");
        String uri = StrUtil.format("http://{}:{}{}", host, port, pathContext);
        log.info("""
                 \r----------------------------------------------------------
                 {}[{}] 已启动!
                 请求路径: {}/web/index.html
                 健康检查: {}/health/time
                 接口文档: {}/doc.html
                 ----------------------------------------------------------
                 """,
                env.getProperty("spring.application.name"), env.getProperty("spring.profiles.active"),
                uri, uri, uri);
    }

    /**
     * 提前初始化一些数据
     */
    private static void init() {
        // ThreadUtil.execAsync(() -> SpringUtil.getBean(ScheduledTask.class).run());
    }
}