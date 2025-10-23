package cn.acecandy.fasaxi.emma.config;

import cn.hutool.v7.http.client.ApacheHttpClientConfig;
import cn.hutool.v7.http.client.engine.ClientEngine;
import cn.hutool.v7.http.client.engine.ClientEngineFactory;
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
        return ClientEngineFactory.createEngine(ApacheHttpClientConfig.of()
                .setMaxTotal(200).setMaxPerRoute(20).setTimeout(30_000));
        // return ClientEngineFactory.createEngine(OkHttpClientConfig.of()
        //         .setMaxIdle(200).setConnectionTimeout(6_000).setReadTimeout(20_000));
    }
}