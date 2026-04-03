package com.example.ad_manager.exception;

public class CampaignRedisSyncException extends ApiException {

  private CampaignRedisSyncException(String message, Throwable cause) {
    super("CAMPAIGN_REDIS_SYNC_FAILED", message, cause);
  }

  public static CampaignRedisSyncException activationFailed(Throwable cause) {
    return new CampaignRedisSyncException(
        "campaign activation failed during redis sync",
        cause
    );
  }

  public static CampaignRedisSyncException deactivationFailed(Throwable cause) {
    return new CampaignRedisSyncException(
        "campaign deactivation failed during redis sync",
        cause
    );
  }

  public static CampaignRedisSyncException activationCompensationFailed(Throwable cause) {
    return new CampaignRedisSyncException(
        "campaign activation failed during redis sync and compensation failed",
        cause
    );
  }

  public static CampaignRedisSyncException deactivationCompensationFailed(Throwable cause) {
    return new CampaignRedisSyncException(
        "campaign deactivation failed during redis sync and compensation failed",
        cause
    );
  }
}
