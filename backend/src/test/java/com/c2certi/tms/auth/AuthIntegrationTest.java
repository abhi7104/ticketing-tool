package com.c2certi.tms.auth;

import static com.c2certi.tms.support.ApiClient.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.support.TestSecrets;
import com.c2certi.tms.user.domain.User;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends IntegrationTest {

  private MvcResult login(String username, String password) throws Exception {
    return mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.toJson(Map.of("username", username, "password", password))))
        .andReturn();
  }

  @Test
  void loginSetsHttpOnlySessionCookieAndReturnsUser() throws Exception {
    users.create("alice", "Alice Moore");

    MvcResult result = login("Alice", TestSecrets.USER_PASSWORD);

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    String setCookie = result.getResponse().getHeader("Set-Cookie");
    assertThat(setCookie)
        .contains("TMS_SESSION=")
        .contains("HttpOnly")
        .contains("SameSite=Strict")
        .contains("Path=/");
    assertThat(result.getResponse().getContentAsString())
        .contains("\"username\":\"alice\"")
        .contains("\"displayName\":\"Alice Moore\"")
        .doesNotContain("password");

    Cookie session = result.getResponse().getCookie("TMS_SESSION");
    mvc.perform(get("/api/v1/auth/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("alice"));
  }

  @Test
  void wrongPasswordAndUnknownUserGiveIdenticalGenericError() throws Exception {
    users.create("alice");

    MvcResult wrongPassword = login("alice", "not-the-password");
    MvcResult unknownUser = login("nobody", "whatever");

    for (MvcResult r : new MvcResult[] {wrongPassword, unknownUser}) {
      assertThat(r.getResponse().getStatus()).isEqualTo(401);
      assertThat(r.getResponse().getContentType()).isEqualTo("application/problem+json");
      assertThat(r.getResponse().getContentAsString())
          .contains("\"code\":\"INVALID_CREDENTIALS\"")
          .contains("Invalid username or password.");
      assertThat(r.getResponse().getHeader("Set-Cookie")).isNull();
    }
  }

  @Test
  void deactivatedUserCannotSignIn() throws Exception {
    users.deactivated("gone");
    assertThat(login("gone", TestSecrets.USER_PASSWORD).getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void loginValidatesInput() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"  \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors", hasSize(2)));
  }

  @Test
  void loginWithoutCsrfTokenIsForbidden() throws Exception {
    users.create("alice");
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    api.toJson(Map.of("username", "alice", "password", TestSecrets.USER_PASSWORD))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void firstResponseCarriesCsrfCookie() throws Exception {
    mvc.perform(get("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Set-Cookie", containsString("XSRF-TOKEN=")));
  }

  @Test
  void protectedEndpointsRequireSession() throws Exception {
    for (String url : new String[] {"/api/v1/tickets", "/api/v1/users", "/api/v1/auth/me"}) {
      mvc.perform(get(url))
          .andExpect(status().isUnauthorized())
          .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
          .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
  }

  @Test
  void tamperedOrForeignTokenIsRejected() throws Exception {
    mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("TMS_SESSION", "abc.def.ghi")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void tokenOfDeactivatedUserIsRejected() throws Exception {
    User user = users.create("temp");
    Cookie session = api.sessionFor(user);
    jdbc.update("UPDATE app_user SET active = false WHERE id = ?", user.getId());
    mvc.perform(get("/api/v1/auth/me").cookie(session))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void logoutClearsCookie() throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/logout").with(csrf())).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(204);
    assertThat(result.getResponse().getHeader("Set-Cookie"))
        .contains("TMS_SESSION=")
        .contains("Max-Age=0");
  }

  @Test
  void usersEndpointListsActiveUsersOnly() throws Exception {
    User alice = users.create("alice", "Alice Moore");
    users.create("bob", "Bob Singh");
    users.deactivated("gone");

    mvc.perform(api.get(alice, "/api/v1/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].displayName").value("Alice Moore"))
        .andExpect(jsonPath("$[1].displayName").value("Bob Singh"))
        .andExpect(jsonPath("$[0].password").doesNotExist());
  }
}
