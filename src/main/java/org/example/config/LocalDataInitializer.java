package org.example.config;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.example.entity.AccountingTransaction;
import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.ExpenseEntry;
import org.example.entity.FiscalYear;
import org.example.entity.IncomeEntry;
import org.example.entity.UserSetting;
import org.example.repository.AccountingTransactionRepository;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.IncomeEntryRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("development & !production")
public class LocalDataInitializer {
    private final AppUserRepository appUserRepository;
    private final UserSettingRepository userSettingRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final AccountingTransactionRepository accountingTransactionRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalDataInitializer(
            AppUserRepository appUserRepository,
            UserSettingRepository userSettingRepository,
            BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository,
            IncomeEntryRepository incomeEntryRepository,
            ExpenseEntryRepository expenseEntryRepository,
            AccountingTransactionRepository accountingTransactionRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.userSettingRepository = userSettingRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.expenseEntryRepository = expenseEntryRepository;
        this.accountingTransactionRepository = accountingTransactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void initialize() {
        if (appUserRepository.existsByEmail("demo@example.com")) {
            return;
        }
        AppUser user = new AppUser();
        user.setLoginId("demo@example.com");
        user.setEmail("demo@example.com");
        user.setDisplayName("デモユーザー");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setStatus("active");
        user = appUserRepository.save(user);

        UserSetting setting = new UserSetting();
        setting.setAppUser(user);
        setting.setHomeApportionmentRate(new BigDecimal("0.00"));
        userSettingRepository.save(setting);

        BusinessProfile profile = new BusinessProfile();
        profile.setAppUser(user);
        profile.setBusinessName("デモ事業");
        profile.setFilingType("blue");
        profile = businessProfileRepository.save(profile);

        int year = LocalDate.now().getYear();
        FiscalYear fiscalYear = new FiscalYear();
        fiscalYear.setBusinessProfile(profile);
        fiscalYear.setTaxYear(year);
        fiscalYear.setPeriodStart(LocalDate.of(year, 1, 1));
        fiscalYear.setPeriodEnd(LocalDate.of(year, 12, 31));
        fiscalYear.setStatus("open");
        fiscalYear = fiscalYearRepository.save(fiscalYear);

        IncomeEntry income = new IncomeEntry();
        income.setFiscalYear(fiscalYear);
        income.setTransactionDate(LocalDate.now().minusDays(10));
        income.setAmount(new BigDecimal("180000"));
        income.setExcludeFromReturn(false);
        income.setDetails("Web制作 売上");
        incomeEntryRepository.save(income);

        ExpenseEntry expense = new ExpenseEntry();
        expense.setFiscalYear(fiscalYear);
        expense.setTransactionDate(LocalDate.now().minusDays(7));
        expense.setAmount(new BigDecimal("42000"));
        expense.setDeductibleAmount(new BigDecimal("42000"));
        expense.setExcludeFromReturn(false);
        expense.setDetails("クラウド利用料");
        expenseEntryRepository.save(expense);

        AccountingTransaction tx1 = new AccountingTransaction();
        tx1.setFiscalYear(fiscalYear);
        tx1.setTransactionDate(LocalDate.now().minusDays(10));
        tx1.setTransactionType("income");
        tx1.setSummary("Web制作 売上");
        tx1.setTotalAmount(new BigDecimal("180000"));
        tx1.setStatus("finalized");
        accountingTransactionRepository.save(tx1);

        AccountingTransaction tx2 = new AccountingTransaction();
        tx2.setFiscalYear(fiscalYear);
        tx2.setTransactionDate(LocalDate.now().minusDays(7));
        tx2.setTransactionType("expense");
        tx2.setSummary("クラウド利用料");
        tx2.setTotalAmount(new BigDecimal("42000"));
        tx2.setStatus("finalized");
        accountingTransactionRepository.save(tx2);
    }
}
