package com.finance.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecordRequest {

    @Data
    public static class Create {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;

        @NotBlank(message = "Type is required (INCOME or EXPENSE)")
        private String type;

        @NotBlank(message = "Category is required")
        private String category;

        @NotNull(message = "Date is required")
        private LocalDate date;

        @Size(max = 500)
        private String notes;
    }

    @Data
    public static class Update {
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;

        private String type;

        @Size(min = 1)
        private String category;

        private LocalDate date;

        @Size(max = 500)
        private String notes;
    }
}
