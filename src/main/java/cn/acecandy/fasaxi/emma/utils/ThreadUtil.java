package cn.acecandy.fasaxi.emma.utils;

import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.crypto.asymmetric.KeyType;
import cn.hutool.v7.crypto.asymmetric.RSA;
import lombok.extern.slf4j.Slf4j;


/**
 * 排序工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class ThreadUtil extends cn.hutool.v7.core.thread.ThreadUtil {

    private ThreadUtil() {
    }

    public static void execVirtual(Runnable task) {
        Thread.startVirtualThread(task);
    }

    public static void main(String[] args) {
        String license = "JpcW8V1tv8RZ/vKr+8sKz9AEYU1DkTA+fJUiL2WJH5mEqikfj5Fg9OuWDS8Jc77R4uV64PO3HLD4PtESkuyzFthZq8XcCGDtjh/Q6FRrfGGMdV1Hc6HYvHNDMstrfp34pdQdbXjSuQ/mbO2oQGVeMYhh/Czj+aDRZiVz0TR9gm8=";
        RSA rsa = new RSA((String) null, "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCNLHKIJfwmCLGa7sNGXT/g2Q9crI/Z9KJprOfcNf5cNwF6zbij5e+oj7rxWSHIsMCW9MR71BQ3CnMNPJ1CSYUn3w7Stvk60+fd6IHpGn6y9uSu1xk7xM8VQlZHb3zKCCjnfNWBcPRek0BxtdsRx7FJIqhsLQkjBf4vjiq7ZCdCJwIDAQAB");
        license = rsa.decryptStr(license, KeyType.PublicKey);

        Console.log(SecureUtil.rsa(null, "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCNLHKIJfwmCLGa7sNGXT/g2Q9crI/Z9KJprOfcNf5cNwF6zbij5e+oj7rxWSHIsMCW9MR71BQ3CnMNPJ1CSYUn3w7Stvk60+fd6IHpGn6y9uSu1xk7xM8VQlZHb3zKCCjnfNWBcPRek0BxtdsRx7FJIqhsLQkjBf4vjiq7ZCdCJwIDAQAB").decryptStr(license, KeyType.PublicKey));
    }

}