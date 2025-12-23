package com.example.bidder.adapter.in.web.impression;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImpressionRequestDto(
    @Schema(description = "광고 요청 ID")
    String rid,
    @Schema(description = "캠페인 ID")
    String cid,
    @Schema(description = "소재 ID")
    String crid
) {

}
