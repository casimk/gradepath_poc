package com.gradepath.content.content.controller;

import com.gradepath.content.content.model.Content;
import com.gradepath.content.content.service.ContentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Content> getContent(@PathVariable String id) {
        try {
            Content content = contentService.findById(id);
            return ResponseEntity.ok(content);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/browse")
    public ResponseEntity<List<Content>> browse() {
        List<Content> content = contentService.findAll();
        return ResponseEntity.ok(content);
    }

    @PostMapping
    public ResponseEntity<Content> createContent(@RequestBody Content content) {
        Content created = contentService.createContent(content);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Content> updateContent(@PathVariable String id, @RequestBody Content content) {
        try {
            Content updated = contentService.updateContent(id, content);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable String id) {
        try {
            contentService.deleteContent(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
