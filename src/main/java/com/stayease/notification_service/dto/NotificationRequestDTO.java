package com.stayease.notification_service.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDTO {

    private Long bookingId;
    private String email;
    private String phoneNumber;
    private String status; // BOOKING_CONFIRMED / PAYMENT_FAILED / BOOKING_CANCELLED
    private String type;
    private String message;
    private Long userId;
}