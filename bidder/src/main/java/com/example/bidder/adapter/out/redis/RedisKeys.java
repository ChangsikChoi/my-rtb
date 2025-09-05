package com.example.bidder.adapter.out.redis;

public class RedisKeys {
    public static final String CAMPAIGN_PREFIX = "campaign:";
    public static final String CAMPAIGN_LIST_KEY = "campaign:ids";
    public static final String CAMPAIGN_TOTAL_BUDGET_SUFFIX = ":budget_total";
    public static final String CAMPAIGN_RESERVED_BUDGET_SUFFIX = ":budget_reserved";

    public static final String RESERVATION_PREFIX = "reservation:";
    public static final String RESERVATION_BACKUP_PREFIX = "reservation_backup:";

    public static String campaignKey(String id) {
        return CAMPAIGN_PREFIX + id;
    }

    public static String campaignTotalBudgetKey(String id) {
        return CAMPAIGN_PREFIX + id + CAMPAIGN_TOTAL_BUDGET_SUFFIX;
    }

    public static String campaignReservedBudgetKey(String id) {
        return CAMPAIGN_PREFIX + id + CAMPAIGN_RESERVED_BUDGET_SUFFIX;
    }

    public static String reservationKey(String requestId) {
        return RESERVATION_PREFIX + requestId;
    }
    public static String reservationBackupKey(String requestId) {
        return RESERVATION_BACKUP_PREFIX + requestId;
    }


}
