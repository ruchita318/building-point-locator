package com.example.locator.service;

import com.example.locator.dto.*;
import com.example.locator.repository.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LocationServiceTest {
    @Test
    void returnsBuildingAndFloorWhenRepositoryFindsMatch() {
        var r = mock(BuildingLocationRepository.class);
        when(r.findFloor(15, 15, 1)).thenReturn(List.of(new LocationCandidate(1L, "Office building", 2L, "Floor 0")));
        var result = new LocationService(r).locate(new Point3DRequest(15.0, 15.0, 1.0));
        assertThat(result.found()).isTrue();
        assertThat(result.building()).isEqualTo("Office building");
        assertThat(result.floor()).isEqualTo("Floor 0");
        verify(r).findFloor(15, 15, 1);
    }

    @Test
    void returnsNotFoundWhenRepositoryHasNoMatch() {
        var r = mock(BuildingLocationRepository.class);
        when(r.findFloor(100, 100, 1)).thenReturn(List.of());
        var result = new LocationService(r).locate(new Point3DRequest(100.0, 100.0, 1.0));
        assertThat(result.found()).isFalse();
        assertThat(result.building()).isNull();
        assertThat(result.floor()).isNull();
    }
}
