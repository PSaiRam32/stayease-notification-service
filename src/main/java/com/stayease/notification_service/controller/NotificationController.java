package com.stayease.notification_service.controller;

import com.stayease.notification_service.dto.ApiResponse;
import com.stayease.notification_service.dto.NotificationRequestDTO;
import com.stayease.notification_service.dto.NotificationResponseDTO;
import com.stayease.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponseDTO> sendNotification(
            @RequestBody NotificationRequestDTO request) {
        log.info("Received notification request for booking: {}", request.getBookingId());
        NotificationResponseDTO response = notificationService.sendNotification(request);
        return ResponseEntity.ok(response);
    }
}