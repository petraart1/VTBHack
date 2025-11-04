package com.bank.controller;

import com.bank.dto.BalanceDto;
import com.bank.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bank/accounts/{accountId}/balances")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Balances", description = "API для работы с балансами счетов")
@SecurityRequirement(name = "bearerAuth")
public class BalanceController {
    
    private final BalanceService balanceService;
    
    @Operation(
            summary = "Получить балансы счета",
            description = "Возвращает текущие балансы счета или историю балансов за указанный период. " +
                    "Без параметров from/to возвращает последние актуальные балансы."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получены балансы"),
            @ApiResponse(responseCode = "401", description = "Не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Счет не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<BalanceDto>> getBalances(
        @Parameter(description = "UUID счета", required = true)
        @PathVariable UUID accountId,
        @Parameter(description = "Дата начала периода для истории балансов", example = "2025-01-01T00:00:00")
        @RequestParam(required = false) LocalDateTime from,
        @Parameter(description = "Дата окончания периода для истории балансов", example = "2025-12-31T23:59:59")
        @RequestParam(required = false) LocalDateTime to,
        @Parameter(hidden = true) Authentication authentication) {
        
        log.info("GET /api/v1/bank/accounts/{}/balances - from={}, to={}", accountId, from, to);
        
        UUID userId = extractUserIdFromJwt(authentication);
        
        List<BalanceDto> balances;
        if (from != null && to != null) {
            balances = balanceService.getBalanceHistory(userId, accountId, from, to);
        } else {
            balances = balanceService.getLatestBalances(userId, accountId);
        }
        
        return ResponseEntity.ok(balances);
    }

    private UUID extractUserIdFromJwt(Authentication authentication) {
        return AuthUtil.extractUserIdFromAuthentication(authentication);
    }
}