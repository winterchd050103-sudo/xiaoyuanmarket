package com.xiaoyuan.market.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：
 * 1. 订单超时取消：下单后发消息到延迟交换机（带消息 TTL），TTL 到期后死信转发到取消队列，消费者执行关单；
 * 2. 支付异步通知：支付成功后发 fanout 事件，消费者异步处理后续动作。
 */
@Configuration
public class RabbitMQConfig {

    public static final String DELAY_EXCHANGE = "order.delay.exchange";
    public static final String DELAY_QUEUE = "order.delay.queue";
    public static final String DELAY_KEY = "order.delay";

    public static final String CANCEL_EXCHANGE = "order.cancel.exchange";
    public static final String CANCEL_QUEUE = "order.cancel.queue";
    public static final String CANCEL_KEY = "order.cancel";

    public static final String PAY_EXCHANGE = "pay.notify.exchange";
    public static final String PAY_QUEUE = "pay.notify.queue";

    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }

    /**
     * 延迟队列：不设消费者，消息 TTL 到期后经死信路由进入取消队列
     */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", CANCEL_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CANCEL_KEY)
                .build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(DELAY_KEY);
    }

    @Bean
    public DirectExchange cancelExchange() {
        return new DirectExchange(CANCEL_EXCHANGE);
    }

    @Bean
    public Queue cancelQueue() {
        return QueueBuilder.durable(CANCEL_QUEUE).build();
    }

    @Bean
    public Binding cancelBinding() {
        return BindingBuilder.bind(cancelQueue()).to(cancelExchange()).with(CANCEL_KEY);
    }

    @Bean
    public FanoutExchange payExchange() {
        return new FanoutExchange(PAY_EXCHANGE);
    }

    @Bean
    public Queue payQueue() {
        return QueueBuilder.durable(PAY_QUEUE).build();
    }

    @Bean
    public Binding payBinding() {
        return BindingBuilder.bind(payQueue()).to(payExchange());
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
