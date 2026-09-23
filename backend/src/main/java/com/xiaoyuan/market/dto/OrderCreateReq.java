package com.xiaoyuan.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 下单请求：clientOrderNo 由前端预生成（UUID），配合订单表唯一索引实现接口幂等
 */
@Data
public class OrderCreateReq {

    @NotBlank(message = "订单号不能为空")
    @Size(min = 8, max = 64, message = "订单号长度须为 8-64 位")
    private String clientOrderNo;

    @NotNull(message = "商品 id 不能为空")
    private Long productId;

    @NotNull(message = "收货地址不能为空")
    private Long addressId;
}
