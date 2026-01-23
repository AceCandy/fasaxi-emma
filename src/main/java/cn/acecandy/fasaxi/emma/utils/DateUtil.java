package cn.acecandy.fasaxi.emma.utils;


import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.hutool.v7.core.date.DateField;
import cn.hutool.v7.core.date.DateFormatPool;
import cn.hutool.v7.core.date.DateTime;
import cn.hutool.v7.core.exception.ValidateException;
import cn.hutool.v7.core.text.StrUtil;

import java.util.List;

/**
 * 日期 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class DateUtil extends cn.hutool.v7.core.date.DateUtil {

    public static final String 时间阶段_日 = "day";
    public static final String 时间阶段_月 = "month";
    public static final List<String> 时间阶段 = List.of(时间阶段_月, 时间阶段_日);

    /**
     * 获取上一个时间阶段的时间
     *
     * @param timeStage        时间阶段
     * @param currentTimeStage 当前时间段
     * @return {@link String }
     */
    public static String getLastTimeStage(String timeStage, String currentTimeStage) {
        if (StrUtil.isBlank(timeStage)) {
            throw new ValidateException(StrUtil.format(" timeStage 不能为空"));
        }
        DateTime currentDateTime = now();
        if (StrUtil.isNotBlank(currentTimeStage)) {
            currentDateTime = parse(currentTimeStage);
        }
        if (时间阶段_日.equals(timeStage)) {
            currentDateTime.offset(DateField.DAY_OF_MONTH, -1);
            return DateFormatPool.NORM_DATE_FORMAT.format(currentDateTime);
        } else if (时间阶段_月.equals(timeStage)) {
            currentDateTime.offset(DateField.MONTH, -1);
            return DateFormatPool.NORM_MONTH_FORMAT.format(currentDateTime);
        }
        throw new BaseException("不支持的时间阶段");
    }

    /**
     * 获取上一个时间阶段的时间
     *
     * @param timeStage 时间阶段
     * @return {@link String }
     */
    public static String getLastTimeStage(String timeStage) {
        return getLastTimeStage(timeStage, null);
    }
}