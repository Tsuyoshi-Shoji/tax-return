package org.example.repository;

import org.example.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdAndDeletedFlagFalseOrderByCreatedAtDesc(Long userId);

    List<Expense> findByUserIdAndDeletedFlagFalseOrderByExpenseDateDescCreatedAtDesc(Long userId);

    Optional<Expense> findByIdAndUserIdAndDeletedFlagFalse(Long id, Long userId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.user.id = :userId and e.deletedFlag = false and e.expenseDate between :fromDate and :toDate")
    BigDecimal sumAmountByUserIdAndPeriod(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Modifying
    @Query("delete from Expense e where e.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}

