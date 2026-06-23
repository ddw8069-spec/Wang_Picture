package com.yupi.yupicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cleanup")
@Data
public class CleanupProperties {

    private int deletedRetentionDays = 30;

    private int rejectedRetentionDays = 30;

    private int archiveUnmodifiedDays = 90;

    private int lockTimeoutSeconds = 300;

    private int batchSize = 100;
}
