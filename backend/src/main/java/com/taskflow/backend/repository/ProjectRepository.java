package com.taskflow.backend.repository;

import com.taskflow.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Every finder here is scoped by owner on purpose. There is deliberately no
 * plain {@code findById} usage in the service layer: making the owner part of
 * the query is what guarantees a user can never load another user's project,
 * rather than relying on a caller remembering to check afterwards.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerIdOrderByIdAsc(Long ownerId);

    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
}
