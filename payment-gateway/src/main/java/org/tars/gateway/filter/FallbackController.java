package org.tars.gateway.filter;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/{service}")
    public Mono<Map<String, Object>> fallback(@PathVariable String service) {
        return Mono.just(Map.of(
                "status", 503,
                "error", "SERVICE_UNAVAILABLE",
                "message", "Service '" + service + "' is temporarily unavailable. Circuit breaker is open.",
                "service", service
        ));
    }
}
