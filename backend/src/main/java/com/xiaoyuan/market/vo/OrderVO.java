package com.xiaoyuan.market.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long buyerId;

    private String buyerNickname;

    private Long sellerId;

    private String sellerNickname;

    private Long productId;

    private String productTitle;

    private String productCover;

    private BigDecimal productPrice;

    private Long addressId;

    /** 收货地址快照信息 */
    private String receiver;

    private String receiverPhone;

    private String receiverAddress;

    private BigDecimal amount;

    /** 状态：0 待付款，1 待发货，2 待收货，3 已完成，4 已取消，5 已退款 */
    private Integer status;

    private String statusText;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    /** 当前用户在订单中的角色：buyer / seller */
    private String role;
}
