package org.example.repository;

import org.example.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByUserIdAndDeletedFlagFalseOrderByCreatedAtDesc(Long userId);

    List<Income> findByUserIdAndDeletedFlagFalseOrderByIncomeDateDescCreatedAtDesc(Long userId);

    Optional<Income> findByIdAndUserIdAndDeletedFlagFalse(Long id, Long userId);

    @Query("select coalesce(sum(i.amount), 0) from Income i where i.user.id = :userId and i.deletedFlag = false and (i.businessTartget is null or i.businessTartget = true) and i.incomeDate between :fromDate and :toDate")
    BigDecimal sumAmountByUserIdAndPeriod(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("select coalesce(sum(i.amount), 0) from Income i where i.user.id = :userId and i.deletedFlag = false and i.incomeDate between :fromDate and :toDate")
    BigDecimal sumAmountByUserIdAndPeriodIncludingAllData(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Modifying
    @Query("delete from Income i where i.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}

