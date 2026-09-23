package com.xiaoyuan.market.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态：0 待付款，1 待发货，2 待收货，3 已完成，4 已取消，5 已退款
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    WAIT_PAY(0, "待付款"),
    WAIT_DELIVER(1, "待发货"),
    WAIT_RECEIVE(2, "待收货"),
    FINISHED(3, "已完成"),
    CANCELED(4, "已取消"),
    REFUNDED(5, "已退款");

    private final Integer code;
    private final String text;

    public static OrderStatus of(Integer code) {
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).findFirst().orElse(null);
    }
}
