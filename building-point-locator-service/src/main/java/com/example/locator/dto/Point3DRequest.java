package com.example.locator.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for locating a point in the building data set.
 *
 * @param x horizontal coordinate in the floor footprint coordinate system
 * @param y vertical coordinate in the floor footprint coordinate system
 * @param z height coordinate used to select the matching floor range
 */
public record Point3DRequest(
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double z
) {}
