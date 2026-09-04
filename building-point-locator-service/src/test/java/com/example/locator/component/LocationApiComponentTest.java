package com.example.locator.component;

import com.example.locator.BuildingPointLocatorApplication;
import com.example.locator.repository.BuildingLocationRepository;
import com.example.locator.repository.LocationCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BuildingPointLocatorApplication.class,
        properties = {
                "app.security.enabled=true",
                "app.security.username=component-user",
                "app.security.password=component-password",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        }
)
@AutoConfigureMockMvc
class LocationApiComponentTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BuildingLocationRepository repository;

    @Test
    void locatesPointThroughSecurityControllerAndService() throws Exception {
        when(repository.findFloor(15.5, 16.25, 1.0)).thenReturn(List.of(
                new LocationCandidate(1L, "Office building", 2L, "Floor 1")
        ));

        mvc.perform(post("/api/v1/location/locate")
                        .with(httpBasic("component-user", "component-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15.5,\"y\":16.25,\"z\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.building").value("Office building"))
                .andExpect(jsonPath("$.floor").value("Floor 1"))
                .andExpect(jsonPath("$.message").value("Point is inside Office building, Floor 1."));

        verify(repository).findFloor(15.5, 16.25, 1.0);
    }

    @Test
    void returnsNotFoundPayloadThroughControllerAndService() throws Exception {
        when(repository.findFloor(999.0, 999.0, 99.0)).thenReturn(List.of());

        mvc.perform(post("/api/v1/location/locate")
                        .with(httpBasic("component-user", "component-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":999,\"y\":999,\"z\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.building").doesNotExist())
                .andExpect(jsonPath("$.floor").doesNotExist())
                .andExpect(jsonPath("$.message").value("The point is not inside any building floor."));

        verify(repository).findFloor(999.0, 999.0, 99.0);
    }

    @Test
    void requiresAuthenticationForLocateWhenApiSecurityIsEnabled() throws Exception {
        mvc.perform(post("/api/v1/location/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15,\"y\":15,\"z\":1}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(repository);
    }

    @Test
    void keepsHealthEndpointPublicWhenApiSecurityIsEnabled() throws Exception {
        mvc.perform(get("/api/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInvalidPayloadBeforeCallingRepository() throws Exception {
        mvc.perform(post("/api/v1/location/locate")
                        .with(httpBasic("component-user", "component-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15,\"y\":15}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("z must not be null"));

        verifyNoInteractions(repository);
    }
}
