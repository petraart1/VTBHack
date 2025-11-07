package com.bank.config;

import com.bank.health.VaultHealthIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Vault configuration for secret management
 * Uses Spring Cloud Vault auto-configuration
 */
@Configuration
@Profile("!test")  // Disable in tests
@Slf4j
public class VaultConfig {

    @Value("${VAULT_ENABLED:false}")
    private boolean vaultEnabled;

    @Value("${VAULT_ADDR:#{null}}")
    private String vaultAddr;

    @Value("${VAULT_TOKEN:#{null}}")
    private String vaultToken;

    @Bean
    public VaultHealthIndicator vaultHealthIndicator() {
        return new VaultHealthIndicator(vaultEnabled, vaultAddr, vaultToken);
    }
}
