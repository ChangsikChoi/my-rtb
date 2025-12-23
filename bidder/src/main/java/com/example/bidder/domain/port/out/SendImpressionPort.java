package com.example.bidder.domain.port.out;

import com.example.bidder.domain.model.Impression;

public interface SendImpressionPort {

  void sendImpression(Impression impression);

}
