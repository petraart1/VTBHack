package com.bank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RadarServiceApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test: проверяем что Spring контекст поднимается без ошибок
        // Если jwt.secret не задан, тест упадет с PlaceholderResolutionException
    }
}
