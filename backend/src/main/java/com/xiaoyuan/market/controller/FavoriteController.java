package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.PageResult;
import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.security.SecurityUtils;
import com.xiaoyuan.market.service.FavoriteService;
import com.xiaoyuan.market.vo.ProductListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "收藏")
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏商品")
    @PostMapping("/{productId}")
    public Result<Boolean> add(@PathVariable Long productId) {
        return Result.ok(favoriteService.add(SecurityUtils.getUserId(), productId));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        favoriteService.remove(SecurityUtils.getUserId(), productId);
        return Result.ok();
    }

    @Operation(summary = "我的收藏列表")
    @GetMapping
    public Result<PageResult<ProductListVO>> page(@RequestParam(defaultValue = "1") Long pageNum,
                                                  @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(PageResult.of(favoriteService.pageFavorites(SecurityUtils.getUserId(), pageNum, pageSize)));
    }
}
