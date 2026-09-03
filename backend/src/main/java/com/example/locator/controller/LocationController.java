package com.example.locator.controller;

import com.example.locator.dto.LocationResponse;
import com.example.locator.dto.Point3DRequest;
import com.example.locator.service.LocationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LocationController {
    private static final Logger log = LoggerFactory.getLogger(LocationController.class);

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @PostMapping("/locate")
    public ResponseEntity<LocationResponse> locate(@Valid @RequestBody Point3DRequest point) {
        log.debug("Received location lookup request for coordinates x={}, y={}, z={}", point.x(), point.y(), point.z());
        LocationResponse response = service.locate(point);
        log.info("Location lookup completed: found={}", response.found());
        if (!response.found()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse("Request validation failed.");
        log.warn("Invalid location lookup request: {}", message);
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        log.warn("Invalid location lookup request body: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Request body must be valid JSON with numeric x, y and z coordinates."
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("Unexpected error while processing location lookup", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Unable to process the location lookup request."
        ));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    public record ApiErrorResponse(int status, String error, String message) {
    }
}
