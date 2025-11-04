package com.bank.controller;

import com.bank.config.BankCredentials;
import com.bank.config.BankProperties;
import com.bank.dto.SyncResultDto;
import com.bank.dto.bank.AddBankRequestDto;
import com.bank.dto.bank.ConsentResponseDto;
import com.bank.service.BankSyncService;
import com.bank.service.ConsentService;
import com.bank.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.bank.util.AuthUtil;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Контроллер для управления банками и синхронизацией данных
 * API: /api/v1/bank/banks
 */
@RestController
@RequestMapping("/api/v1/bank/banks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banks", description = "API для управления банками и синхронизацией данных")
@SecurityRequirement(name = "bearerAuth")
public class BankController {

    private final BankProperties bankProperties;
    private final BankSyncService bankSyncService;
    private final TokenService tokenService;
    private final ConsentService consentService;

    /**
     * Получение списка доступных банков
     * Возвращает список банков, которые можно подключить
     */
    @Operation(
            summary = "Получить список доступных банков",
            description = "Возвращает список всех банков, которые можно подключить к приложению. " +
                    "Для каждого банка указывается ID и название."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получен список банков"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<BankInfo>> getAvailableBanks() {
        log.info("GET /api/v1/bank/banks - fetching available banks");
        
        List<BankInfo> banks = bankProperties.getConfigs().entrySet().stream()
                .map(entry -> new BankInfo(entry.getKey(), entry.getValue().getName()))
                .collect(Collectors.toList());
        
        log.info("returning {} available banks", banks.size());
        return ResponseEntity.ok(banks);
    }

