package com.gradepath.content.content.service;

import com.gradepath.content.content.model.Content;
import com.gradepath.content.content.repository.ContentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public Content createContent(Content content) {
        return contentRepository.save(content);
    }

    @Cacheable(value = "content", key = "#id")
    public Content findById(String id) {
        return contentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Content not found: " + id));
    }

    public List<Content> findAll() {
        return contentRepository.findAll();
    }

    public Content updateContent(String id, Content content) {
        Content existing = findById(id);
        content.setId(existing.getId());
        return contentRepository.save(content);
    }

    public void deleteContent(String id) {
        Content existing = findById(id);
        contentRepository.deleteById(existing.getId());
    }
}
