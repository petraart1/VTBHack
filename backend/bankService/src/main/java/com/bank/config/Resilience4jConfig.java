package com.bank.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Resilience4j для устойчивости к сбоям внешних API
 * Включает:
 * - Retry: 3 попытки с экспоненциальной задержкой
 * - Circuit Breaker: предотвращение каскадных сбоев
 * 
 * Конфигурация определена в application.yaml
 */
@Configuration
@Slf4j
public class Resilience4jConfig {
    
    // Конфигурация полностью определена в application.yaml
    // Этот класс остается для будущих дополнительных настроек
}

