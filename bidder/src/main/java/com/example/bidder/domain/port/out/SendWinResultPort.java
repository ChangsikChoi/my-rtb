package com.example.bidder.domain.port.out;

import com.example.bidder.domain.model.Win;

public interface SendWinResultPort {

  void sendWinResult(Win winResult);

}
