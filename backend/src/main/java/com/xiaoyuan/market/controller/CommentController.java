package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.PageResult;
import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.dto.CommentReq;
import com.xiaoyuan.market.security.SecurityUtils;
import com.xiaoyuan.market.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评论")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "发表评价（需登录，交易完成后）")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CommentReq req) {
        commentService.add(SecurityUtils.getUserId(), req);
        return Result.ok();
    }

    @Operation(summary = "商品评论列表（公开）")
    @GetMapping
    public Result<PageResult<CommentService.CommentVO>> page(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(PageResult.of(commentService.pageByProduct(productId, pageNum, pageSize)));
    }
}
