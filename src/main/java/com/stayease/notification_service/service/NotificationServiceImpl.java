package com.stayease.notification_service.service;

import com.stayease.notification_service.dto.NotificationRequestDTO;
import com.stayease.notification_service.dto.NotificationResponseDTO;
import com.stayease.notification_service.entity.Notification;
import com.stayease.notification_service.entity.NotificationStatus;
import com.stayease.notification_service.entity.NotificationType;
import com.stayease.notification_service.exception.BusinessException;
import com.stayease.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private static final int MAX_RETRY = 3;

    @Override
    public NotificationResponseDTO sendNotification(NotificationRequestDTO request) {
        if (request.getBookingId() == null) {
            throw new BusinessException("Booking ID is required");
        }
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BusinessException("User ID is required");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException("Message cannot be empty");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new BusinessException("Notification type is required");
        }

        // Parse type
        NotificationType type;
        try {
            type = NotificationType.valueOf(request.getType().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException("Invalid notification type. Allowed: EMAIL, SMS");
        }

        // Use shared logic
        Notification notification = createAndProcess(request, type);
        return mapToDTO(notification);
    }

    // Public methods for direct email/SMS sending (forces type)
    public void sendEmail(NotificationRequestDTO request) {
        request.setType("EMAIL");  // Force type
        createAndProcess(request, NotificationType.EMAIL);
    }

    public void sendSMS(NotificationRequestDTO request) {
        request.setType("SMS");  // Force type
        createAndProcess(request, NotificationType.SMS);
    }

    // Shared method for creation and processing
    private Notification createAndProcess(NotificationRequestDTO request, NotificationType type) {
        Notification notification = Notification.builder()
                .bookingId(request.getBookingId())
                .userId(request.getUserId())
                .type(type)
                .message(request.getMessage())
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
        notificationRepository.save(notification);
        processNotification(notification);
        return notification;
    }

    // Process with retry logic
    private void processNotification(Notification notification) {
        try {
            notification.setStatus(NotificationStatus.PROCESSING);
            notificationRepository.save(notification);

            // Actual sending logic
            if (notification.getType() == NotificationType.EMAIL) {
                sendEmail(notification);
            } else if (notification.getType() == NotificationType.SMS) {
                sendSMS(notification);
            }

            notification.setStatus(NotificationStatus.SENT);
        } catch (Exception ex) {
            notification.setRetryCount(notification.getRetryCount() + 1);
            if (notification.getRetryCount() >= MAX_RETRY) {
                notification.setStatus(NotificationStatus.FAILED);
            } else {
                notification.setStatus(NotificationStatus.PENDING);
            }
        }
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Placeholder for actual email sending (integrate with Spring Mail or similar)
    private void sendEmail(Notification notification) {
        // Example: Use JavaMailSender or external service
        // throw new RuntimeException("Email send failed"); // For testing failures
        // For now, assume success
    }

    // Placeholder for actual SMS sending (integrate with Twilio or similar)
    private void sendSMS(Notification notification) {
        // Example: Use SMS API
        // throw new RuntimeException("SMS send failed"); // For testing failures
        // For now, assume success
    }

    // Scheduled retry for pending notifications
    @Scheduled(fixedRate = 60000)  // Every 60 seconds
    public void retryFailedNotifications() {
        List<Notification> pendingList = notificationRepository.findByStatus(NotificationStatus.PENDING);
        pendingList.forEach(this::processNotification);
    }

    // Map to DTO
    private NotificationResponseDTO mapToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .bookingId(notification.getBookingId())
                .userId(notification.getUserId())
                .type(notification.getType().name())
                .status(notification.getStatus().name())
                .build();
    }
}
