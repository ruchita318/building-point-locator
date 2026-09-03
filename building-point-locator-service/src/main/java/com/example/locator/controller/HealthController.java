package com.example.locator.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);

    private final ApplicationAvailability availability;

    /**
     * Creates the health controller using Spring Boot's application availability state.
     *
     * @param availability current liveness and readiness state provider
     */
    public HealthController(ApplicationAvailability availability) {
        this.availability = availability;
    }

    /**
     * Reports aggregate application health for load balancers and smoke tests.
     *
     * @return 200 when the application is live and ready, otherwise 503
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        LivenessState liveness = availability.getLivenessState();
        ReadinessState readiness = availability.getReadinessState();
        boolean up = isLive(liveness) && isReady(readiness);

        LOGGER.debug("Health check requested: liveness={}, readiness={}, status={}", liveness, readiness, status(up));
        return ResponseEntity.status(httpStatus(up))
                .body(new HealthResponse(status(up), liveness.name(), readiness.name()));
    }

    /**
     * Reports whether the running process should be considered alive.
     *
     * @return 200 when liveness is correct, otherwise 503
     */
    @GetMapping("/live")
    public ResponseEntity<ProbeResponse> liveness() {
        LivenessState liveness = availability.getLivenessState();
        boolean up = isLive(liveness);

        LOGGER.debug("Liveness check requested: liveness={}, status={}", liveness, status(up));
        return ResponseEntity.status(httpStatus(up)).body(new ProbeResponse(status(up)));
    }

    /**
     * Reports whether the application is ready to accept traffic.
     *
     * @return 200 when readiness accepts traffic, otherwise 503
     */
    @GetMapping("/ready")
    public ResponseEntity<ProbeResponse> readiness() {
        ReadinessState readiness = availability.getReadinessState();
        boolean up = isReady(readiness);

        LOGGER.debug("Readiness check requested: readiness={}, status={}", readiness, status(up));
        return ResponseEntity.status(httpStatus(up)).body(new ProbeResponse(status(up)));
    }

    private boolean isLive(LivenessState liveness) {
        return liveness == LivenessState.CORRECT;
    }

    private boolean isReady(ReadinessState readiness) {
        return readiness == ReadinessState.ACCEPTING_TRAFFIC;
    }

    private HttpStatus httpStatus(boolean up) {
        return up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    }

    private String status(boolean up) {
        return up ? "UP" : "DOWN";
    }

    /**
     * Aggregate health payload containing application status and availability states.
     *
     * @param status overall health status
     * @param liveness current liveness state
     * @param readiness current readiness state
     */
    public record HealthResponse(String status, String liveness, String readiness) {
    }

    /**
     * Simple probe payload returned by liveness and readiness checks.
     *
     * @param status probe status
     */
    public record ProbeResponse(String status) {
    }
}
