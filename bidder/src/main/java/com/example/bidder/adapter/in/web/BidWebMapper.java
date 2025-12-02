package com.example.bidder.adapter.in.web;

import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.model.Device;
import com.example.bidder.domain.model.Device.DeviceBuilder;
import com.example.bidder.domain.model.Gender;
import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.model.User;
import com.example.bidder.domain.model.User.UserBuilder;
import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.utils.MicroConverter;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BidWebMapper {

  public BidCommand toCommand(BidRequestDto dto) {
    if (dto == null) {
      throw new IllegalArgumentException("BidRequestDto must not be null");
    }

    BidRequestDto.ImpressionDto impDto = dto.imp();
    if (impDto == null) {
      throw new IllegalArgumentException("imp is required");
    }

    Impression impression = Impression.builder()
        .bidFloorMicro(MicroConverter.toMicro(
            dto.imp().bidFloor() != null ? dto.imp().bidFloor() : BigDecimal.ZERO))
        .placementId(impDto.placementId())
        .width(impDto.width())
        .height(impDto.height())
        .build();

    DeviceBuilder deviceBuilder = Device.builder();
    if (dto.device() != null) {
      deviceBuilder
          .ip(dto.device().ip())
          .country(dto.device().country())
          .os(dto.device().os());
    }
    Device device = deviceBuilder.build();

    UserBuilder userBuilder = User.builder();
    String userGender = null;
    if (dto.user() != null) {
      userBuilder
          .age(dto.user().age());
      userGender = dto.user().gender();
    }
    userBuilder.gender(Gender.fromCode(userGender));
    User user = userBuilder.build();

    return new BidCommand(dto.id(), impression, device, user);
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
