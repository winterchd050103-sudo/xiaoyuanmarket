package com.xiaoyuan.market.controller;

import com.xiaoyuan.market.common.Result;
import com.xiaoyuan.market.security.LoginUser;
import com.xiaoyuan.market.security.SecurityUtils;
import com.xiaoyuan.market.service.OrderService;
import com.xiaoyuan.market.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟支付
 */
@Tag(name = "支付")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final OrderService orderService;

    @Operation(summary = "模拟支付（买家，构造回调并验签后更新订单状态）")
    @PostMapping("/mock/{orderNo}")
    public Result<OrderVO> mockPay(@PathVariable String orderNo) {
        LoginUser buyer = SecurityUtils.getLoginUser();
        return Result.ok(orderService.mockPay(buyer, orderNo));
    }

    @Operation(summary = "模拟支付网关回调（第三方调用：orderNo + amount + sign，sign=md5(orderNo|amount|salt)）")
    @PostMapping("/notify")
    public Result<OrderVO> notify(@RequestParam String orderNo,
                                  @RequestParam String amount,
                                  @RequestParam String sign) {
        return Result.ok(orderService.payCallback(orderNo, amount, sign));
    }
}
