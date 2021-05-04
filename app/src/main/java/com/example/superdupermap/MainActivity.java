package com.example.superdupermap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.mapbox.geojson.Point;
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
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {
    public static final int REQUEST_CODE_SEARCH = 0;
    public static final int REQUEST_CODE_SETTINGS = 1;
    public static final int REQUEST_CODE_BOOKMARKS = 2;
    public static final int RESULT_CODE_GOTO_BOOKMARK = 100;
    public static final int RESULT_CODE_GOTO_SETTINGS = 101;
    public static final int RESULT_CODE_DROP_PIN = 102;
    public static final int RESULT_CODE_UPDATE_THEME = 103;
    private static final String DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID";
    private MapView mapView;
    private AppDatabase db;
    private ThreadPoolExecutor threadPoolExecutor;
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private LocationManager locationManager;
    private LatLng markerLocation;
    private Layer droppedMarkerLayer;
    private boolean isDark;

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
            isDark = true;
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            isDark = false;
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setContentView(R.layout.activity_main);

        Activity from = MainActivity.this;
        findViewById(R.id.bookmark_nav).setOnClickListener(v -> {
            openBookmarks();
        });

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        ((ImageView) findViewById(R.id.map_nav)).setColorFilter(filter);

        findViewById(R.id.setting_nav).setOnClickListener(v -> {
            openSettings();
        });

        findViewById(R.id.searchBoxReal).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.clearFocus();
                    startActivityForResult(new Intent(from, SearchActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), REQUEST_CODE_SEARCH);
                }
            }
        });
    }

    private void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), REQUEST_CODE_SETTINGS);
    }

    private void openBookmarks() {
        startActivityForResult(new Intent(this, BookmarkActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), REQUEST_CODE_BOOKMARKS);
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
        if (resultCode == RESULT_CODE_GOTO_BOOKMARK) {
            System.out.println("Gonna open bookmarks");
            openBookmarks();
        }
        if (resultCode == RESULT_CODE_GOTO_SETTINGS)
            openSettings();
        if (resultCode == RESULT_CODE_UPDATE_THEME && ConfigStorage.darkMode != isDark)
            recreate();
        if (resultCode == RESULT_CODE_DROP_PIN) {
            LatLng location = mapboxMap.getCameraPosition().target;
            location.setLatitude(data.getDoubleExtra("lat", location.getLatitude()));
            location.setLongitude(data.getDoubleExtra("lng", location.getLongitude()));
            gotoPosition(location);
            dropMarker(location);
        }
        if (requestCode == REQUEST_CODE_SEARCH) {
            if (resultCode != RESULT_OK)
                return;
            LatLng location = mapboxMap.getCameraPosition().target;
            location.setLatitude(data.getDoubleExtra("lat", location.getLatitude()));
            location.setLongitude(data.getDoubleExtra("lng", location.getLongitude()));
            gotoPosition(location);
            dropMarker(location);
            showBookmarkModal(location);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initDroppedMarker(@NonNull Style loadedMapStyle) {
        // Add the marker image to map
//        loadedMapStyle.addImage("dropped-icon-image", BitmapFactory.decodeResource(
//                getResources(), R.drawable.blue_marker));

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.blue_marker);
        assert drawable != null;
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        loadedMapStyle.addImage("dropped-icon-image", bitmap);

        loadedMapStyle.addSource(new GeoJsonSource("dropped-marker-source-id"));
        loadedMapStyle.addLayer(new SymbolLayer(DROPPED_MARKER_LAYER_ID,
                "dropped-marker-source-id").withProperties(
                iconImage("dropped-icon-image"),
                visibility(NONE),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        ));
    }

    private void gotoPosition(LatLng location) {
        CameraPosition position = new CameraPosition.Builder()
                .target(location)
                .zoom(15)
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 3000);
    }

    private void dropMarker(LatLng location) {
        if (markerLocation != null)
            clearMarker();

        Style style = mapboxMap.getStyle();
        if (style == null || style.getLayer(DROPPED_MARKER_LAYER_ID) == null)
            return;
        GeoJsonSource source = style.getSourceAs("dropped-marker-source-id");
        if (source != null) {
            source.setGeoJson(Point.fromLngLat(location.getLongitude(), location.getLatitude()));
        }
        droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID);
        if (droppedMarkerLayer != null) {
            droppedMarkerLayer.setProperties(visibility(VISIBLE));
        }
        markerLocation = location;
    }

    private void clearMarker() {
        Style style = mapboxMap.getStyle();
        if (style == null || style.getLayer(DROPPED_MARKER_LAYER_ID) == null)
            return;
        droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID);
        if (droppedMarkerLayer != null) {
            droppedMarkerLayer.setProperties(visibility(NONE));
        }
        markerLocation = null;
    }

    private void showBookmarkModal(LatLng location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add bookmark");
        builder.setMessage(String.format(
                Locale.getDefault(),
                "Enter the name for (%.2f, %.2f)",
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
                clearMarker();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                clearMarker();
            }
        });

        builder.show();
    }

    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        mapboxMap.addOnMapClickListener(location -> {
            clearMarker();
            return true;
        });
        mapboxMap.addOnMapLongClickListener(location -> {
            gotoPosition(location);
            dropMarker(location);
            showBookmarkModal(location);
            return true;
        });
        mapboxMap.setStyle(isDark ? Style.DARK : Style.LIGHT, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                initDroppedMarker(style);
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