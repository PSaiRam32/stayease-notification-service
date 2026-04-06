package com.stayease.notification_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NotificationResponseDTO {
    private Long id;
    private Long bookingId;
    private String userId;
    private String type;
    private String status;
}