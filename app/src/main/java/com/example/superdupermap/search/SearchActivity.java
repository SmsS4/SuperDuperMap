package com.example.superdupermap.search;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.superdupermap.R;
import com.google.android.material.textfield.TextInputEditText;
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
    private static final int SPEECH_REQUEST_CODE = 0;
    private RecyclerView.Adapter adapter;
    private List<SearchResult> searchResults;
    private RecyclerView searchResultsView;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initVoiceSearch();

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

        initVoiceSearch();
    }

    private void initVoiceSearch() {
        /// todo add this feature to bookmark
        ImageView mic = findViewById(R.id.mic);
        mic.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            /// remove this lint for english
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa");

            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode != RESULT_OK)
                return;
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            // Do something with spokenText.
            System.out.println(spokenText);
            ((TextInputEditText) findViewById(R.id.searchTextInput)).setText(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initSearchResultsList() {
        searchResults = new ArrayList<>();
        searchResultsView = findViewById(R.id.searchResults);
        RecyclerView.LayoutManager searchResultsLayoutManager = new LinearLayoutManager(this);
        searchResultsView.setLayoutManager(searchResultsLayoutManager);

        adapter = new SearchResultListAdapter(searchResults);
        searchResultsView.setAdapter(adapter);

        searchResultsView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, searchResultsView,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                LatLng location = searchResults.get(position).getLocation();
                                Intent data = new Intent();
                                data.putExtra("lat", location.getLatitude());
                                data.putExtra("lng", location.getLongitude());
                                setResult(Activity.RESULT_OK, data);
                                finish();
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                            }
                        }
                )
        );
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