package com.example.realtimetrackingsystem;

import static android.app.ProgressDialog.show;

import static androidx.concurrent.futures.ResolvableFuture.create;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.ViewAnnotationOptions;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImpl;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;
import com.mapbox.maps.viewannotation.ViewAnnotationManager;
import com.mapbox.maps.viewannotation.ViewAnnotationOptionsKtxKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/** @noinspection deprecation*/
public class tracking extends AppCompatActivity {
    private ValueEventListener locationUpdateListener;

    MapView mapView;
    FloatingActionButton floatingActionButton;
    private TextView busNumberTextView;
    private TextView busNameTextView;
    private ImageView backicon;
    private FirebaseFirestore db;
    private static final String TAG = "tracking";
private  String driverMobileNumber;

    private PointAnnotationManager pointAnnotationManager;
    private PointAnnotation pointAnnotation;



    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override

        public void onActivityResult(Boolean result) {
            if (result) {
                Toast.makeText(tracking.this, "Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(tracking.this, "Permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    });
    private final OnIndicatorBearingChangedListener OnIndicatorBearingChangedListener = new OnIndicatorBearingChangedListener() {
        @Override
        public void onIndicatorBearingChanged(double v) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder().bearing(v).build());

        }
    };

    private final OnIndicatorPositionChangedListener OnIndicatorPositionChangedListener = new OnIndicatorPositionChangedListener() {
        @Override
        public void onIndicatorPositionChanged(@NonNull Point point) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder().center(point).zoom(7.0).build());
            getGestures(mapView).setFocalPoint(mapView.getMapboxMap().pixelForCoordinate(point));
        }
    };
    private final OnMoveListener OnMoveListener = new OnMoveListener() {
        @Override
        public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {
            getLocationComponent(mapView).removeOnIndicatorPositionChangedListener(OnIndicatorPositionChangedListener);
            getLocationComponent(mapView).removeOnIndicatorBearingChangedListener(OnIndicatorBearingChangedListener);
            getGestures(mapView).removeOnMoveListener(OnMoveListener);
            floatingActionButton.show();
        }

        @Override
        public boolean onMove(@NonNull MoveGestureDetector moveGestureDetector) {
            return true;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        mapView = findViewById(R.id.mapView);
        busNumberTextView = findViewById(R.id.busnumber);
        busNameTextView = findViewById(R.id.busname);
        backicon = findViewById(R.id.back_icon);
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        String BusNumber = intent.getStringExtra("busnumber");
        String BusName = intent.getStringExtra("busname");

        busNumberTextView.setText(BusNumber);
        busNameTextView.setText(BusName);

        backicon.setOnClickListener(view -> finish());
        floatingActionButton = findViewById(R.id.focusLocation);
        floatingActionButton.hide();
        if (ContextCompat.checkSelfPermission(tracking.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (savedInstanceState == null) {


            mapView.getMapboxMap().loadStyleUri("mapbox://styles/utk123/clotvoloa00tr01pr1zso69u8", new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    mapView.getMapboxMap().setCamera(new CameraOptions.Builder().zoom(7.0).build());


                    LocationComponentPlugin locationComponentPlugin = getLocationComponent(mapView);
                    locationComponentPlugin.setEnabled(true);
                    LocationPuck2D locationPuck2D = new LocationPuck2D();
                    locationPuck2D.setBearingImage(AppCompatResources.getDrawable(tracking.this, R.drawable.baseline_circle_24));
                    locationComponentPlugin.setLocationPuck(locationPuck2D);
                    locationComponentPlugin.addOnIndicatorPositionChangedListener(OnIndicatorPositionChangedListener);
                    locationComponentPlugin.removeOnIndicatorBearingChangedListener(OnIndicatorBearingChangedListener);
                    getGestures(mapView).addOnMoveListener(OnMoveListener);

                    floatingActionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            getGestures(mapView).addOnMoveListener(OnMoveListener);
                            floatingActionButton.hide();
                        }
                    });
                }
            });
        }
        setupRealTimeJourneyDetails(BusNumber);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start listening for real-time location updates
