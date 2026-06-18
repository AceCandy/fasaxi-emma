package cn.acecandy.fasaxi.emma.utils;


/**
 * 排序工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
public final class ThreadUtil extends cn.hutool.v7.core.thread.ThreadUtil {

    private ThreadUtil() {
    }

    public static void execVirtual(Runnable task) {
        Thread.startVirtualThread(task);
    }
}
