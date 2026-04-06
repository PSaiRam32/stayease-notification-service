package com.stayease.notification_service.repository;

import com.stayease.notification_service.entity.Notification;
import com.stayease.notification_service.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStatus(NotificationStatus status);
}