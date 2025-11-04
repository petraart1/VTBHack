package com.bank.controller;

import com.bank.dto.PageDto;
import com.bank.dto.TransactionDto;
import com.bank.dto.TransactionFilterRequest;
import com.bank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import com.bank.util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bank/accounts/{accountId}/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "API для работы с банковскими транзакциями")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "Получить список транзакций",
            description = "Возвращает список транзакций по счету с поддержкой фильтрации и пагинации. " +
                    "По умолчанию возвращает транзакции за последние 6 месяцев."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список транзакций"),
            @ApiResponse(responseCode = "401", description = "Не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Счет не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<PageDto<TransactionDto>> listTransactions(
            @Parameter(description = "UUID счета", required = true)
            @PathVariable UUID accountId,
            @Parameter(description = "Дата начала периода (ISO 8601)", example = "2025-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromBookingDateTime,
            @Parameter(description = "Дата окончания периода (ISO 8601)", example = "2025-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toBookingDateTime,
            @Parameter(description = "Фильтр по имени мерчанта", example = "Netflix")
            @RequestParam(required = false) String merchantName,
            @Parameter(description = "Фильтр по категории мерчанта (MCC)", example = "5814")
            @RequestParam(required = false) String merchantCategoryCode,
            @Parameter(description = "Тип транзакции: DEBIT или CREDIT", example = "DEBIT")
            @RequestParam(required = false) String debitCreditIndicator,
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы (макс. 500)", example = "50")
            @RequestParam(defaultValue = "50") int size,
            @Parameter(hidden = true) Authentication authentication) {

        log.info("get /api/v1/bank/accounts/{}/transactions", accountId);

        UUID userId = extractUserIdFromJwt(authentication);

        TransactionFilterRequest filter = new TransactionFilterRequest(
                fromBookingDateTime,
                toBookingDateTime,
                merchantName,
                merchantCategoryCode,
                debitCreditIndicator,
                page,
                size
        );

        PageDto<TransactionDto> result = transactionService.listTransactions(userId, accountId, filter);

        log.info("returning {} transactions for account {}", result.content().size(), accountId);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Получить детали транзакции",
            description = "Возвращает подробную информацию о конкретной транзакции. " +
                    "Проверяется принадлежность транзакции к указанному счету."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получены детали транзакции"),
            @ApiResponse(responseCode = "401", description = "Не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Транзакция или счет не найдены", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDto> getTransactionDetails(
            @Parameter(description = "UUID счета", required = true)
            @PathVariable UUID accountId,
            @Parameter(description = "UUID транзакции", required = true)
            @PathVariable UUID transactionId,
            @Parameter(hidden = true) Authentication authentication) {

        log.info("get /api/v1/bank/accounts/{}/transactions/{}", accountId, transactionId);

        UUID userId = extractUserIdFromJwt(authentication);
        TransactionDto transaction = transactionService.getTransactionDetails(userId, accountId, transactionId);

        return ResponseEntity.ok(transaction);
    }

        private UUID extractUserIdFromJwt(Authentication authentication) {
        return AuthUtil.extractUserIdFromAuthentication(authentication);
    }
}

