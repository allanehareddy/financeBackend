package com.finance.entity;

public enum Role {
    VIEWER,   // read-only dashboard access
    ANALYST,  // read + create/update records
    ADMIN     // full access including user management
}
