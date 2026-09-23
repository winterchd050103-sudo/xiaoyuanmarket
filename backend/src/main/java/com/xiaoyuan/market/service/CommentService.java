package com.xiaoyuan.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.common.OrderStatus;
import com.xiaoyuan.market.dto.CommentReq;
import com.xiaoyuan.market.entity.Comment;
import com.xiaoyuan.market.entity.Orders;
import com.xiaoyuan.market.entity.User;
import com.xiaoyuan.market.mapper.CommentMapper;
import com.xiaoyuan.market.mapper.OrdersMapper;
import com.xiaoyuan.market.mapper.UserMapper;
import com.xiaoyuan.market.vo.ProductDetailVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品评论服务：交易完成后才能评价
 */
@Service
@RequiredArgsConstructor
public class CommentService extends ServiceImpl<CommentMapper, Comment> {

    private final OrdersMapper ordersMapper;
    private final UserMapper userMapper;

    /**
     * 发表评论：必须存在本人对应该商品的已完成订单
     */
    public Comment add(Long userId, CommentReq req) {
        Orders order = null;
        if (req.getOrderId() != null) {
            order = ordersMapper.selectById(req.getOrderId());
            if (order == null || !Objects.equals(order.getBuyerId(), userId)
                    || !Objects.equals(order.getProductId(), req.getProductId())) {
                throw new BusinessException("订单与商品不匹配");
            }
            if (!Objects.equals(order.getStatus(), OrderStatus.FINISHED.getCode())) {
                throw new BusinessException("订单完成后才能评价");
            }
        } else {
            // 未传订单 id：查询本人该商品的已完成订单
            order = ordersMapper.selectOne(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getBuyerId, userId)
                    .eq(Orders::getProductId, req.getProductId())
                    .eq(Orders::getStatus, OrderStatus.FINISHED.getCode())
                    .orderByDesc(Orders::getId)
                    .last("limit 1"));
            if (order == null) {
                throw new BusinessException("购买该商品并完成交易后才能评价");
            }
        }
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setProductId(req.getProductId());
        comment.setOrderId(order.getId());
        comment.setContent(req.getContent());
        comment.setRating(req.getRating());
        save(comment);
        return comment;
    }

    /**
     * 商品评论分页（公开）
     */
    public Page<CommentVO> pageByProduct(Long productId, Long pageNum, Long pageSize) {
        Page<Comment> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Comment>().eq(Comment::getProductId, productId).orderByDesc(Comment::getId));
        List<Long> userIds = page.getRecords().stream().map(Comment::getUserId).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setContent(c.getContent());
            vo.setRating(c.getRating());
            vo.setCreateTime(c.getCreateTime());
            User u = users.get(c.getUserId());
            if (u != null) {
                vo.setNickname(u.getNickname());
                vo.setAvatar(u.getAvatar());
            }
            return vo;
        }).toList());
        return voPage;
    }

    @Data
    public static class CommentVO {
        private Long id;
        private String nickname;
        private String avatar;
        private String content;
        private Integer rating;
        private LocalDateTime createTime;
    }
}
