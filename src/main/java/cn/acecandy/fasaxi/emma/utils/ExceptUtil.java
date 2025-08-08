package cn.acecandy.fasaxi.emma.utils;


import org.dromara.hutool.core.exception.ExceptionUtil;
import org.dromara.hutool.core.text.StrUtil;

import java.io.IOException;

/**
 * 异常 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class ExceptUtil extends ExceptionUtil {
    private ExceptUtil() {
    }

    /**
     * undertow使用
     *
     * @param e e
     * @return boolean
     */
    public static boolean isConnectionTerminated(IOException e) {
        String msg = getSimpleMessage(e);
        return StrUtil.containsAnyIgnoreCase(msg, "Connection terminated",
                "Broken pipe", "Connection reset by peer");
    }
}