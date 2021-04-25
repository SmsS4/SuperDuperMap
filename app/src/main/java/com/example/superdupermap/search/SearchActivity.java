package com.example.superdupermap.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.superdupermap.R;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {
    private RecyclerView.Adapter adapter;
    private List<SearchResult> searchResults;
    private RecyclerView searchResultsView;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initSearchResultsList();

        searchInput = findViewById(R.id.searchTextInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchPattern(s.toString());
            }
        });
    }

    private void initSearchResultsList() {
        searchResults = new ArrayList<>();
        searchResultsView = findViewById(R.id.searchResults);
        RecyclerView.LayoutManager searchResultsLayoutManager = new LinearLayoutManager(this);
        searchResultsView.setLayoutManager(searchResultsLayoutManager);

        adapter = new SearchResultListAdapter(searchResults);
        searchResultsView.setAdapter(adapter);
    }

    private void searchPattern(String pattern) {
        if (pattern.length() == 0) {
            setChoices(new ArrayList<>(), pattern);
            return;
        }
        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(pattern)
                .build();

        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeocodingResponse> call,
                                   @NonNull Response<GeocodingResponse> response) {
                assert response.body() != null;
                List<CarmenFeature> carmenFeatures = response.body().features();
                ArrayList<SearchResult> results = new ArrayList<>();
                for (CarmenFeature cf : carmenFeatures) {
                    Point center = cf.center();
                    assert center != null;

                    LatLng location;
                    location = new LatLng(center.latitude(), center.longitude());
                    results.add(new SearchResult(cf.placeName(), location));
                }
                setChoices(results, pattern);
            }

            @Override
            public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    private void setChoices(List<SearchResult> choices, String pattern) {
        if (!this.searchInput.getText().toString().equals(pattern))
            return;
        this.searchResults.clear();
        this.searchResults.addAll(choices);
        this.adapter.notifyDataSetChanged();
    }
}