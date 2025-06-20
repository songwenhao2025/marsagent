package com.marsreg.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.marsreg.web",
    "com.marsreg.common",
    "com.marsreg.document.service",
    "com.marsreg.search.service",
    "com.marsreg.vector.service",
    "com.marsreg.inference.service"
}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.marsreg\\.search\\.exception\\..*GlobalExceptionHandler"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.marsreg\\.common\\.exception\\..*GlobalExceptionHandler"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.marsreg\\.inference\\.config\\..*MetricsConfig"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.marsreg\\.search\\.config\\..*MetricsConfig")
})
public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
} 