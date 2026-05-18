package com.gestionstock.iam.infrastructure.repository;

import com.gestionstock.iam.infrastructure.entity.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganisationRepository extends JpaRepository<Organisation, Long> {

    boolean existsByName(String name);

    Optional<Organisation> findByName(String name);
}
