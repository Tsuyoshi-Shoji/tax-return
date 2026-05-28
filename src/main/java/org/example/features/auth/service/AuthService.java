package org.example.features.auth.service;

import org.example.features.auth.dto.AuthOperationResult;
import org.example.features.auth.form.LoginForm;
import org.example.features.auth.form.UserRegisterForm;

public interface AuthService {

    AuthOperationResult login(LoginForm form);

    AuthOperationResult register(UserRegisterForm form);
}

