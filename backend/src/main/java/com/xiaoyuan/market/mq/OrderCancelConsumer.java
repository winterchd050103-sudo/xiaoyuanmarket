package com.xiaoyuan.market.mq;

import com.xiaoyuan.market.config.RabbitMQConfig;
import com.xiaoyuan.market.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时取消消费者：监听取消队列（延迟消息 TTL 到期后经死信路由进入）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.CANCEL_QUEUE)
    public void handle(String orderNo) {
        log.info("[MQ] 收到订单超时消息: {}", orderNo);
        try {
            orderService.cancelExpiredOrder(orderNo);
        } catch (Exception e) {
            log.error("[MQ] 处理订单超时取消失败: {}", orderNo, e);
        }
    }
}