        /**
     * Добавление нового банка (только получение токена)
     * Пользователь вводит логин и пароль для доступа к банку
     * Синхронизацию данных можно выполнить позже через POST /banks/{bankId}/sync
     */
    @Operation(
            summary = "Добавить банк",
            description = "Добавляет новый банк для текущего пользователя. Проверяет валидность учетных данных, " +
                    "получает токен доступа. Синхронизацию данных (счета, транзакции, балансы) можно выполнить " +
                    "позже через отдельный endpoint POST /banks/{bankId}/sync. " +
                    "Требуется ввести логин и пароль для доступа к банку."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Банк успешно добавлен, токен получен"),                                                
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса", content = @Content),                                                 
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные", content = @Content),                                                             
            @ApiResponse(responseCode = "503", description = "Банковский API недоступен", content = @Content)                                                   
    })
    @PostMapping
    public ResponseEntity<AddBankResponseDto> addBank(
            @Valid @RequestBody AddBankRequestDto request,
            @Parameter(hidden = true) Authentication authentication) {
        
        log.info("POST /api/v1/bank/banks - adding bank: {}", request.bankId());
        
        UUID userId = extractUserIdFromJwt(authentication);
        
        // Проверяем, что банк существует в конфигурации
        if (!bankProperties.getConfigs().containsKey(request.bankId())) {
            throw new IllegalArgumentException("Unknown bank: " + request.bankId());                                                                            
        }
        
        // Создаем credentials (НЕ сохраняем в базе)
        // clientId опционален для addBank, используется дефолтное значение если не передан
        BankCredentials credentials = new BankCredentials(
                userId,
                request.bankId(),
                request.username(),
                request.password(),
                request.clientId()  // может быть null
        );
        
        // Только получаем токен (валидация учетных данных)
        // Если токен не удалось получить - будет выброшено исключение
        String accessToken = tokenService.getAccessToken(userId, credentials);
        
        log.info("bank added successfully: bank={}, token obtained", request.bankId());
        
        AddBankResponseDto response = new AddBankResponseDto(
                request.bankId(),
                bankProperties.getConfigs().get(request.bankId()).getName(),
                true,
                "Банк успешно добавлен. Токен получен. Для синхронизации данных используйте POST /banks/{bankId}/sync"
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Ручная синхронизация данных для конкретного банка
     * Используется для обновления данных по требованию пользователя
     */
    @Operation(
            summary = "Синхронизировать данные банка",
            description = "Выполняет полную синхронизацию данных для указанного банка: " +
                    "счета, транзакции, балансы. Требуется повторный ввод логина и пароля."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные успешно синхронизированы"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса", content = @Content),
            @ApiResponse(responseCode = "401", description = "Не авторизован", content = @Content),
            @ApiResponse(responseCode = "503", description = "Банковский API недоступен", content = @Content)
    })
    @PostMapping("/{bankId}/sync")
    public ResponseEntity<SyncResultDto> syncBank(
            @Parameter(description = "ID банка (vbank, abank, sbank)", required = true)
            @PathVariable String bankId,
            @Valid @RequestBody BankSyncRequestDto request,
            @Parameter(hidden = true) Authentication authentication) {
        
        log.info("POST /api/v1/bank/banks/{}/sync - syncing bank", bankId);
        
        UUID userId = extractUserIdFromJwt(authentication);
        
        // Проверяем, что банк существует
        if (!bankProperties.getConfigs().containsKey(bankId)) {
            throw new IllegalArgumentException("Unknown bank: " + bankId);
        }
        
        // clientId опционален для sync, используется дефолтное значение если не передан
        BankCredentials credentials = new BankCredentials(
                userId,
                bankId,
                request.username(),
                request.password(),
                request.clientId()  // может быть null
        );
        
        BankSyncService.SyncResult result = bankSyncService.syncAll(userId, credentials);
        
        SyncResultDto response = new SyncResultDto(
                result.success,
                result.accountsSynced,
                result.transactionsSynced,
                result.balancesSynced,
                result.durationMs,
                result.errorMessage
        );
        
        log.info("bank synced: bank={}, accounts={}, transactions={}, balances={}, duration={}ms",
                bankId, result.accountsSynced, result.transactionsSynced,
                result.balancesSynced, result.durationMs);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Создание согласия на доступ к счетам для конкретного банка
     * OpenBanking Russia: POST /account-consents/request
     */
            @Operation(
            summary = "Создать согласие на доступ к счетам",
            description = "Создает согласие на доступ к счетам клиента в указанном банке. " +
                    "Согласие необходимо для межбанковых запросов данных. " +
                    "Требуется ввести логин, пароль и clientId (предопределенный: team210-1 до team210-10) для доступа к банку. " +
                    "Для VBank и ABank согласие одобряется автоматически, для SBank требуется ручное подтверждение клиентом."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Согласие успешно создано"),
            @ApiResponse(responseCode = "400", description = "Неверные параметры запроса", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные", content = @Content),
            @ApiResponse(responseCode = "403", description = "Согласие не одобрено банком", content = @Content),
            @ApiResponse(responseCode = "503", description = "Банковский API недоступен", content = @Content)
    })
    @PostMapping("/{bankId}/account-consents/request")
    public ResponseEntity<ConsentResponseDto> createConsent(
            @Parameter(description = "ID банка (vbank, abank, sbank)", required = true)
            @PathVariable String bankId,
            @Valid @RequestBody BankSyncRequestDto request,
            @Parameter(hidden = true) Authentication authentication) {

        log.info("POST /api/v1/bank/banks/{}/account-consents/request - creating consent", bankId);

        UUID userId = extractUserIdFromJwt(authentication);

        // Проверяем, что банк существует
        if (!bankProperties.getConfigs().containsKey(bankId)) {
            throw new IllegalArgumentException("Unknown bank: " + bankId);
        }

        // Проверяем, что clientId передан
        if (request.clientId() == null || request.clientId().isBlank()) {
            throw new IllegalArgumentException("clientId is required. Must be in format: team210-1 to team210-10");
        }
        
        // Создаем credentials с clientId
        BankCredentials credentials = new BankCredentials(
                userId,
                bankId,
                request.username(),
                request.password(),
                request.clientId()
        );
        
        // Создаем согласие
        ConsentResponseDto response = consentService.createConsentPublic(userId, credentials, request.clientId());

        log.info("consent created successfully: bank={}, consent_id={}, status={}, auto_approved={}",
                bankId, response.consentId(), response.status(), response.autoApproved());

        return ResponseEntity.ok(response);
    }

    private UUID extractUserIdFromJwt(Authentication authentication) {
        return AuthUtil.extractUserIdFromAuthentication(authentication);
    }

    /**
     * DTO для информации о банке
     */
    public record BankInfo(String id, String name) {
    }

    /**
     * DTO для запроса синхронизации
     */
    public record BankSyncRequestDto(
            String username, 
            String password,
            String clientId  // client_id из предопределенного списка (team210-1 до team210-10)
    ) {
    }

    /**
     * DTO для ответа при добавлении банка
     */
    public record AddBankResponseDto(
            String bankId,
            String bankName,
            boolean success,
            String message
    ) {
    }
}
