package com.example.locator.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BuildingLocationRepository {
    private static final Logger log = LoggerFactory.getLogger(BuildingLocationRepository.class);

    private final JdbcTemplate jdbc;

    public BuildingLocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * PostGIS performs the spatial predicate in the database. ST_Covers is
     * deliberately used instead of ST_Contains so points on the outline are
     * considered inside. Floor upper height is exclusive to avoid overlap at
     * shared boundaries.
     *
     * @param x horizontal coordinate in the same coordinate system as stored footprints
     * @param y vertical coordinate in the same coordinate system as stored footprints
     * @param z height coordinate compared against each floor's z range
     * @return matching floor candidates ordered deterministically, limited to the first match
     */
    public List<LocationCandidate> findFloor(double x, double y, double z) {
        log.debug("Executing floor lookup query for x={}, y={}, z={}", x, y, z);
        String sql = """
            SELECT b.id, b.name, f.id, f.name
            FROM floors f
            JOIN buildings b ON b.id = f.building_id
            WHERE f.min_z <= ? AND ? < f.max_z
              AND ST_Covers(f.footprint, ST_SetSRID(ST_Point(?, ?), 0))
            ORDER BY b.id, f.min_z
            LIMIT 1
            """;
        List<LocationCandidate> candidates = jdbc.query(sql,
                (rs, rowNum) -> new LocationCandidate(
                        rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4)),
                z, z, x, y);
        log.debug("Floor lookup query returned {} candidate(s)", candidates.size());
        return candidates;
    }
}
