package com.example.superdupermap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.example.superdupermap.bookmark.BookmarkActivity;
import com.example.superdupermap.database.AppDatabase;
import com.example.superdupermap.database.Bookmark;
import com.example.superdupermap.database.Config;
import com.example.superdupermap.search.SearchActivity;
import com.example.superdupermap.setttings.ConfigStorage;
import com.example.superdupermap.setttings.SettingsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {
    private static final int SEARCH_REQUEST_CODE = 0;
    private MapView mapView;
    private AppDatabase db;
    private ThreadPoolExecutor threadPoolExecutor;
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private LocationManager locationManager;

    public void initThreadPool() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        threadPoolExecutor = new ThreadPoolExecutor(
                0, 2, 15, TimeUnit.MINUTES, queue
        );
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                db = AppDatabase.getDatabase(getApplicationContext());
                if (db.configDao().getAll().size() == 0) {
                    db.configDao().insert(
                            new Config(false)
                    );

                }
                ConfigStorage.darkMode = db.configDao().getDarkMode();
            }
        });
    }

    public void pre() {
        if (ConfigStorage.darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setContentView(R.layout.activity_main);

        Activity from = MainActivity.this;
        findViewById(R.id.bookmark_nav).setOnClickListener(v -> {
            System.out.println("bookmark_nav called on Main");
            finish();
            startActivity(new Intent(from, BookmarkActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        });

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        ((ImageView) findViewById(R.id.map_nav)).setColorFilter(filter);

        findViewById(R.id.setting_nav).setOnClickListener(v -> {
            finish();
            startActivity(new Intent(from, SettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        });

        findViewById(R.id.searchBoxReal).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.clearFocus();
                    startActivityForResult(new Intent(from, SearchActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), 0);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initThreadPool();
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        pre();

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        FloatingActionButton myLocationBtn = findViewById(R.id.myLocationBtn);
        myLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                double latitude = location.getLatitude(),
                        longitude = location.getLongitude();
                gotoPosition(new LatLng(latitude, longitude));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SEARCH_REQUEST_CODE) {
            if (resultCode != RESULT_OK)
                return;
            LatLng location = mapboxMap.getCameraPosition().target;
            location.setLatitude(data.getDoubleExtra("lat", location.getLatitude()));
            location.setLongitude(data.getDoubleExtra("lng", location.getLongitude()));
            gotoPosition(location);
            dropPin(location);
            showBookmarkModal(location);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void gotoPosition(LatLng location) {
        CameraPosition position = new CameraPosition.Builder()
                .target(location)
                .zoom(15)
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 3000);
    }

    private void dropPin(LatLng location) {
    }

    private void clearPin() {
    }

    private void showBookmarkModal(LatLng location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add bookmark");
        builder.setMessage(String.format(
                Locale.getDefault(),
                "(%.2f, %.2f)",
                location.getLatitude(),
                location.getLongitude()
        ));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bookmark bookmark = new Bookmark(
                        input.getText().toString(),
                        location.getLatitude(),
                        location.getLongitude()
                );
                threadPoolExecutor.execute(() -> db.bookmarkDao().insert(bookmark));
                clearPin();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                clearPin();
            }
        });

        builder.show();
    }

    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        mapboxMap.addOnMapLongClickListener(location -> {
            gotoPosition(location);
            dropPin(location);
            showBookmarkModal(location);
            return true;
        });
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
            }
        });
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            // Set the location manager of this
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}