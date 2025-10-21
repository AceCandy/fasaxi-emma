package cn.acecandy.fasaxi.emma.dao.embyboss.service;

import cn.acecandy.fasaxi.emma.dao.embyboss.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.embyboss.mapper.EmbyItemPicMapper;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.embyboss.entity.table.EmbyItemPicTableDef.EMBY_ITEM_PIC;

/**
 * 卧底用户 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@UseDataSource("embyboss")
@Component
public class EmbyItemPicDao extends ServiceImpl<EmbyItemPicMapper, EmbyItemPic> {

    /**
     * 插入或更新
     * <p>
     * 自带的saveOrUpdate好像对非自增id有问题 所以重写了个
     *
     * @param dto 到
     * @return boolean
     */
    public synchronized boolean insertOrUpdate(EmbyItemPic dto) {
        if (dto == null) {
            return false;
        }
        Integer itemId = dto.getItemId();
        EmbyItemPic embyItemPic = findByItemId(itemId);
        if (embyItemPic == null) {
            return mapper.insertSelective(dto) > 0;
        }
        return mapper.update(dto) > 0;
    }

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