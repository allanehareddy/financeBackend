package com.finance.repository;

import com.finance.entity.Role;
import com.finance.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        WHERE (:active IS NULL OR u.active = :active)
          AND (:role   IS NULL OR u.role   = :role)
        """)
    Page<User> findAllFiltered(
            @Param("active") Boolean active,
            @Param("role")   Role role,
            Pageable pageable
    );
}
