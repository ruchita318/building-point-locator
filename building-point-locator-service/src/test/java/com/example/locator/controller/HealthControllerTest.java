package com.example.locator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

    @Test
    void returnsAggregateHealthWhenLiveAndReady() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HealthController(
                new FixedAvailability(LivenessState.CORRECT, ReadinessState.ACCEPTING_TRAFFIC)
        )).build();

        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.liveness").value("CORRECT"))
                .andExpect(jsonPath("$.readiness").value("ACCEPTING_TRAFFIC"));
    }

    @Test
    void returnsServiceUnavailableWhenNotReady() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HealthController(
                new FixedAvailability(LivenessState.CORRECT, ReadinessState.REFUSING_TRAFFIC)
        )).build();

        mvc.perform(get("/api/health/ready"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void returnsServiceUnavailableWhenLivenessIsBroken() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new HealthController(
                new FixedAvailability(LivenessState.BROKEN, ReadinessState.ACCEPTING_TRAFFIC)
        )).build();

        mvc.perform(get("/api/health/live"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    private record FixedAvailability(
            LivenessState livenessState,
            ReadinessState readinessState
    ) implements ApplicationAvailability {

        @Override
        public LivenessState getLivenessState() {
            return livenessState;
        }

        @Override
        public ReadinessState getReadinessState() {
            return readinessState;
        }

        @Override
        public <S extends AvailabilityState> S getState(Class<S> stateType, S defaultState) {
            if (stateType == LivenessState.class) {
                return stateType.cast(livenessState);
            }
            if (stateType == ReadinessState.class) {
                return stateType.cast(readinessState);
            }
            return defaultState;
        }

        @Override
        public <S extends AvailabilityState> S getState(Class<S> stateType) {
            return getState(stateType, null);
        }

        @Override
        public <S extends AvailabilityState> AvailabilityChangeEvent<S> getLastChangeEvent(Class<S> stateType) {
            return null;
        }
    }
}
