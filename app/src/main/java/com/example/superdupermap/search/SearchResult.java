package com.example.superdupermap.search;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class SearchResult {
    private final String title;
    private final LatLng location;

    public SearchResult(String title, LatLng location) {
        this.title = title;
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public LatLng getLocation() {
        return location;
    }
}
