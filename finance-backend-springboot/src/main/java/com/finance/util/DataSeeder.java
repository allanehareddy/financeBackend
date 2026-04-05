package com.finance.util;

import com.finance.entity.FinancialRecord;
import com.finance.entity.Role;
import com.finance.entity.TransactionType;
import com.finance.entity.User;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FinancialRecordRepository recordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return; // Skip if already seeded

        log.info("Seeding initial data...");

        // ── Users ─────────────────────────────────────────────────────────────
        User admin = userRepository.save(User.builder()
                .name("Super Admin")
                .email("admin@finance.com")
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .active(true)
                .build());

        User analyst = userRepository.save(User.builder()
                .name("Anna Analyst")
                .email("analyst@finance.com")
                .password(passwordEncoder.encode("Analyst@123"))
                .role(Role.ANALYST)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .name("Victor Viewer")
                .email("viewer@finance.com")
                .password(passwordEncoder.encode("Viewer@123"))
                .role(Role.VIEWER)
                .active(true)
                .build());

        log.info("Seeded users: admin@finance.com / Admin@123");
        log.info("             analyst@finance.com / Analyst@123");
        log.info("             viewer@finance.com  / Viewer@123");

        // ── Financial Records ─────────────────────────────────────────────────
        List<FinancialRecord> records = List.of(
            record(50000, TransactionType.INCOME,  "Salary",     "2025-10-01", "Monthly salary",       admin),
            record(15000, TransactionType.INCOME,  "Freelance",  "2025-10-05", "Web project",          analyst),
            record(12000, TransactionType.EXPENSE, "Rent",       "2025-10-02", "October rent",         admin),
            record(3500,  TransactionType.EXPENSE, "Groceries",  "2025-10-10", "Weekly groceries",     analyst),
            record(1200,  TransactionType.EXPENSE, "Utilities",  "2025-10-12", "Electricity bill",     admin),
            record(50000, TransactionType.INCOME,  "Salary",     "2025-11-01", "Monthly salary",       admin),
            record(8000,  TransactionType.INCOME,  "Freelance",  "2025-11-15", "Logo design",          analyst),
            record(12000, TransactionType.EXPENSE, "Rent",       "2025-11-02", "November rent",        admin),
            record(4200,  TransactionType.EXPENSE, "Groceries",  "2025-11-08", "Weekly groceries",     analyst),
            record(5000,  TransactionType.EXPENSE, "Travel",     "2025-11-20", "Business trip",        admin),
            record(50000, TransactionType.INCOME,  "Salary",     "2025-12-01", "Monthly salary",       admin),
            record(2500,  TransactionType.EXPENSE, "Healthcare", "2025-12-05", "Doctor visit",         analyst),
            record(12000, TransactionType.EXPENSE, "Rent",       "2025-12-02", "December rent",        admin),
            record(3800,  TransactionType.EXPENSE, "Groceries",  "2025-12-15", "Monthly groceries",    analyst),
            record(20000, TransactionType.INCOME,  "Freelance",  "2025-12-20", "Annual contract",      admin)
        );

        recordRepository.saveAll(records);
        log.info("Seeded {} financial records.", records.size());
    }

    private FinancialRecord record(double amount, TransactionType type,
                                   String category, String date,
                                   String notes, User user) {
        return FinancialRecord.builder()
                .amount(BigDecimal.valueOf(amount))
                .type(type)
                .category(category)
                .date(LocalDate.parse(date))
                .notes(notes)
                .createdBy(user)
                .deleted(false)
                .build();
    }
}
