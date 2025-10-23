package cn.acecandy.fasaxi.emma.utils;

import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.extra.pinyin.PinyinUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

import static cn.hutool.v7.core.text.CharSequenceUtil.subPre;
import static cn.hutool.v7.core.text.CharSequenceUtil.upperFirst;
import static cn.hutool.v7.core.text.StrPool.SPACE;
import static cn.hutool.v7.core.text.StrValidator.EMPTY;
import static cn.hutool.v7.core.text.StrValidator.isBlank;


/**
 * 拼音工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class PinYinUtil extends PinyinUtil {

    private PinYinUtil() {
    }

    /**
     * 将字符串中的中文转化为拼音,其他字符不变(全大写输出)
     *
     * @param chinese 中文字符串
     * @return {@link String}
     */
    public static String getPingYin(String chinese) {
        if (isBlank(chinese)) {
            return EMPTY;
        }
        return PinyinUtil.getPinyin(chinese, EMPTY).toUpperCase(Locale.ROOT);
    }

    public static List<String> getPingYinList(String chinese) {
        if (isBlank(chinese)) {
            return List.of();
        }
        return SplitUtil.splitTrim(PinyinUtil.getPinyin(chinese), SPACE);
    }

    /**
     * 获取中文首字母对应的数值
     * <p>
     * 不区分大小写
     *
     * @param chinese 中文
     * @return int
     */
    public static int getFirstInt(String chinese) {
        if (isBlank(chinese)) {
            return 0;
        }
        return getFirstLetter(chinese).charAt(0);
    }


    /**
     * 获取字符串中第一个字符首字母,若第一个字符不是中文则返回第一个字符
     * <p>
     * 不区分大小写 返回大写
     *
     * @param chinese 中文
     * @return String
     */
    public static String getFirstLetter(String chinese) {
        if (isBlank(chinese)) {
            return EMPTY;
        }
        String letterStr = PinyinUtil.getFirstLetter(chinese, EMPTY);
        return upperFirst(subPre(letterStr, 1));
    }

    /**
     * 获取拼音首字母 比如重庆->CQ
     *
     * @param chinese 中国人
     * @return {@link String }
     */
    public static String getFirstLetters(String chinese) {
        if (isBlank(chinese)) {
            return EMPTY;
        }
        String letterStr = PinyinUtil.getFirstLetter(chinese, EMPTY);
        return letterStr.toUpperCase();
    }

    /**
     * 查找字符在所有文字中存在大于2
     *
     * @param text 文本
     * @param have 有
     * @return boolean
     */
    public static boolean findTwoChar(String text, String have) {
        if (isBlank(text)) {
            return false;
        }
        int count = 0;
        for (char c : have.toCharArray()) {
            if (StrUtil.contains(text, c)) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     * 查找字符在所有文字中存在
     *
     * @param text 文本
     * @param have 有
     * @return boolean
     */
    public static boolean findAllChar(String text, String have) {
        if (isBlank(text)) {
            return false;
        }
        for (char c : have.toCharArray()) {
            if (!StrUtil.contains(text, c)) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        Console.log(findAllChar("奇奇怪怪的曲", "曲奇1"));
        Console.log(PinYinUtil.findAllChar("白色板子", "白板"));
        Console.log(PinYinUtil.findTwoChar("白色板子", "白板"));
        Console.log(PinYinUtil.findTwoChar("白色板子", "白"));
        Console.log(PinYinUtil.findTwoChar("奇奇怪怪的曲", "曲奇1"));
    }
}