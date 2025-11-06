package com.bank.repository;

import com.bank.model.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для аудита операций синхронизации с Open Banking API
 * Retention период: 90 дней (автоматическая очистка через scheduled job)
 */
@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, UUID> {

    /**
     * Получение всех логов синхронизации пользователя
     * Используется индекс: idx_sync_user_time
     */
    @Query("SELECT s FROM SyncLog s WHERE s.userId = :userId " +
            "ORDER BY s.startedAt DESC")
    List<SyncLog> findByUserIdOrderByStartedAtDesc(@Param("userId") UUID userId);

    /**
     * Фильтрация по статусу синхронизации
     * Используется partial индекс: idx_sync_failed (для FAILED статусов)
     * Статусы: PENDING, SUCCESS, FAILED
     */
    @Query("SELECT s FROM SyncLog s WHERE s.userId = :userId AND s.status = :status " +
            "ORDER BY s.startedAt DESC")
    List<SyncLog> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") String status
    );

    /**
     * Поиск старых логов для cleanup процесса
     * Retention policy: 90 дней по требованиям проекта
     */
    @Query("SELECT s FROM SyncLog s WHERE s.startedAt < :before")
    List<SyncLog> findOlderThan(@Param("before") LocalDateTime before);

    /**
     * Подсчет общего количества синхронизаций после указанного времени
     * Используется для health checks
     */
    @Query("SELECT COUNT(s) FROM SyncLog s WHERE s.startedAt >= :since")
    long countRecentSyncs(@Param("since") LocalDateTime since);

    /**
     * Подсчет успешных синхронизаций после указанного времени
     * Используется для health checks
     */
    @Query("SELECT COUNT(s) FROM SyncLog s WHERE s.startedAt >= :since AND s.status = 'SUCCESS'")
    long countSuccessfulSyncs(@Param("since") LocalDateTime since);

    /**
     * Подсчет неудачных синхронизаций после указанного времени
     * Используется для health checks
     */
    @Query("SELECT COUNT(s) FROM SyncLog s WHERE s.startedAt >= :since AND s.status = 'FAILED'")
    long countFailedSyncs(@Param("since") LocalDateTime since);
}

