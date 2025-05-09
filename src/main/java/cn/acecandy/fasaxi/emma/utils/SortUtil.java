package cn.acecandy.fasaxi.emma.utils;

import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemsInfoOut;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.dromara.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cn.acecandy.fasaxi.emma.utils.ReUtil.REGEX_SPILT_TITLE;


/**
 * 排序工具类
 *
 * @author tangningzhu
 * @since 2023/12/20
 */
@Slf4j
public final class SortUtil {

    private SortUtil() {
    }

    private static final String[] REMOVE_SYMBOL = {",", ".", "，", "。", "；",
            ";", "'", "\"", "“", "”", "‘", "’", "：", ":", "【", "】"};

    /**
     * 搜索排序
     *
     * @param items  项目
     * @param search 搜索
     * @return {@link List }<{@link EmbyItem }>
     */
    public static List<EmbyItem> searchSortItem(List<EmbyItem> items, String search) {
        // 全匹配
        List<EmbyItem> exactMatch = ListUtil.of();
        // 全匹配(剔除符号)
        List<EmbyItem> noSymbolExactMatch = ListUtil.of();
        // 分割后全匹配(符号分割)
        List<EmbyItem> splitExactMatch = ListUtil.of();
        // 无书名号
        List<EmbyItem> containsNoBracket = ListUtil.of();
        // 包含有书名号
        List<EmbyItem> containsWithBracket = ListUtil.of();
        // 其他元素
        List<EmbyItem> others = ListUtil.of();

        // 第一遍遍历：分组处理
        for (EmbyItem item : items) {
            String name = item.getName();
            String noSymbolName = StrUtil.removeAll(name, REMOVE_SYMBOL);
            List<String> splitName = SplitUtil.splitByRegex(
                    name, REGEX_SPILT_TITLE, 0, true, true);

            if (StrUtil.equalsIgnoreCase(name, search)) {
                exactMatch.add(item);
            } else if (StrUtil.equalsIgnoreCase(noSymbolName, search)) {
                noSymbolExactMatch.add(item);
            } else if (CollUtil.contains(splitName, search)) {
                splitExactMatch.add(item);
            } else if (StrUtil.containsIgnoreCase(name, search)) {
                if (hasBookBrackets(name)) {
                    containsWithBracket.add(item);
                } else {
                    containsNoBracket.add(item);
                }
            } else {
                others.add(item);
            }
        }

        // 定义包含组的排序规则（关键字位置 + 年份降序）
        Comparator<EmbyItem> containsComparator = Comparator
                .comparingInt((EmbyItem b) -> StrUtil.lastIndexOfIgnoreCase(b.getName(), search))
                .thenComparingInt(EmbyItem::getProductionYear).reversed();

        Comparator<EmbyItem> matchComparator = Comparator.comparingInt(EmbyItem::getProductionYear).reversed();

        // 分组排序
        exactMatch.sort(matchComparator);
        noSymbolExactMatch.sort(matchComparator);
        splitExactMatch.sort(matchComparator);
        containsNoBracket.sort(containsComparator);
        containsWithBracket.sort(containsComparator);

        // 合并结果（保持others原序）
        List<EmbyItem> result = new ArrayList<>();
        result.addAll(exactMatch);
        result.addAll(noSymbolExactMatch);
        result.addAll(splitExactMatch);
        result.addAll(containsNoBracket);
        result.addAll(containsWithBracket);
        result.addAll(others);

        return result;
    }

    /**
     * 有书名号
     *
     * @param name 名称
     * @return boolean
     */
    private static boolean hasBookBrackets(String name) {
        return StrUtil.containsAny(name, "《", "》");
    }


