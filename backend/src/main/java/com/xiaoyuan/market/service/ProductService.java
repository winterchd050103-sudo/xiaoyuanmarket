package com.xiaoyuan.market.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.common.ProductStatus;
import com.xiaoyuan.market.dto.ProductPublishReq;
import com.xiaoyuan.market.dto.ProductQuery;
import com.xiaoyuan.market.entity.Category;
import com.xiaoyuan.market.entity.Product;
import com.xiaoyuan.market.entity.ProductImage;
import com.xiaoyuan.market.entity.User;
import com.xiaoyuan.market.mapper.CategoryMapper;
import com.xiaoyuan.market.mapper.ProductImageMapper;
import com.xiaoyuan.market.mapper.ProductMapper;
import com.xiaoyuan.market.mapper.UserMapper;
import com.xiaoyuan.market.vo.ProductDetailVO;
import com.xiaoyuan.market.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务：列表/详情使用 Redis 缓存（先更新数据库，再删除缓存）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    public static final String DETAIL_KEY_PREFIX = "product:detail:";
    public static final String LIST_KEY_PREFIX = "product:list:";

    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final FavoriteService favoriteService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${market.product-audit-enabled:false}")
    private boolean auditEnabled;

    /**
     * 分页查询（公开列表走缓存；带关键词或按用户查询不走缓存）
     */
    @SuppressWarnings("unchecked")
    public Page<ProductListVO> page(ProductQuery query) {
        boolean cacheable = query.getUserId() == null
                && !StringUtils.hasText(query.getKeyword())
                && query.getStatus() == null;
        String cacheKey = null;
        if (cacheable) {
            cacheKey = LIST_KEY_PREFIX + query.getCategoryId() + ":" + query.getPageNum()
                    + ":" + query.getPageSize() + ":" + (query.getSort() == null ? "newest" : query.getSort());
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached instanceof Page<?> p) {
                    return (Page<ProductListVO>) p;
                }
            } catch (Exception e) {
                log.warn("redis read failed: {}", e.getMessage());
            }
        }

        Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId())
                .eq(query.getUserId() != null, Product::getUserId, query.getUserId())
                .like(StringUtils.hasText(query.getKeyword()), Product::getTitle, query.getKeyword());
        if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus());
        } else if (query.getUserId() == null) {
            // 公开列表只展示在售商品
            wrapper.eq(Product::getStatus, ProductStatus.ON_SALE.getCode());
        }
        String sort = StringUtils.hasText(query.getSort()) ? query.getSort() : "newest";
        switch (sort) {
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice);
            case "hot" -> wrapper.orderByDesc(Product::getViewCount);
            default -> wrapper.orderByDesc(Product::getCreateTime);
        }
        Page<Product> result = page(page, wrapper);
        Page<ProductListVO> voPage = enrichToPage(result);

        if (cacheKey != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, voPage, Duration.ofMinutes(5));
            } catch (Exception e) {
                log.warn("redis write failed: {}", e.getMessage());
            }
        }
        return voPage;
    }

    /**
     * 商品详情（缓存 30 分钟 + 随机抖动；空对象缓存 60 秒防缓存穿透）
     */
    public ProductDetailVO getDetail(Long id, Long viewerId) {
        String key = DETAIL_KEY_PREFIX + id;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ProductDetailVO vo) {
                return attachFavorited(vo, viewerId);
            }
            if (cached instanceof String s && s.isEmpty()) {
                // 空值缓存：防止穿透
                throw new BusinessException("商品不存在或已删除");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("redis read failed: {}", e.getMessage());
        }

        Product product = getById(id);
        if (product == null) {
            try {
                redisTemplate.opsForValue().set(key, "", Duration.ofSeconds(60));
            } catch (Exception ignore) {
            }
            throw new BusinessException("商品不存在或已删除");
        }

        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setUserId(product.getUserId());
        vo.setCategoryId(product.getCategoryId());
        vo.setTitle(product.getTitle());
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setCoverImage(product.getCoverImage());
        vo.setStatus(product.getStatus());
        vo.setStatusText(statusText(product.getStatus()));
        vo.setViewCount(product.getViewCount());
        vo.setCreateTime(product.getCreateTime());
        Category category = categoryMapper.selectById(product.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        User seller = userMapper.selectById(product.getUserId());
        vo.setSellerNickname(seller == null ? "未知用户" : seller.getNickname());
        vo.setImages(productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id).orderByAsc(ProductImage::getSort))
                .stream().map(ProductImage::getUrl).toList());
        vo.setFavorited(false);

        try {
            long ttl = Duration.ofMinutes(30).getSeconds() + RandomUtil.randomLong(0, 300);
            redisTemplate.opsForValue().set(key, vo, Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("redis write failed: {}", e.getMessage());
        }
        return attachFavorited(vo, viewerId);
    }

    /**
     * 浏览量 +1（直接落库；详情缓存中的浏览量允许短暂滞后）
     */
    public void increaseViewCount(Long id) {
        update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .setSql("view_count = view_count + 1"));
    }

    /**
     * 发布商品
     */
    @Transactional
    public Product publish(Long userId, ProductPublishReq req) {
        Category category = categoryMapper.selectById(req.getCategoryId());
        if (category == null || category.getStatus() != 1) {
            throw new BusinessException("分类不存在或已停用");
        }
        Product product = new Product();
        product.setUserId(userId);
        product.setCategoryId(req.getCategoryId());
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setOriginalPrice(req.getOriginalPrice());
        product.setCoverImage(req.getCoverImage());
        // 可配置是否需要人工审核（默认直接在售）
        product.setStatus(auditEnabled ? ProductStatus.PENDING.getCode() : ProductStatus.ON_SALE.getCode());
        product.setViewCount(0);
        save(product);
        saveImages(product.getId(), req.getImages());
        evictListCache();
        return product;
    }

    /**
     * 编辑商品（已售出商品不可编辑）
     */
    @Transactional
    public void update(Long userId, Long id, ProductPublishReq req) {
        Product product = getOwned(userId, id);
        if (Objects.equals(product.getStatus(), ProductStatus.SOLD.getCode())) {
            throw new BusinessException("商品已售出，无法编辑");
        }
        product.setCategoryId(req.getCategoryId());
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setOriginalPrice(req.getOriginalPrice());
        product.setCoverImage(req.getCoverImage());
        updateById(product);
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        saveImages(id, req.getImages());
        evictAllCache(id);
    }

    /**
     * 删除商品（已售出商品不可删除，保证订单数据完整）
     */
    @Transactional
    public void remove(Long userId, Long id) {
        Product product = getOwned(userId, id);
        if (Objects.equals(product.getStatus(), ProductStatus.SOLD.getCode())) {
            throw new BusinessException("商品已售出，无法删除");
        }
        removeById(id);
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        evictAllCache(id);
    }

    /**
     * 上架 / 下架（仅 1 <-> 2）
     */
    public void changeStatus(Long userId, Long id, Integer targetStatus) {
        Product product = getOwned(userId, id);
        if (Objects.equals(product.getStatus(), ProductStatus.SOLD.getCode())
                || Objects.equals(product.getStatus(), ProductStatus.PENDING.getCode())) {
            throw new BusinessException("当前状态不支持该操作");
        }
        if (!Objects.equals(targetStatus, ProductStatus.ON_SALE.getCode())
                && !Objects.equals(targetStatus, ProductStatus.OFF_SALE.getCode())) {
            throw new BusinessException("非法的目标状态");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(targetStatus);
        updateById(update);
        evictAllCache(id);
    }

    /**
     * 我的商品（分页，不过滤状态）
     */
    public Page<ProductListVO> myProducts(Long userId, Long pageNum, Long pageSize) {
        ProductQuery query = new ProductQuery();
        query.setUserId(userId);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        Page<Product> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Product>().eq(Product::getUserId, userId)
                        .orderByDesc(Product::getCreateTime));
        return enrichToPage(page);
    }

    /**
     * 标记售出 / 取消售出（支付成功、退款时调用）
     */
    public void markSold(Long productId, boolean sold) {
        Product update = new Product();
        update.setId(productId);
        update.setStatus(sold ? ProductStatus.SOLD.getCode() : ProductStatus.ON_SALE.getCode());
        updateById(update);
        evictAllCache(productId);
    }

    private Product getOwned(Long userId, Long id) {
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人商品");
        }
        return product;
    }

    private void saveImages(Long productId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        int sort = 0;
        for (String url : images) {
            ProductImage img = new ProductImage();
            img.setProductId(productId);
            img.setUrl(url);
            img.setSort(sort++);
            productImageMapper.insert(img);
        }
    }

    private Page<ProductListVO> enrichToPage(Page<Product> page) {
        List<Product> records = page.getRecords();
        Map<Long, String> categoryNames = loadCategoryNames(records);
        Map<Long, String> nicknames = loadNicknames(records);
        Page<ProductListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records.stream().map(p -> {
            ProductListVO vo = new ProductListVO();
            vo.setId(p.getId());
            vo.setUserId(p.getUserId());
            vo.setSellerNickname(nicknames.get(p.getUserId()));
            vo.setCategoryId(p.getCategoryId());
            vo.setCategoryName(categoryNames.get(p.getCategoryId()));
            vo.setTitle(p.getTitle());
            vo.setPrice(p.getPrice());
            vo.setOriginalPrice(p.getOriginalPrice());
            vo.setCoverImage(p.getCoverImage());
            vo.setStatus(p.getStatus());
            vo.setStatusText(statusText(p.getStatus()));
            vo.setViewCount(p.getViewCount());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).toList());
        return voPage;
    }

    private Map<Long, String> loadCategoryNames(List<Product> products) {
        List<Long> ids = products.stream().map(Product::getCategoryId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private Map<Long, String> loadNicknames(List<Product> products) {
        List<Long> ids = products.stream().map(Product::getUserId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    private ProductDetailVO attachFavorited(ProductDetailVO vo, Long viewerId) {
        if (viewerId != null) {
            vo.setFavorited(favoriteService.isFavorite(viewerId, vo.getId()));
        }
        return vo;
    }

    private String statusText(Integer status) {
        ProductStatus ps = ProductStatus.of(status);
        return ps == null ? "未知" : ps.getText();
    }

    /**
     * 删除商品详情缓存 + 列表缓存（先更新数据库，再删除缓存）
     */
    public void evictAllCache(Long productId) {
        try {
            redisTemplate.delete(DETAIL_KEY_PREFIX + productId);
            evictListCache();
        } catch (Exception e) {
            log.warn("redis evict failed: {}", e.getMessage());
        }
    }

    private void evictListCache() {
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(LIST_KEY_PREFIX + "*").count(200).build())) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        } catch (Exception e) {
            log.warn("redis scan evict failed: {}", e.getMessage());
        }
    }
}
