package com.marsreg.common.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            // 检查数据库连接
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // 检查用户表
            Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
            
            return Health.up()
                    .withDetail("database", "connected")
                    .withDetail("users", userCount)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
} 