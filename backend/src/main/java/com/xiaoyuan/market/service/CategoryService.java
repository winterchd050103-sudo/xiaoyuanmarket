package com.xiaoyuan.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.entity.Category;
import com.xiaoyuan.market.mapper.CategoryMapper;
import com.xiaoyuan.market.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务
 */
@Service
@RequiredArgsConstructor
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    private final ProductMapper productMapper;

    /**
     * 启用中的分类（商城前台使用）
     */
    public List<Category> listEnabled() {
        return list(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
    }

    public List<Category> listAll() {
        return list(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
    }

    public Category add(Category category) {
        checkName(category.getName(), null);
        save(category);
        return category;
    }

    public void update(Category category) {
        if (getById(category.getId()) == null) {
            throw new BusinessException("分类不存在");
        }
        checkName(category.getName(), category.getId());
        updateById(category);
    }

    public void remove(Long id) {
        if (productMapper.selectCount(new LambdaQueryWrapper<com.xiaoyuan.market.entity.Product>()
                .eq(com.xiaoyuan.market.entity.Product::getCategoryId, id)) > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }
        removeById(id);
    }

    private void checkName(String name, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>().eq(Category::getName, name);
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
    }
}
