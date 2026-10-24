package pl.rynek.identity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class IdentityServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
