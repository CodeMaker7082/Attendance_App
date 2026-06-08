package com.goat.attendance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private MainBinding      binding;
    private StudentListAdapter adapter;
    private DataManager      dm;
    private boolean          uiInitialized = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("Student List");

        dm = DataManager.getInstance();

        setupUI();
        setupAdapter();
        uiInitialized = true;
        // onResume fires right after and refreshes the list.
    }

    /**
     * onResume refreshes the student list from the current in-memory state.
     * It does NOT call dm.loadAll() — in-memory data is always current because
     * every DataManager operation saves immediately.
     * loadIfNeeded() only loads from disk on process restart.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!uiInitialized) return;

        // Re-register the live-update callback cleared in onPause.
        DataManager.setRefreshCallback(() -> {
            if (adapter != null) adapter.notifyDataSetChanged();
        });

        // Only hits disk if the process restarted — otherwise a no-op.
        dm.loadIfNeeded();

        // Refresh display from in-memory state.
        dm.reloadFilterList();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        DataManager.setRefreshCallback(null);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupUI() {
        binding.Fab.setOnClickListener(v -> {
            DialogDialogFragmentActivity dialog = new DialogDialogFragmentActivity();
            dialog.setDialogListener(data -> dm.addStudent(data));
            dialog.show(getSupportFragmentManager(), "dialog");
        });
        
        applyBackground(binding.activityLayout, 0xFFCFD8DC);
        applyRounded(binding.titleLayout, 18, 0xFFB0BEC5);
        applyRounded(binding.searchview1, 999, 0xFFB0BEC5);
        binding.searchview1.setQueryHint("Enter a Place, Name, Number to Search Students");
        // EditText searchEditText = (EditText)binding.searchview1.findViewById(androidx.appcompat.R.id.search_src_text);
        binding.searchview1.setIconifiedByDefault(false); // Key line 1
        binding.searchview1.setIconified(false);          // Key line 2
        applyRounded(binding.listStudent, 12, 0xFFB0BEC5);
        binding.listStudent.setEmptyView(binding.emptyView);

        binding.searchview1.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query)    { dm.filterList(query); return true; }
            @Override public boolean onQueryTextChange(String newText)  { dm.filterList(newText); return true; }
        });
    }
    
    private void applyRounded(View view, int cornerRadius, int fillColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(cornerRadius); drawable.setColor(fillColor);
        view.setBackground(drawable);
    }
    
    private void applyBackground(View view, int fillColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        view.setBackground(drawable);
    }
    
    private void applyRounded(View view, int cornerRadius, int fillColor, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(cornerRadius); drawable.setColor(fillColor); drawable.setStroke(strokeWidth, strokeColor);
        view.setBackground(drawable);
    }

    private void setupAdapter() {
        dm.loadIfNeeded();

        if (dm.isStudentListEmpty()) {
            Bundle defaults = new Bundle();
            defaults.putString("name",  "Student");
            defaults.putString("place", "Chennai");
            defaults.putString("phone", "999999");
            dm.seedDefaultStudentIfEmpty(defaults);
        }

        dm.reloadFilterList();
        adapter = new StudentListAdapter(dm.getFilteredStudents());
        binding.listStudent.setAdapter(adapter);
    }

    // ── Popup Menu ────────────────────────────────────────────────────────────

    private void showFloatingMenu(int x, int y, int position) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_options, null);
        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        popupView.findViewById(R.id.edit)  .<TextView>findViewById(R.id.edit)  .setOnClickListener(v -> { popupWindow.dismiss(); dm.showToast("Edit clicked"); });
        popupView.findViewById(R.id.delete).setOnClickListener(v -> { popupWindow.dismiss(); dm.deleteStudent(position); });
        popupView.findViewById(R.id.qrcode).setOnClickListener(v -> {
            popupWindow.dismiss();
            Intent intent = new Intent(MainActivity.this, QrviewerActivity.class);
            intent.putExtra("qr_data", dm.getQrString(position));
            startActivity(intent);
        });

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupW  = popupView.getMeasuredWidth();
        int popupH  = popupView.getMeasuredHeight();
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int posX    = Math.max(20, x - popupW / 2);
        if (posX + popupW > screenW) posX = screenW - popupW - 20;
        int posY = (y - popupH - 50 < 0) ? y + 50 : y - popupH - 50;

        popupWindow.showAtLocation(binding.getRoot(), android.view.Gravity.NO_GRAVITY, posX, posY);
        popupView.setAlpha(0f); popupView.setScaleX(0.9f); popupView.setScaleY(0.9f);
        popupView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).start();
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    public class StudentListAdapter extends BaseAdapter {
        private final ArrayList<HashMap<String, Object>> data;
        public StudentListAdapter(ArrayList<HashMap<String, Object>> data) { this.data = data; }

        @Override public int    getCount()              { return data.size(); }
        @Override public Object getItem(int pos)        { return data.get(pos); }
        @Override public long   getItemId(int pos)      { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RowBinding row = RowBinding.inflate(getLayoutInflater());
            HashMap<String, Object> item = data.get(position);

            String name   = String.valueOf(item.get("name"));
            String place  = String.valueOf(item.get("place"));
            String number = String.valueOf(item.get("number"));

            row.userName.setText("null".equals(name)   ? "" : "Name: " + name);
            row.userPlc.setText ("null".equals(place)  ? "" : place);
            row.userNum.setText ("null".equals(number) ? "" : number);

            applyRounded(row.idBar, 33, 0xFFEEEEEE, 2, 0xFF212121);
            applyRounded(row.layoutItems, 18, 0xFFE0E0E0);
            applyRounded(row.userName, 18, 0xFFF5F5F5);
            applyRounded(row.dataHolder, 18, 0xFFF5F5F5);
            applyRounded(row.imgBack, 999, 0xFFE0E0E0, 2, 0xFF212121);

            row.indexTxt.setText(String.valueOf(position));

            row.getRoot().setOnLongClickListener(v -> {
                int[] loc = new int[2];
                v.getLocationOnScreen(loc);
                showFloatingMenu(loc[0] + v.getWidth() / 2, loc[1] + v.getHeight() / 2, position);
                return true;
            });
            return row.getRoot();
        }
        
        private void applyRounded(View view, int cornerRadius, int fillColor) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(cornerRadius); drawable.setColor(fillColor);
            view.setBackground(drawable);
        }
        
        private void applyBackground(View view, int fillColor) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(fillColor);
            view.setBackground(drawable);
        }
        
        private void applyRounded(View view, int cornerRadius, int fillColor, int strokeWidth, int strokeColor) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(cornerRadius); drawable.setColor(fillColor); drawable.setStroke(strokeWidth, strokeColor);
            view.setBackground(drawable);
        }
    }
}
