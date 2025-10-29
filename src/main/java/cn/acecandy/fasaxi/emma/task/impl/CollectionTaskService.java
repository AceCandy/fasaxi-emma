package cn.acecandy.fasaxi.emma.task.impl;

import cn.acecandy.fasaxi.emma.config.EmbyConfig;
import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import cn.acecandy.fasaxi.emma.dao.toolkit.service.CustomCollectionsDao;
import cn.acecandy.fasaxi.emma.sao.client.RedisClient;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.convert.ConvertUtil;
import cn.hutool.v7.core.lang.Console;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.text.split.SplitUtil;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.hutool.v7.core.text.StrPool.COMMA;

/**
 * 自定义合集任务 实现
 *
 * @author tangningzhu
 * @since 2025/3/3
 */
@Slf4j
@Component
public class CollectionTaskService {

    @Resource
    private EmbyConfig embyConfig;

    @Resource
    private CustomCollectionsDao customCollectionsDao;

    @Resource
    private EmbyProxy embyProxy;

    @Resource
    private RedisClient redisClient;

    /**
     * 有效猫眼平台
     */
    private static final List<String> VALID_MAOYAN_PLATFORMS =
            List.of("tencent", "iqiyi", "youku", "mango");

    public void syncCollection() {
        List<CustomCollections> collections = customCollectionsDao.findAllByStatus("active");
        if (CollUtil.isEmpty(collections)) {
            return;
        }
        collections.forEach((v) -> {
            long start = System.currentTimeMillis();
            try {
                obtainCollectionToRedis(v.getId());
            } catch (Exception e) {
                log.warn("[同步自定义合集-{}:{}] 异常！", v.getName(), v.getId(), e);
            } finally {
                log.warn("[同步自定义合集-{}:{}] 执行耗时: {}ms", v.getName(), v.getId(),
                        System.currentTimeMillis() - start);
            }
        });
    }

    private void obtainCollectionToRedis(Long id) {
        if (null == id) {
            return;
        }
        CustomCollections coll = customCollectionsDao.getById(id);
        if (null == coll) {
            log.warn("[同步自定义合集-id:{}] 不存在！", id);
            return;
        }
        String collectionType = coll.getType();
        JSONObject definition = JSONUtil.parseObj(coll.getDefinitionJson());
        // if (StrUtil.equals(collectionType, "list")) {
        //     // 列表类型合集
        //     ListImporter importer = new ListImporter(processor.getTmdbApiKey());
        //     ImportResult result = importer.process(definition);
        //     tmdbItems = result.getTmdbItems();
        // } else if (StrUtil.equals(collectionType, "filter")) {
        //     // 筛选器类型合集
        //     FilterEngine engine = new FilterEngine();
        //     tmdbItems = engine.executeFilter(definition);
        // }


        // List<EmbyItem> items = embyProxy.getItemsByParentIdOnLock(parentId);
        // if (CollUtil.isEmpty(items)) {
        //     return;
        // }
        // redisClient.set(CacheUtil.buildItemsCacheKey(parentId), items);
        // redisClient.set(CacheUtil.buildItemsIdCacheKey(parentId),
        //         StrUtil.join(COMMA, items.stream().map(EmbyItem::getItemId).toList()));
    }

