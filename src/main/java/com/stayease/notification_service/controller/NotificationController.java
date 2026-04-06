package com.stayease.notification_service.controller;

import com.stayease.notification_service.dto.NotificationRequestDTO;
import com.stayease.notification_service.dto.NotificationResponseDTO;
import com.stayease.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponseDTO> sendNotification(@Valid @RequestBody NotificationRequestDTO request) {
        NotificationResponseDTO response = notificationService.sendNotification(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email")
    public ResponseEntity<Void> sendEmail(@Valid @RequestBody NotificationRequestDTO request) {
        notificationService.sendEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sms")
    public ResponseEntity<Void> sendSms(@Valid @RequestBody NotificationRequestDTO request) {
        notificationService.sendSMS(request);  // Fixed method name
        return ResponseEntity.ok().build();
    }
}
