package cn.acecandy.fasaxi.emma.common.resp;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * 视频信息 出参
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Data
public class EmbyItemsResp {
    /**
     * emby项目
     */
    @Alias("Items")
    private List<EmbyItems> items;
    /**
     * 总记录数
     */
    @Alias("TotalRecordCount")
    private Integer totalRecordCount;

    @Data
    public static class EmbyItems {
        /**
         * 文件名称
         */
        @Alias("Name")
        private String name;
        /**
         * 文件id
         */
        @Alias("Id")
        private String id;
        /**
         * 媒体来源信息
         */
        @Alias("MediaSources")
        private List<MediaSources> mediaSources;
        /**
         * 本地路径
         */
        @Alias("Path")
        private String path;

        @Data
        public static class MediaSources {
            /**
             * strm具体路径
             */
            @Alias("Path")
            private String path;
        }
    }

    /*@SneakyThrows
    public static void main(String[] args) {
        List<String> existAnswer = CollUtil.newArrayList();
        List<Entity> buffer = CollUtil.newArrayList();
        Db db = Db.use();
        List<Entity> en = db.find(CollUtil.newArrayList("answer"), Entity.create("game_ktccy")
                .set("source", 4));
        en.forEach(e -> {
            existAnswer.add(e.getStr("answer"));
        });
        Console.log("添加{}个存在答案", existAnswer.size());
        String u = "https://api.s01s.cn/API/ktccy/";
        String res = "";
        JSONObject jn;
        String url;
        String answer;
        while (true) {
            try {
                res = HttpUtil.get(StrUtil.format(u, ""));
                jn = JSONUtil.parseObj(res);
                if (jn.getInt("状态", 500) != 0) {
                    Console.log("请求失败,{}", res);
                    continue;
                }
                // jn = jn.getJSONObject("data");
                url = jn.getStr("图片");
                answer = jn.getStr("答案");
                if (existAnswer.contains(answer)) {
                    System.out.print(".");
                    continue;
                } else {
                    Console.log("添加答案{}", answer);
                    existAnswer.add(answer);
                }
                buffer.add(Entity.create("game_ktccy")
                        .set("pic_url", url)
                        .set("answer", answer)
                        .set("source", 4));

                if (buffer.size() >= 10 || RandomUtil.randomInt(1, 10) < 4) {
                    db.insert(buffer);
                    CollUtil.clear(buffer);
                }
                ThreadUtil.safeSleep(200);
            } catch (cn.hutool.http.HttpException he) {
                System.err.print("X");
                ThreadUtil.safeSleep(8000);
            }catch (Exception e) {
                Console.error("插入失败", e);
                ThreadUtil.safeSleep(1000);
            }
        }
    }*/
}