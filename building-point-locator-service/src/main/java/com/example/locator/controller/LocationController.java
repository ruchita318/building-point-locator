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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LocationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationController.class);

    private final LocationService service;

    /**
     * Creates the REST controller with the service that performs location lookups.
     *
     * @param service service responsible for resolving 3D points to building floors
     */
    public LocationController(LocationService service) {
        this.service = service;
    }

    /**
     * Handles location lookup requests for a point expressed as x, y and z coordinates.
     *
     * @param point validated request body containing the point coordinates
     * @return 200 with building and floor details when found, otherwise 404 with a not-found response
     */
    @PostMapping("/locate")
    public ResponseEntity<LocationResponse> locate(@Valid @RequestBody Point3DRequest point) {
        LOGGER.debug("Received location lookup request for coordinates x={}, y={}, z={}", point.x(), point.y(), point.z());
        LocationResponse response = service.locate(point);
        LOGGER.info("Location lookup completed: found={}", response.found());
        if (!response.found()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Converts Bean Validation failures into the API's standard bad-request response shape.
     *
     * @param exception validation exception raised while binding the request body
     * @return 400 response containing the first field validation message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::formatFieldError)
                .orElse("Request validation failed.");
        LOGGER.warn("Invalid location lookup request: {}", message);
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message
        ));
    }

    /**
     * Converts malformed JSON or incompatible request bodies into a stable client error response.
     *
     * @param exception unreadable-message exception raised by Spring MVC
     * @return 400 response explaining the expected coordinate JSON structure
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        LOGGER.warn("Invalid location lookup request body: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Request body must be valid JSON with numeric x, y and z coordinates."
        ));
    }

    /**
     * Catches unexpected failures so clients receive the API's standard error response.
     *
     * @param exception unexpected exception raised while processing a request
     * @return 500 response with a generic failure message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        LOGGER.error("Unexpected error while processing location lookup", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Unable to process the location lookup request."
        ));
    }

    /**
     * Formats a validation field error into the message exposed by the API.
     *
     * @param error validation field error returned by Spring
     * @return field name followed by the validation message
     */
    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    /**
     * Error payload returned by controller exception handlers.
     *
     * @param status HTTP status code
     * @param error short HTTP error label
     * @param message client-facing error detail
     */
    public record ApiErrorResponse(int status, String error, String message) {
    }
}
