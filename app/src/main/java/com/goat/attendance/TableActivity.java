package com.goat.attendance;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class TableActivity extends AppCompatActivity {

    TableLayout headerTable, bodyTable;
    HorizontalScrollView headerScroll, bodyScroll;

    final int COL_WIDTH  = 40;
    final int ROW_HEIGHT = 18;

    String jsonData = "{"
        + "\"days\":[\"Day 1\",\"Day 2\",\"Day 3\",\"Day 4\",\"Day 5\"],"
        + "\"students\":["
        + "{\"name\":\"Alice\", \"attendance\":[1,1,0,1,1]},"
        + "{\"name\":\"Bob\",   \"attendance\":[1,0,0,1,1]},"
        + "{\"name\":\"Carol\", \"attendance\":[0,1,1,1,0]},"
        + "{\"name\":\"David\", \"attendance\":[1,1,1,1,1]}"
        + "]}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.table);

        headerTable  = findViewById(R.id.headerTable);
        bodyTable    = findViewById(R.id.bodyTable);
        headerScroll = findViewById(R.id.headerScroll);
        bodyScroll   = findViewById(R.id.bodyScroll);

        String extra = getIntent().getStringExtra("jsondata");
        if (extra != null) jsonData = extra;

        // Sync header scroll with body scroll
        bodyScroll.getViewTreeObserver().addOnScrollChangedListener(() ->
            headerScroll.scrollTo(bodyScroll.getScrollX(), 0));

        loadTable();
    }

    private void loadTable() {
        try {
            JSONObject root     = new JSONObject(jsonData);
            JSONArray  days     = root.getJSONArray("days");
            JSONArray  students = root.getJSONArray("students");

            // ── Header row ───────────────────────────────────────
            TableRow headerRow = new TableRow(this);
            headerRow.setBackgroundColor(Color.parseColor("#1565C0"));

            headerRow.addView(makeNameCell("Name", true, Color.WHITE));

            for (int d = 0; d < days.length(); d++) {
                headerRow.addView(makeFixedCell(days.getString(d), COL_WIDTH, true, Color.WHITE));
            }

            headerRow.addView(makeFixedCell("%", COL_WIDTH, true, Color.WHITE));

            headerTable.addView(headerRow);

            // ── Data rows ────────────────────────────────────────
            for (int i = 0; i < students.length(); i++) {
                JSONObject student    = students.getJSONObject(i);
                JSONArray  attendance = student.getJSONArray("attendance");

                TableRow row = new TableRow(this);
                row.setBackgroundColor(i % 2 == 0
                    ? Color.WHITE
                    : Color.parseColor("#E3F2FD"));

                // Name cell — WRAP_CONTENT width
                row.addView(makeNameCell(student.getString("name"), false, Color.BLACK));

                // P / A cells
                int presentCount = 0;
                for (int d = 0; d < attendance.length(); d++) {
                    int val = attendance.getInt(d);
                    if (val == 1) presentCount++;

                    String label     = (val == 1) ? "P" : "A";
                    int    textColor = (val == 1)
                        ? Color.parseColor("#2E7D32")   // green - Present
                        : Color.parseColor("#C62828");  // red   - Absent

                    row.addView(makeFixedCell(label, COL_WIDTH, false, textColor));
                }

                // Percentage cell
                int percentage = (int) Math.round((presentCount * 100.0) / attendance.length());

                int pctColor;
                if      (percentage >= 75) pctColor = Color.parseColor("#2E7D32"); // green
                else if (percentage >= 50) pctColor = Color.parseColor("#F57F17"); // amber
                else                       pctColor = Color.parseColor("#C62828"); // red

                row.addView(makeFixedCell(percentage + "%", COL_WIDTH, true, pctColor));

                bodyTable.addView(row);

                // Divider
                View div = new View(this);
                div.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT, 1));
                div.setBackgroundColor(Color.parseColor("#BBDEFB"));
                bodyTable.addView(div);
            }

            // ── Sync header name cell width to body after layout ──
            bodyTable.post(() -> {
                TableRow firstBodyRow = (TableRow) bodyTable.getChildAt(0);
                if (firstBodyRow == null) return;

                View bodyNameCell   = firstBodyRow.getChildAt(0);
                View headerNameCell = ((TableRow) headerTable.getChildAt(0)).getChildAt(0);

                headerNameCell.getLayoutParams().width = bodyNameCell.getWidth();
                headerNameCell.requestLayout();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Name column cell — WRAP_CONTENT width so long names are never clipped. */
    private TextView makeNameCell(String text, boolean bold, int textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(12f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(6, 0, 6, 0);
        tv.setBackground(makeBorder(bold));

        float density = getResources().getDisplayMetrics().density;
        tv.setLayoutParams(new TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            (int)(ROW_HEIGHT * density)));

        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    /** Fixed-width cell for day columns, % column, etc. */
    private TextView makeFixedCell(String text, int widthDp, boolean bold, int textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(12f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(6, 0, 6, 0);
        tv.setBackground(makeBorder(bold));

        float density = getResources().getDisplayMetrics().density;
        tv.setLayoutParams(new TableRow.LayoutParams(
            (int)(widthDp * density),
            (int)(ROW_HEIGHT * density)));

        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private android.graphics.drawable.GradientDrawable makeBorder(boolean bold) {
        android.graphics.drawable.GradientDrawable border =
            new android.graphics.drawable.GradientDrawable();
        border.setColor(Color.TRANSPARENT);
        border.setStroke(1, bold
            ? Color.parseColor("#BBDEFB")   // header border
            : Color.parseColor("#90CAF9")); // body border
        return border;
    }
}