package cn.acecandy.fasaxi.emma.utils;


import cn.acecandy.fasaxi.emma.sao.entity.MatchedItem;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.math.NumberUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.core.util.RandomUtil;
import cn.hutool.v7.core.xml.XPathUtil;
import cn.hutool.v7.core.xml.XmlUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import java.util.List;

/**
 * emby 工具类
 *
 * @author tangningzhu
 * @since 2024/10/16
 */
public final class HtmlUtil extends cn.hutool.v7.http.html.HtmlUtil {
    private HtmlUtil() {
    }

    /**
     * 随机用户代理
     */
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0",
            "Mozilla/5.0 (X11; Linux x86_64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    );

    /**
     * 随机用户代理
     *
     * @return {@link String }
     */
    public static String randomUserAgent() {
        return RandomUtil.randomEle(USER_AGENTS);
    }

    /**
     * 解析豆列
     *
     * @param html 超文本标记语言
     * @return {@link List }<{@link MatchedItem.Doulist }>
     */
    public static List<MatchedItem.Doulist> parseDoulist(String html) {
        if (StrUtil.isBlank(html)) {
            return null;
        }
        List<MatchedItem.Doulist> allItems = ListUtil.of();
        Document soup = Jsoup.parse(html);
        // 查找页面上所有的条目容器
        Elements doulistItems = soup.select("div.doulist-item");
        if (doulistItems.isEmpty()) {
            return null;
        }

        for (Element item : doulistItems) {
            Element titleDiv = item.selectFirst("div.title");
            if (titleDiv == null) continue;

            Element linkTag = titleDiv.selectFirst("a");
            if (linkTag == null) continue;

            // 提取标题
            String title = linkTag.text().trim();
            // 提取豆瓣链接
            String doubanLink = linkTag.attr("href");
            String doubanId = ReUtil.findDouBanIdByJson(doubanLink);

            // 尝试提取年份
            Integer year = null;
            Element abstractDiv = item.selectFirst("div.abstract");
            if (abstractDiv != null) {
                year = NumberUtil.parseInt(ReUtil.parseYearByHtml(abstractDiv.text()), null);
            }

            if (StrUtil.isNotBlank(title)) {
                allItems.add(new MatchedItem.Doulist(title, year, doubanLink, doubanId, null));
            }
        }
        return allItems;
    }

    /**
     * 解析豆瓣rss
     *
     * @param xml 可扩展标记语言
     * @return {@link List }<{@link MatchedItem.Doulist }>
     */
    public static List<MatchedItem.Doulist> parseDoubanRss(String xml) {
        if (StrUtil.isBlank(xml)) {
            return null;
        }
        List<MatchedItem.Doulist> allItems = ListUtil.of();
        org.w3c.dom.Document doc = XmlUtil.readXml(xml);
        NodeList items = XPathUtil.getNodeListByXPath("//rss/channel/item", doc);
        for (int i = 0; i < items.getLength(); i++) {
            Node itemNode = items.item(i);
            if (itemNode.getNodeType() != Node.ELEMENT_NODE) {
                continue; // 过滤非元素节点（如文本、注释）
            }
            String title = XPathUtil.getByXPath("title", itemNode, XPathConstants.STRING).toString().trim();
            // 会包含中文名和其他名字 所有按照空格分割之后取第一个 待观察 可能有bug
            title = CollUtil.getFirst(SplitUtil.split(title, " "));
            String link = XPathUtil.getByXPath("link", itemNode, XPathConstants.STRING).toString().trim();
            String doubanId = ReUtil.findDouBanIdByJson(link);
            String description = XPathUtil.getByXPath("description",
                    itemNode, XPathConstants.STRING).toString().trim();
            Integer year = NumberUtil.parseInt(ReUtil.parseYearByHtml(description), null);

            allItems.add(new MatchedItem.Doulist(title, year, link, doubanId, null));
        }
        return allItems;
    }

    /**
     * 标准化字符串（剔除无用字段）
     *
     * @param s s
     * @return {@link String }
     */
    public static String normalizeString(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replaceAll("[\\s:：·\\-*'!,?.。]+", "").toLowerCase();
    }

}