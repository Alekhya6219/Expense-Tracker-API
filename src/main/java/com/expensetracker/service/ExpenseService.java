package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Expense;
import com.expensetracker.model.User;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private ExpenseResponse toResponse(Expense expense) {
        ExpenseResponse res = new ExpenseResponse();
        res.setId(expense.getId());
        res.setTitle(expense.getTitle());
        res.setAmount(expense.getAmount());
        res.setCategory(expense.getCategory());
        res.setDate(expense.getDate());
        res.setNotes(expense.getNotes());
        res.setCreatedAt(expense.getCreatedAt());
        return res;
    }

    public Page<ExpenseResponse> getExpenses(int page, int size, String category,
                                              String startDate, String endDate) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        Page<Expense> expenses;

        if (category != null && !category.isBlank()) {
            expenses = expenseRepository.findByUserIdAndCategoryOrderByDateDesc(
                    user.getId(), category, pageable);
        } else if (startDate != null && endDate != null) {
            expenses = expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(
                    user.getId(),
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    pageable);
        } else {
            expenses = expenseRepository.findByUserIdOrderByDateDesc(user.getId(), pageable);
        }

        return expenses.map(this::toResponse);
    }

    public ExpenseResponse getExpenseById(Long id) {
        User user = getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Access denied");
        }

        return toResponse(expense);
    }

    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = getCurrentUser();

        Expense expense = Expense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .category(request.getCategory())
                .date(request.getDate())
                .notes(request.getNotes())
                .user(user)
                .build();

        return toResponse(expenseRepository.save(expense));
    }

    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        User user = getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Access denied");
        }

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDate(request.getDate());
        expense.setNotes(request.getNotes());

        return toResponse(expenseRepository.save(expense));
    }

    public void deleteExpense(Long id) {
        User user = getCurrentUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Access denied");
        }

        expenseRepository.delete(expense);
    }

    public Map<String, Object> getDashboardStats() {
        User user = getCurrentUser();
        LocalDate now = LocalDate.now();

        BigDecimal totalAllTime = expenseRepository.getTotalByUserId(user.getId());
        BigDecimal totalThisMonth = expenseRepository.getMonthlyTotalByUserId(
                user.getId(), now.getYear(), now.getMonthValue());

        List<CategorySummary> categorySummary = expenseRepository.getCategorySummary(user.getId());

        LocalDate sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1);
        List<MonthlyTotal> monthlyTotals = expenseRepository.getMonthlyTotals(user.getId(), sixMonthsAgo);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAllTime", totalAllTime);
        stats.put("totalThisMonth", totalThisMonth);
        stats.put("categorySummary", categorySummary);
        stats.put("monthlyTotals", monthlyTotals);

        return stats;
    }
}