// Start listening for real-time location updates
        if (driverMobileNumber != null) {
            setupRealTimeLocation(driverMobileNumber);
        }
    }

    protected void onStop() {
        super.onStop();
        // Stop listening for real-time location updates
        removeLocationUpdateListener();
    }



    private void removeLocationUpdateListener() {
        // Remove the ValueEventListener to stop listening for updates
        if (locationUpdateListener != null) {
            DatabaseReference locationsRef = FirebaseDatabase.getInstance().getReference("locations");
            locationsRef.child(driverMobileNumber).removeEventListener(locationUpdateListener);
        }
    }


    private boolean hasLoggedDetails = false;
    private void setupRealTimeJourneyDetails(String busnumber) {
        db.collection("Localbuses")
                .whereEqualTo("busNumber", busnumber)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Handle error
                        return;
                    }
                    if (value != null && !value.isEmpty()) {
                        // Assuming there is only one document with the specified bus number
                        Map<String, Object> data = value.getDocuments().get(0).getData();
                        if (data != null) {
                            processJourneyDetailsWithCoordinates(data);
                        }
                    }
                });
    }
    private void processJourneyDetailsWithCoordinates(Map<String, Object> data) {
        // Extract other details as needed
        String busNumber = (String) data.get("busNumber");
         driverMobileNumber = (String) data.get("driverMobileNumber");
        setupRealTimeLocation(driverMobileNumber);

        // Extract stations information
        List<Map<String, Object>> stations = (List<Map<String, Object>>) data.get("Stations");
        if (stations != null && !stations.isEmpty()) {
            int n = stations.size();
            Map<String, Object> station = stations.get(0);
            int i;
            for (i = 0; i < n; i++) {
                station = stations.get(i);
                double latitude = Double.parseDouble((String) station.get("latitude"));
                double longitude = Double.parseDouble((String) station.get("longitude"));
                String placeName = (String) station.get("placeName");
                Log.d(TAG, "Place Name: " + placeName);
                Log.d(TAG, "Latitude: " + latitude);
                Log.d(TAG, "Longitude: " + longitude);
                Point addpoint = Point.fromLngLat(longitude, latitude);
                if (i == 0 || i == n - 1) {
                    sourcepoint(addpoint);
                } else {
                    AddMarker(addpoint);
                }
            }
        }
    }
    private void AddMarker(Point point) {
        AnnotationPlugin annotationApi = AnnotationPluginImplKt.getAnnotations(mapView);
        CircleAnnotationManager circleAnnotationManager = CircleAnnotationManagerKt.createCircleAnnotationManager(annotationApi, new AnnotationConfig());
        CircleAnnotationOptions circleAnnotationOptions = new CircleAnnotationOptions()
                .withPoint(point)
                .withCircleRadius(7.0)
                .withCircleColor("#ee4e8b")
                .withCircleStrokeWidth(1.0)
                .withDraggable(false)
                .withCircleStrokeColor("#ffffff");
        circleAnnotationManager.create(circleAnnotationOptions);
    }
    private void sourcepoint(Point point)
    {
        Bitmap myLogo = ((BitmapDrawable)getResources().getDrawable(R.drawable.destination)).getBitmap();
        AnnotationPlugin annotationApi = AnnotationPluginImplKt.getAnnotations(mapView);
        PointAnnotationManager pointAnnotationManager= PointAnnotationManagerKt.createPointAnnotationManager(annotationApi,new AnnotationConfig());
        PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(myLogo)
                .withIconSize(1)
                .withDraggable(false);
        pointAnnotationManager.create(pointAnnotationOptions);
    }
    private void setupRealTimeLocation(String driverNumber) {
        DatabaseReference locationsRef = FirebaseDatabase.getInstance().getReference("locations");

        locationUpdateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                    if (data != null) {
                        processLocationUpdate(data);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
                Log.e(TAG, "Failed to read value.", error.toException());
            }
        };

        locationsRef.child(driverNumber).addValueEventListener(locationUpdateListener);
    }

    private void processLocationUpdate(Map<String, Object> data) {
        String deviceDate = (String) data.get("deviceDate");
        String deviceTime = (String) data.get("deviceTime");
        double latitude = ((Number) data.get("latitude")).doubleValue();
        double longitude = ((Number) data.get("longitude")).doubleValue();


        Point currentLocation = Point.fromLngLat(longitude, latitude);
        // Add a marker at the current location
        realMarker(currentLocation);
        ;
        // Now you can use latitude, longitude, deviceDate, and deviceTime as needed
        // For example, you can print them or perform other actions
        Log.d(TAG, "Device Date: " + deviceDate);
        Log.d(TAG, "Device Time: " + deviceTime);
        Log.d(TAG, "Latitude: " + latitude);
        Log.d(TAG, "Longitude: " + longitude);
    }
    private void realMarker(Point point) {
        Bitmap myLogo = ((BitmapDrawable)getResources().getDrawable(R.drawable.buslivelocation)).getBitmap();
        AnnotationPlugin annotationApi = AnnotationPluginImplKt.getAnnotations(mapView);

        // Create PointAnnotationManager if not already created
        if (pointAnnotationManager == null) {
            pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationApi, new AnnotationConfig());
        }

        // Remove previous marker if exists
        if (pointAnnotation != null) {
            pointAnnotationManager.delete(pointAnnotation);
        }

        // Create new PointAnnotationOptions
        PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(myLogo)
                .withIconSize(1)
                .withDraggable(false);

        // Create or update the PointAnnotation
        pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions);
    }




}


