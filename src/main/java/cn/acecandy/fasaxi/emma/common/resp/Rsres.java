package cn.acecandy.fasaxi.emma.common.resp;

import cn.acecandy.fasaxi.emma.common.enums.ErrCode;
import cn.acecandy.fasaxi.emma.common.ex.BaseException;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;

import java.util.List;

import static cn.acecandy.fasaxi.emma.common.enums.ErrCode.DEFAULT_CODE;


/**
 * 通用返回
 *
 * @author AceCandy
 * @since 2024/09/14
 */
@Schema(description = "通用返回包装器")
@AllArgsConstructor
@Data
public class Rsres<T> {

    @Schema(title = "响应编码", description = "0-正常，小于0-系统级错误，大于0-业务级错误")
    private Integer returncode;

    @Schema(title = "响应消息", description = "code非0时，message非空")
    private String msg;

    @Schema(title = "响应结果", description = "接口的具体相应结果")
    private T result;

    private static <T> Rsres<T> of(int returncode, String msg, T result) {
        return new Rsres<>(returncode, msg, result);
    }

    private static <T> Rsres<T> of(ErrCode code, T result) {
        return of(code.getCode(), code.getMsg(), result);
    }

    public static <T> Rsres<T> success(T result) {
        return of(DEFAULT_CODE, result);
    }

    public static <T> Rsres<T> success() {
        return of(DEFAULT_CODE, null);
    }

    public static <T> Rsres<T> fail(BaseException ex) {
        return of(ex.getCode(), ex.getMsg(), null);
    }

    public static <T> Rsres<T> fail(ErrCode errCode, String msg) {
        return of(errCode.getCode(), errCode.getMsg() + msg, null);
    }

    public static <T> Rsres<T> fail(ErrCode errCode) {
        return of(errCode, null);
    }

    public static <T> Rsres<T> fail(String msg) {
        return of(ErrCode.FAIL.getCode(), msg, null);
    }

    public static <T> Rsres<T> fail() {
        return of(ErrCode.FAIL, null);
    }

    @JsonIgnore
    public boolean isOk() {
        return returncode == 0;
    }

    @SneakyThrows
    public static void main(String[] args) {
        List<String> existAnswer = CollUtil.newArrayList();
        List<Entity> buffer = CollUtil.newArrayList();
        Db db = Db.use();
        List<Entity> en = db.find(CollUtil.newArrayList("answer"), Entity.create("game_ktccy")
                .set("source", 2));
        en.forEach(e -> {
            existAnswer.add(e.getStr("answer"));
        });
        Console.log(en.size());
        String u = "https://xiaoapi.cn/API/game_ktccy.php?msg={}&id=11111110";
        String res = "";
        JSONObject jn;
        String url;
        String answer;
        while (true) {
            try {
                res = HttpUtil.get(StrUtil.format(u, "开始游戏"));
                jn = JSONUtil.parseObj(res);
                if (jn.getInt("code", 500) != 200) {
                    Console.log("请求失败,{}", res);
                    continue;
                }
                jn = jn.getJSONObject("data");
                url = jn.getStr("pic");
                answer = jn.getStr("answer");
                if (StrUtil.isBlank(answer) || existAnswer.contains(answer)) {
                    System.out.print(".");
                    continue;
                } else {
                    Console.log("添加答案{}", answer);
                    existAnswer.add(answer);
                }
                String tips = "";
                try {
                    res = HttpUtil.get(StrUtil.format(u, "提示"));
                    jn = JSONUtil.parseObj(res);
                    if (jn.getInt("code", 500) != 200) {
                        Console.log("请求失败,{}", res);
                        continue;
                    }
                    jn = jn.getJSONObject("data");
                    tips = StrUtil.removePrefix(jn.getStr("msg"), "好吧，给你一点点提示！n");
                    tips = StrUtil.removePrefix(tips, "好吧，给你一点点提示！");
                } catch (Exception e) {
                    Console.error("获取提示失败", e);
                }
                buffer.add(Entity.create("game_ktccy")
                        .set("pic_url", url)
                        .set("answer", answer)
                        .set("tips", tips)
                        .set("source", 2));

                if (buffer.size() >= 10 || RandomUtil.randomInt(1, 10) < 4) {
                    db.insert(buffer);
                    CollUtil.clear(buffer);
                }
                ThreadUtil.safeSleep(300);
            } catch (cn.hutool.http.HttpException he) {
                System.err.print("X");
                ThreadUtil.safeSleep(5000);
            } catch (Exception e) {
                Console.error("插入失败", e);
            }
        }
    }
}