package com.example.bidder.adapter.in.web;

import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.model.Device;
import com.example.bidder.domain.model.Gender;
import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.model.User;
import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.utils.MicroConverter;
import org.springframework.stereotype.Component;

@Component
public class BidWebMapper {

  public BidCommand toCommand(BidRequestDto dto) {
    return new BidCommand(
        dto.id(),
        new Impression(
            MicroConverter.toMicro(dto.imp().bidFloor()),
            dto.imp().placementId(),
            dto.imp().width(),
            dto.imp().height()),
        new Device(
            dto.device().ip(),
            dto.device().country(),
            dto.device().os()),
        new User(
            Gender.fromCode(dto.user().gender()),
            dto.user().age())
    );
  }

  public BidResponseDto toDto(Bid domain) {
    return new BidResponseDto(
        domain.requestId(),
        MicroConverter.fromMirco(domain.bidPriceCpmMicro()),
        domain.adMarkup(),
        domain.winUrl()
    );
  }
}
