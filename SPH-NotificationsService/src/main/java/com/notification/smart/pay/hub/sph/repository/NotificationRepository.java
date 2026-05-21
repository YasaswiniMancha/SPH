package com.notification.smart.pay.hub.sph.repository;

import com.notification.smart.pay.hub.sph.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    @Query("SELECT n FROM NotificationEntity n WHERE n.isActive = true ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findAllActiveNotifications(Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.recipientId = ?1 AND n.isActive = true ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findByRecipientId(String recipientId, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = ?1 AND n.isActive = true")
    List<NotificationEntity> findByStatus(NotificationEntity.NotificationStatus status);

    @Query("SELECT n FROM NotificationEntity n WHERE n.status = ?1 AND n.retryCount < n.maxRetries AND n.isActive = true")
    List<NotificationEntity> findRetryableNotifications(NotificationEntity.NotificationStatus status);

    @Query("SELECT n FROM NotificationEntity n WHERE n.createdAt BETWEEN ?1 AND ?2 AND n.isActive = true")
    List<NotificationEntity> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.status = 'SENT' AND n.isActive = true")
    long countSuccessfulNotifications();

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.status = 'FAILED' AND n.isActive = true")
    long countFailedNotifications();
}