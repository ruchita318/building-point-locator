CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE buildings (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    footprint geometry(Polygon, 0) NOT NULL,
    min_z DOUBLE PRECISION NOT NULL,
    max_z DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT buildings_height_ck CHECK (min_z < max_z)
);

CREATE TABLE floors (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    footprint geometry(Polygon, 0) NOT NULL,
    min_z DOUBLE PRECISION NOT NULL,
    max_z DOUBLE PRECISION NOT NULL,
    floor_number INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT floors_height_ck CHECK (min_z < max_z),
    CONSTRAINT floors_name_uk UNIQUE (building_id, name),
    CONSTRAINT floors_number_uk UNIQUE (building_id, floor_number)
);

CREATE INDEX buildings_footprint_gist_idx ON buildings USING GIST (footprint);
CREATE INDEX floors_footprint_gist_idx ON floors USING GIST (footprint);
CREATE INDEX floors_height_idx ON floors (min_z, max_z);
CREATE INDEX floors_building_id_idx ON floors (building_id);

CREATE TABLE flyway_metadata_note (
    id INTEGER PRIMARY KEY,
    note TEXT NOT NULL
);
INSERT INTO flyway_metadata_note(id, note) VALUES (1, 'Schema managed by Flyway. Spatial lookups use PostGIS GIST indexes.');
