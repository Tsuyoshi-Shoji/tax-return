package org.example.config;

import org.example.entity.BusinessProfile;
import org.example.features.accounting.service.AccountMappingService;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.repository.BusinessProfileRepository;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 起動時に勘定科目マスタ・様式欄対応の初期値を投入し、既存仕訳へ科目コードを付与する。
 * あわせて未確定年度の決算整理仕訳・減価償却仕訳を現行ルール（固定資産台帳・残高調整）で再生成する。
 * いずれも冪等で、登録済みのマスタ行と確定済み年度は変更しない。
 */
@Component
public class AccountMasterInitializer {
    private final AccountMappingService accountMappingService;
    private final ClosingJournalService closingJournalService;
    private final BusinessProfileRepository businessProfileRepository;

    public AccountMasterInitializer(AccountMappingService accountMappingService,
            ClosingJournalService closingJournalService, BusinessProfileRepository businessProfileRepository) {
        this.accountMappingService = accountMappingService;
        this.closingJournalService = closingJournalService;
        this.businessProfileRepository = businessProfileRepository;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initialize() {
        accountMappingService.ensureMasterData();
        accountMappingService.backfillJournalAccountCodes();
        for (BusinessProfile profile : businessProfileRepository.findAll()) {
            closingJournalService.rebuildOpenYears(profile.getId());
        }
    }
}
