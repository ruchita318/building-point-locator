package com.example.locator.controller;

import com.example.locator.dto.LocationResponse;
import com.example.locator.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocationControllerTest {
    private LocationService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(LocationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LocationController(service)).build();
    }

    @Test
    void returnsLocationForValidPoint() throws Exception {
        when(service.locate(any())).thenReturn(LocationResponse.found("Office building", "Floor 0"));

        mvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15,\"y\":15,\"z\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.building").value("Office building"))
                .andExpect(jsonPath("$.floor").value("Floor 0"));
    }

    @Test
    void returnsNotFoundWhenPointDoesNotMatchAnyFloor() throws Exception {
        when(service.locate(any())).thenReturn(LocationResponse.notFound());

        mvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":999,\"y\":999,\"z\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.message").value("The point is not inside any building floor."));
    }

    @Test
    void rejectsMissingCoordinate() throws Exception {
        mvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15,\"y\":15}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("z must not be null"));
    }

    @Test
    void rejectsNullCoordinate() throws Exception {
        mvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15,\"y\":null,\"z\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("y must not be null"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request body must be valid JSON with numeric x, y and z coordinates."));
    }

    @Test
    void returnsInternalServerErrorForUnexpectedFailure() throws Exception {
        when(service.locate(any())).thenThrow(new IllegalStateException("database unavailable"));

        mvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15,\"y\":15,\"z\":1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unable to process the location lookup request."));
    }
}
