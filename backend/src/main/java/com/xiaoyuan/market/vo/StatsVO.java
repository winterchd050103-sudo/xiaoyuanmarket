package com.xiaoyuan.market.vo;

import lombok.Data;

/**
 * 管理后台统计数据
 */
@Data
public class StatsVO {

    private Long userCount;

    private Long productCount;

    private Long orderCount;

    /** 成交额（已完成订单金额合计） */
    private java.math.BigDecimal gmv;

    /** 近 7 日订单量趋势 */
    private java.util.List<TrendItem> orderTrend;

    /** 近 7 日商品发布量趋势 */
    private java.util.List<TrendItem> productTrend;

    @Data
    public static class TrendItem {
        private String date;
        private Long count;

        public TrendItem(String date, Long count) {
            this.date = date;
            this.count = count;
        }
    }
}
