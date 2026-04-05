package com.finance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    // ── Convenience subclasses ────────────────────────────────────────────────

    public static class NotFoundException extends AppException {
        public NotFoundException(String msg) { super(msg, HttpStatus.NOT_FOUND); }
    }

    public static class ConflictException extends AppException {
        public ConflictException(String msg) { super(msg, HttpStatus.CONFLICT); }
    }

    public static class BadRequestException extends AppException {
        public BadRequestException(String msg) { super(msg, HttpStatus.BAD_REQUEST); }
    }

    public static class ForbiddenException extends AppException {
        public ForbiddenException(String msg) { super(msg, HttpStatus.FORBIDDEN); }
    }
}
