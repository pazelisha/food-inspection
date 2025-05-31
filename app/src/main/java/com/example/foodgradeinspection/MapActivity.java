package com.example.foodgradeinspection;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView tasksRecycler;
    private TaskAdapter taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Log.d(TAG, "onCreate: Activity started");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Log.d(TAG, "onCreate: Firebase initialized");
        Log.d(TAG, "onCreate: Current user: " + (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "null"));

        // Setup Map Fragment
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            Log.d(TAG, "onCreate: Map fragment found, requesting map");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "onCreate: Map fragment is null!");
        }

        // Setup RecyclerView for tasks
        tasksRecycler = findViewById(R.id.tasks_recycler);
        tasksRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        taskAdapter = new TaskAdapter(new ArrayList<>(), this::onTaskClicked);
        tasksRecycler.setAdapter(taskAdapter);

        loadTasks();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;
        loadLocations();
    }

    private void loadLocations() {
        Log.d(TAG, "loadLocations: Starting to load locations from Firestore");

        // Check authentication status
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "loadLocations: User is not authenticated!");
            return;
        } else {
            Log.d(TAG, "loadLocations: User is authenticated: " + mAuth.getCurrentUser().getUid());
        }

        db.collection("locations")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "loadLocations: Error listening to locations", e);
                            return;
                        }

                        if (snapshots == null) {
                            Log.w(TAG, "loadLocations: Snapshots is null");
                            return;
                        }

                        Log.d(TAG, "loadLocations: Received " + snapshots.size() + " documents");

                        if (snapshots.isEmpty()) {
                            Log.w(TAG, "loadLocations: No locations found in database");
                            return;
                        }

                        mMap.clear();
                        int markersAdded = 0;

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Log.d(TAG, "loadLocations: Processing document: " + doc.getId());

                            try {
                                Double latObj = doc.getDouble("lat");
                                Double lngObj = doc.getDouble("lng");
                                String status = doc.getString("status");
                                Double ratingObj = doc.getDouble("healthRating");
                                String name = doc.getString("name");

                                // Check for null values
                                if (latObj == null || lngObj == null) {
                                    Log.w(TAG, "loadLocations: Document " + doc.getId() + " missing lat/lng coordinates");
                                    continue;
                                }

                                double lat = latObj;
                                double lng = lngObj;
                                double rating = ratingObj != null ? ratingObj : 0.0;

                                Log.d(TAG, "loadLocations: Adding marker - Name: " + name +
                                        ", Lat: " + lat + ", Lng: " + lng +
                                        ", Status: " + status + ", Rating: " + rating);

                                LatLng pos = new LatLng(lat, lng);
                                float hue = status != null && status.equals("open")
                                        ? BitmapDescriptorFactory.HUE_AZURE
                                        : getColorForRating(rating);

                                mMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .title(name != null ? name : "Unknown Location")
                                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));

                                markersAdded++;

                            } catch (Exception ex) {
                                Log.e(TAG, "loadLocations: Error processing document " + doc.getId(), ex);
                            }
                        }

                        Log.d(TAG, "loadLocations: Successfully added " + markersAdded + " markers to map");

                        // Optional: move camera to first location
                        if (!snapshots.isEmpty()) {
                            try {
                                DocumentSnapshot first = snapshots.getDocuments().get(0);
                                Double latObj = first.getDouble("lat");
                                Double lngObj = first.getDouble("lng");

                                if (latObj != null && lngObj != null) {
                                    LatLng pos = new LatLng(latObj, lngObj);
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 12f));
                                    Log.d(TAG, "loadLocations: Camera moved to first location");
                                } else {
                                    Log.w(TAG, "loadLocations: First document missing coordinates for camera move");
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "loadLocations: Error moving camera", ex);
                            }
                        }
                    }
                });
    }

    private float getColorForRating(double rating) {
        // scale 1.0–5.0 onto 0° (red)–120° (green)
        float color = (float) ((rating - 1) / 4 * 120);
        Log.d(TAG, "getColorForRating: Rating " + rating + " -> Color hue " + color);
        return color;
    }

    private void loadTasks() {
        String uid = mAuth.getUid();
        Log.d(TAG, "loadTasks: Loading tasks for user: " + uid);

        if (uid == null) {
            Log.w(TAG, "loadTasks: User ID is null, cannot load tasks");
            return;
        }

        db.collection("tasks")
                .whereEqualTo("inspectorId", uid)
                .whereEqualTo("status", "open")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "loadTasks: Error listening to tasks", e);
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "loadTasks: Snapshots is null");
                        return;
                    }

                    Log.d(TAG, "loadTasks: Received " + snapshots.size() + " task documents");

                    List<Task> list = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        try {
                            Task t = doc.toObject(Task.class);
                            if (t != null) {
                                t.setId(doc.getId());

                                // Fetch restaurant name from 'locations' collection
                                String locationId = t.getLocationId();
                                db.collection("locations").document(locationId).get()
                                        .addOnSuccessListener(locationDoc -> {
                                            if (locationDoc.exists()) {
                                                t.setLocationName(locationDoc.getString("name"));
                                            } else {
                                                t.setLocationName("Unknown Location");
                                            }

                                            list.add(t);
                                            taskAdapter.updateTasks(new ArrayList<>(list));  // Update adapter after each task is ready
                                        });
                            } else {
                                Log.w(TAG, "loadTasks: Failed to convert document to Task: " + doc.getId());
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "loadTasks: Error processing task document " + doc.getId(), ex);
                        }
                    }
                });
    }


    private void onTaskClicked(Task task) {
        Log.d(TAG, "onTaskClicked: Task clicked: " + task.getId());
        /*
        Intent i = new Intent(this, InspectionFormActivity.class);
        i.putExtra("taskId", task.getId());
        i.putExtra("locationId", task.getLocationId());
        startActivity(i);*/
    }
}