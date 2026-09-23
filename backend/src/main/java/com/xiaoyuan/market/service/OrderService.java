package com.xiaoyuan.market.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.common.OrderStatus;
import com.xiaoyuan.market.common.ProductStatus;
import com.xiaoyuan.market.config.RabbitMQConfig;
import com.xiaoyuan.market.dto.OrderCreateReq;
import com.xiaoyuan.market.entity.Address;
import com.xiaoyuan.market.entity.Orders;
import com.xiaoyuan.market.entity.Product;
import com.xiaoyuan.market.entity.User;
import com.xiaoyuan.market.mapper.AddressMapper;
import com.xiaoyuan.market.mapper.OrdersMapper;
import com.xiaoyuan.market.mapper.ProductMapper;
import com.xiaoyuan.market.mapper.UserMapper;
import com.xiaoyuan.market.security.LoginUser;
import com.xiaoyuan.market.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务：下单幂等 + 状态机 + 超时取消（MQ 延迟队列）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService extends ServiceImpl<OrdersMapper, Orders> {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final ProductService productService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${market.order-timeout-minutes:15}")
    private int orderTimeoutMinutes;

    @Value("${market.pay-salt}")
    private String paySalt;

    /**
     * 下单（幂等）：
     * 1. 前端预生成 clientOrderNo，数据库 order_no 唯一索引兜底；
     * 2. 重复提交时返回已有订单，不会产生重复数据；
     * 3. 下单成功后向 MQ 延迟队列发送消息（TTL = 超时时间），到期未支付自动取消。
     */
    @Transactional
    public OrderVO create(LoginUser buyer, OrderCreateReq req) {
        Product product = productMapper.selectById(req.getProductId());
        if (product == null || !Objects.equals(product.getStatus(), ProductStatus.ON_SALE.getCode())) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (Objects.equals(product.getUserId(), buyer.getId())) {
            throw new BusinessException("不能购买自己发布的商品");
        }
        Address address = addressMapper.selectById(req.getAddressId());
        if (address == null || !address.getUserId().equals(buyer.getId())) {
            throw new BusinessException("收货地址不存在");
        }

        Orders existing = getByOrderNo(req.getClientOrderNo());
        if (existing != null) {
            // 幂等：同一订单号重复提交，返回原订单
            if (Objects.equals(existing.getBuyerId(), buyer.getId())
                    && Objects.equals(existing.getProductId(), req.getProductId())) {
                return toVO(existing, buyer.getId());
            }
            throw new BusinessException("订单号冲突，请重新下单");
        }

        Orders order = new Orders();
        order.setOrderNo(req.getClientOrderNo());
        order.setBuyerId(buyer.getId());
        order.setSellerId(product.getUserId());
        order.setProductId(product.getId());
        order.setAddressId(address.getId());
        order.setAmount(product.getPrice());
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        try {
            save(order);
        } catch (DuplicateKeyException e) {
            // 并发下单兜底：唯一索引拦截
            Orders concurrent = getByOrderNo(req.getClientOrderNo());
            if (concurrent != null && Objects.equals(concurrent.getBuyerId(), buyer.getId())) {
                return toVO(concurrent, buyer.getId());
            }
            throw new BusinessException("订单号冲突，请重新下单");
        }

        // 发送延迟消息（TTL 到期后死信进入取消队列）
        long ttlMs = orderTimeoutMinutes * 60_000L;
        String orderNo = order.getOrderNo();
        rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_EXCHANGE, RabbitMQConfig.DELAY_KEY, orderNo, msg -> {
            msg.getMessageProperties().setExpiration(String.valueOf(ttlMs));
            return msg;
        });
        log.info("订单 {} 已创建，{} 分钟后未支付将自动取消", orderNo, orderTimeoutMinutes);
        return toVO(order, buyer.getId());
    }

    /**
     * 模拟支付：服务端构造回调参数并验签（模拟网关回调）
     */
    public OrderVO mockPay(LoginUser buyer, String orderNo) {
        Orders order = getByOrderNo(orderNo);
        if (order == null || !Objects.equals(order.getBuyerId(), buyer.getId())) {
            throw new BusinessException("订单不存在");
        }
        String sign = SecureUtil.md5(orderNo + "|" + order.getAmount() + "|" + paySalt);
        return payCallback(orderNo, order.getAmount().toPlainString(), sign);
    }

    /**
     * 支付回调（验签 + 状态机 0 -> 1，幂等：重复回调直接返回成功）
     */
    @Transactional
    public OrderVO payCallback(String orderNo, String amount, String sign) {
        String expected = SecureUtil.md5(orderNo + "|" + amount + "|" + paySalt);
        if (!expected.equalsIgnoreCase(sign)) {
            throw new BusinessException("支付回调验签失败");
        }
        Orders order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getAmount().compareTo(new java.math.BigDecimal(amount)) != 0) {
            throw new BusinessException("回调金额与订单金额不一致");
        }
        if (order.getStatus() >= OrderStatus.WAIT_DELIVER.getCode()) {
            // 幂等：已支付订单重复回调
            log.info("订单 {} 重复支付回调，忽略", orderNo);
            return toVO(order, order.getBuyerId());
        }
        boolean updated = update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getOrderNo, orderNo)
                .eq(Orders::getStatus, OrderStatus.WAIT_PAY.getCode())
                .set(Orders::getStatus, OrderStatus.WAIT_DELIVER.getCode())
                .set(Orders::getPayTime, LocalDateTime.now()));
        if (!updated) {
            throw new BusinessException("订单状态已变更，支付失败");
        }
        // 标记商品已售出
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, order.getProductId())
                .eq(Product::getStatus, ProductStatus.ON_SALE.getCode())
                .set(Product::getStatus, ProductStatus.SOLD.getCode()));
        productService.evictAllCache(order.getProductId());

        // 发送支付成功事件（异步通知）
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAY_EXCHANGE, "", orderNo);
        log.info("订单 {} 支付成功", orderNo);
        return toVO(getByOrderNo(orderNo), order.getBuyerId());
    }

    /**
     * 买家取消未支付订单（0 -> 4）
     */
    public void cancel(LoginUser buyer, String orderNo) {
        Orders order = getByOrderNo(orderNo);
        if (order == null || !Objects.equals(order.getBuyerId(), buyer.getId())) {
            throw new BusinessException("订单不存在");
        }
        if (!transition(orderNo, OrderStatus.WAIT_PAY, OrderStatus.CANCELED)) {
            throw new BusinessException("当前状态不可取消");
        }
    }

    /**
     * 卖家发货（1 -> 2）
     */
    public void deliver(LoginUser seller, String orderNo) {
        Orders order = getByOrderNo(orderNo);
        if (order == null || !Objects.equals(order.getSellerId(), seller.getId())) {
            throw new BusinessException("订单不存在");
        }
        if (!transition(orderNo, OrderStatus.WAIT_DELIVER, OrderStatus.WAIT_RECEIVE)) {
            throw new BusinessException("当前状态不可发货");
        }
    }

    /**
     * 买家确认收货（2 -> 3）
     */
    public void receive(LoginUser buyer, String orderNo) {
        Orders order = getByOrderNo(orderNo);
        if (order == null || !Objects.equals(order.getBuyerId(), buyer.getId())) {
            throw new BusinessException("订单不存在");
        }
        if (!transition(orderNo, OrderStatus.WAIT_RECEIVE, OrderStatus.FINISHED)) {
            throw new BusinessException("当前状态不可确认收货");
        }
    }

    /**
     * 买家申请退款（1/2 -> 5），商品恢复在售
     */
    @Transactional
    public void refund(LoginUser buyer, String orderNo) {
        Orders order = getByOrderNo(orderNo);
        if (order == null || !Objects.equals(order.getBuyerId(), buyer.getId())) {
            throw new BusinessException("订单不存在");
        }
        boolean updated = update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getOrderNo, orderNo)
                .in(Orders::getStatus, OrderStatus.WAIT_DELIVER.getCode(), OrderStatus.WAIT_RECEIVE.getCode())
                .set(Orders::getStatus, OrderStatus.REFUNDED.getCode()));
        if (!updated) {
            throw new BusinessException("当前状态不可退款");
        }
        // 商品恢复在售
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, order.getProductId())
                .eq(Product::getStatus, ProductStatus.SOLD.getCode())
                .set(Product::getStatus, ProductStatus.ON_SALE.getCode()));
        productService.evictAllCache(order.getProductId());
    }

    /**
     * 超时取消订单（MQ 消费者与定时任务共用；条件更新保证只对未支付订单生效）
     */
    public void cancelExpiredOrder(String orderNo) {
        boolean updated = transition(orderNo, OrderStatus.WAIT_PAY, OrderStatus.CANCELED);
        if (updated) {
            log.info("订单 {} 超时未支付，已自动取消", orderNo);
        }
    }

    /**
     * 定时任务兜底：扫描超时未支付订单（防止延迟消息丢失）
     */
    public List<Orders> listExpiredUnpaid() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderTimeoutMinutes);
        return list(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, OrderStatus.WAIT_PAY.getCode())
                .lt(Orders::getCreateTime, deadline)
                .last("limit 100"));
    }

    /**
     * 我买到的
     */
    public Page<OrderVO> pageBought(Long buyerId, Long pageNum, Long pageSize) {
        return pageOrders(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Orders>().eq(Orders::getBuyerId, buyerId).orderByDesc(Orders::getId),
                buyerId);
    }

    /**
     * 我卖出的
     */
    public Page<OrderVO> pageSold(Long sellerId, Long pageNum, Long pageSize) {
        return pageOrders(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Orders>().eq(Orders::getSellerId, sellerId).orderByDesc(Orders::getId),
                sellerId);
    }

    /**
     * 管理后台订单列表
     */
    public Page<OrderVO> pageAll(Integer status, Long pageNum, Long pageSize) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>().orderByDesc(Orders::getId);
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        return pageOrders(new Page<>(pageNum, pageSize), wrapper, null);
    }

    public OrderVO detail(LoginUser viewer, String orderNo) {
        Orders order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        boolean related = Objects.equals(order.getBuyerId(), viewer.getId())
                || Objects.equals(order.getSellerId(), viewer.getId());
        if (!related && !viewer.isAdmin()) {
            throw new BusinessException("无权查看该订单");
        }
        return toVO(order, viewer.getId());
    }

    /**
     * 状态机条件流转（CAS：仅当当前状态符合预期时更新）
     */
    private boolean transition(String orderNo, OrderStatus expected, OrderStatus target) {
        return update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getOrderNo, orderNo)
                .eq(Orders::getStatus, expected.getCode())
                .set(Orders::getStatus, target.getCode()));
    }

    public Orders getByOrderNo(String orderNo) {
        return getOne(new LambdaQueryWrapper<Orders>().eq(Orders::getOrderNo, orderNo));
    }

    private Page<OrderVO> pageOrders(Page<Orders> page, LambdaQueryWrapper<Orders> wrapper, Long viewerId) {
        Page<Orders> result = page(page, wrapper);
        List<Orders> orders = result.getRecords();
        Page<OrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());

        List<Long> productIds = orders.stream().map(Orders::getProductId).distinct().toList();
        List<Long> userIds = orders.stream()
                .flatMap(o -> java.util.stream.Stream.of(o.getBuyerId(), o.getSellerId())).distinct().toList();
        Map<Long, Product> products = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Set<Long> addressIds = orders.stream().map(Orders::getAddressId).collect(Collectors.toSet());
        Map<Long, Address> addresses = addressIds.isEmpty() ? Map.of()
                : addressMapper.selectBatchIds(addressIds).stream()
                        .collect(Collectors.toMap(Address::getId, Function.identity(), (a, b) -> a));

        voPage.setRecords(orders.stream().map(o -> buildVO(o, products, users, addresses, viewerId)).toList());
        return voPage;
    }

    public OrderVO toVO(Orders order, Long viewerId) {
        Product product = productMapper.selectById(order.getProductId());
        User buyer = userMapper.selectById(order.getBuyerId());
        User seller = userMapper.selectById(order.getSellerId());
        Address address = addressMapper.selectById(order.getAddressId());
        return buildVO(order,
                product == null ? Map.of() : Map.of(product.getId(), product),
                Map.of(
                        buyer == null ? -1L : buyer.getId(), buyer == null ? new User() : buyer,
                        seller == null ? -1L : seller.getId(), seller == null ? new User() : seller),
                address == null ? Map.of() : Map.of(address.getId(), address),
                viewerId);
    }

    private OrderVO buildVO(Orders order, Map<Long, Product> products, Map<Long, User> users,
                            Map<Long, Address> addresses, Long viewerId) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setBuyerId(order.getBuyerId());
        vo.setSellerId(order.getSellerId());
        vo.setProductId(order.getProductId());
        vo.setAddressId(order.getAddressId());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        OrderStatus status = OrderStatus.of(order.getStatus());
        vo.setStatusText(status == null ? "未知" : status.getText());
        vo.setPayTime(order.getPayTime());
        vo.setCreateTime(order.getCreateTime());

        Product product = products.get(order.getProductId());
        if (product != null) {
            vo.setProductTitle(product.getTitle());
            vo.setProductCover(product.getCoverImage());
            vo.setProductPrice(product.getPrice());
        }
        User buyer = users.get(order.getBuyerId());
        User seller = users.get(order.getSellerId());
        if (buyer != null) {
            vo.setBuyerNickname(buyer.getNickname());
        }
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
        }
        Address address = addresses.get(order.getAddressId());
        if (address != null) {
            vo.setReceiver(address.getReceiver());
            vo.setReceiverPhone(address.getPhone());
            vo.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
        }
        if (viewerId != null) {
            vo.setRole(Objects.equals(order.getBuyerId(), viewerId) ? "buyer" : "seller");
        }
        return vo;
    }
}
