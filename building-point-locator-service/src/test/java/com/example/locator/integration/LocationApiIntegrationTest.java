package com.example.locator.integration;

import com.example.locator.BuildingPointLocatorApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BuildingPointLocatorApplication.class)
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc
@Testcontainers
class LocationApiIntegrationTest {
    private static final DockerImageName POSTGIS_IMAGE = DockerImageName.parse("postgis/postgis:16-3.5")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withDatabaseName("building_locator_integration")
            .withUsername("locator")
            .withPassword("locator_password");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Test
    void locatesSeededPointThroughApiServiceRepositoryAndPostgis() throws Exception {
        mvc.perform(post("/api/v1/location/locate")
                        .with(httpBasic("integration-user", "integration-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15,\"y\":15,\"z\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.building").value("Office building"))
                .andExpect(jsonPath("$.floor").value("Floor 1"))
                .andExpect(jsonPath("$.message").value("Point is inside Office building, Floor 1."));
    }

    @Test
    void treatsSeededFootprintBoundaryAsInsideTheFloor() throws Exception {
        mvc.perform(post("/api/v1/location/locate")
                        .with(httpBasic("integration-user", "integration-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":10,\"y\":10,\"z\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.building").value("Office building"))
                .andExpect(jsonPath("$.floor").value("Floor 1"));
    }

    @Test
    void locatesPointInsideSeededOctagonBuilding() throws Exception {
        mvc.perform(post("/api/v1/location/locate")
                        .with(httpBasic("integration-user", "integration-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":320,\"y\":50,\"z\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.building").value("Octagon Atrium"))
                .andExpect(jsonPath("$.floor").value("Floor 0"))
                .andExpect(jsonPath("$.message").value("Point is inside Octagon Atrium, Floor 0."));
    }

    @Test
    void returnsNotFoundForPointOutsideSeededDatabase() throws Exception {
        mvc.perform(post("/api/v1/location/locate")
                        .with(httpBasic("integration-user", "integration-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":999,\"y\":999,\"z\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.message").value("The point is not inside any building floor."));
    }

    @Test
    void requiresAuthenticationBeforeQueryingTestDatabase() throws Exception {
        mvc.perform(post("/api/v1/location/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":15,\"y\":15,\"z\":1}"))
                .andExpect(status().isUnauthorized());
    }
}
