package com.stayease.notification_service.service;

import com.stayease.notification_service.dto.NotificationRequestDTO;
import com.stayease.notification_service.dto.NotificationResponseDTO;
import com.stayease.notification_service.entity.Notification;
import com.stayease.notification_service.entity.NotificationStatus;
import com.stayease.notification_service.entity.NotificationType;
import com.stayease.notification_service.exception.BusinessException;
import com.stayease.notification_service.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Value("${spring.mail.username}")
    private String fromEmail;
    private static final int MAX_RETRY = 3;
    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final Executor asyncExecutor;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            JavaMailSender mailSender,
            @Qualifier("notificationExecutor") Executor asyncExecutor
    ) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public NotificationResponseDTO sendNotification(NotificationRequestDTO request) {
        validateRequest(request);
        NotificationType type = parseType(request.getType());
        validateChannelSpecificFields(request, type);
        Optional<Notification> existing = notificationRepository
                .findByBookingIdAndTypeAndEventTypeAndStatus(
                        request.getBookingId(),
                        type,
                        request.getStatus(),
                        NotificationStatus.SENT
                );
        if (existing.isPresent()) {
            log.warn("Duplicate notification skipped for bookingId={}", request.getBookingId());
            return mapToDTO(existing.get());
        }
        Notification notification = createNotification(request, type);
        asyncExecutor.execute(() -> processNotification(notification.getId()));

        return mapToDTO(notification);
    }
    private void validateRequest(NotificationRequestDTO request) {
        if (request.getBookingId() == null) {
            throw new BusinessException("Booking ID is required");
        }
        if (request.getUserId() == null) {
            throw new BusinessException("User ID is required");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException("Message cannot be empty");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new BusinessException("Notification type is required");
        }
    }

    private NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException("Invalid notification type. Allowed: EMAIL, SMS");
        }
    }

    private void validateChannelSpecificFields(NotificationRequestDTO request, NotificationType type) {

        if (type == NotificationType.EMAIL &&
                (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new BusinessException("Email is required for EMAIL notification");
        }
    }

    private Notification createNotification(NotificationRequestDTO request, NotificationType type) {
        Notification notification = Notification.builder()
                .bookingId(request.getBookingId())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .userId(String.valueOf(request.getUserId()))
                .type(type)
                .message(request.getMessage())
                .eventType(request.getStatus())
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void processNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow();
        try {
            log.info("Processing notification ID={}, bookingId={}, userId={}, eventType={}",
                    notification.getId(),
                    notification.getBookingId(),
                    notification.getUserId(),
                    notification.getEventType());
            notification.setStatus(NotificationStatus.PROCESSING);
            notificationRepository.save(notification);
            if (notification.getType() == NotificationType.EMAIL) {
                processEmail(notification);
            }
            notification.setStatus(NotificationStatus.SENT);
        }catch (Exception ex) {
        log.error("Notification failed ID={}, bookingId={}, eventType={}, retryCount={}",
                notification.getId(),
                notification.getBookingId(),
                notification.getEventType(),
                notification.getRetryCount(),
                ex);
        int retryCount = notification.getRetryCount() + 1;
        notification.setRetryCount(retryCount);
        if (retryCount >= MAX_RETRY) {
            notification.setStatus(NotificationStatus.FAILED);
        } else {
            notification.setStatus(NotificationStatus.PENDING);
            long delaySeconds = (long) Math.pow(2, retryCount) * 30;
            notification.setNextRetryAt(
                    LocalDateTime.now().plusSeconds(delaySeconds)
            );
            log.warn("Retry scheduled for notificationId={} after {} seconds",
                    notification.getId(), delaySeconds);
        }
    }
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private void processEmail(Notification notification) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromEmail);
        mail.setTo(notification.getEmail());
        mail.setSubject(buildSubject(notification));
        mail.setText(notification.getMessage());
        mailSender.send(mail);
        log.info("Email sent successfully for bookingId={}, eventType={}",
                notification.getBookingId(),
                notification.getEventType());
    }

    private String buildSubject(Notification notification) {
        String event = notification.getEventType();
        if (event == null) {
            return "StayEase Notification";
        }
        switch (event) {
            case "CONFIRMED":
                return "Your Booking is Confirmed - StayEase";
            case "CANCELLED":
                return "Your Booking has been Cancelled - StayEase";
            case "FAILED":
                return "Payment Failed for Your Booking - StayEase";
            default:
                return "StayEase Booking Update";
        }
    }



//    S6Q5BEGMPKV84JFVN31YPH6F
    @Scheduled(fixedRate = 60000) // every 60 sec
    public void retryFailedNotifications() {
        List<Notification> pendingList =
                notificationRepository
                        .findByStatusAndRetryCountLessThanAndNextRetryAtBefore(
                                NotificationStatus.PENDING,
                                MAX_RETRY,
                                LocalDateTime.now()
                        );
        log.info("Retry scheduler triggered. Eligible notifications count={}", pendingList.size());
        pendingList.forEach(n ->
                asyncExecutor.execute(() -> processNotification(n.getId()))
        );
    }

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