package com.example.ad_manager.mapper.web;

import com.example.ad_manager.model.dto.CampaignCreateRequestDto;
import com.example.ad_manager.model.dto.CampaignResponseDto;
import com.example.ad_manager.model.dto.CreativeCreateRequestDto;
import com.example.ad_manager.model.dto.TargetCreateRequestDto;
import com.example.ad_manager.model.request.CampaignCreateRequest;
import com.example.ad_manager.model.response.CampaignResponse;
import com.example.ad_manager.model.response.CreativeResponse;
import com.example.ad_manager.model.response.TargetResponse;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class CampaignWebMapper {

  public CampaignCreateRequestDto createRequestToDto(CampaignCreateRequest request) {
    return CampaignCreateRequestDto.builder()
        .name(request.name())
        .targetCpm(request.targetCpm())
        .budget(request.budget())
        .startDate(request.startDate().atStartOfDay())
        .endDate(request.endDate().atTime(LocalTime.of(23, 59, 59)))
        .target(TargetCreateRequestDto.builder()
            .os(request.target().os())
            .country(request.target().country())
            .minAge(request.target().minAge())
            .maxAge(request.target().maxAge())
            .build())
        .creative(CreativeCreateRequestDto.builder()
            .name(request.creative().name())
            .imageUrl(request.creative().imageUrl())
            .clickUrl(request.creative().clickUrl())
            .width(request.creative().width())
            .height(request.creative().height())
            .build())
        .build();
  }

  public CampaignResponse responseDtoToResponse(CampaignResponseDto dto) {
    return CampaignResponse.builder()
        .id(dto.id())
        .name(dto.name())
        .targetCpm(dto.targetCpm())
        .budget(dto.budget())
        .startDate(dto.startDate().toLocalDate())
        .endDate(dto.endDate().toLocalDate())
        .active(dto.active())
        .target(TargetResponse.builder()
            .id(dto.target().id())
            .os(dto.target().os())
            .country(dto.target().country())
            .minAge(dto.target().minAge())
            .maxAge(dto.target().maxAge())
            .createdAt(dto.target().createdAt())
            .updatedAt(dto.target().updatedAt())
            .build())
        .creative(CreativeResponse.builder()
            .id(dto.creative().id())
            .name(dto.creative().name())
            .imageUrl(dto.creative().imageUrl())
            .clickUrl(dto.creative().clickUrl())
            .width(dto.creative().width())
            .height(dto.creative().height())
            .createdAt(dto.creative().createdAt())
            .updatedAt(dto.creative().updatedAt())
            .build())
        .createdAt(dto.createdAt())
        .updatedAt(dto.updatedAt())
        .build();
  }
}
