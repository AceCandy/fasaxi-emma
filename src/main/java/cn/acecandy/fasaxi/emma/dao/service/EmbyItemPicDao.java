package cn.acecandy.fasaxi.emma.dao.service;

import cn.acecandy.fasaxi.emma.dao.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.mapper.EmbyItemPicMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.entity.table.EmbyItemPicTableDef.EMBY_ITEM_PIC;

/**
 * 卧底用户 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class EmbyItemPicDao extends ServiceImpl<EmbyItemPicMapper, EmbyItemPic> {

    public List<Integer> findAllItemId() {
        QueryWrapper wrapper = QueryWrapper.create()
                .select(EMBY_ITEM_PIC.ITEM_ID)
                .orderBy(EMBY_ITEM_PIC.ITEM_ID, true);
        return mapper.selectListByQueryAs(wrapper, Integer.class);
    }

    public Page<EmbyItemPic> findAllByPage(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageSize == null) {
            return null;
        }
        return mapper.paginate(pageNum, pageSize, QueryWrapper.create());
    }

    public int delById(List<Integer> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            return 0;
        }
        return mapper.deleteBatchByIds(itemIds);
    }

    public List<EmbyItemPic> findByItemId(List<Integer> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            return ListUtil.of();
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .where(EMBY_ITEM_PIC.ITEM_ID.in(itemIds));
        return mapper.selectListByQuery(wrapper);
    }

    public EmbyItemPic findByItemId(Integer itemId) {
        if (null == itemId) {
            return null;
        }
        return CollUtil.getFirst(findByItemId(ListUtil.of(itemId)));
    }
}