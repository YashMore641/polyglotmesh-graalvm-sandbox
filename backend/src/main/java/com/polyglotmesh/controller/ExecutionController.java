package com.polyglotmesh.controller;

import com.polyglotmesh.dto.ExecutionRequest;
import com.polyglotmesh.dto.ExecutionResponse;
import com.polyglotmesh.service.PolyglotExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final PolyglotExecutionService polyglotExecutionService;

    public ExecutionController(PolyglotExecutionService polyglotExecutionService) {
        this.polyglotExecutionService = polyglotExecutionService;
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> execute(@Valid @RequestBody ExecutionRequest request) {
        ExecutionResponse response = polyglotExecutionService.execute(request.language(), request.code());
        return ResponseEntity.ok(response);
    }
}
