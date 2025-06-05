package com.example.foodgradeinspection;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Marker-tap menu version:
 *      – Tap restaurant → dialog:
 *            • Open Latest Report
 *            • History  (if ≥2 completed)
 *            • Cancel
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /* ------------------------------------------------------------------ */
    private static final String TAG = "MapActivity";

    private FirebaseFirestore db;
    private FirebaseAuth       mAuth;
    private GoogleMap          mMap;

    private RecyclerView tasksRecycler;
    private TaskAdapter taskAdapter;
    private ImageButton filterButton, refreshButton;

    private String currentFilter = "open";
    /* ------------------------------------------------------------------ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        tasksRecycler = findViewById(R.id.tasks_recycler);
        tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(new ArrayList<>(), this::onTaskClicked);
        tasksRecycler.setAdapter(taskAdapter);

        filterButton  = findViewById(R.id.filter_button);
        refreshButton = findViewById(R.id.refresh_button);

        filterButton.setOnClickListener(v -> showFilterMenu());
        refreshButton.setOnClickListener(v -> loadTasks());

        loadTasks();
    }

    /* ---------------- Google Map ready -------------------------------- */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof String) fetchReportsMenu((String) tag);
            return true;            // consume the click
        });

        loadLocations();
    }

    /* ---------------- Load restaurant markers ------------------------- */
    private void loadLocations() {
        db.collection("locations").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) { Log.e(TAG, "loadLocations", e); return; }

            mMap.clear();
            LatLng first = null;

            for (DocumentSnapshot doc : snap) {
                Double lat = doc.getDouble("lat");
                Double lng = doc.getDouble("lng");
                if (lat == null || lng == null) continue;

                LatLng pos = new LatLng(lat, lng);
                if (first == null) first = pos;

                String name   = doc.getString("name");
                String status = doc.getString("status");
                String rating = doc.getString("healthRating");

                BitmapDescriptor icon = "open".equalsIgnoreCase(status)
                        ? BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        : createCustomMarkerWithLetter(rating != null ? rating : "N");

                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(name != null ? name : "Unknown")
                        .icon(icon));

                if (m != null) m.setTag(doc.getId());
            }
            if (first != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 12f));
        });
    }

    /* ---------------- Marker-tap dialog -------------------------------- */
    private void fetchReportsMenu(String locationId) {
        db.collection("tasks")
                .whereEqualTo("locationId", locationId)
                .whereEqualTo("status", "completed")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(this::buildReportMenu)
                .addOnFailureListener(err ->
                        Toast.makeText(this, "Error fetching tasks", Toast.LENGTH_SHORT).show());
    }

    private void buildReportMenu(QuerySnapshot snap) {
        if (snap.isEmpty()) {
            Toast.makeText(this, "No completed tasks", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Task> history = new ArrayList<>();
        for (DocumentSnapshot d : snap) {
            Task t = d.toObject(Task.class);
            if (t != null) { t.setId(d.getId()); history.add(t); }
        }
        Task latest = history.get(0);

        List<String> opts = new ArrayList<>();
        opts.add("Open Latest Report");
        if (history.size() > 1) opts.add("History");
        opts.add("Cancel");

        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(opts.toArray(new CharSequence[0]), (dlg, which) -> {
                    String choice = opts.get(which);
                    if ("Open Latest Report".equals(choice)) openInspectionForm(latest);
                    else if ("History".equals(choice))      showHistoryDialog(history);
                })
                .show();
    }

    private void showHistoryDialog(List<Task> history) {
        CharSequence[] labels = new CharSequence[history.size()];
        for (int i = 0; i < history.size(); i++) {
            Date d = history.get(i).getCreatedAtAsDate();
            labels[i] = android.text.format.DateFormat
                    .format("MMM d, yyyy HH:mm", d);
        }
        new AlertDialog.Builder(this)
                .setTitle("Inspection History")
                .setItems(labels,
                        (dlg, which) -> openInspectionForm(history.get(which)))
                .setNegativeButton("Close", null)
                .show();
    }

    /* ---------------- Task list, filter menu --------------------------- */
    private void showFilterMenu() {
        PopupMenu p = new PopupMenu(this, filterButton);
        p.getMenuInflater().inflate(R.menu.filter_menu, p.getMenu());
        p.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.filter_open)      currentFilter = "open";
            else if (id == R.id.filter_completed) currentFilter = "completed";
            else                                  currentFilter = "all";
            loadTasks();
            return true;
        });
        p.show();
    }

    private void loadTasks() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        Query q = db.collection("tasks").whereEqualTo("inspectorId", uid);
        if (!"all".equals(currentFilter)) q = q.whereEqualTo("status", currentFilter);

        q.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            if (snap.isEmpty()) { taskAdapter.updateTasks(new ArrayList<>()); return; }

            List<Task> out = new ArrayList<>();
            int total = snap.size();
            int[] processed = {0};

            for (DocumentSnapshot d : snap) {
                Task t = d.toObject(Task.class);
                if (t == null) { processed[0]++; continue; }
                t.setId(d.getId());

                String locId = t.getLocationId();
                if (locId != null && !locId.isEmpty()) {
                    db.collection("locations").document(locId).get()
                            .addOnSuccessListener(l -> {
                                t.setLocationName(l.exists()
                                        ? l.getString("name") : "Unknown Location");
                                syncAdd(out, t, processed, total);
                            })
                            .addOnFailureListener(err -> {
                                t.setLocationName("Unknown Location");
                                syncAdd(out, t, processed, total);
                            });
                } else {
                    t.setLocationName("Unknown Location");
                    syncAdd(out, t, processed, total);
                }
            }
        });
    }

    private void syncAdd(List<Task> list, Task t, int[] processed, int total) {
        synchronized (list) {
            list.add(t); processed[0]++;
            if (processed[0] == total) taskAdapter.updateTasks(new ArrayList<>(list));
        }
    }

    /* ---------------- Task card click → pan + open --------------------- */
    private void onTaskClicked(Task task) {
        if (mMap == null) { openInspectionForm(task); return; }

        String locId = task.getLocationId();
        if (locId == null || locId.isEmpty()) { openInspectionForm(task); return; }

        db.collection("locations").document(locId).get()
                .addOnSuccessListener(l -> {
                    Double lat = l.getDouble("lat"), lng = l.getDouble("lng");
                    if (lat == null || lng == null) { openInspectionForm(task); return; }

                    LatLng tgt = new LatLng(lat, lng);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(tgt, 16f), 1000,
                            new GoogleMap.CancelableCallback() {
                                @Override public void onFinish() {
                                    new Handler(Looper.getMainLooper())
                                            .postDelayed(() -> openInspectionForm(task), 750);
                                }
                                @Override public void onCancel() {
                                    openInspectionForm(task);
                                }
                            });
                })
                .addOnFailureListener(err -> openInspectionForm(task));
    }

    private void openInspectionForm(Task task) {
        Intent i = new Intent(this, InspectionFormActivity.class);
        i.putExtra("taskId", task.getId());
        i.putExtra("locationId", task.getLocationId());
        startActivity(i);
    }

    /* ---------------- Drawing custom grade marker ---------------------- */
    private BitmapDescriptor createCustomMarkerWithLetter(String rating) {
        float hue = getColorForRating(rating);
        float[] hsv = "N".equalsIgnoreCase(rating)
                ? new float[]{0f, 0f, 0.7f}
                : new float[]{hue, 1f, 0.85f};
        int pinColor = Color.HSVToColor(hsv);

        int size = 100;
        float headR   = size * 0.38f;
        float headCX  = size / 2f;
        float headCY  = size * 0.02f + headR;
        float tailH   = size * 0.45f;
        float neckR   = 0.75f;

        float tipY = headCY + headR + tailH;
        int bmpH   = (int) Math.ceil(tipY + size * 0.05f);
        tipY       = bmpH - size * 0.05f;

        Bitmap bmp = Bitmap.createBitmap(size, bmpH, Bitmap.Config.ARGB_8888);
        Canvas cvs = new Canvas(bmp);

        // body
        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setStyle(Paint.Style.FILL); body.setColor(pinColor);

        Path pin = new Path();
        RectF head = new RectF(headCX - headR, headCY - headR, headCX + headR, headCY + headR);

        float neckDX = headR * neckR;
        float neckDY = (float) Math.sqrt(headR * headR - neckDX * neckDX);
        float neckY  = headCY + neckDY;
        float leftNX = headCX - neckDX;
        float rightNX= headCX + neckDX;

        pin.moveTo(headCX, tipY);
        pin.lineTo(leftNX, neckY);

        float startDeg = (float) Math.toDegrees(Math.atan2(neckY - headCY, leftNX  - headCX));
        float endDeg   = (float) Math.toDegrees(Math.atan2(neckY - headCY, rightNX - headCX));
        float sweep    = endDeg - startDeg; if (sweep <= 0) sweep += 360;

        pin.arcTo(head, startDeg, sweep, false);
        pin.close();
        cvs.drawPath(pin, body);

        // letter
        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        txt.setColor(Color.BLACK);
        txt.setTextSize(size * 0.4f);
        txt.setFakeBoldText(true);
        txt.setTextAlign(Paint.Align.CENTER);
        cvs.drawText(rating.toUpperCase(),
                headCX,
                headCY - (txt.ascent() + txt.descent()) / 2f,
                txt);

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private float getColorForRating(String r) {
        switch (r.toUpperCase()) {
            case "A": return 120f;
            case "B": return  90f;
            case "C": return  45f;
            case "D": return   0f;
            default:  return 240f;   // N / unknown
        }
    }
}
