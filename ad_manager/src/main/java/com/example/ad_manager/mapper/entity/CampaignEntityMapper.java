package com.example.ad_manager.mapper.entity;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CreativeEntity;
import com.example.ad_manager.entity.TargetEntity;
import com.example.ad_manager.model.dto.CampaignCreateRequestDto;
import com.example.ad_manager.model.dto.CampaignResponseDto;
import com.example.ad_manager.model.dto.CreativeResponseDto;
import com.example.ad_manager.model.dto.TargetResponseDto;
import org.springframework.stereotype.Component;

@Component
public class CampaignEntityMapper {

  public CampaignEntity dtoToEntity(CampaignCreateRequestDto dto) {
    return CampaignEntity.builder()
        .name(dto.name())
        .targetCpm(dto.targetCpm())
        .budget(dto.budget())
        .startDate(dto.startDate())
        .endDate(dto.endDate())
        .target(TargetEntity.builder()
            .os(dto.target().os())
            .country(dto.target().country())
            .minAge(dto.target().minAge())
            .maxAge(dto.target().maxAge())
            .build())
        .creative(CreativeEntity.builder()
            .name(dto.creative().name())
            .imageUrl(dto.creative().imageUrl())
            .clickUrl(dto.creative().clickUrl())
            .width(dto.creative().width())
            .height(dto.creative().height())
            .build())
        .build();
  }

  public CampaignResponseDto entityToResponseDto(CampaignEntity entity) {
    return CampaignResponseDto.builder()
        .id(entity.getId())
        .name(entity.getName())
        .targetCpm(entity.getTargetCpm())
        .budget(entity.getBudget())
        .startDate(entity.getStartDate())
        .endDate(entity.getEndDate())
        .active(entity.isActive())
        .target(TargetResponseDto.builder()
            .id(entity.getTarget().getId())
            .os(entity.getTarget().getOs())
            .country(entity.getTarget().getCountry())
            .minAge(entity.getTarget().getMinAge())
            .maxAge(entity.getTarget().getMaxAge())
            .createdAt(entity.getTarget().getCreatedAt())
            .updatedAt(entity.getTarget().getUpdatedAt())
            .build())
        .creative(CreativeResponseDto.builder()
            .id(entity.getCreative().getId())
            .name(entity.getCreative().getName())
            .imageUrl(entity.getCreative().getImageUrl())
            .clickUrl(entity.getCreative().getClickUrl())
            .width(entity.getCreative().getWidth())
            .height(entity.getCreative().getHeight())
            .createdAt(entity.getCreative().getCreatedAt())
            .updatedAt(entity.getCreative().getUpdatedAt())
            .build())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
