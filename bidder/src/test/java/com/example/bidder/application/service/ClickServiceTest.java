package com.example.bidder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.bidder.domain.model.Click;
import com.example.bidder.domain.port.in.ClickCommand;
import com.example.bidder.domain.port.out.SendClickPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ClickServiceTest {

  @Mock
  private SendClickPort sendClickPort;

  @InjectMocks
  private ClickService clickService;


  @Test
  void whenRequestIdIsEmpty_thenReturnEmptyMono() {
    ClickCommand command = mock(ClickCommand.class);

    Mono<Click> clickMono = clickService.handleClick(command);

    StepVerifier.create(clickMono)
        .expectNextCount(0)
        .verifyComplete();

    verify(sendClickPort, times(0)).sendClick(any());
  }

  @Test
  void whenRequestIdNotIsEmpty_thenReturnClickMono() {
    ClickCommand command = new ClickCommand("rid1", "cid1", "crid1");

    Mono<Click> clickMono = clickService.handleClick(command);

    StepVerifier.create(clickMono)
        .assertNext(click -> {
          assertThat(click.id()).isEqualTo(command.requestId());
          assertThat(click.campaignId()).isEqualTo(command.campaignId());
          assertThat(click.creativeId()).isEqualTo(command.creativeId());
        })
        .verifyComplete();

    verify(sendClickPort, times(1)).sendClick(any());
  }

}