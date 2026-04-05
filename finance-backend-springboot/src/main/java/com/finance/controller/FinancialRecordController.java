package com.finance.controller;

import com.finance.dto.request.RecordRequest;
import com.finance.dto.response.ApiResponse.PagedResponse;
import com.finance.dto.response.ApiResponse.RecordResponse;
import com.finance.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "3. Financial Records", description = "CRUD for financial records. VIEWER can read. ANALYST can create/update. ADMIN can delete.")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    @Operation(
        summary = "List records",
        description = "Returns paginated financial records with optional filters. All roles can access."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of records"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<RecordResponse>> list(
            @Parameter(description = "Filter by type: INCOME or EXPENSE") @RequestParam(required = false) String type,
            @Parameter(description = "Filter by category name (case-insensitive)") @RequestParam(required = false) String category,
            @Parameter(description = "Start date (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Keyword search in category and notes") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(
                recordService.listRecords(type, category, startDate, endDate, search, page, size));
    }

    @Operation(summary = "Get record by ID", description = "Returns a single financial record. All roles can access.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Record found"),
        @ApiResponse(responseCode = "404", description = "Record not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RecordResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.getRecord(id));
    }

    @Operation(summary = "Create record", description = "Creates a new financial record. **ANALYST or ADMIN only.**")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Record created"),
        @ApiResponse(responseCode = "403", description = "Insufficient role (VIEWER cannot create)"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<RecordResponse> create(
            @Valid @RequestBody RecordRequest.Create req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createRecord(req, userDetails.getUsername()));
    }

    @Operation(summary = "Update record", description = "Partially updates a record. **ANALYST or ADMIN only.**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Record updated"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "404", description = "Record not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<RecordResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RecordRequest.Update req) {
        return ResponseEntity.ok(recordService.updateRecord(id, req));
    }

    @Operation(summary = "Delete record", description = "Soft-deletes a record (data is retained, marked deleted=true). **ADMIN only.**")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Record deleted"),
        @ApiResponse(responseCode = "403", description = "Admin access required"),
        @ApiResponse(responseCode = "404", description = "Record not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
