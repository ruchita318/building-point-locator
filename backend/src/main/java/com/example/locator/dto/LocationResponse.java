package com.example.locator.dto;

public record LocationResponse(
        boolean found,
        String building,
        String floor,
        String message
) {
    public static LocationResponse found(String building, String floor) {
        return new LocationResponse(true, building, floor,
                "Point is inside " + building + ", " + floor + ".");
    }
    public static LocationResponse notFound() {
        return new LocationResponse(false, null, null,
                "The point is not inside any building floor.");
    }
}
