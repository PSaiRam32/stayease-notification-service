package com.stayease.notification_service.service;

import com.stayease.notification_service.dto.*;

public interface NotificationService {

    NotificationResponseDTO sendNotification(NotificationRequestDTO request);
    void sendEmail(NotificationRequestDTO request);
    void sendSMS(NotificationRequestDTO request);
}