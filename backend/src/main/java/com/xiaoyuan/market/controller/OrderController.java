package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.PageResult;
import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.dto.OrderCreateReq;
import com.xiaoyuan.market.security.LoginUser;
import com.xiaoyuan.market.security.SecurityUtils;
import com.xiaoyuan.market.service.OrderService;
import com.xiaoyuan.market.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单（幂等，clientOrderNo 由前端生成）")
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateReq req) {
        return Result.ok(orderService.create(SecurityUtils.getLoginUser(), req));
    }

    @Operation(summary = "我买到的订单")
    @GetMapping("/bought")
    public Result<PageResult<OrderVO>> bought(@RequestParam(defaultValue = "1") Long pageNum,
                                              @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(PageResult.of(orderService.pageBought(SecurityUtils.getUserId(), pageNum, pageSize)));
    }

    @Operation(summary = "我卖出的订单")
    @GetMapping("/sold")
    public Result<PageResult<OrderVO>> sold(@RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(PageResult.of(orderService.pageSold(SecurityUtils.getUserId(), pageNum, pageSize)));
    }

    @Operation(summary = "订单详情（买卖双方或管理员可见）")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detail(SecurityUtils.getLoginUser(), orderNo));
    }

    @Operation(summary = "买家取消订单（待付款状态）")
    @PutMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        orderService.cancel(SecurityUtils.getLoginUser(), orderNo);
        return Result.ok();
    }

    @Operation(summary = "卖家发货（待发货状态）")
    @PutMapping("/{orderNo}/deliver")
    public Result<Void> deliver(@PathVariable String orderNo) {
        orderService.deliver(SecurityUtils.getLoginUser(), orderNo);
        return Result.ok();
    }

    @Operation(summary = "买家确认收货（待收货状态）")
    @PutMapping("/{orderNo}/receive")
    public Result<Void> receive(@PathVariable String orderNo) {
        orderService.receive(SecurityUtils.getLoginUser(), orderNo);
        return Result.ok();
    }

    @Operation(summary = "买家申请退款（待发货/待收货状态，商品恢复在售）")
    @PutMapping("/{orderNo}/refund")
    public Result<Void> refund(@PathVariable String orderNo) {
        orderService.refund(SecurityUtils.getLoginUser(), orderNo);
        return Result.ok();
    }
}
