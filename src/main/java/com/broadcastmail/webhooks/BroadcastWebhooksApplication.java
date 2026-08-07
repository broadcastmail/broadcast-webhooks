package com.broadcastmail.webhooks;

import com.broadcastmail.webhooks.config.EncryptionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties(EncryptionProperties.class)
@EnableJpaRepositories(basePackages = {"com.broadcastmail.webhooks", "com.broadcastmail.common"})
@EntityScan(basePackages = {"com.broadcastmail.webhooks", "com.broadcastmail.common"})
public class BroadcastWebhooksApplication {

	public static void main(String[] args) {
		SpringApplication.run(BroadcastWebhooksApplication.class, args);
	}

}
