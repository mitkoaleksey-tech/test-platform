package com.example.test_platform.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateSubscriptionRequest {

    private LocalDateTime subscriptionPaidAt;

    private LocalDateTime nextPaymentAt;
}
