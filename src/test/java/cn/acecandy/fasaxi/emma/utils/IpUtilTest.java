package cn.acecandy.fasaxi.emma.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpUtilTest {

    @Test
    void isInnerIp_shouldTreatBlankLoopbackAndPrivateIpAsInner() {
        assertTrue(IpUtil.isInnerIp(""));
        assertTrue(IpUtil.isInnerIp("127.0.0.1"));
        assertTrue(IpUtil.isInnerIp("192.168.1.204"));
    }

    @Test
    void isInnerIp_shouldTreatPublicAndInvalidIpAsOuter() {
        assertFalse(IpUtil.isInnerIp("8.8.8.8"));
        assertFalse(IpUtil.isInnerIp("not-an-ip"));
    }
}
