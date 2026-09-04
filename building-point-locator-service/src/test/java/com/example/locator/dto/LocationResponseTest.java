package com.example.locator;

import com.example.locator.dto.LocationResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationResponseTest {
    @Test
    void foundFactoryBuildsExpectedMessage() {
        var r = LocationResponse.found("HQ", "Floor 3");
        assertThat(r.message()).isEqualTo("Point is inside HQ, Floor 3.");
    }

    @Test
    void notFoundFactoryReturnsNullNames() {
        var r = LocationResponse.notFound();
        assertThat(r.found()).isFalse();
        assertThat(r.building()).isNull();
        assertThat(r.floor()).isNull();
    }
}
