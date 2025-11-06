package com.bank.controller;

import com.bank.dto.response.AccountDto;
import com.bank.service.BankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.bank.util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер для работы с банковскими счетами
 * API: /api/v1/bank/accounts
 */
@RestController
@RequestMapping("/api/v1/bank/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounts", description = "API для работы с банковскими счетами пользователя")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final BankService bankService;

    /**
     * Получение списка всех счетов пользователя
     * Опциональная фильтрация по банку через query параметр ?bankId=vbank
     */
    @Operation(
            summary = "Получить список счетов",
            description = "Возвращает список всех активных банковских счетов текущего пользователя. " +
                    "Поддерживает фильтрацию по bankId для получения счетов конкретного банка."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список счетов"),
            @ApiResponse(responseCode = "401", description = "Не авторизован", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<AccountDto>> listAccounts(
            @Parameter(description = "ID банка для фильтрации (vbank, abank, sbank)", example = "vbank")
            @RequestParam(required = false) String bankId,
            @Parameter(hidden = true) Authentication authentication) {

        log.info("GET /api/v1/bank/accounts - bankId={}", bankId);

        UUID userId = extractUserIdFromJwt(authentication);

        List<AccountDto> accounts;
        if (bankId != null && !bankId.isEmpty()) {
            accounts = bankService.getAccountsByBank(userId, bankId);
        } else {
            accounts = bankService.getAccounts(userId);
        }

        log.info("Returning {} accounts", accounts.size());
        return ResponseEntity.ok(accounts);
    }

    /**
     * Получение деталей конкретного счета
     * Проверка прав доступа: пользователь может видеть только свои счета
     */
    @Operation(
            summary = "Получить детали счета",
            description = "Возвращает подробную информацию о конкретном банковском счете. " +
                    "Проверяется право доступа: пользователь может получить только свои счета."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получены детали счета"),
            @ApiResponse(responseCode = "401", description = "Не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Счет не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDto> getAccountDetails(
            @Parameter(description = "UUID счета", required = true)
            @PathVariable UUID accountId,
            @Parameter(hidden = true) Authentication authentication) {

        log.info("GET /api/v1/bank/accounts/{}", accountId);

        UUID userId = extractUserIdFromJwt(authentication);
        AccountDto account = bankService.getAccountDetails(userId, accountId);

        return ResponseEntity.ok(account);
    }

    private UUID extractUserIdFromJwt(Authentication authentication) {
        return AuthUtil.extractUserIdFromAuthentication(authentication);
    }
}
