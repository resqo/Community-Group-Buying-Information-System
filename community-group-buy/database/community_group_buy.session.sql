CREATE DATABASE IF NOT EXISTS `isdemo`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `isdemo`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：USER/LEADER/MERCHANT/ADMIN',
  `community_name` VARCHAR(100) DEFAULT NULL COMMENT '所属社区',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `shop_name` VARCHAR(100) DEFAULT NULL COMMENT '店铺名称',
  `shop_address` VARCHAR(255) DEFAULT NULL COMMENT '店铺地址',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：0禁用，1正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `free_group_count` INT NOT NULL DEFAULT 3 COMMENT '剩余免拼次数';

DROP PROCEDURE IF EXISTS add_user_avatar_url;
DELIMITER //
CREATE PROCEDURE add_user_avatar_url()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'avatar_url'
  ) THEN
    ALTER TABLE `user` ADD COLUMN `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址' AFTER `address`;
  END IF;
END//
DELIMITER ;
CALL add_user_avatar_url();
DROP PROCEDURE add_user_avatar_url;

CREATE TABLE IF NOT EXISTS `category` (
  `category_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父级分类ID',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  PRIMARY KEY (`category_id`),
  KEY `idx_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS `product` (
  `product_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `merchant_id` BIGINT NOT NULL COMMENT '商家用户ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `description` TEXT COMMENT '商品描述',
  `main_image` VARCHAR(255) DEFAULT NULL COMMENT '主图',
  `detail_images` TEXT COMMENT '详情图',
  `original_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
  `group_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '拼团价',
  `single_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单买价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `sales_count` INT NOT NULL DEFAULT 0 COMMENT '销量',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0下架，1上架',
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核：0待审，1通过，2驳回',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`product_id`),
  KEY `idx_product_merchant_id` (`merchant_id`),
  KEY `idx_product_category_id` (`category_id`),
  KEY `idx_product_status_audit` (`status`, `audit_status`),
  CONSTRAINT `fk_product_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `group_activity` (
  `activity_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `group_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '拼团价',
  `group_size` INT NOT NULL COMMENT '成团人数',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `allow_free_group` TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许免拼',
  `free_group_limit` INT NOT NULL DEFAULT 0 COMMENT '免拼次数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`activity_id`),
  KEY `idx_group_activity_product_id` (`product_id`),
  CONSTRAINT `fk_group_activity_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团活动表';

CREATE TABLE IF NOT EXISTS `group_instance` (
  `group_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '拼团ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `leader_user_id` BIGINT NOT NULL COMMENT '开团用户ID',
  `current_count` INT NOT NULL DEFAULT 0 COMMENT '当前人数',
  `required_count` INT NOT NULL COMMENT '成团人数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0进行中，1成功，2失败',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`group_id`),
  KEY `idx_group_instance_activity_id` (`activity_id`),
  KEY `idx_group_instance_leader_user_id` (`leader_user_id`),
  CONSTRAINT `fk_group_instance_activity` FOREIGN KEY (`activity_id`) REFERENCES `group_activity` (`activity_id`),
  CONSTRAINT `fk_group_instance_leader_user` FOREIGN KEY (`leader_user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拼团实例表';

CREATE TABLE IF NOT EXISTS `pickup_point` (
  `pickup_point_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自提点ID',
  `leader_id` BIGINT NOT NULL COMMENT '团长用户ID',
  `point_name` VARCHAR(100) NOT NULL COMMENT '自提点名称',
  `community_name` VARCHAR(100) DEFAULT NULL COMMENT '所属社区',
  `address` VARCHAR(255) NOT NULL COMMENT '地址',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `business_hours` VARCHAR(100) DEFAULT NULL COMMENT '营业时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  PRIMARY KEY (`pickup_point_id`),
  KEY `idx_pickup_point_leader_id` (`leader_id`),
  CONSTRAINT `fk_pickup_point_leader` FOREIGN KEY (`leader_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自提点表';

CREATE TABLE IF NOT EXISTS `orders` (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '下单用户ID',
  `merchant_id` BIGINT NOT NULL COMMENT '商家用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品快照',
  `product_price` DECIMAL(10,2) NOT NULL COMMENT '下单价格',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `group_id` BIGINT DEFAULT NULL COMMENT '拼团ID',
  `order_type` VARCHAR(20) NOT NULL COMMENT '订单类型',
  `pickup_point_id` BIGINT NOT NULL COMMENT '自提点ID',
  `pickup_code` VARCHAR(20) DEFAULT NULL COMMENT '取件码',
  `pickup_status` TINYINT NOT NULL DEFAULT 0 COMMENT '取件状态',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态',
  `delivery_status` TINYINT NOT NULL DEFAULT 0 COMMENT '发货状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`),
  KEY `idx_orders_user_id` (`user_id`),
  KEY `idx_orders_merchant_id` (`merchant_id`),
  KEY `idx_orders_product_id` (`product_id`),
  KEY `idx_orders_group_id` (`group_id`),
  KEY `idx_orders_pickup_point_id` (`pickup_point_id`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_orders_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_orders_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`),
  CONSTRAINT `fk_orders_group` FOREIGN KEY (`group_id`) REFERENCES `group_instance` (`group_id`),
  CONSTRAINT `fk_orders_pickup_point` FOREIGN KEY (`pickup_point_id`) REFERENCES `pickup_point` (`pickup_point_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `payment` (
  `payment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `pay_no` VARCHAR(100) NOT NULL COMMENT '支付流水号',
  `pay_method` VARCHAR(50) NOT NULL COMMENT '支付方式',
  `pay_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payment_pay_no` (`pay_no`),
  KEY `idx_payment_order_id` (`order_id`),
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';

CREATE TABLE IF NOT EXISTS `refund` (
  `refund_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '退款ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `user_id` BIGINT NOT NULL COMMENT '申请用户ID',
  `refund_reason` VARCHAR(255) NOT NULL COMMENT '退款原因',
  `refund_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`refund_id`),
  KEY `idx_refund_order_id` (`order_id`),
  KEY `idx_refund_user_id` (`user_id`),
  KEY `idx_refund_admin_id` (`admin_id`),
  CONSTRAINT `fk_refund_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  CONSTRAINT `fk_refund_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_refund_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款售后表';

CREATE TABLE IF NOT EXISTS `notice` (
  `notice_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '接收用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `notice_type` VARCHAR(50) NOT NULL COMMENT '通知类型',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_notice_user_id` (`user_id`),
  CONSTRAINT `fk_notice_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知表';

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `category` (`category_name`, `parent_id`, `sort`, `status`)
SELECT '新鲜果蔬', 0, 1, 1 WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `category_name` = '新鲜果蔬');
INSERT INTO `category` (`category_name`, `parent_id`, `sort`, `status`)
SELECT '肉禽蛋奶', 0, 2, 1 WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `category_name` = '肉禽蛋奶');
INSERT INTO `category` (`category_name`, `parent_id`, `sort`, `status`)
SELECT '粮油副食', 0, 3, 1 WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `category_name` = '粮油副食');
INSERT INTO `category` (`category_name`, `parent_id`, `sort`, `status`)
SELECT '日用百货', 0, 4, 1 WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `category_name` = '日用百货');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'admin_1', '123456', '13800000001', '沈明远', 'ADMIN', '幸福社区', '社区服务中心', NULL, NULL, NULL, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'admin_1');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'leader_1', '123456', '13800000003', '周莉莉', 'LEADER', '幸福社区', '幸福小区北门', NULL, NULL, NULL, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'leader_1');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'user_1', '123456', '13800001001', '林小满', 'USER', '幸福社区', '幸福小区 3 栋 1202', NULL, NULL, NULL, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'user_1');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'user_2', '123456', '13800001002', '陈一诺', 'USER', '幸福社区', '幸福小区 5 栋 802', NULL, NULL, NULL, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'user_2');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'user_3', '123456', '13800001003', '王若溪', 'USER', '幸福社区', '幸福小区 8 栋 301', NULL, NULL, NULL, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'user_3');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'merchant_1', '123456', '13800002000', '顾清源', 'MERCHANT', '幸福社区', '幸福路 66 号', NULL, '鲜选优品', '幸福路 66 号', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'merchant_1');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'merchant_2', '123456', '13800002001', '许青禾', 'MERCHANT', '幸福社区', '幸福路 88 号', NULL, '青禾鲜果蔬', '幸福路 88 号', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'merchant_2');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'merchant_3', '123456', '13800002002', '赵牧原', 'MERCHANT', '幸福社区', '团结街 16 号', NULL, '牧原肉禽蛋奶', '团结街 16 号', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'merchant_3');

INSERT INTO `user` (`username`, `password`, `phone`, `real_name`, `role`, `community_name`, `address`, `avatar_url`, `shop_name`, `shop_address`, `status`, `create_time`)
SELECT 'merchant_4', '123456', '13800002003', '苏简', 'MERCHANT', '幸福社区', '邻里巷 9 号', NULL, '苏简生活百货', '邻里巷 9 号', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'merchant_4');

INSERT INTO `pickup_point` (`leader_id`, `point_name`, `community_name`, `address`, `phone`, `business_hours`, `status`)
SELECT u.`user_id`, '幸福社区北门自提点', '幸福社区', '幸福小区北门 1 号', '13800000003', '09:00-20:00', 1
FROM `user` u
WHERE u.`username` = 'leader_1'
  AND NOT EXISTS (SELECT 1 FROM `pickup_point` WHERE `point_name` = '幸福社区北门自提点');

INSERT INTO `product` (`merchant_id`, `category_id`, `product_name`, `description`, `main_image`, `detail_images`, `original_price`, `group_price`, `single_price`, `stock`, `sales_count`, `status`, `audit_status`, `create_time`)
SELECT u.`user_id`, c.`category_id`, '精品红富士苹果 5斤装', '产地直采，脆甜多汁，适合家庭分享。', 'https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=900&q=80', '', 39.90, 29.90, 35.90, 120, 28, 1, 1, NOW()
FROM `user` u JOIN `category` c ON c.`category_name` = '新鲜果蔬'
WHERE u.`username` = 'merchant_2'
  AND NOT EXISTS (SELECT 1 FROM `product` WHERE `product_name` = '精品红富士苹果 5斤装');

INSERT INTO `product` (`merchant_id`, `category_id`, `product_name`, `description`, `main_image`, `detail_images`, `original_price`, `group_price`, `single_price`, `stock`, `sales_count`, `status`, `audit_status`, `create_time`)
SELECT u.`user_id`, c.`category_id`, '云南甜玉米 8根', '清甜糯香，真空保鲜，早餐和晚餐都方便。', 'https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&w=900&q=80', '', 28.80, 19.90, 24.90, 90, 35, 1, 1, NOW()
FROM `user` u JOIN `category` c ON c.`category_name` = '新鲜果蔬'
WHERE u.`username` = 'merchant_2'
  AND NOT EXISTS (SELECT 1 FROM `product` WHERE `product_name` = '云南甜玉米 8根');

INSERT INTO `product` (`merchant_id`, `category_id`, `product_name`, `description`, `main_image`, `detail_images`, `original_price`, `group_price`, `single_price`, `stock`, `sales_count`, `status`, `audit_status`, `create_time`)
SELECT u.`user_id`, c.`category_id`, '冷鲜鸡胸肉 1kg', '低脂高蛋白，当日分切冷链配送。', 'https://images.unsplash.com/photo-1604503468506-a8da13d82791?auto=format&fit=crop&w=900&q=80', '', 42.00, 31.90, 36.90, 75, 18, 1, 1, NOW()
FROM `user` u JOIN `category` c ON c.`category_name` = '肉禽蛋奶'
WHERE u.`username` = 'merchant_3'
  AND NOT EXISTS (SELECT 1 FROM `product` WHERE `product_name` = '冷鲜鸡胸肉 1kg');

INSERT INTO `product` (`merchant_id`, `category_id`, `product_name`, `description`, `main_image`, `detail_images`, `original_price`, `group_price`, `single_price`, `stock`, `sales_count`, `status`, `audit_status`, `create_time`)
SELECT u.`user_id`, c.`category_id`, '农家土鸡蛋 30枚', '谷物散养鸡蛋，蛋香浓郁，破损包赔。', 'https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?auto=format&fit=crop&w=900&q=80', '', 36.80, 27.90, 32.90, 160, 52, 1, 1, NOW()
FROM `user` u JOIN `category` c ON c.`category_name` = '肉禽蛋奶'
WHERE u.`username` = 'merchant_3'
  AND NOT EXISTS (SELECT 1 FROM `product` WHERE `product_name` = '农家土鸡蛋 30枚');

INSERT INTO `product` (`merchant_id`, `category_id`, `product_name`, `description`, `main_image`, `detail_images`, `original_price`, `group_price`, `single_price`, `stock`, `sales_count`, `status`, `audit_status`, `create_time`)
SELECT u.`user_id`, c.`category_id`, '五常稻花香大米 5kg', '新米现碾，米香清润，家庭常备。', 'https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=900&q=80', '', 79.00, 59.90, 68.90, 55, 24, 1, 1, NOW()
FROM `user` u JOIN `category` c ON c.`category_name` = '粮油副食'
WHERE u.`username` = 'merchant_4'
  AND NOT EXISTS (SELECT 1 FROM `product` WHERE `product_name` = '五常稻花香大米 5kg');

INSERT INTO `product` (`merchant_id`, `category_id`, `product_name`, `description`, `main_image`, `detail_images`, `original_price`, `group_price`, `single_price`, `stock`, `sales_count`, `status`, `audit_status`, `create_time`)
SELECT u.`user_id`, c.`category_id`, '家用抽纸 24包整箱', '柔韧亲肤，家庭囤货装，客厅卧室都适用。', 'https://images.unsplash.com/photo-1584556812952-905ffd0c611a?auto=format&fit=crop&w=900&q=80', '', 49.90, 36.90, 42.90, 110, 41, 1, 1, NOW()
FROM `user` u JOIN `category` c ON c.`category_name` = '日用百货'
WHERE u.`username` = 'merchant_4'
  AND NOT EXISTS (SELECT 1 FROM `product` WHERE `product_name` = '家用抽纸 24包整箱');

INSERT INTO `group_activity` (`product_id`, `group_price`, `group_size`, `start_time`, `end_time`, `allow_free_group`, `free_group_limit`, `status`, `create_time`)
SELECT p.`product_id`, p.`group_price`, 3, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 1, 1, NOW()
FROM `product` p
WHERE p.`product_name` IN ('精品红富士苹果 5斤装', '农家土鸡蛋 30枚', '家用抽纸 24包整箱')
  AND NOT EXISTS (SELECT 1 FROM `group_activity` WHERE `product_id` = p.`product_id`);

-- Fix zero group activity prices after existing data imports.
UPDATE `group_activity` ga
JOIN `product` p ON ga.`product_id` = p.`product_id`
SET ga.`group_price` = p.`group_price`,
    ga.`allow_free_group` = 1
WHERE ga.`group_price` IS NULL OR ga.`group_price` <= 0;
