package com.xiaoyuan.market.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentReq {

    @NotNull(message = "商品 id 不能为空")
    private Long productId;

    /** 关联订单 id（可空） */
    private Long orderId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论最长 500 字符")
    private String content;

    @Min(value = 1, message = "评分须为 1-5")
    @Max(value = 5, message = "评分须为 1-5")
    private Integer rating;
}
