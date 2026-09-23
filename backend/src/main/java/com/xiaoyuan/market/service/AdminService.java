package com.xiaoyuan.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.common.ProductStatus;
import com.xiaoyuan.market.entity.Orders;
import com.xiaoyuan.market.entity.Product;
import com.xiaoyuan.market.entity.User;
import com.xiaoyuan.market.mapper.OrdersMapper;
import com.xiaoyuan.market.mapper.ProductMapper;
import com.xiaoyuan.market.mapper.UserMapper;
import com.xiaoyuan.market.vo.ProductListVO;
import com.xiaoyuan.market.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理后台服务：用户管理 / 商品审核 / 订单管理 / 数据统计 / 分类管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    public static final String STATS_CACHE_KEY = "admin:stats";

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrdersMapper ordersMapper;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final RedisTemplate<String, Object> redisTemplate;

    // ---------- 用户管理 ----------

    public Page<User> pageUsers(String keyword, Long pageNum, Long pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().orderByDesc(User::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        Page<User> result = userMapper.selectPage(page, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    /**
     * 禁用 / 启用用户（管理员账号不可被禁用）
     */
    public void setUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() == 1) {
            throw new BusinessException("管理员账号不可被禁用");
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
        // 清除用户缓存，使禁用即时生效
        try {
            redisTemplate.delete("user:info:" + userId);
        } catch (Exception ignore) {
        }
    }

    // ---------- 商品管理 ----------

    public Page<ProductListVO> pageProducts(Integer status, String keyword, Long pageNum, Long pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().orderByDesc(Product::getId);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getTitle, keyword);
        }
        Page<Product> result = productMapper.selectPage(page, wrapper);

        List<Long> userIds = result.getRecords().stream().map(Product::getUserId).distinct().toList();
        Map<Long, String> nicknames = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));

        Page<ProductListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(p -> {
            ProductListVO vo = new ProductListVO();
            vo.setId(p.getId());
            vo.setUserId(p.getUserId());
            vo.setSellerNickname(nicknames.get(p.getUserId()));
            vo.setCategoryId(p.getCategoryId());
            vo.setTitle(p.getTitle());
            vo.setPrice(p.getPrice());
            vo.setOriginalPrice(p.getOriginalPrice());
            vo.setCoverImage(p.getCoverImage());
            vo.setStatus(p.getStatus());
            ProductStatus ps = ProductStatus.of(p.getStatus());
            vo.setStatusText(ps == null ? "未知" : ps.getText());
            vo.setViewCount(p.getViewCount());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).toList());
        return voPage;
    }

    /**
     * 审核通过（0 待审核 -> 1 在售）
     */
    public void auditPass(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!Objects.equals(product.getStatus(), ProductStatus.PENDING.getCode())) {
            throw new BusinessException("仅待审核商品可通过审核");
        }
        Product update = new Product();
        update.setId(productId);
        update.setStatus(ProductStatus.ON_SALE.getCode());
        productMapper.updateById(update);
        productService.evictAllCache(productId);
    }

    /**
     * 违规下架
     */
    public void offline(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (Objects.equals(product.getStatus(), ProductStatus.SOLD.getCode())) {
            throw new BusinessException("已售出商品不可下架");
        }
        Product update = new Product();
        update.setId(productId);
        update.setStatus(ProductStatus.OFF_SALE.getCode());
        productMapper.updateById(update);
        productService.evictAllCache(productId);
    }

    // ---------- 订单管理 ----------

    public Page<Orders> pageOrders(Integer status, Long pageNum, Long pageSize) {
        Page<Orders> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>().orderByDesc(Orders::getId);
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        return ordersMapper.selectPage(page, wrapper);
    }

    // ---------- 数据统计（Redis 缓存 60 秒） ----------

    public StatsVO stats() {
        try {
            Object cached = redisTemplate.opsForValue().get(STATS_CACHE_KEY);
            if (cached instanceof StatsVO vo) {
                return vo;
            }
        } catch (Exception e) {
            log.warn("redis read failed: {}", e.getMessage());
        }

        StatsVO stats = new StatsVO();
        stats.setUserCount(userMapper.selectCount(null));
        stats.setProductCount(productMapper.selectCount(null));
        stats.setOrderCount(ordersMapper.selectCount(null));
        stats.setGmv(ordersMapper.selectList(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, 3))
                .stream().map(Orders::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        LocalDate today = LocalDate.now();
        Map<String, Long> orderMap = groupCount("orders");
        Map<String, Long> productMap = groupCount("product");
        List<StatsVO.TrendItem> orderTrend = new ArrayList<>();
        List<StatsVO.TrendItem> productTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String key = today.minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd"));
            orderTrend.add(new StatsVO.TrendItem(key, orderMap.getOrDefault(key, 0L)));
            productTrend.add(new StatsVO.TrendItem(key, productMap.getOrDefault(key, 0L)));
        }
        stats.setOrderTrend(orderTrend);
        stats.setProductTrend(productTrend);

        try {
            redisTemplate.opsForValue().set(STATS_CACHE_KEY, stats, Duration.ofSeconds(60));
        } catch (Exception e) {
            log.warn("redis write failed: {}", e.getMessage());
        }
        return stats;
    }

    private Map<String, Long> groupCount(String table) {
        List<Map<String, Object>> rows;
        if ("orders".equals(table)) {
            QueryWrapper<Orders> wrapper = new QueryWrapper<Orders>()
                    .select("DATE_FORMAT(create_time, '%m-%d') AS d", "COUNT(*) AS c")
                    .apply("create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)")
                    .groupBy("d");
            rows = ordersMapper.selectMaps(wrapper);
        } else {
            QueryWrapper<Product> wrapper = new QueryWrapper<Product>()
                    .select("DATE_FORMAT(create_time, '%m-%d') AS d", "COUNT(*) AS c")
                    .apply("create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)")
                    .groupBy("d");
            rows = productMapper.selectMaps(wrapper);
        }
        return rows.stream().collect(Collectors.toMap(
                r -> String.valueOf(r.get("d")),
                r -> ((Number) r.get("c")).longValue(),
                (a, b) -> a));
    }

    public void evictStatsCache() {
        try {
            redisTemplate.delete(STATS_CACHE_KEY);
        } catch (Exception ignore) {
        }
    }

    // ---------- 分类管理 ----------

    public List<com.xiaoyuan.market.entity.Category> listCategories() {
        return categoryService.listAll();
    }

    public com.xiaoyuan.market.entity.Category addCategory(com.xiaoyuan.market.entity.Category category) {
        return categoryService.add(category);
    }

    public void updateCategory(com.xiaoyuan.market.entity.Category category) {
        categoryService.update(category);
    }

    public void removeCategory(Long id) {
        categoryService.remove(id);
    }
}
