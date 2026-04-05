package com.finance.dto.response;

import com.finance.entity.FinancialRecord;
import com.finance.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ApiResponse {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Data
    public static class LoginResponse {
        private String token;
        private UserResponse user;
    }

    // ── User ──────────────────────────────────────────────────────────────────

    @Data
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private String role;
        private boolean active;
        private LocalDateTime createdAt;

        public static UserResponse from(User u) {
            UserResponse r = new UserResponse();
            r.id        = u.getId();
            r.name      = u.getName();
            r.email     = u.getEmail();
            r.role      = u.getRole().name();
            r.active    = u.isActive();
            r.createdAt = u.getCreatedAt();
            return r;
        }
    }

    // ── Financial Record ──────────────────────────────────────────────────────

    @Data
    public static class RecordResponse {
        private Long id;
        private BigDecimal amount;
        private String type;
        private String category;
        private LocalDate date;
        private String notes;
        private Long createdById;
        private String createdByName;
        private LocalDateTime createdAt;

        public static RecordResponse from(FinancialRecord r) {
            RecordResponse dto = new RecordResponse();
            dto.id            = r.getId();
            dto.amount        = r.getAmount();
            dto.type          = r.getType().name();
            dto.category      = r.getCategory();
            dto.date          = r.getDate();
            dto.notes         = r.getNotes();
            dto.createdById   = r.getCreatedBy().getId();
            dto.createdByName = r.getCreatedBy().getName();
            dto.createdAt     = r.getCreatedAt();
            return dto;
        }
    }

    // ── Paginated wrapper ─────────────────────────────────────────────────────

    @Data
    public static class PagedResponse<T> {
        private List<T> data;
        private long total;
        private int page;
        private int size;
        private int totalPages;

        public static <T> PagedResponse<T> of(
                List<T> data, long total, int page, int size) {
            PagedResponse<T> r = new PagedResponse<>();
            r.data       = data;
            r.total      = total;
            r.page       = page;
            r.size       = size;
            r.totalPages = (int) Math.ceil((double) total / size);
            return r;
        }
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Data
    public static class DashboardSummary {
        private OverviewStats overview;
        private List<CategoryTotal> byCategory;
        private List<RecordResponse> recentActivity;
    }

    @Data
    public static class OverviewStats {
        // Raw values (for charts/calculations)
        private BigDecimal totalIncome;
        private BigDecimal totalExpenses;
        private BigDecimal netBalance;

        // Formatted for display (e.g. "₹1,43,200.00")
        private String totalIncomeFormatted;
        private String totalExpensesFormatted;
        private String netBalanceFormatted;

        // Derived insights
        private String balanceStatus;      // "SURPLUS", "DEFICIT", or "BREAK_EVEN"
        private double savingsRatePercent; // (income - expenses) / income * 100
        private double expenseRatioPercent;// expenses / income * 100

        public static OverviewStats of(BigDecimal income, BigDecimal expenses) {
            OverviewStats s = new OverviewStats();
            s.totalIncome    = income;
            s.totalExpenses  = expenses;
            s.netBalance     = income.subtract(expenses);

            s.totalIncomeFormatted   = format(income);
            s.totalExpensesFormatted = format(expenses);
            s.netBalanceFormatted    = format(s.netBalance);

            int cmp = s.netBalance.compareTo(BigDecimal.ZERO);
            s.balanceStatus = cmp > 0 ? "SURPLUS" : cmp < 0 ? "DEFICIT" : "BREAK_EVEN";

            if (income.compareTo(BigDecimal.ZERO) > 0) {
                double inc = income.doubleValue();
                double exp = expenses.doubleValue();
                s.savingsRatePercent  = Math.round(((inc - exp) / inc * 100) * 100.0) / 100.0;
                s.expenseRatioPercent = Math.round((exp / inc * 100) * 100.0) / 100.0;
            }
            return s;
        }

        private static String format(BigDecimal value) {
            java.text.NumberFormat fmt = java.text.NumberFormat.getCurrencyInstance(
                    new java.util.Locale("en", "IN"));
            return fmt.format(value);
        }
    }

    @Data
    public static class CategoryTotal {
        private String category;
        private String type;
        private BigDecimal total;
        private String totalFormatted;
        private Long count;
        private double percentageOfType; // filled in by service
    }

    @Data
    public static class MonthlyTrend {
        private String month;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;           // income - expenses per month
        private String netStatus;         // SURPLUS / DEFICIT / BREAK_EVEN
    }

    @Data
    public static class TrendsResponse {
        private int months;
        private List<MonthlyTrend> trends;
        private BigDecimal periodTotalIncome;
        private BigDecimal periodTotalExpenses;
        private BigDecimal periodNetBalance;
    }

    // ── Error ─────────────────────────────────────────────────────────────────

    @Data
    public static class ErrorResponse {
        private String error;
        private Map<String, String> details;
        private int status;

        public static ErrorResponse of(String error, int status) {
            ErrorResponse e = new ErrorResponse();
            e.error  = error;
            e.status = status;
            return e;
        }

        public static ErrorResponse of(String error, Map<String, String> details, int status) {
            ErrorResponse e = new ErrorResponse();
            e.error   = error;
            e.details = details;
            e.status  = status;
            return e;
        }
    }
}
