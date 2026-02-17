package org.example.service.impl;

import org.example.service.HomeService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class HomeServiceImpl implements HomeService {

    @Override
    public String getWelcomeMessage() {
        return "Welcome to Spring MVC!";
    }

    @Override
    public String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }
}