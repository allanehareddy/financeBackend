package com.finance.service;

import com.finance.dto.request.RecordRequest;
import com.finance.dto.response.ApiResponse.PagedResponse;
import com.finance.dto.response.ApiResponse.RecordResponse;
import com.finance.entity.FinancialRecord;
import com.finance.entity.TransactionType;
import com.finance.entity.User;
import com.finance.exception.AppException;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository userRepository;

    public PagedResponse<RecordResponse> listRecords(
            String type, String category,
            LocalDate startDate, LocalDate endDate,
            String search, int page, int size) {

        TransactionType typeEnum = parseType(type);
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        PageRequest pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<FinancialRecord> result = recordRepository.findAllFiltered(
                typeEnum, category, startDate, endDate, searchTerm, pageable);

        return PagedResponse.of(
                result.getContent().stream().map(RecordResponse::from).toList(),
                result.getTotalElements(), page, size
        );
    }

    public RecordResponse getRecord(Long id) {
        return RecordResponse.from(findOrThrow(id));
    }

    @Transactional
    public RecordResponse createRecord(RecordRequest.Create req, String creatorEmail) {
        TransactionType type = parseTypeRequired(req.getType());
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));

        FinancialRecord record = FinancialRecord.builder()
                .amount(req.getAmount())
                .type(type)
                .category(req.getCategory().trim())
                .date(req.getDate())
                .notes(req.getNotes())
                .createdBy(creator)
                .deleted(false)
                .build();

        return RecordResponse.from(recordRepository.save(record));
    }

    @Transactional
    public RecordResponse updateRecord(Long id, RecordRequest.Update req) {
        FinancialRecord record = findOrThrow(id);

        if (req.getAmount()   != null) record.setAmount(req.getAmount());
        if (req.getCategory() != null) record.setCategory(req.getCategory().trim());
        if (req.getDate()     != null) record.setDate(req.getDate());
        if (req.getNotes()    != null) record.setNotes(req.getNotes());
        if (req.getType()     != null) record.setType(parseTypeRequired(req.getType()));

        return RecordResponse.from(recordRepository.save(record));
    }

    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = findOrThrow(id);
        record.setDeleted(true);
        recordRepository.save(record);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FinancialRecord findOrThrow(Long id) {
        FinancialRecord r = recordRepository.findById(id)
                .orElseThrow(() -> new AppException.NotFoundException("Record not found with id: " + id));
        if (r.isDeleted()) throw new AppException.NotFoundException("Record not found with id: " + id);
        return r;
    }

    private TransactionType parseType(String type) {
        if (type == null) return null;
        return parseTypeRequired(type);
    }

    private TransactionType parseTypeRequired(String type) {
        try {
            return TransactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException.BadRequestException("Invalid type. Must be INCOME or EXPENSE");
        }
    }
}
