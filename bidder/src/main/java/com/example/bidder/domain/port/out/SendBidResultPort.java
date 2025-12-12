package com.example.bidder.domain.port.out;

import com.example.bidder.domain.model.Bid;

public interface SendBidResultPort {

  void sendBidResult(Bid bidResult);
}
