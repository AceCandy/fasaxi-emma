package cn.acecandy.fasaxi.emma;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

/**
 * 应用入口
 *
 * @author AceCandy
 * @since 2025/01/03
 */
@Slf4j
@EnableScheduling
@EnableRetry
@RestController
@ConfigurationPropertiesScan
@ServletComponentScan
@SpringBootApplication
public class Application {

    @SneakyThrows
    public static void main(String[] args) {
            ConfigurableApplicationContext application = SpringApplication.run(Application.class, args);
            init();
            Environment env = application.getEnvironment();
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