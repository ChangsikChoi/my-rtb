package com.example.ad_manager.exception;

public class CampaignTransactionException extends ApiException {

  private CampaignTransactionException(String message) {
    super("CAMPAIGN_TRANSACTION_FAILED", message);
  }

  public static CampaignTransactionException activationFailed() {
    return new CampaignTransactionException(
        "campaign activation failed during database transaction"
    );
  }

  public static CampaignTransactionException deactivationFailed() {
    return new CampaignTransactionException(
        "campaign deactivation failed during database transaction"
    );
  }
}
