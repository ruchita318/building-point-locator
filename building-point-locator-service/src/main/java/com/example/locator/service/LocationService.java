package com.example.locator.service;

import com.example.locator.dto.LocationResponse;
import com.example.locator.dto.Point3DRequest;
import com.example.locator.repository.BuildingLocationRepository;
import com.example.locator.repository.LocationCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LocationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationService.class);

    private final BuildingLocationRepository repository;

    /**
     * Creates the service with the repository that performs spatial floor lookups.
     *
     * @param repository repository used to query candidate building floors
     */
    public LocationService(BuildingLocationRepository repository) {
        this.repository = repository;
    }

    /**
     * Resolves a validated 3D point to the first matching building floor.
     *
     * @param point request containing x, y and z coordinates
     * @return successful location response when a floor matches, otherwise a not-found response
     */
    @Transactional(readOnly = true)
    public LocationResponse locate(Point3DRequest point) {
        LOGGER.debug("Looking up location candidate for x={}, y={}, z={}", point.x(), point.y(), point.z());
        Optional<LocationCandidate> candidate = repository.findFloor(point.x(), point.y(), point.z()).stream()
                .findFirst();

        if (candidate.isPresent()) {
            LocationCandidate match = candidate.get();
            LOGGER.info("Point matched buildingId={}, floorId={}", match.buildingId(), match.floorId());
            return LocationResponse.found(match.buildingName(), match.floorName());
        }

        LOGGER.info("No building floor matched requested point");
        return LocationResponse.notFound();
    }
}
