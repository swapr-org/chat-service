package app.swapr.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    ChatServiceApplicationTest.NO_DATABASE,
    "INTERNAL_TOKEN=test-internal-token",
    "SUPABASE_URL=https://test-project.supabase.co",
    "CORS_ALLOWED_ORIGINS=https://qa.swapr.test"
})
@ActiveProfiles("qa")
class ChatServiceApplicationTest {

  static final String NO_DATABASE =
      "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration";

  @Autowired
  private HealthEndpoint health;

  @Test
  void reportsHealthyWithoutDatabaseIndicator() {
    HealthDescriptor result = health.health();

    assertThat(result.getStatus()).isEqualTo(Status.UP);
    assertThat(((CompositeHealthDescriptor) result).getComponents()).doesNotContainKey("db");
  }

  @Test
  void livenessHoldsOnlyLivenessState() {
    HealthDescriptor liveness = health.healthForPath("liveness");

    assertThat(liveness.getStatus()).isEqualTo(Status.UP);
    assertThat(((CompositeHealthDescriptor) liveness).getComponents()).containsOnlyKeys("livenessState");
  }

  @Test
  void failsToStartWhenInternalTokenIsEmpty() {
    SpringApplicationBuilder app = new SpringApplicationBuilder(ChatServiceApplication.class)
        .profiles("qa")
        .properties(
            NO_DATABASE,
            "server.port=0",
            "INTERNAL_TOKEN=",
            "SUPABASE_URL=https://test-project.supabase.co");

    assertThatThrownBy(app::run).rootCause().hasMessageContaining("INTERNAL_TOKEN");
  }
}
