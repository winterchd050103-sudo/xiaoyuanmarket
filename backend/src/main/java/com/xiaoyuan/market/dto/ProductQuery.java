package com.xiaoyuan.market.dto;

import lombok.Data;

/**
 * 商品列表查询
 */
@Data
public class ProductQuery {

    private Long categoryId;

    private String keyword;

    private Long userId;

    private Integer status;

    /** 排序：newest 最新 / price_asc 价格升序 / price_desc 价格降序 / hot 浏览最多 */
    private String sort;

    private Long pageNum = 1L;

    private Long pageSize = 10L;
}
