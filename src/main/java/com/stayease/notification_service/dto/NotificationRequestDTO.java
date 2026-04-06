package com.stayease.notification_service.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class NotificationRequestDTO {

    @NotNull
    private Long bookingId;
    @NotBlank
    private String userId;
    private String type;
    @NotBlank
    private String message;
}