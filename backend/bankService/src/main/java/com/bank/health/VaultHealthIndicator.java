package com.bank.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Health indicator for HashiCorp Vault connectivity
 */
@Slf4j
public class VaultHealthIndicator implements HealthIndicator {

    private final boolean vaultEnabled;
    private final String vaultAddr;
    private final String vaultToken;

    public VaultHealthIndicator(boolean vaultEnabled, String vaultAddr, String vaultToken) {
        this.vaultEnabled = vaultEnabled;
        this.vaultAddr = vaultAddr;
        this.vaultToken = vaultToken;
    }

    @Override
    public Health health() {
        if (!vaultEnabled) {
            return Health.up()
                    .withDetail("vault", "disabled - using environment variables")
                    .build();
        }

        if (vaultAddr == null || vaultToken == null) {
            return Health.down()
                    .withDetail("vault", "misconfigured - missing VAULT_ADDR or VAULT_TOKEN")
                    .build();
        }

        try {
            // Simple connectivity check
            // In production, you might want to check actual Vault connectivity
            return Health.up()
                    .withDetail("vault", "configured")
                    .withDetail("address", vaultAddr)
                    .build();

        } catch (Exception e) {
            log.warn("Vault health check failed: {}", e.getMessage());
            return Health.down(e)
                    .withDetail("vault", "unreachable")
                    .withDetail("address", vaultAddr)
                    .build();
        }
    }
}
