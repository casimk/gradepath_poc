package com.gradepath.content.content.repository;

import com.gradepath.content.content.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentRepository extends JpaRepository<Content, String>, JpaSpecificationExecutor<Content> {

    Optional<Content> findByTypeAndStatus(Content.ContentType type, Content.ContentStatus status);

    Optional<Content> findByIdAndStatus(String id, Content.ContentStatus status);

    long countByTypeAndStatus(Content.ContentType type, Content.ContentStatus status);

    List<Content> findByStatus(Content.ContentStatus status);
}
