package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.PageResult;
import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.entity.Address;
import com.xiaoyuan.market.entity.Category;
import com.xiaoyuan.market.entity.Orders;
import com.xiaoyuan.market.entity.User;
import com.xiaoyuan.market.service.AdminService;
import com.xiaoyuan.market.vo.ProductListVO;
import com.xiaoyuan.market.vo.StatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.List;
import java.util.Map;

/**
 * 管理后台（ROLE_ADMIN）
 */
@Tag(name = "管理后台")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ---------- 用户管理 ----------

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageResult<User>> users(@RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") Long pageNum,
                                          @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(PageResult.of(adminService.pageUsers(keyword, pageNum, pageSize)));
    }

    @Operation(summary = "禁用 / 启用用户（status: 0 禁用，1 启用）")
    @PutMapping("/users/{id}/status")
    public Result<Void> setUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.setUserStatus(id, body.get("status"));
        return Result.ok();
    }

    // ---------- 商品管理 ----------

    @Operation(summary = "商品列表（全量）")
    @GetMapping("/products")
    public Result<PageResult<ProductListVO>> products(@RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(defaultValue = "1") Long pageNum,
                                                      @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(PageResult.of(adminService.pageProducts(status, keyword, pageNum, pageSize)));
    }

    @Operation(summary = "审核通过（待审核 -> 在售）")
    @PutMapping("/products/{id}/audit")
    public Result<Void> audit(@PathVariable Long id) {
        adminService.auditPass(id);
        return Result.ok();
    }

    @Operation(summary = "违规下架")
    @PutMapping("/products/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        adminService.offline(id);
        return Result.ok();
    }

    // ---------- 订单管理 ----------

    @Operation(summary = "订单列表（全量）")
    @GetMapping("/orders")
    public Result<PageResult<Orders>> orders(@RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") Long pageNum,
                                             @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(PageResult.of(adminService.pageOrders(status, pageNum, pageSize)));
    }

    // ---------- 数据统计 ----------

    @Operation(summary = "数据统计（用户/商品/订单数、成交额、近 7 日趋势）")
    @GetMapping("/stats")
    public Result<StatsVO> stats() {
        return Result.ok(adminService.stats());
    }

    // ---------- 分类管理 ----------

    @Operation(summary = "分类列表（含停用）")
    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.ok(adminService.listCategories());
    }

    @Operation(summary = "新增分类")
    @PostMapping("/categories")
    public Result<Category> addCategory(@RequestBody Category category) {
        return Result.ok(adminService.addCategory(category));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        adminService.updateCategory(category);
        return Result.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/categories/{id}")
    public Result<Void> removeCategory(@PathVariable Long id) {
        adminService.removeCategory(id);
        return Result.ok();
    }
}
