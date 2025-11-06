package com.bank.repository;

import com.bank.model.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с балансами счетов
 * Поддерживает историю изменений балансов с индексом idx_balances_recent (90 дней)
 */
@Repository
public interface BalanceRepository extends JpaRepository<AccountBalance, UUID> {

    /**
     * Получение всех балансов счета, отсортированных по дате (от новых к старым)
     * Используется индекс: idx_balances_account_time
     */
    @Query("select b from AccountBalance b WHERE b.accountId = :accountId " +
            "ORDER BY b.asOfDateTime DESC")
    List<AccountBalance> findByAccountIdOrderByAsOfDesc(@Param("accountId") UUID accountId);

    /**
     * Получение истории балансов за период
     * Используется partial индекс: idx_balances_recent (оптимизирован для последних 90 дней)
     */
    @Query("select b from AccountBalance b WHERE b.accountId = :accountId " +
            "AND b.asOfDateTime BETWEEN :from AND :to ORDER BY b.asOfDateTime DESC")
    List<AccountBalance> findByAccountAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Получение последних балансов определенного типа
     * Типы: AVAILABLE, INTERIM_BOOKED по стандарту Open Banking
     */
    @Query("select b from AccountBalance b WHERE b.accountId = :accountId " +
            "AND b.balanceType = :balanceType ORDER BY b.asOfDateTime DESC")
    List<AccountBalance> findLatestByAccountAndType(
            @Param("accountId") UUID accountId,
            @Param("balanceType") String balanceType
    );
}

