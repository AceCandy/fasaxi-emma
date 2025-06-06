package cn.acecandy.fasaxi.emma.dao.service;

import cn.acecandy.fasaxi.emma.dao.entity.EmbyItemPic;
import cn.acecandy.fasaxi.emma.dao.mapper.EmbyItemPicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    public List<Integer> findAllItemId() {
        LambdaQueryWrapper<EmbyItemPic> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(EmbyItemPic::getItemId).orderByAsc(EmbyItemPic::getItemId)
        ;
        return embyItemPicMapper.selectObjs(wrapper);
    }

    public IPage<EmbyItemPic> findAllByPage(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageSize == null) {
            return null;
        }
        Page<EmbyItemPic> page = new Page<>(pageNum, pageSize);

        // 执行分页查询（这里可以添加查询条件，若无则传null）
        QueryWrapper<EmbyItemPic> queryWrapper = new QueryWrapper<>();
        return embyItemPicMapper.selectPage(page, queryWrapper);
    }

    public int delById(List<Integer> itemIds) {
        if (CollUtil.isEmpty(itemIds)) {
            return 0;
        }
        return embyItemPicMapper.deleteByIds(itemIds);
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