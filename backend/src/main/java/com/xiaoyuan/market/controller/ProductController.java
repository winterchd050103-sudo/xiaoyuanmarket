package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.PageResult;
import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.dto.ProductPublishReq;
import com.xiaoyuan.market.dto.ProductQuery;
import com.xiaoyuan.market.entity.Product;
import com.xiaoyuan.market.security.LoginUser;
import com.xiaoyuan.market.security.SecurityUtils;
import com.xiaoyuan.market.service.ProductService;
import com.xiaoyuan.market.vo.ProductDetailVO;
import com.xiaoyuan.market.vo.ProductListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "商品")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页列表（公开，支持分类/关键词/排序）")
    @GetMapping
    public Result<PageResult<ProductListVO>> page(ProductQuery query) {
        return Result.ok(PageResult.of(productService.page(query)));
    }

    @Operation(summary = "我发布的商品（需登录）")
    @GetMapping("/my")
    public Result<PageResult<ProductListVO>> my(@RequestParam(defaultValue = "1") Long pageNum,
                                                @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(PageResult.of(productService.myProducts(SecurityUtils.getUserId(), pageNum, pageSize)));
    }

    @Operation(summary = "商品详情（公开，浏览量 +1）")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        LoginUser viewer = null;
        try {
            viewer = SecurityUtils.getLoginUser();
        } catch (Exception ignore) {
            // 未登录也可查看
        }
        productService.increaseViewCount(id);
        return Result.ok(productService.getDetail(id, viewer == null ? null : viewer.getId()));
    }

    @Operation(summary = "发布商品（需登录）")
    @PostMapping
    public Result<Product> publish(@Valid @RequestBody ProductPublishReq req) {
        return Result.ok(productService.publish(SecurityUtils.getUserId(), req));
    }

    @Operation(summary = "编辑商品（卖家本人）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductPublishReq req) {
        productService.update(SecurityUtils.getUserId(), id, req);
        return Result.ok();
    }

    @Operation(summary = "删除商品（卖家本人）")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        productService.remove(SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "上架 / 下架（targetStatus: 1 上架，2 下架）")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        productService.changeStatus(SecurityUtils.getUserId(), id, body.get("status"));
        return Result.ok();
    }
}
