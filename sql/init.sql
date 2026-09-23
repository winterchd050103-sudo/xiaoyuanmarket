-- =============================================
-- 校园二手交易平台 数据库初始化脚本
-- MySQL 8.x / InnoDB / utf8mb4
-- =============================================
-- 强制本次连接使用 utf8mb4（防止容器初始化导入时按 latin1 解析 UTF-8 文件导致中文乱码）
SET NAMES utf8mb4;
DROP DATABASE IF EXISTS xiaoyuan_market;
CREATE DATABASE xiaoyuan_market DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE xiaoyuan_market;

-- 1. 用户表
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名，唯一',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号，唯一',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0 普通用户，1 管理员',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB COMMENT = '用户表';

-- 2. 商品分类表
CREATE TABLE `category` (
    `id`     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`   VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort`   INT         NOT NULL DEFAULT 0 COMMENT '排序权重',
    `status` TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0 停用，1 启用',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '商品分类表';

-- 3. 商品表
CREATE TABLE `product` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT        NOT NULL COMMENT '卖家用户 id',
    `category_id`    BIGINT        NOT NULL COMMENT '分类 id',
    `title`          VARCHAR(100)  NOT NULL COMMENT '商品标题',
    `description`    TEXT          COMMENT '商品描述',
    `price`          DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价（可选）',
    `cover_image`    VARCHAR(255)  DEFAULT NULL COMMENT '封面图',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0 待审核，1 在售，2 已下架，3 已售出',
    `view_count`     INT           NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_status` (`category_id`, `status`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '商品表';

-- 4. 商品图片表
CREATE TABLE `product_image` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id` BIGINT       NOT NULL COMMENT '商品 id',
    `url`        VARCHAR(255) NOT NULL COMMENT '图片地址',
    `sort`       INT          NOT NULL DEFAULT 0 COMMENT '图片顺序',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB COMMENT = '商品图片表';

-- 5. 收藏表
CREATE TABLE `favorite` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户 id',
    `product_id`  BIGINT   NOT NULL COMMENT '商品 id',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE = InnoDB COMMENT = '收藏表';

-- 6. 收货地址表
CREATE TABLE `address` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户 id',
    `receiver`    VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '收货人电话',
    `province`    VARCHAR(50)  NOT NULL COMMENT '省',
    `city`        VARCHAR(50)  NOT NULL COMMENT '市',
    `district`    VARCHAR(50)  NOT NULL COMMENT '区县',
    `detail`      VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认：0 否，1 是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '收货地址表';

-- 7. 订单表
CREATE TABLE `orders` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`    VARCHAR(64)   NOT NULL COMMENT '订单号，唯一',
    `buyer_id`    BIGINT        NOT NULL COMMENT '买家用户 id',
    `seller_id`   BIGINT        NOT NULL COMMENT '卖家用户 id',
    `product_id`  BIGINT        NOT NULL COMMENT '商品 id',
    `address_id`  BIGINT        NOT NULL COMMENT '收货地址 id',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待付款，1 待发货，2 待收货，3 已完成，4 已取消，5 已退款',
    `pay_time`    DATETIME      DEFAULT NULL COMMENT '支付时间（可空）',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE = InnoDB COMMENT = '订单表';

-- 8. 商品评论表
CREATE TABLE `comment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '评论用户 id',
    `product_id`  BIGINT       NOT NULL COMMENT '商品 id',
    `order_id`    BIGINT       DEFAULT NULL COMMENT '关联订单 id（可空）',
    `content`     VARCHAR(500) NOT NULL COMMENT '评论内容',
    `rating`      TINYINT      NOT NULL DEFAULT 5 COMMENT '评分 1 至 5',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '商品评论表';

-- =============================================
-- 种子数据
-- =============================================
-- 账号说明：admin/123456（管理员）  test/123456  buyer/123456（普通用户）
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
('admin',  '$2a$10$uWCv9fXpKpHN2rzCV1sihu9S0LcTin.gVpmRfZU5qFz6gZfOeWzM6', '平台管理员', '13800000000', 1, 1),
('test',   '$2a$10$uWCv9fXpKpHN2rzCV1sihu9S0LcTin.gVpmRfZU5qFz6gZfOeWzM6', '小明同学',   '13800000001', 0, 1),
('buyer',  '$2a$10$uWCv9fXpKpHN2rzCV1sihu9S0LcTin.gVpmRfZU5qFz6gZfOeWzM6', '小红同学',   '13800000002', 0, 1);
-- 注意：上方 BCrypt 串对应明文 123456（三个账号统一），admin 登录后建议自行改密

INSERT INTO `category` (`name`, `sort`, `status`) VALUES
('手机数码', 1, 1),
('电脑办公', 2, 1),
('图书教材', 3, 1),
('生活用品', 4, 1),
('运动健身', 5, 1),
('服饰鞋包', 6, 1),
('乐器文创', 7, 1),
('其他闲置', 8, 1);

-- 演示商品（test 用户发布；配图位于 backend/upload/seed/，由静态映射 /api/upload/** 提供）
INSERT INTO `product` (`user_id`, `category_id`, `title`, `description`, `price`, `original_price`, `cover_image`, `status`, `view_count`) VALUES
(2, 1, 'iPhone 12 128G 蓝色 95新', '大一入手，平时贴膜带壳使用，电池效率89%，无拆无修，配件齐全，诚心出。', 2699.00, 5999.00, '/api/upload/seed/1.jpg', 1, 35),
(2, 2, '联想小新Pro16 2022款', 'R7-6800H + 16G + 512G，毕业急出，屏幕无亮点，可小刀。', 3899.00, 5499.00, '/api/upload/seed/2.jpg', 1, 22),
(2, 3, '高等数学 同济第七版 上下册', '期末救命神器，笔记齐全，九成新，打包出。', 25.00, 78.00, '/api/upload/seed/3.jpg', 1, 48),
(2, 4, '宿舍折叠书桌 小户型必备', '买来用了两次，宿舍改朝换代用不上了，几乎全新。', 39.00, 89.00, '/api/upload/seed/4.jpg', 1, 12),
(2, 5, '尤尼克斯羽毛球拍 双拍', '陪练 twice 就吃灰了，拍线无断，手胶新换。', 129.00, 299.00, '/api/upload/seed/5.jpg', 1, 9);

-- 演示商品相册图
INSERT INTO `product_image` (`product_id`, `url`, `sort`) VALUES
(1, '/api/upload/seed/1.jpg', 0),
(2, '/api/upload/seed/2.jpg', 0),
(3, '/api/upload/seed/3.jpg', 0),
(4, '/api/upload/seed/4.jpg', 0),
(5, '/api/upload/seed/5.jpg', 0);
