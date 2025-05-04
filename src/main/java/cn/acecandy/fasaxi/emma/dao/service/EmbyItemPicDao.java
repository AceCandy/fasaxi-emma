package cn.acecandy.fasaxi.emma.dao.service;

import cn.acecandy.fasaxi.emma.dao.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.mapper.EmbyItemPicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 卧底用户 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@Component
public class EmbyItemPicDao {

    @Resource
    private EmbyItemPicMapper embyItemPicMapper;

    public boolean insertOrUpdate(EmbyItemPic dto) {
        if (dto == null) {
            return false;
        }
        return embyItemPicMapper.insertOrUpdate(dto);
    }

    public List<EmbyItemPic> findByItemId(List<Integer> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            return ListUtil.of();
        }
        LambdaQueryWrapper<EmbyItemPic> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EmbyItemPic::getItemId, itemIds)
        ;
        return embyItemPicMapper.selectList(wrapper);
    }

    public EmbyItemPic findByItemId(Integer itemId) {
        if (null == itemId) {
            return null;
        }
        return CollUtil.getFirst(findByItemId(ListUtil.of(itemId)));
    }
}