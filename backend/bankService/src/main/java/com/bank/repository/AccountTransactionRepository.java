package com.bank.repository;

import com.bank.model.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с транзакциями банковских счетов
 * Оптимизирован для time-series данных с BRIN индексами
 */
@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {

    /**
     * Получение транзакций по счету и диапазону дат
     * Используется основной индекс: idx_tx_account_booking
     */
    @Query("select t from AccountTransaction t where t.accountId = :accountId " +
            "and t.bookingDateTime between :from and :to order by t.bookingDateTime desc")
    Page<AccountTransaction> findByAccountAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    /**
     * Поиск транзакций по имени мерчанта
     * Используется индекс: idx_tx_merchant (partial index where merchant_name is not null)
     */
    @Query("SELECT t FROM AccountTransaction t WHERE t.accountId = :accountId " +
            "AND t.bookingDateTime BETWEEN :from AND :to " +
            "AND LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :merchantName, '%')) " +
            "ORDER BY t.bookingDateTime DESC")
    Page<AccountTransaction> findByAccountAndDateRangeAndMerchant(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("merchantName") String merchantName,
            Pageable pageable
    );

    /**
     * Фильтрация по типу транзакции (DEBIT/CREDIT)
     * Используется индекс: idx_tx_debits (partial index for DEBIT)
     */
    @Query("SELECT t FROM AccountTransaction t WHERE t.accountId = :accountId " +
            "AND t.bookingDateTime BETWEEN :from AND :to " +
            "AND t.debitCreditIndicator = :indicator ORDER BY t.bookingDateTime DESC")
    Page<AccountTransaction> findByAccountAndDateRangeAndDebitCredit(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("indicator") String indicator,
            Pageable pageable
    );

    /**
     * Поиск по категории мерчанта (MCC - Merchant Category Code)
     * Используется для детекции подписок в radarService
     */
    @Query("SELECT t FROM AccountTransaction t WHERE t.accountId = :accountId " +
            "AND t.merchantCategoryCode = :categoryCode " +
            "AND t.bookingDateTime BETWEEN :from AND :to ORDER BY t.bookingDateTime DESC")
    Page<AccountTransaction> findByAccountAndCategoryAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("categoryCode") String categoryCode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    /**
     * Проверка существования транзакции по внешнему ID
     * Используется для предотвращения дубликатов при синхронизации
     * Используется индекс: idx_tx_external_id
     */
    @Query("SELECT t FROM AccountTransaction t WHERE t.accountId = :accountId " +
            "AND t.externalTransactionId = :externalId")
    Optional<AccountTransaction> findByAccountAndExternalId(
            @Param("accountId") UUID accountId,
            @Param("externalId") String externalId
    );
}

