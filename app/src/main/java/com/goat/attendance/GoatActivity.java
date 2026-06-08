package com.goat.attendance;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class GoatActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 1001;

    private GoatBinding binding;
    private DataManager dm;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = GoatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dm = DataManager.getInstance();
        dm.setContext(getApplicationContext());
        dm.start(); // initialise TableAPI once

        setupButtons();
        applyStyles();
        checkSmsPermission();

        // Load from disk ONCE at app start.
        // All subsequent data changes save immediately (in DataManager methods),
        // so the in-memory singleton is always the source of truth — no reload
        // in onResume needed.
        dm.loadAll();
    }

    // onResume intentionally does NOT call dm.loadAll().
    // Reason: DataManager is a singleton. Every change (addDay, markAttendance,
    // addStudent, etc.) saves to disk immediately AND keeps memory current.
    // Reloading from disk in onResume would overwrite the correct in-memory
    // state with whatever was last written to disk — which was the root cause
    // of stale data appearing on the second visit to TableActivity.

    // ── Buttons ───────────────────────────────────────────────────────────────

    private void setupButtons() {
        setSupportActionBar(binding.Toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        binding.Toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // QR scan — mark student present on the current day
        binding.materialbutton1.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan Student QR");
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });

        // Student list
        binding.materialbutton2.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        // View attendance table.
        // attendanceJson is always up to date — every change in DataManager
        // calls saveAttendance() which updates it. No extra save needed here.
        binding.materialbutton3.setOnClickListener(v -> {
            Intent intent = new Intent(this, TableActivity.class);
            intent.putExtra("jsondata", dm.getAttendanceJson());
            startActivity(intent);
        });

        // Class list (per-day attendance)
        binding.materialbutton4.setOnClickListener(v ->
                startActivity(new Intent(this, ClasslistActivity.class)));

        // Add day — saves to disk inside addDay()
        binding.button1.setOnClickListener(v -> dm.addDay());

        // Remove last day (requires checkbox confirmation)
        binding.button2.setOnClickListener(v -> showRemoveDayDialog());

        // Generate QR codes
        binding.button3.setOnClickListener(v -> dm.generateQrForAllStudents());

        // Send SMS
        binding.button4.setOnClickListener(v -> dm.sendAttendanceSms());
    }

    // ── Remove Day Dialog ─────────────────────────────────────────────────────

    private void showRemoveDayDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView message = new TextView(this);
        message.setText("Are you sure you want to remove the last class day?");

        CheckBox confirmCheck = new CheckBox(this);
        confirmCheck.setText("Delete data permanently");

        layout.addView(message);
        layout.addView(confirmCheck);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Remove Class Day?")
                .setView(layout)
                .setPositiveButton("Yes", null)
                .setNegativeButton("No", null)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (confirmCheck.isChecked()) {
                dm.removeLastDay(); // saves to disk inside removeLastDay()
                dialog.dismiss();
            } else {
                dm.showToast("Please check the box to confirm deletion");
            }
        });
    }

    // ── QR Result ─────────────────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null) return;
        String scanned = result.getContents();
        if (scanned != null) {
            dm.showToast("Scanned: " + scanned);
            dm.processAttendanceQr(scanned); // marks present + saves inside
        } else {
            dm.showToast("Scan cancelled");
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    private void applyStyles() {
        binding.Toolbar.setTitleTextColor(Color.BLACK);

        applyRoundedStyle(binding.backgroundVard, 24,  1, 0xFF212121, 0xFFE0E0E0);
        applyRoundedStyle(binding.textview1,      32,  1, 0xFF212121,
                getResources().getColor(R.color.colorControlHighlight));
        applyRoundedStyle(binding.linear2,        999, 1, 0xFF212121, 0xFFEEEEEE);
        applyRoundedStyle(binding.linear4,        999, 1, 0xFF212121, 0xFFEEEEEE);

        int btnColor  = 0xFFBDBDBD;
        int textBlack = 0xFF000000;

        View[] buttons = {
            binding.materialbutton1, binding.materialbutton2,
            binding.materialbutton3, binding.materialbutton4,
            binding.button1, binding.button2,
            binding.button3, binding.button4
        };
        for (View btn : buttons) {
            applyRoundedStyle(btn, 999, 1, 0xFF212121, btnColor);
            if (btn instanceof TextView) ((TextView) btn).setTextColor(textBlack);
        }
    }

    private void applyRoundedStyle(View view, int cornerRadius, int strokeWidth,
                                   int strokeColor, int fillColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(cornerRadius);
        bg.setStroke(strokeWidth, strokeColor);
        bg.setColor(fillColor);
        view.setBackground(bg);
    }
}
