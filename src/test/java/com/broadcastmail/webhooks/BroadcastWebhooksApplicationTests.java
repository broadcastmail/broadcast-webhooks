package com.broadcastmail.webhooks;

import com.broadcastmail.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestContainersConfiguration.class)
class BroadcastWebhooksApplicationTests {

	@Test
	void contextLoads() {
	}

}
