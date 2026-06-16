package org.tars.gateway.feature;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/gateway/features")
public class FeatureFlagController {

    private final FeatureFlagService service;

    public FeatureFlagController(FeatureFlagService service) {
        this.service = service;
    }

    @GetMapping
    public Mono<Map<String, ?>> list() {
        return Mono.just(service.getAll());
    }

    @PutMapping("/{name}")
    public Mono<Map<String, Object>> toggle(@PathVariable String name, @RequestParam boolean enabled) {
        service.toggle(name, enabled);
        return Mono.just(Map.of("feature", name, "enabled", enabled));
    }
}
