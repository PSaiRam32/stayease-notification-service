package com.stayease.notification_service.repository;

import com.stayease.notification_service.entity.Notification;
import com.stayease.notification_service.entity.NotificationStatus;
import com.stayease.notification_service.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStatusAndRetryCountLessThanAndNextRetryAtBefore(NotificationStatus status, int retryCount,LocalDateTime now);
      Optional<Notification> findByBookingIdAndTypeAndEventTypeAndStatus(
            Long bookingId,
            NotificationType type,
            String eventType,
            NotificationStatus status
    );
}