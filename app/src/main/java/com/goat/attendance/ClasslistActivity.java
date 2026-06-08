package com.goat.attendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;

public class ClasslistActivity extends AppCompatActivity {

    private ClasslistBinding binding;
    private DataManager dm;

    private final ArrayList<HashMap<String, Object>> studentAttendanceList = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> dates                 = new ArrayList<>();

    private DatePickAdapter         datePickAdapter;
    private LinearSnapHelper        snapHelper;
    private ClassListRecycleAdapter classAdapter;

    private int     selectedDayIndex = 0;
    private boolean uiInitialized    = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ClasslistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dm = DataManager.getInstance();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)  == PackageManager.PERMISSION_DENIED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                 Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        } else {
            setupUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) setupUI();
    }

    /**
     * onResume refreshes the display from the in-memory singleton state.
     * It does NOT call dm.loadAll() — DataManager is a singleton and all
     * changes (markAttendanceForDay, addDay, etc.) already saved to disk and
     * updated memory. Reloading from disk here would undo any in-memory
     * changes that hadn't been flushed yet, causing stale data.
     *
     * loadIfNeeded() is the only exception: it only fires if the process was
     * restarted (dataLoaded == false), e.g. app killed and reopened.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!uiInitialized) return;

        // Only reload from disk if this is after a process restart.
        dm.loadIfNeeded();

        // Refresh the display from current in-memory state — no disk read.
        refreshDisplay();
    }

    // ── One-time UI setup ─────────────────────────────────────────────────────

    private void setupUI() {
        dm.loadIfNeeded(); // ensure data is present (e.g. if activity launched standalone)
        setupClassListRecycler();
        setupDatePickerRecycler();
        uiInitialized = true;
        // onResume fires immediately after, calling refreshDisplay().
    }

    // ── RecyclerView setup ────────────────────────────────────────────────────

    private void setupClassListRecycler() {
        binding.classListRecycle.setLayoutManager(new LinearLayoutManager(this));
        classAdapter = new ClassListRecycleAdapter(studentAttendanceList);
        binding.classListRecycle.setAdapter(classAdapter);
    }

    private void setupDatePickerRecycler() {
        binding.datePick.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.datePick.setClipToPadding(false);

        datePickAdapter = new DatePickAdapter(dates);
        binding.datePick.setAdapter(datePickAdapter);

        binding.datePick.post(() -> {
            int rvWidth  = binding.datePick.getWidth();
            int rvHeight = binding.datePick.getHeight();
            DatePickAdapter.ITEM_WIDTH = rvWidth / 5;
            DatePickAdapter.TEXT_SIZE  = Math.min(rvHeight, DatePickAdapter.ITEM_WIDTH) * 0.5f;
            int padding = (rvWidth - DatePickAdapter.ITEM_WIDTH) / 2;
            binding.datePick.setPadding(padding, 0, padding, 0);
            datePickAdapter.notifyDataSetChanged();
        });

        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(binding.datePick);

        binding.datePick.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                int centerX = rv.getWidth() / 2;
                for (int i = 0; i < rv.getChildCount(); i++) {
                    View child    = rv.getChildAt(i);
                    float center  = (child.getLeft() + child.getRight()) / 2f;
                    float factor  = Math.max(0.75f, 1f - (Math.abs(centerX - center) / centerX));
                    float scale   = 0.85f + factor * 0.25f;
                    child.setScaleX(scale);
                    child.setScaleY(scale);
                    child.setAlpha(0.4f + factor * 0.6f);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView rv, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return;
                View snap = snapHelper.findSnapView(rv.getLayoutManager());
                if (snap == null) return;
                int newIndex = rv.getChildAdapterPosition(snap);
                if (newIndex == selectedDayIndex) return;
                selectedDayIndex = newIndex;
                datePickAdapter.setSelectedIndex(selectedDayIndex);
                rebuildAttendanceList();
                classAdapter.notifyDataSetChanged();
                datePickAdapter.notifyDataSetChanged();
            }
        });
    }

    // ── Display refresh ───────────────────────────────────────────────────────

    private void refreshDisplay() {
        rebuildDayList();
        clampSelectedDayIndex();
        datePickAdapter.setSelectedIndex(selectedDayIndex);
        rebuildAttendanceList();
        classAdapter.notifyDataSetChanged();
        datePickAdapter.notifyDataSetChanged();
        binding.datePick.post(() -> binding.datePick.scrollToPosition(selectedDayIndex));
        Toast.makeText(this, "Students: " + studentAttendanceList.size(), Toast.LENGTH_SHORT).show();
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    private void rebuildDayList() {
        dates.clear();
        for (int i = 1; i <= dm.getTotalDays(); i++) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("date", String.format("Day %02d", i));
            dates.add(map);
        }
    }

    private void clampSelectedDayIndex() {
        int total = dm.getTotalDays();
        if (total == 0)                   selectedDayIndex = 0;
        else if (selectedDayIndex >= total) selectedDayIndex = total - 1;
    }

    private void rebuildAttendanceList() {
        studentAttendanceList.clear();
        if (dm.getTotalDays() == 0) return;

        ArrayList<String> names  = dm.getStudentNames();
        String            colJson = dm.getColumnAttendance(selectedDayIndex);
        try {
            JSONArray vals = new JSONArray(colJson);
            for (int i = 0; i < names.size(); i++) {
                HashMap<String, Object> entry = new HashMap<>();
                entry.put("name",      names.get(i));
                entry.put("isPresent", "1".equals(vals.optString(i, "0")));
                studentAttendanceList.add(entry);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    public class ClassListRecycleAdapter
            extends RecyclerView.Adapter<ClassListRecycleAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> data;

        public ClassListRecycleAdapter(ArrayList<HashMap<String, Object>> data) { this.data = data; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.class_attendance, null);
            v.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ClassAttendanceBinding rowBinding = ClassAttendanceBinding.bind(holder.itemView);
            HashMap<String, Object> item      = data.get(position);
            String  name      = item.get("name").toString();
            boolean isPresent = Boolean.TRUE.equals(item.get("isPresent"));

            rowBinding.studentName.setText(name);
            rowBinding.attendanceCheckbox.setOnCheckedChangeListener(null);
            rowBinding.attendanceCheckbox.setChecked(isPresent);
            rowBinding.attendanceCheckbox.setOnCheckedChangeListener(
                    (CompoundButton button, boolean checked) -> {
                        if (!button.isPressed()) return;
                        // Update local map so the checkbox stays correct if RecyclerView rebinds.
                        item.put("isPresent", checked);
                        // markAttendanceForDay saves to disk internally — rule: save when data changes.
                        dm.markAttendanceForDay(name, checked, selectedDayIndex);
                    });
        }

        @Override public int getItemCount() { return data.size(); }
        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) { super(v); }
        }
    }
}
