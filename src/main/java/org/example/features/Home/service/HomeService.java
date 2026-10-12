package org.example.features.home.service;

import org.example.features.home.dto.HomeViewDto;

public interface HomeService {
    HomeViewDto getHomeView(Long appUserId, Long selectedFiscalYearId);
}
