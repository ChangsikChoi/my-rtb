package com.example.bidder.domain.port.out;

import com.example.bidder.domain.model.Click;

public interface SendClickPort {

  void sendClick(Click click);

}
