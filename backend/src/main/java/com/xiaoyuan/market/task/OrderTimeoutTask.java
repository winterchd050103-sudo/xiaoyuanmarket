package com.xiaoyuan.market.task;

import com.xiaoyuan.market.entity.Orders;
import com.xiaoyuan.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单超时兜底定时任务：每 30 秒扫描超时未支付订单（与 MQ 延迟队列双保险）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void cancelExpiredOrders() {
        List<Orders> expired = orderService.listExpiredUnpaid();
        for (Orders order : expired) {
            log.info("[Task] 扫描到超时订单 {}", order.getOrderNo());
            orderService.cancelExpiredOrder(order.getOrderNo());
        }
    }
}
