package com.expensetracker.repository;

import com.expensetracker.dto.CategorySummary;
import com.expensetracker.dto.MonthlyTotal;
import com.expensetracker.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Paginated list of all expenses for a user (latest)
    Page<Expense> findByUserIdOrderByDateDesc(Long userId, Pageable pageable);

    // Filter by category
    Page<Expense> findByUserIdAndCategoryOrderByDateDesc(Long userId, String category, Pageable pageable);

    // Filter by date range
    Page<Expense> findByUserIdAndDateBetweenOrderByDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Total spending by category (for chart)
    @Query("SELECT new com.expensetracker.dto.CategorySummary(e.category, SUM(e.amount)) " +
           "FROM Expense e WHERE e.user.id = :userId " +
           "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<CategorySummary> getCategorySummary(@Param("userId") Long userId);

    // Monthly totals for last N months (for bar chart)
    @Query("SELECT new com.expensetracker.dto.MonthlyTotal(YEAR(e.date), MONTH(e.date), SUM(e.amount)) " +
           "FROM Expense e WHERE e.user.id = :userId AND e.date >= :startDate " +
           "GROUP BY YEAR(e.date), MONTH(e.date) ORDER BY YEAR(e.date), MONTH(e.date)")
    List<MonthlyTotal> getMonthlyTotals(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    // Total alltime spending for a user
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId")
    BigDecimal getTotalByUserId(@Param("userId") Long userId);

    // Total spending for current month
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.user.id = :userId AND YEAR(e.date) = :year AND MONTH(e.date) = :month")
    BigDecimal getMonthlyTotalByUserId(@Param("userId") Long userId,
                                       @Param("year") int year,
                                       @Param("month") int month);
}
