package com.finance.controller;

import com.finance.dto.response.ApiResponse.DashboardSummary;
import com.finance.dto.response.ApiResponse.TrendsResponse;
import com.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2. Dashboard", description = "Aggregated financial analytics. Returns formatted totals, category breakdowns, monthly trends, and insights. All roles can access.")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
        summary = "Summary",
        description = "Returns total income, total expenses, net balance (raw + formatted in ₹), balance status (SURPLUS/DEFICIT/BREAK_EVEN), savings rate %, expense ratio %, category breakdowns, and recent activity."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard summary returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> summary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @Operation(
        summary = "Monthly trends",
        description = "Returns month-by-month income vs expense breakdown with net and status for the last N months. Also includes period-level totals."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Trends returned"),
        @ApiResponse(responseCode = "400", description = "Invalid months parameter")
    })
    @GetMapping("/trends")
    public ResponseEntity<TrendsResponse> trends(
            @Parameter(description = "Number of months to look back (default 6, max 24)")
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(dashboardService.getTrends(months));
    }
}