    public static void main(String[] args) {
        String s = "{\"Items\":[{\"Name\":\"再次人生\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"1107267\",\"CanDelete\":false,\"CanDownload\":false,\"SortName\":\"ZCRS\",\"Path\":\"/vol2/1000/strm-REDMT2/国产剧/再次人生 (2025)\",\"ProductionYear\":2025,\"IsFolder\":true,\"Type\":\"Series\",\"UserData\":{\"UnplayedItemCount\":24,\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"Status\":\"Ended\",\"AirDays\":[],\"PrimaryImageAspectRatio\":0.6666666666666666,\"ImageTags\":{\"Primary\":\"be9fcfad1beaeff9f53d43640d36d934\"},\"BackdropImageTags\":[\"27aca8fe84ff54764b3c5d7877ad102c\"],\"EndDate\":\"2025-02-01T00:00:00.0000000Z\"},{\"Name\":\"僵尸再翻生\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"85316\",\"CanDelete\":false,\"CanDownload\":false,\"Container\":\"mp4\",\"SortName\":\"JSZFS\",\"Path\":\"/vol2/1000/strm-REDMT/华语电影/僵尸再翻生 (1986)/僵尸再翻生 (1986) - 4k - OPS.strm\",\"CommunityRating\":6.2,\"RunTimeTicks\":45297490000,\"ProductionYear\":1986,\"IsFolder\":false,\"Type\":\"Movie\",\"UserData\":{\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"PrimaryImageAspectRatio\":0.6666666666666666,\"ImageTags\":{\"Primary\":\"f5a57f97584982aa60b564ee08b57f97\"},\"BackdropImageTags\":[\"cdcee7e4e051efa028d35f1dcfa694be\"],\"MediaType\":\"Video\"},{\"Name\":\"叶总来生不愿再相见\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"1367527\",\"CanDelete\":false,\"CanDownload\":false,\"SortName\":\"YZLSBYZXJ\",\"Path\":\"/vol2/1000/strm-123/短剧/叶总来生不愿再相见\",\"IsFolder\":true,\"Type\":\"Series\",\"UserData\":{\"UnplayedItemCount\":74,\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"AirDays\":[],\"PrimaryImageAspectRatio\":0.6658322903629537,\"ImageTags\":{\"Primary\":\"928b46ec875a476f3a0f3d1acf4a6cf1\"},\"BackdropImageTags\":[]},{\"Name\":\"诈欺游戏：再生\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"797179\",\"CanDelete\":false,\"CanDownload\":false,\"Container\":\"mkv\",\"SortName\":\"ZQYX：ZS\",\"Path\":\"/vol2/1000/strm-REDMT2/外语电影/诈欺游戏：再生 (2012)/诈欺游戏：再生 (2012) - 1080p - CHD.strm\",\"CommunityRating\":6.4,\"RunTimeTicks\":78278300000,\"ProductionYear\":2012,\"IsFolder\":false,\"Type\":\"Movie\",\"UserData\":{\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"PrimaryImageAspectRatio\":0.6666666666666666,\"ImageTags\":{\"Primary\":\"d51695e2e7b5db1cdb637929f3b8cc7f\"},\"BackdropImageTags\":[\"9fb6f30453b9384f45d7f8820149f03a\"],\"MediaType\":\"Video\"},{\"Name\":\"生化危机4：战神再生\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"1250726\",\"CanDelete\":false,\"CanDownload\":false,\"Container\":\"mkv\",\"SortName\":\"SHWJ4：ZSZS\",\"Path\":\"/vol2/1000/strm-REDMT/外语电影/生化危机4：战神再生 (2010)/生化危机4：战神再生 (2010) - 2160p - REDMT.strm\",\"OfficialRating\":\"R\",\"CommunityRating\":6.057,\"RunTimeTicks\":58087630000,\"ProductionYear\":2010,\"IsFolder\":false,\"Type\":\"Movie\",\"UserData\":{\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"PrimaryImageAspectRatio\":0.6666666666666666,\"ImageTags\":{\"Primary\":\"5a7ef2c68d54831b52af0fe9d44d4f42\",\"Thumb\":\"5e3197e4036b36a474d1ea39f82d8ae5\"},\"BackdropImageTags\":[\"7b0f347c871486c310dd532e6bc60f7f\"],\"MediaType\":\"Video\"},{\"Name\":\"重生七零再高嫁\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"547437\",\"CanDelete\":false,\"CanDownload\":false,\"SortName\":\"ZSQLZGJ\",\"Path\":\"/vol2/1000/strm-115/短剧/重生七零再高嫁 (2024)\",\"ProductionYear\":2024,\"IsFolder\":true,\"Type\":\"Series\",\"UserData\":{\"UnplayedItemCount\":82,\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"Status\":\"Continuing\",\"AirDays\":[],\"PrimaryImageAspectRatio\":0.6666666666666666,\"ImageTags\":{\"Primary\":\"9ee6289740eaa8e0cbe055775f82aba3\"},\"BackdropImageTags\":[]},{\"Name\":\"再见龙生，你好人生\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"397730\",\"CanDelete\":false,\"CanDownload\":false,\"SortName\":\"ZJLS，NHRS\",\"Path\":\"/vol2/1000/strm-REDMT2/动漫/再见龙生你好人生 (2024)\",\"OfficialRating\":\"BR-16\",\"ProductionYear\":2024,\"IsFolder\":true,\"Type\":\"Series\",\"UserData\":{\"UnplayedItemCount\":12,\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"Status\":\"Continuing\",\"AirDays\":[],\"PrimaryImageAspectRatio\":0.7079646017699115,\"ImageTags\":{\"Primary\":\"4d4771f731a5b0f00487f54bb5b72557\"},\"BackdropImageTags\":[\"3978f8e3163d07422ee681bdab1eae0c\"]},{\"Name\":\"人生半载，重头再来\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"685464\",\"CanDelete\":false,\"CanDownload\":false,\"SortName\":\"RSBZ，ZTZL\",\"Path\":\"/vol2/1000/strm-115/短剧2/人生半载，重头再来\",\"IsFolder\":true,\"Type\":\"Series\",\"UserData\":{\"UnplayedItemCount\":30,\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"AirDays\":[],\"PrimaryImageAspectRatio\":0.6666666666666666,\"ImageTags\":{\"Primary\":\"a773ca5b8e215e083ccbf28611fddecd\"},\"BackdropImageTags\":[]},{\"Name\":\"地下11楼的森崎《再生门》重生之我是你爹！\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"1367091\",\"CanDelete\":false,\"CanDownload\":false,\"Container\":\"mp4\",\"SortName\":\"DX11LDSQ《ZSM》ZSZWSND！\",\"Path\":\"/vol2/1000/strm-123/B站-解说视频/2333958_地下11楼的森崎/地下11楼的森崎《再生门》重生之我是你爹！/再生门B站成片(BV1XZ4y11718).strm\",\"RunTimeTicks\":5987630000,\"ProductionYear\":2022,\"IsFolder\":false,\"Type\":\"Movie\",\"UserData\":{\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"PrimaryImageAspectRatio\":1.599835661462613,\"ImageTags\":{\"Primary\":\"628b2003a98d6166696fda7d3b69341f\"},\"BackdropImageTags\":[\"7966f679acbdc75c3eccf4fa57d6e0b1\"],\"MediaType\":\"Video\"},{\"Name\":\"重生后王妃再也不逃了\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"782428\",\"CanDelete\":false,\"CanDownload\":false,\"SortName\":\"ZSHWFZYBTL\",\"Path\":\"/vol2/1000/strm-115/短剧3/重生后王妃再也不逃了\",\"IsFolder\":true,\"Type\":\"Series\",\"UserData\":{\"UnplayedItemCount\":81,\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"AirDays\":[],\"PrimaryImageAspectRatio\":0.6666666666666666,\"ImageTags\":{\"Primary\":\"b4ead73153b5bb33f6eee13feec2e31c\"},\"BackdropImageTags\":[]},{\"Name\":\"惊悚哥《再生门》同一时空的不同自己，甚至连身边的人都是穿越而来\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"1335857\",\"CanDelete\":false,\"CanDownload\":false,\"Container\":\"mp4\",\"SortName\":\"JSG《ZSM》TYSKDBTZJ，SZLSBDRDSCYEL\",\"Path\":\"/vol2/1000/strm-123/B站-解说视频/454709958_惊悚哥/惊悚哥《再生门》同一时空的不同自己，甚至连身边的人都是穿越而来/《再生门》：德国冷门惊悚悬疑片(BV1Et4y1P7gD).strm\",\"RunTimeTicks\":10909660000,\"ProductionYear\":2022,\"IsFolder\":false,\"Type\":\"Movie\",\"UserData\":{\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"PrimaryImageAspectRatio\":1.6,\"ImageTags\":{\"Primary\":\"69062e817824a91b3ced62a5c41535d2\"},\"BackdropImageTags\":[\"59bfe6179609072bdc003566022a9b96\"],\"MediaType\":\"Video\"},{\"Name\":\"惊悚哥《惊心食人族2》23年后食人魔再现，把整个校车的学生当做猎物\",\"ServerId\":\"eee6a81e370a4f039be517b686c462b0\",\"Id\":\"1336763\",\"CanDelete\":false,\"CanDownload\":false,\"Container\":\"mp4\",\"SortName\":\"JSG《JXSRZ2》23NHSRMZX，BZGXCDXSDZLW\",\"Path\":\"/vol2/1000/strm-123/B站-解说视频/454709958_惊悚哥/惊悚哥《惊心食人族2》23年后食人魔再现，把整个校车的学生当做猎物/惊心食人族2-成片(BV1nL411g7bU).strm\",\"RunTimeTicks\":7562030000,\"ProductionYear\":2021,\"IsFolder\":false,\"Type\":\"Movie\",\"UserData\":{\"PlaybackPositionTicks\":0,\"PlayCount\":0,\"IsFavorite\":false,\"Played\":false},\"PrimaryImageAspectRatio\":1.5988857938718664,\"ImageTags\":{\"Primary\":\"b02ef8260b7e2d176396ec3967e81312\"},\"BackdropImageTags\":[\"cc55b7caed6a5eed22b6ef0d17686e58\"],\"MediaType\":\"Video\"}],\"TotalRecordCount\":0}";
        EmbyItemsInfoOut itemsInfoOut = JSONUtil.toBean(s, EmbyItemsInfoOut.class);
        Console.log(JSONUtil.toJsonStr(searchSortItem(itemsInfoOut.getItems(), "再生")));
        Console.log(SplitUtil.splitByRegex("再生'魔人: 你好", REGEX_SPILT_TITLE, 0, true, true));
    }
}