package org.example.integration;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.config.LocalDataInitializer;
import org.example.config.WebConfig;
import org.example.controller.AuthController;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.example.features.auth.dto.AuthOperationResult;
import org.example.features.auth.form.LoginForm;
import org.example.features.auth.form.UserRegisterForm;
import org.example.features.auth.service.AuthService;
import org.example.features.common.session.FormCsrfTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.ui.ExtendedModelMap;

public final class AuthSecurityHarness {
    public static void main(String[] args) throws Exception {
        verifyDemoProfiles();
        verifyPasswordResolution();
        verifyRejectedTokens();
        verifyCredentialRetry();
        verifySuccessfulLogin(false);
        verifySuccessfulLogin(true);
        System.out.println("Auth security harness passed");
    }

    private static void verifyDemoProfiles() {
        Profiles condition = Profiles.of(LocalDataInitializer.class.getAnnotation(Profile.class).value());
        StandardEnvironment environment = new StandardEnvironment();
        environment.setDefaultProfiles("default");
        require(!environment.acceptsProfiles(condition), "demo enabled without explicit development profile");
        environment.setActiveProfiles("production");
        require(!environment.acceptsProfiles(condition), "demo enabled in production");
        environment.setActiveProfiles("development");
        require(environment.acceptsProfiles(condition), "development demo disabled");
        environment.setActiveProfiles("development", "production");
        require(!environment.acceptsProfiles(condition), "development bypasses production restriction");
    }

    private static void verifyPasswordResolution() throws Exception {
        String expression = WebConfig.class.getDeclaredField("datasourcePassword")
                .getAnnotation(Value.class).value();
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        Map<String, Object> properties = new HashMap<>();
        properties.put("app.datasource.password", "synthetic-local-password");
        environment.getPropertySources().addFirst(new MapPropertySource("synthetic", properties));
        require("synthetic-local-password".equals(environment.resolveRequiredPlaceholders(expression)),
                "local password fallback changed");
        properties.put("DB_PASS", "synthetic-environment-password");
        require("synthetic-environment-password".equals(environment.resolveRequiredPlaceholders(expression)),
                "environment password not used");
    }

    private static void verifyRejectedTokens() {
        for (String token : new String[] {null, "", "invalid"}) {
            Fixture f = new Fixture();
            f.showLogin();
            require("Auth/login".equals(f.login(token)), "invalid token did not render error");
            require(f.status == 403 && f.service.calls == 0, "invalid token reached authentication");
            require(f.model.containsAttribute("failureMessage"), "missing CSRF failure message");
            require(f.model.get("loginCsrfToken") instanceof String, "missing retry token");
        }
        Fixture missingSession = new Fixture();
        missingSession.login("invalid");
        require(missingSession.status == 403 && missingSession.service.calls == 0,
                "missing session reached authentication");

        Fixture first = new Fixture();
        Fixture other = new Fixture();
        String firstToken = first.showLogin();
        other.showLogin();
        other.login(firstToken);
        require(other.status == 403 && other.service.calls == 0, "token crossed session boundary");
        String settingsToken = other.tokens.issue(other.state.session, "settings");
        other.login(settingsToken);
        require(other.status == 403 && other.service.calls == 0, "token crossed scope boundary");
    }

    private static void verifyCredentialRetry() {
        Fixture f = new Fixture();
        String token = f.showLogin();
        f.service.result = AuthOperationResult.unauthorized("Login failed");
        require("Auth/login".equals(f.login(token)), "credential failure did not render login");
        require(f.status == 401 && f.service.calls == 1, "credential failure status changed");
        require(token.equals(f.model.get("loginCsrfToken")), "retry token lost");
        f.service.result = AuthOperationResult.success(new AuthUserContext(1L, "Synthetic", "test@example.invalid"), "");
        require("redirect:/home".equals(f.login(token)), "valid retry failed");
    }

    private static void verifySuccessfulLogin(boolean rememberMe) {
        Fixture f = new Fixture();
        String token = f.showLogin();
        SessionState old = f.state;
        f.form.setRememberMe(rememberMe);
        require("redirect:/home".equals(f.login(token)), "valid login failed");
        require(old.invalidated && old != f.state, "session was not rotated");
        require(f.state.attributes.get(AuthSessionAttributes.CURRENT_USER) instanceof AuthUserContext,
                "authenticated user missing");
        require(f.state.timeout == (rememberMe ? 60 * 60 * 24 * 14 : 60 * 30), "session timeout changed");
        require(!f.tokens.isValid(f.state.session, "login", token), "old CSRF token survived login");
        require("redirect:/home".equals(f.controller.showLogin(f.model, f.request, "")),
                "authenticated GET no longer redirects");
    }

    private static final class FakeAuthService implements AuthService {
        private int calls;
        private AuthOperationResult result = AuthOperationResult.success(
                new AuthUserContext(1L, "Synthetic", "test@example.invalid"), "");

        @Override
        public AuthOperationResult login(LoginForm form) {
            calls++;
            return result;
        }

        @Override
        public AuthOperationResult register(UserRegisterForm form) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class Fixture {
        private final FakeAuthService service = new FakeAuthService();
        private final FormCsrfTokenManager tokens = new FormCsrfTokenManager();
        private final AuthController controller = new AuthController(service, tokens);
        private final LoginForm form = new LoginForm();
        private final ExtendedModelMap model = new ExtendedModelMap();
        private SessionState state;
        private int status = 200;
        private final HttpServletRequest request = proxy(HttpServletRequest.class, (object, method, args) -> {
            if ("getSession".equals(method.getName())) {
                boolean create = args == null || args.length == 0 || Boolean.TRUE.equals(args[0]);
                if ((state == null || state.invalidated) && create) {
                    state = new SessionState();
                }
                return state == null || state.invalidated ? null : state.session;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        private final HttpServletResponse response = proxy(HttpServletResponse.class, (object, method, args) -> {
            if ("setStatus".equals(method.getName())) {
                status = (Integer) args[0];
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });

        private String showLogin() {
            require("Auth/login".equals(controller.showLogin(model, request, "")), "GET login failed");
            return (String) model.get("loginCsrfToken");
        }

        private String login(String token) {
            model.clear();
            status = 200;
            return controller.login(form, token, model, request, response);
        }
    }

    private static final class SessionState {
        private final Map<String, Object> attributes = new HashMap<>();
        private boolean invalidated;
        private int timeout;
        private final HttpSession session = proxy(HttpSession.class, (object, method, args) -> {
            return switch (method.getName()) {
                case "getAttribute" -> attributes.get((String) args[0]);
                case "setAttribute" -> {
                    attributes.put((String) args[0], args[1]);
                    yield null;
                }
                case "invalidate" -> {
                    invalidated = true;
                    attributes.clear();
                    yield null;
                }
                case "setMaxInactiveInterval" -> {
                    timeout = (Integer) args[0];
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
