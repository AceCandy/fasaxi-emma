package cn.acecandy.fasaxi.emma.utils;

import lombok.extern.slf4j.Slf4j;


/**
 * 排序工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class ThreadUtil extends org.dromara.hutool.core.thread.ThreadUtil {

    private ThreadUtil() {
    }

    public static void execVirtual(Runnable task) {
        Thread.startVirtualThread(task);
    }
}