package com.xiaoyuan.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.entity.Favorite;
import com.xiaoyuan.market.entity.Product;
import com.xiaoyuan.market.mapper.FavoriteMapper;
import com.xiaoyuan.market.mapper.ProductMapper;
import com.xiaoyuan.market.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品收藏服务
 */
@Service
@RequiredArgsConstructor
public class FavoriteService extends ServiceImpl<FavoriteMapper, Favorite> {

    private final ProductMapper productMapper;
    private final ProductService productService;

    /**
     * 收藏（幂等：重复收藏不报错）
     */
    public boolean add(Long userId, Long productId) {
        if (productMapper.selectById(productId) == null) {
            throw new BusinessException("商品不存在");
        }
        if (isFavorite(userId, productId)) {
            return true;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        try {
            save(favorite);
        } catch (DuplicateKeyException e) {
            // 联合唯一索引兜底
        }
        return true;
    }

    /**
     * 取消收藏（幂等）
     */
    public void remove(Long userId, Long productId) {
        remove(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
    }

    public boolean isFavorite(Long userId, Long productId) {
        return count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)) > 0;
    }

    /**
     * 我的收藏（分页，按收藏时间倒序）
     */
    public Page<ProductListVO> pageFavorites(Long userId, Long pageNum, Long pageSize) {
        Page<Favorite> favPage = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId).orderByDesc(Favorite::getId));
        List<Long> productIds = favPage.getRecords().stream().map(Favorite::getProductId).toList();
        Page<ProductListVO> voPage = new Page<>(favPage.getCurrent(), favPage.getSize(), favPage.getTotal());
        if (productIds.isEmpty()) {
            voPage.setRecords(List.of());
            return voPage;
        }
        List<Product> products = productMapper.selectBatchIds(productIds);
        // 按收藏顺序排列
        var byId = products.stream().collect(java.util.stream.Collectors
                .toMap(Product::getId, java.util.function.Function.identity(), (a, b) -> a));
        voPage.setRecords(productIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(p -> {
                    ProductListVO vo = new ProductListVO();
                    vo.setId(p.getId());
                    vo.setUserId(p.getUserId());
                    vo.setCategoryId(p.getCategoryId());
                    vo.setTitle(p.getTitle());
                    vo.setPrice(p.getPrice());
                    vo.setOriginalPrice(p.getOriginalPrice());
                    vo.setCoverImage(p.getCoverImage());
                    vo.setStatus(p.getStatus());
                    vo.setViewCount(p.getViewCount());
                    vo.setCreateTime(p.getCreateTime());
                    return vo;
                }).toList());
        return voPage;
    }
}
