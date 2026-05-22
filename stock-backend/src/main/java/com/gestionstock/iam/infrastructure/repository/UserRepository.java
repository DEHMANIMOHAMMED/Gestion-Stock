package com.gestionstock.iam.infrastructure.repository;

import com.gestionstock.iam.infrastructure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByOrganisationIdOrderByEmailAsc(Long organisationId);

    Optional<User> findByIdAndOrganisationId(Long id, Long organisationId);

    long countByOrganisationId(Long organisationId);
}
