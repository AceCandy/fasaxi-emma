package cn.acecandy.fasaxi.emma.sao.client;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.array.ArrayUtil;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * redis客户端
 *
 * @author AceCandy
 * @since 2025/05/03
 */
@Slf4j
@Component
public class RedisClient {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.warn("redis set方法异常:", e);
            return false;
        }
    }

    public boolean set(String key, Object value, Integer time) {
        try {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("redis set方法异常:", e);
            return false;
        }
    }

    public Object get(String key) {
        return StrUtil.isBlank(key) ? null : redisTemplate.opsForValue().get(key);
    }

    public String getStr(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return (String) value;
    }

    /**
     * 从多个key中查询
     *
     * @param keys 关键
     * @return {@link String }
     */
    public List<String> getStr(String... keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return ListUtil.of();
        }
        try {
            List<String> validKeys = CollUtil.removeBlank(ListUtil.of(keys));
            List<Object> value = redisTemplate.opsForValue().multiGet(validKeys);
            if (CollUtil.isEmpty(value)) {
                return ListUtil.of();
            }
            return value.stream().map(Object::toString).collect(Collectors.toList());
        } catch (Exception e) {
            // 这里可以根据实际情况进行日志记录或者其他处理
            log.warn("redis getStr方法异常:", e);
            return null;
        }
    }

    /**
     * 从多个key中获取找到的第一个字符串
     *
     * @param keys 关键
     * @return {@link String }
     */
    public String getStrFindOne(String... keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return null;
        }
        try {
            List<String> validKeys = CollUtil.removeBlank(ListUtil.of(keys));
            List<Object> value = CollUtil.removeNull(redisTemplate.opsForValue().multiGet(validKeys));
            if (CollUtil.isEmpty(value)) {
                return null;
            }
            for (Object v : value) {
                if (v instanceof String) {
                    return (String) v;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("redis getStrFindOne方法异常:", e);
            return null;
        }
    }

    public boolean setBean(String key, Object value, Integer time) {
        try {
            /*ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(value);
            }
            String base64 = Base64.encode(bos.toByteArray());*/
            if (time > 0) {
                return set(key, value, time);
            } else {
                return set(key, value);
            }
        } catch (Exception e) {
            log.warn("redis setBean方法异常:", e);
            return false;
        }
    }

    public <T> T getBean(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public void del(String... key) {
        if (ArrayUtil.isEmpty(key)) {
            return;
        }
        redisTemplate.delete(ListUtil.of(key));
    }
}