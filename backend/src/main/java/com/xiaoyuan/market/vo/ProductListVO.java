package com.xiaoyuan.market.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项
 */
@Data
public class ProductListVO {

    private Long id;

    private Long userId;

    private String sellerNickname;

    private Long categoryId;

    private String categoryName;

    private String title;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String coverImage;

    /** 状态：0 待审核，1 在售，2 已下架，3 已售出 */
    private Integer status;

    private String statusText;

    private Integer viewCount;

    private LocalDateTime createTime;
}
