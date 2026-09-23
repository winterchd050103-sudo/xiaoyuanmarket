package com.xiaoyuan.market.mq;

import com.xiaoyuan.market.config.RabbitMQConfig;
import com.xiaoyuan.market.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 支付成功异步通知消费者：记录交易通知并刷新统计缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayNotifyConsumer {

    private final AdminService adminService;

    @RabbitListener(queues = RabbitMQConfig.PAY_QUEUE)
    public void handle(String orderNo) {
        log.info("[MQ] 交易异步通知：订单 {} 已支付成功", orderNo);
        // 刷新管理后台统计缓存
        adminService.evictStatsCache();
    }
}
