package cn.acecandy.fasaxi.emma.control;

import cn.acecandy.fasaxi.emma.common.resp.Rsres;
import cn.acecandy.fasaxi.emma.dao.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.service.EmbyItemPicDao;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItem;
import cn.acecandy.fasaxi.emma.sao.out.EmbyItemsInfoOut;
import cn.acecandy.fasaxi.emma.sao.proxy.EmbyProxy;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.lang.Console;
import org.dromara.hutool.core.math.NumberUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "内部api")
@RestController
@RequestMapping("/api")
public class ApiController {

    @Resource
    private EmbyItemPicDao embyItemPicDao;

    @Resource
    private EmbyProxy embyProxy;

    @Operation(summary = "当前系统时间")
    @GetMapping("/time")
    public Rsres<Object> health() {
        return Rsres.success(System.currentTimeMillis());
    }

    @Operation(summary = "清除db无用图片")
    @GetMapping("/clear/db-pic")
    public Rsres<Object> clearDbPic() {
        int removeCnt = 0;
        int pageNum = 1;
        int pageSize = 200;
        while (true) {
            IPage<EmbyItemPic> picPage = embyItemPicDao.findAllByPage(pageNum, pageSize);
            List<EmbyItemPic> records = picPage.getRecords();

            List<List<EmbyItemPic>> partitions = CollUtil.partition(records, 50);
            for (List<EmbyItemPic> p : partitions) {
                List<String> itemIds = p.stream().map(i -> NumberUtil.toStr(i.getItemId())).toList();
                EmbyItemsInfoOut infoOut = embyProxy.getItemInfos(itemIds);
                if (infoOut.getTotalRecordCount() == p.size()) {
                    continue;
                }
                List<String> realItemIds = infoOut.getItems().stream().map(EmbyItem::getItemId).toList();
                List<String> removeItemIds = CollUtil.subtractToList(itemIds, realItemIds);
                Console.log(CollUtil.subtractToList(itemIds, realItemIds));
                removeCnt += embyItemPicDao.delById(removeItemIds.stream().map(NumberUtil::parseInt).toList());
            }

            long totalPages = picPage.getPages();
            if (pageNum >= totalPages) {
                break;
            }
            pageNum++;
        }

        return Rsres.success(removeCnt);
    }

    @Operation(summary = "清除缓存文件")
    @GetMapping("/clear/file-cache")
    public Rsres<Object> clearFileCache() {
        int removeCnt = 0;
        int pageNum = 1;
        int pageSize = 200;
        while (true) {
            IPage<EmbyItemPic> picPage = embyItemPicDao.findAllByPage(pageNum, pageSize);
            List<EmbyItemPic> records = picPage.getRecords();

            List<List<EmbyItemPic>> partitions = CollUtil.partition(records, 50);
            for (List<EmbyItemPic> p : partitions) {
                List<String> itemIds = p.stream().map(i -> NumberUtil.toStr(i.getItemId())).toList();
                EmbyItemsInfoOut infoOut = embyProxy.getItemInfos(itemIds);
                if (infoOut.getTotalRecordCount() == p.size()) {
                    continue;
                }
                List<String> realItemIds = infoOut.getItems().stream().map(EmbyItem::getItemId).toList();
                List<String> removeItemIds = CollUtil.subtractToList(itemIds, realItemIds);
                Console.log(CollUtil.subtractToList(itemIds, realItemIds));
                removeCnt += embyItemPicDao.delById(removeItemIds.stream().map(NumberUtil::parseInt).toList());
            }

            long totalPages = picPage.getPages();
            if (pageNum >= totalPages) {
                break;
            }
            pageNum++;
        }

        return Rsres.success(removeCnt);
    }

}