package cn.acecandy.fasaxi.emma.config;

import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.client.engine.ClientEngineFactory;
import org.dromara.hutool.http.client.engine.okhttp.OkHttpClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * hutool相关配置
 *
 * @author tangningzhu
 * @since 2025/4/15
 */
@Configuration
public class HutoolConfig {

    @Bean
    public ClientEngine httpClient() {
        // return ClientEngineFactory.createEngine(ApacheHttpClientConfig.of()
        //         .setMaxTotal(200).setMaxPerRoute(20).setConnectionTimeout(10_000).setReadTimeout(60_000));
        return ClientEngineFactory.createEngine(OkHttpClientConfig.of()
                .setMaxIdle(200).setConnectionTimeout(10_000).setReadTimeout(60_000));
    }
}