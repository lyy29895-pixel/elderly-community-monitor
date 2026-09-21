package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 独居老人智能监护系统后端服务启动入口。
 * <p>启用 Spring Boot 自动装配、MyBatis-Plus Mapper 扫描与定时任务支持。</p>
 *
 * @author 甲
 */
@SpringBootApplication(scanBasePackages = "com.example.demo")
@MapperScan("com.example.demo.mapper")
@EnableScheduling
public class DemoApplication {
    /**
     * 应用程序主入口，启动内嵌 Web 容器并初始化 Spring 上下文。
     *
     * @param args 启动时传入的命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
