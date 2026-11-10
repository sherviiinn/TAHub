package ir.TAHub.TAHub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** The whole app starts (all beans, all tables) on an empty database. */
@SpringBootTest(properties = {"tahub.demo-data=false", "tahub.storage.dir=target/test-files"})
@Import(TestcontainersConfiguration.class)
class TaHubApplicationTests {

	@Test
	void contextLoads() {
	}

}
