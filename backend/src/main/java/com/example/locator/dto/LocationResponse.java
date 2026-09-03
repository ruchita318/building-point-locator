package com.example.locator.dto;

public record LocationResponse(
        boolean found,
        String building,
        String floor,
        String message
) {
    /**
     * Builds a successful lookup response for a matched building floor.
     *
     * @param building matched building name
     * @param floor matched floor name
     * @return response with found set to true and a human-readable message
     */
    public static LocationResponse found(String building, String floor) {
        return new LocationResponse(true, building, floor,
                "Point is inside " + building + ", " + floor + ".");
    }

    /**
     * Builds the response used when no building floor contains the requested point.
     *
     * @return response with found set to false and no building or floor names
     */
    public static LocationResponse notFound() {
        return new LocationResponse(false, null, null,
                "The point is not inside any building floor.");
    }
}
