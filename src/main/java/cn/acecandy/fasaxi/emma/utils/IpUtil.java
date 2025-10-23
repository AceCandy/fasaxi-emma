package cn.acecandy.fasaxi.emma.utils;

import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.net.NetUtil;
import cn.hutool.v7.core.text.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * ip工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class IpUtil extends NetUtil {

    private IpUtil() {
    }

    /**
     * 判断IP是否为内网地址
     */
    public static boolean isInnerIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            // 检查是否为本地回环地址
            if (address.isLoopbackAddress()) return true;
            // 检查是否为链路本地地址（如169.254.x.x）
            if (address.isLinkLocalAddress()) return true;
            // 检查是否为站点本地地址（IPv4私有地址）
            if (address.isSiteLocalAddress()) return true;
            return NetUtil.isInnerIP(ip);
        } catch (UnknownHostException e) {
            // 无效IP地址，默认为外网
            return false;
        }
    }

    /**
     * 判断请求是否来自内网
     */
    public static boolean isInnerIp(HttpServletRequest request) {
        // 先尝试从X-Forwarded-For获取客户端IP（处理代理情况）
        String xffHeader = request.getHeader("X-Forwarded-For");
        String clientIp;
        if (StrUtil.isNotBlank(xffHeader)) {
            // 多个IP时取第一个（客户端真实IP）
            clientIp = xffHeader.split(",")[0].trim();
        } else {
            // 没有代理，直接获取远程地址
            clientIp = request.getRemoteAddr();
        }
        return isInnerIp(clientIp);
    }

    public static void main(String[] args) {
        Console.log(isInnerIp("192.168.1.204"));
    }
}