    /**
     * python 转为java代码
     * def process(self, definition: Dict) -> Tuple[List[Dict[str, str]], str]:
     * url = definition.get('url')
     * source_type = 'list_rss'
     * <p>
     * if not url:
     * return [], source_type
     * <p>
     * # ★★★ 核心修正：在这里直接处理猫眼逻辑 ★★★
     * if url.startswith('maoyan://'):
     * source_type = 'list_maoyan'
     * logger.info(f"  ➜ 检测到猫眼榜单，将启动异步后台脚本...")
     * # 使用 gevent 异步执行耗时的子进程调用
     * greenlet = gevent.spawn(self._execute_maoyan_fetch, definition)
     * # .get() 会等待 greenlet 执行完毕并返回结果
     * tmdb_items = greenlet.get()
     * return tmdb_items, source_type
     * <p>
     * # --- 对于非猫眼榜单，保持原有逻辑不变 ---
     * item_types = definition.get('item_type', ['Movie'])
     * if isinstance(item_types, str): item_types = [item_types]
     * limit = definition.get('limit')
     * <p>
     * # ★★★ 核心修改 2/2: 接收 _get_titles_and_imdbids_from_url 返回的 source_type ★★★
     * items, source_type = self._get_titles_and_imdbids_from_url(url)
     * <p>
     * if not items: return [], source_type
     * <p>
     * if items and 'id' in items[0] and 'type' in items[0]:
     * logger.info(f"  ➜ 检测到来自TMDb源 ({source_type}) 的预匹配ID，将跳过标题匹配。")
     * if limit and isinstance(limit, int) and limit > 0:
     * items = items[:limit]
     * return items, source_type # 直接返回结果和类型
     * <p>
     * if limit and isinstance(limit, int) and limit > 0:
     * items = items[:limit]
     * <p>
     * tmdb_items = []
     * douban_api = DoubanApi()
     * <p>
     * with ThreadPoolExecutor(max_workers=5) as executor:
     * def find_first_match(item: Dict[str, str], types_to_check):
     * title = item.get('title')
     * year = item.get('year')
     * rss_imdb_id = item.get('imdb_id')
     * douban_link = item.get('douban_link')
     * <p>
     * cleaned_title_for_parsing = re.sub(r'^\s*\d+\.\s*', '', title)
     * cleaned_title_for_parsing = re.sub(r'\s*\(\d{4}\)$', '', cleaned_title_for_parsing).strip()
     * _, season_number = self._parse_series_title(cleaned_title_for_parsing)
     * <p>
     * def create_result(tmdb_id, item_type):
     * result = {'id': tmdb_id, 'type': item_type}
     * if item_type == 'Series' and season_number is not None:
     * logger.debug(f"  ➜ 为剧集 '{title}' 附加季号: {season_number}")
     * result['season'] = season_number
     * return result
     * <p>
     * if rss_imdb_id:
     * for item_type in types_to_check:
     * # _match_by_ids 只返回 tmdb_id，这部分逻辑正确
     * tmdb_id = self._match_by_ids(rss_imdb_id, None, item_type)
     * if tmdb_id:
     * logger.info(f"  ➜ 成功通过RSS自带的IMDb ID '{rss_imdb_id}' 匹配到 '{title}'。")
     * return create_result(tmdb_id, item_type)
     * <p>
     * cleaned_title = re.sub(r'^\s*\d+\.\s*', '', title)
     * cleaned_title = re.sub(r'\s*\(\d{4}\)$', '', cleaned_title).strip()
     * for item_type in types_to_check:
     * # _match_title_to_tmdb 现在返回 (tmdb_id, item_type) 或 None
     * match_result = self._match_title_to_tmdb(cleaned_title, item_type, year=year)
     * if match_result:
     * tmdb_id, matched_type = match_result
     * return create_result(tmdb_id, matched_type)
     * <p>
     * if douban_link:
     * logger.info(f"  ➜ 片名+年份匹配 '{title}' 失败，启动备用方案：通过豆瓣链接获取更多信息...")
     * douban_details = douban_api.get_details_from_douban_link(douban_link, mtype=types_to_check[0] if types_to_check else None)
     * <p>
     * if douban_details:
     * imdb_id_from_douban = douban_details.get("imdb_id")
     * if not imdb_id_from_douban and douban_details.get("attrs", {}).get("imdb"):
     * imdb_ids = douban_details["attrs"]["imdb"]
     * if isinstance(imdb_ids, list) and len(imdb_ids) > 0:
     * imdb_id_from_douban = imdb_ids[0]
     * <p>
     * if imdb_id_from_douban:
     * logger.info(f"  ➜ 豆瓣备用方案(3a)成功！拿到IMDb ID: {imdb_id_from_douban}，现在用它匹配TMDb...")
     * for item_type in types_to_check:
     * tmdb_id = self._match_by_ids(imdb_id_from_douban, None, item_type)
     * if tmdb_id:
     * return create_result(tmdb_id, item_type)
     * <p>
     * logger.info(f"  ➜ 豆瓣备用方案(3a)失败，尝试方案(3b): 使用 original_title...")
     * original_title = douban_details.get("original_title")
     * if original_title:
     * for item_type in types_to_check:
     * match_result = self._match_title_to_tmdb(original_title, item_type, year=year)
     * if match_result:
     * tmdb_id, matched_type = match_result
     * logger.info(f"  ➜ 豆瓣备用方案(3b)成功！通过 original_title '{original_title}' 匹配成功。")
     * return create_result(tmdb_id, matched_type)
     * <p>
     * logger.debug(f"  ➜ 所有优先方案均失败，尝试不带年份进行最后的回退搜索: '{title}'")
     * for item_type in types_to_check:
     * match_result = self._match_title_to_tmdb(cleaned_title, item_type, year=None)
     * if match_result:
     * tmdb_id, matched_type = match_result
     * logger.warning(f"  ➜ 注意：'{title}' 在最后的回退搜索中匹配成功，但年份可能不准。")
     * return create_result(tmdb_id, matched_type)
     * <p>
     * logger.error(f"  ➜ 彻底失败：所有方案都无法为 '{title}' 找到匹配项。")
     * return None
     * <p>
     * results_in_order = executor.map(lambda item: find_first_match(item, item_types), items)
     * tmdb_items = [result for result in results_in_order if result is not None]
     * <p>
     * douban_api.close()
     * logger.info(f"  ➜ RSS匹配完成，成功获得 {len(tmdb_items)} 个TMDb项目。")
     * <p>
     * unique_items = list({f"{item['type']}-{item['id']}-{item.get('season')}": item for item in tmdb_items}.values())
     * return unique_items, source_type
     *
     * @param definition
     * @return
     */
    private Object handleCollectionList(JSONObject definition) {
        String url = definition.getStr("url");
        if (StrUtil.isBlank(url)) {
            return null;
        }
        String sourceType = "list_rss";
        if (StrUtil.startWith(url, "maoyan://")) {
            sourceType = "list_maoyan";
        }


        return null;
    }

    private void handleMaoyan(String url,Integer limit) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        limit = null != limit ? limit : 50;
        List<String> keys = SplitUtil.splitTrim(StrUtil.removePrefix(url, "maoyan://"), COMMA);
        if (CollUtil.isNotEmpty(keys)) {
            return;
        }
        String platform = "all";



        // return tmdb_items,source_type
    }

    static void main() {
        Console.log(ConvertUtil.chineseToNumber("二十三"));
    }
}