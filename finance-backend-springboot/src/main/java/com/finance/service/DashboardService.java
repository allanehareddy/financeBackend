package com.finance.service;

import com.finance.dto.response.ApiResponse.*;
import com.finance.entity.TransactionType;
import com.finance.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository recordRepository;

    public DashboardSummary getSummary() {
        BigDecimal totalIncome   = recordRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumByType(TransactionType.EXPENSE);

        // ── Overview with formatting + insights ───────────────────────────────
        OverviewStats overview = OverviewStats.of(totalIncome, totalExpenses);

        // ── Category totals with percentage of their type ─────────────────────
        List<Object[]> rawCategory = recordRepository.categoryTotals();

        Map<String, BigDecimal> typeSums = rawCategory.stream()
                .collect(Collectors.groupingBy(
                        row -> row[1].toString(),
                        Collectors.reducing(BigDecimal.ZERO,
                                row -> (BigDecimal) row[2],
                                BigDecimal::add)
                ));

        NumberFormat currencyFmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        List<CategoryTotal> byCategory = rawCategory.stream().map(row -> {
            CategoryTotal ct = new CategoryTotal();
            ct.setCategory((String) row[0]);
            ct.setType(row[1].toString());
            ct.setTotal((BigDecimal) row[2]);
            ct.setTotalFormatted(currencyFmt.format(row[2]));
            ct.setCount((Long) row[3]);

            BigDecimal typeSum = typeSums.getOrDefault(row[1].toString(), BigDecimal.ONE);
            if (typeSum.compareTo(BigDecimal.ZERO) > 0) {
                double pct = ((BigDecimal) row[2]).doubleValue() / typeSum.doubleValue() * 100;
                ct.setPercentageOfType(Math.round(pct * 100.0) / 100.0);
            }
            return ct;
        }).toList();

        // ── Recent activity ───────────────────────────────────────────────────
        List<RecordResponse> recent = recordRepository
                .findRecentActivity(PageRequest.of(0, 10))
                .stream().map(RecordResponse::from).toList();

        DashboardSummary summary = new DashboardSummary();
        summary.setOverview(overview);
        summary.setByCategory(byCategory);
        summary.setRecentActivity(recent);
        return summary;
    }

    public TrendsResponse getTrends(int months) {
        if (months < 1 || months > 24) months = 6;
        LocalDate since = LocalDate.now().minusMonths(months);

        List<MonthlyTrend> trends = recordRepository.monthlyTrends(since).stream()
                .map(row -> {
                    BigDecimal income   = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    BigDecimal expenses = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    BigDecimal net      = income.subtract(expenses);

                    MonthlyTrend t = new MonthlyTrend();
                    t.setMonth((String) row[0]);
                    t.setIncome(income);
                    t.setExpenses(expenses);
                    t.setNet(net);
                    t.setNetStatus(net.compareTo(BigDecimal.ZERO) > 0 ? "SURPLUS"
                            : net.compareTo(BigDecimal.ZERO) < 0 ? "DEFICIT" : "BREAK_EVEN");
                    return t;
                }).toList();

        BigDecimal periodIncome   = trends.stream().map(MonthlyTrend::getIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal periodExpenses = trends.stream().map(MonthlyTrend::getExpenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TrendsResponse response = new TrendsResponse();
        response.setMonths(months);
        response.setTrends(trends);
        response.setPeriodTotalIncome(periodIncome);
        response.setPeriodTotalExpenses(periodExpenses);
        response.setPeriodNetBalance(periodIncome.subtract(periodExpenses));
        return response;
    }
}
