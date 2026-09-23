package com.xiaoyuan.market.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评论
 */
@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    /** 关联订单 id（可空） */
    private Long orderId;

    private String content;

    /** 评分 1 至 5 */
    private Integer rating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
