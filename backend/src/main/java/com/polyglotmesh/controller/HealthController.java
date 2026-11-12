package com.polyglotmesh.controller;

import com.polyglotmesh.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Mono<HealthResponse> getHealth() {
        return Mono.just(new HealthResponse("UP", "PolyglotMesh"));
    }
}
