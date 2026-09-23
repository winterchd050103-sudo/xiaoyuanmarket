package com.xiaoyuan.market.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情
 */
@Data
public class ProductDetailVO {

    private Long id;

    private Long userId;

    private String sellerNickname;

    private Long categoryId;

    private String categoryName;

    private String title;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String coverImage;

    /** 相册图片 */
    private List<String> images;

    private Integer status;

    private String statusText;

    private Integer viewCount;

    /** 当前用户是否已收藏（未登录为 false） */
    private Boolean favorited;

    private LocalDateTime createTime;
}
