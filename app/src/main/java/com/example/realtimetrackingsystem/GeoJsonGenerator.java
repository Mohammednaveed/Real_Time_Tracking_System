package com.example.realtimetrackingsystem;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoJsonGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String generateGeoJson(double sourceLongitude, double sourceLatitude,
                                         double destLongitude, double destLatitude,
                                         List<Double> intermediateLongitudes, List<Double> intermediateLatitudes) {

        List<Map<String, Object>> features = new ArrayList<>();

        // LineString Feature connecting all points
        List<List<Double>> lineStringCoordinates = new ArrayList<>();
        lineStringCoordinates.add(createCoordinateList(sourceLongitude, sourceLatitude));
        for (int i = 0; i < intermediateLongitudes.size(); i++) {
            Double longitude = intermediateLongitudes.get(i);
            Double latitude = intermediateLatitudes.get(i);

            if (latitude != null && longitude != null) {
                lineStringCoordinates.add(createCoordinateList(longitude, latitude));
            }
        }
        lineStringCoordinates.add(createCoordinateList(destLongitude, destLatitude));
        features.add(createLineStringFeature("LineString", lineStringCoordinates));

        // Point Features for source, destination, and intermediate stations
        features.add(createPointFeature("Source", sourceLongitude, sourceLatitude));
        features.add(createPointFeature("Destination", destLongitude, destLatitude));

        for (int i = 0; i < intermediateLongitudes.size(); i++) {
            Double longitude = intermediateLongitudes.get(i);
            Double latitude = intermediateLatitudes.get(i);

            if (latitude != null && longitude != null) {
                features.add(createPointFeature("Intermediate Station " + (i + 1), longitude, latitude));
            }
        }

        // Create GeoJSON FeatureCollection
        Map<String, Object> geoJsonData = new HashMap<>();
        geoJsonData.put("type", "FeatureCollection");
        geoJsonData.put("features", features);

        // Convert to JSON String using Jackson library
        try {
            return objectMapper.writeValueAsString(geoJsonData);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}"; // Return an empty JSON object in case of an error
        }
    }

    private static Map<String, Object> createPointFeature(String name, double longitude, double latitude) {
        Map<String, Object> feature = new HashMap<>();
        feature.put("type", "Feature");

        // Create geometry map using alternative approach
        Map<String, Object> geometryMap = new HashMap<>();
        geometryMap.put("type", "Point");
        geometryMap.put("coordinates", createCoordinateList(longitude, latitude));

        feature.put("geometry", geometryMap);
        feature.put("properties", Collections.singletonMap("name", name));
        return feature;
    }

    private static Map<String, Object> createLineStringFeature(String name, List<List<Double>> lineStringCoordinates) {
        Map<String, Object> feature = new HashMap<>();
        feature.put("type", "Feature");

        // Create geometry map using alternative approach
        Map<String, Object> geometryMap = new HashMap<>();
        geometryMap.put("type", "LineString");
        geometryMap.put("coordinates", lineStringCoordinates);

        feature.put("geometry", geometryMap);
        feature.put("properties", Collections.singletonMap("name", name));
        return feature;
    }

    private static List<Double> createCoordinateList(double longitude, double latitude) {
        List<Double> coordinates = new ArrayList<>();
        coordinates.add(longitude);
        coordinates.add(latitude);
        return coordinates;
    }
}
