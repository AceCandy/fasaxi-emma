package cn.acecandy.fasaxi.emma.sao.client;

import cn.hutool.v7.core.array.ArrayUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.collection.set.SetUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ObjUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
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

    // 默认key前缀（避免与其他Rediskey冲突，可自定义）
    private static final String DEFAULT_KEY_PREFIX = "repeat_limit:";

    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("redis hasKey方法异常:", e);
            return false;
        }
    }

    /**
     * 获取剩余生存时间
     *
     * @param key 钥匙
     * @return {@link Long }
     */
    public Long ttl(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.warn("redis ttl方法异常:", e);
            return -1L;
        }
    }

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
        if (ObjUtil.isEmpty(value)) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("redis set方法异常:", e);
            return false;
        }
    }

    public boolean hset(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            return true;
        } catch (Exception e) {
            log.warn("redis hset方法异常:", e);
            return false;
        }
    }

    public String hgetStr(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            return null;
        }
        return (String) value;
    }

    public Boolean setnx(String key, Object value, Integer time) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("redis set方法异常:", e);
            return false;
        }
    }

    public Object get(String key) {
        return StrUtil.isBlank(key) ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 从多个key中查询
     *
     * @param keys 关键
     * @return {@link String }
     */
    public List<Object> get(String... keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return ListUtil.of();
        }
        try {
            List<String> validKeys = CollUtil.removeBlank(ListUtil.of(keys));
            List<Object> value = redisTemplate.opsForValue().multiGet(validKeys);
            if (CollUtil.isEmpty(value)) {
                return ListUtil.of();
            }
            return value;
        } catch (Exception e) {
            // 这里可以根据实际情况进行日志记录或者其他处理
            log.warn("redis getStr方法异常:", e);
            return null;
        }
    }

    /**
     * 从多个key中查询
     *
     * @param keys 关键
     * @return {@link String }
     */
    public List<Object> get(List<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return ListUtil.of();
        }
        try {
            List<String> validKeys = CollUtil.removeBlank(keys);
            List<Object> value = redisTemplate.opsForValue().multiGet(validKeys);
            if (CollUtil.isEmpty(value)) {
                return ListUtil.of();
            }
            return value;
        } catch (Exception e) {
            // 这里可以根据实际情况进行日志记录或者其他处理
            log.warn("redis getStr方法异常:", e);
            return null;
        }
    }

    public String getStr(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String valueStr) {
            return valueStr;
        }
        return null;
    }

    /**
     * 从多个key中查询
     *
     * @param keys 关键
     * @return {@link String }
     */
    public List<String> getStr(String... keys) {
        List<Object> value = get(keys);
        if (CollUtil.isEmpty(value)) {
            return ListUtil.of();
        }
        return value.stream().map(v -> {
            if (v instanceof String vStr) {
                return vStr;
            }
            return null;
        }).filter(StrUtil::isNotBlank).collect(Collectors.toList());
    }

    /**
     * 从多个key中获取找到的第一个字符串
     *
     * @param keys 关键
     * @return {@link String }
     */
    public String getStrFindOne(List<String> keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return null;
        }
        try {
            List<String> validKeys = CollUtil.removeBlank(keys);
            List<Object> value = CollUtil.removeNull(redisTemplate.opsForValue().multiGet(validKeys));
            if (CollUtil.isEmpty(value)) {
                return null;
            }
            for (Object v : value) {
                if (v instanceof String vStr) {
                    return vStr;
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
            if (null == value) {
                return false;
            }
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

    @SuppressWarnings("unchecked")
    public <T> T getBean(String key) {

        Object result = redisTemplate.opsForValue().get(key);
        if (null == result) {
            return null;
        }
        try {
            return (T) result;
        } catch (Exception e) {
            log.warn("redis getBean方法异常:", e);
            return null;
        }
    }

    public void del(String... key) {
        if (ArrayUtil.isEmpty(key)) {
            return;
        }
        redisTemplate.delete(ListUtil.of(key));
    }

    /**
     * 通过前缀批量删除
     *
     * @param prefix 前缀
     */
    public void delByPrefix(String prefix) {
        if (StrUtil.isBlank(prefix)) {
            return;
        }
        Set<String> keysToDelete = scanKeysByPrefix(prefix);
        if (CollUtil.isEmpty(keysToDelete)) {
            return;
        }
        // 统一序列化方式后删除
        redisTemplate.unlink(keysToDelete);
    }

    /**
     * 通过前缀批量删除
     *
     * @param prefixes 前缀
     */
    public void delByPrefix(List<String> prefixes) {
        if (CollUtil.isEmpty(prefixes)) {
            return;
        }
        Set<String> keysToDelete = SetUtil.of();
        prefixes.forEach(prefix -> keysToDelete.addAll(scanKeysByPrefix(prefix)));
        if (CollUtil.isEmpty(keysToDelete)) {
            return;
        }
        // 统一序列化方式后删除
        redisTemplate.unlink(keysToDelete);
    }

    /**
     * 获取对应前缀的所有值
     *
     * @param prefix 前缀
     * @return boolean
     */
    public List<String> getStrOnScan(String prefix) {
        if (StrUtil.isBlank(prefix)) {
            return ListUtil.of();
        }
        try {
            Set<String> matchKeys = scanKeysByPrefix(prefix);
            if (CollUtil.isEmpty(matchKeys)) {
                return ListUtil.of();
            }
            List<String> result = getStr(matchKeys.toArray(String[]::new));
            if (CollUtil.isEmpty(result)) {
                return ListUtil.of();
            }
            return result;
        } catch (Exception e) {
            log.warn("redis getStrOnScan方法异常", e);
            return ListUtil.of();
        }
    }

    /**
     * 单个前缀的key扫描（封装scan逻辑，避免重复代码）
     *
     * @param prefix 单个前缀（如 "user:100:"）
     * @return 匹配该前缀的所有key
     */
    public Set<String> scanKeysByPrefix(String prefix) {
        Set<String> keys = SetUtil.of();
        // 每次扫描100个key（可根据Redis性能调整，不宜过大）
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(prefix + "*").count(100).build())) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            throw new RuntimeException("扫描Redis前缀[" + prefix + "]的key失败", e);
        }
        return keys;
    }

    /**
     * 设置set元素
     *
     * @param key   钥匙
     * @param value 值
     * @return boolean
     */
    public boolean sadd(String key, Object value) {
        try {
            redisTemplate.opsForSet().add(key, value);
            return true;
        } catch (Exception e) {
            log.warn("redis sadd方法异常", e);
            return false;
        }
    }

    /**
     * 获取set所有元素
     *
     * @param key 钥匙
     * @return boolean
     */
    public Set<Object> smembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.warn("redis smembers方法异常", e);
            return SetUtil.of();
        }
    }

    /**
     * 删除set元素
     *
     * @param key   钥匙
     * @param value 值
     * @return boolean
     */
    public boolean srem(String key, Object value) {
        try {
            redisTemplate.opsForSet().remove(key, value);
            return true;
        } catch (Exception e) {
            log.warn("redis srem方法异常", e);
            return false;
        }
    }

    /**
     * 获取set元数量
     *
     * @param key 钥匙
     * @return 数量
     */
    public int scard(String key) {
        try {
            Long size = redisTemplate.opsForSet().size(key);
            if (null == size) {
                return 0;
            }
            return size.intValue();
        } catch (Exception e) {
            log.warn("redis scard方法异常", e);
            return 0;
        }
    }

}