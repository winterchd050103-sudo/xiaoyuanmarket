package com.xiaoyuan.market.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 商品状态：0 待审核，1 在售，2 已下架，3 已售出
 */
@Getter
@AllArgsConstructor
public enum ProductStatus {

    PENDING(0, "待审核"),
    ON_SALE(1, "在售"),
    OFF_SALE(2, "已下架"),
    SOLD(3, "已售出");

    private final Integer code;
    private final String text;

    public static ProductStatus of(Integer code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst().orElse(null);
    }
}
