package com.example.locator.dto;

import jakarta.validation.constraints.NotNull;

public record Point3DRequest(
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double z
) {}
