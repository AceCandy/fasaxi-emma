package cn.acecandy.fasaxi.emma.dao.toolkit.service;

import cn.acecandy.fasaxi.emma.dao.toolkit.entity.CustomCollections;
import cn.acecandy.fasaxi.emma.dao.toolkit.mapper.CustomCollectionsMapper;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.acecandy.fasaxi.emma.dao.toolkit.entity.table.CustomCollectionsTableDef.CUSTOM_COLLECTIONS;

/**
 * 自定义合集 dao
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Slf4j
@UseDataSource("toolkit")
@Component
public class CustomCollectionsDao extends ServiceImpl<CustomCollectionsMapper, CustomCollections> {

    /**
     * 按状态查找全部
     *
     * @param status 状态
     * @return {@link List }<{@link CustomCollections }>
     */
    public List<CustomCollections> findAllByStatus(String status) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CUSTOM_COLLECTIONS.STATUS.eq(status))
                .orderBy(CUSTOM_COLLECTIONS.SORT_ORDER, true)
                .orderBy(CUSTOM_COLLECTIONS.ID, true);
        return mapper.selectListByQuery(wrapper);
    }

    /**
     * 按状态查找全部
     *
     * @param status 状态
     * @return {@link List }<{@link CustomCollections }>
     */
    public List<CustomCollections> findAllByStatus(List<String> status) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CUSTOM_COLLECTIONS.STATUS.in(status))
                .orderBy(CUSTOM_COLLECTIONS.SORT_ORDER, true)
                .orderBy(CUSTOM_COLLECTIONS.ID, true);
        return mapper.selectListByQuery(wrapper);
    }

}