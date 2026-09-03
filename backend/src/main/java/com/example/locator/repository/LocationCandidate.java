package com.example.locator.repository;

/**
 * Projection returned by the spatial lookup query for a candidate floor match.
 *
 * @param buildingId database identifier for the matched building
 * @param buildingName display name for the matched building
 * @param floorId database identifier for the matched floor
 * @param floorName display name for the matched floor
 */
public record LocationCandidate(Long buildingId, String buildingName, Long floorId, String floorName) {}
