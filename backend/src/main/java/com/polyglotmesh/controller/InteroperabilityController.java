package com.polyglotmesh.controller;

import com.polyglotmesh.dto.InteroperabilityRequest;
import com.polyglotmesh.dto.InteroperabilityResponse;
import com.polyglotmesh.service.LanguageInteroperabilityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interop")
public class InteroperabilityController {

    private final LanguageInteroperabilityService interoperabilityService;

    public InteroperabilityController(LanguageInteroperabilityService interoperabilityService) {
        this.interoperabilityService = interoperabilityService;
    }

    @PostMapping
    public InteroperabilityResponse exchange(@Valid @RequestBody InteroperabilityRequest request) {
        return interoperabilityService.exchange(request);
    }
}
