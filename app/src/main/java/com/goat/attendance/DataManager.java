package com.goat.attendance;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class DataManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final DataManager INSTANCE = new DataManager();
    private DataManager() {}
    public static DataManager getInstance() { return INSTANCE; }

    // ── State ─────────────────────────────────────────────────────────────────
    private ArrayList<HashMap<String, Object>> students         = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> filteredStudents = new ArrayList<>();
    private ArrayList<String>                  qrStrings        = new ArrayList<>();
    private String                             attendanceJson   = "";

    // True after the first successful loadAll() — used to skip redundant reloads.
    private boolean dataLoaded = false;

    private static TableAPI table;
    private static Context  appContext;

    // ── Refresh Callback ──────────────────────────────────────────────────────
    public interface RefreshCallback { void onRefresh(); }
    private static RefreshCallback refreshCallback;
    public static void setRefreshCallback(RefreshCallback cb) { refreshCallback = cb; }
    public static void notifyRefresh() { if (refreshCallback != null) refreshCallback.onRefresh(); }

    // ── Init ──────────────────────────────────────────────────────────────────
    public static void setContext(Context c) { if (c != null) appContext = c; }

    public void start() {
        table = TableManager.getTable();
    }

    // ── Queries ───────────────────────────────────────────────────────────────
    public boolean isDataLoaded()       { return dataLoaded; }
    public boolean isStudentListEmpty() { return students == null || students.isEmpty(); }

    public ArrayList<HashMap<String, Object>> getStudents()         { return students; }
    public ArrayList<HashMap<String, Object>> getFilteredStudents() { return filteredStudents; }
    public String                             getAttendanceJson()   { return attendanceJson; }

    public ArrayList<String> getStudentNames() {
        ArrayList<String> names = new ArrayList<>();
        for (HashMap<String, Object> s : students) names.add(String.valueOf(s.get("name")));
        return names;
    }

    private int indexOfStudent(String name) { return getStudentNames().indexOf(name); }

    // ── Day-specific access ───────────────────────────────────────────────────
    public int getTotalDays() { return table.getTotalColumns(); }

    public String getColumnAttendance(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= table.getTotalColumns()) return "[]";
        return table.getColumn(dayIndex);
    }

    /**
     * Marks attendance for a student on a specific day and saves immediately.
     * Saving inside this method follows the rule: save when data changes.
     */
    public void markAttendanceForDay(String name, boolean isPresent, int dayIndex) {
        int rowIndex = indexOfStudent(name);
        if (rowIndex < 0) {
            Log.w("DataManager", "markAttendanceForDay: student not found – " + name);
            return;
        }
        if (dayIndex < 0 || dayIndex >= table.getTotalColumns()) {
            Log.w("DataManager", "markAttendanceForDay: dayIndex out of range – " + dayIndex);
            return;
        }
        table.setCell(rowIndex, dayIndex, isPresent ? "1" : "0");
        saveAttendance();
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    public void reloadFilterList() {
        filteredStudents.clear();
        filteredStudents.addAll(students);
    }

    public void filterList(String keyword) {
        filteredStudents.clear();
        if (keyword == null || keyword.trim().isEmpty()) {
            filteredStudents.addAll(students);
        } else {
            String kw = keyword.toLowerCase().trim();
            for (HashMap<String, Object> item : students) {
                String name   = String.valueOf(item.get("name")).toLowerCase();
                String place  = String.valueOf(item.get("place")).toLowerCase();
                String number = String.valueOf(item.get("number")).toLowerCase();
                if (name.contains(kw) || place.contains(kw) || number.contains(kw))
                    filteredStudents.add(item);
            }
        }
        notifyRefresh();
    }

    // ── Student CRUD ──────────────────────────────────────────────────────────
    public void addStudent(Bundle data) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name",   data.getString("name"));
        map.put("place",  data.getString("place"));
        map.put("number", data.getString("phone"));
        addStudent(map);
    }

    public void addStudent(HashMap<String, Object> map) {
        students.add(map);
        addRowToTable(String.valueOf(map.get("name")), table.getTotalColumns());
        saveStudentData();   // save when data changes
        saveAttendance();    // save when data changes
        reloadFilterList();
        notifyRefresh();
    }

    public void seedDefaultStudentIfEmpty(Bundle data) {
        if (!isStudentListEmpty()) return;
        HashMap<String, Object> map = new HashMap<>();
        map.put("name",   data.getString("name"));
        map.put("place",  data.getString("place"));
        map.put("number", data.getString("phone"));
        students.add(map);
        saveStudentData();
        saveAttendance();
    }

    public void deleteStudent(int position) {
        HashMap<String, Object> selected = filteredStudents.get(position);
        String name = String.valueOf(selected.get("name"));

        filteredStudents.remove(position);
        students.removeIf(s -> name.equals(String.valueOf(s.get("name"))));
        deleteRowFromTable(name);

        // In-memory table is already correct after deleteRowFromTable —
        // save immediately, no reloadTable() round-trip needed.
        saveStudentData();   // save when data changes
        saveAttendance();    // save when data changes

        notifyRefresh();
        showToast("Student deleted");
    }

    // ── Attendance ────────────────────────────────────────────────────────────
    public void markAttendance(String name, boolean isPresent) {
        int index = indexOfStudent(name);
        if (index < 0) { Log.w("DataManager", "markAttendance: not found – " + name); return; }
        table.setCell(index, table.getTotalColumns() - 1, isPresent ? "1" : "0");
    }

    public void processAttendanceQr(String qrData) {
        int sep = qrData.indexOf(',');
        if (sep <= 0 || sep >= qrData.length() - 1) {
            showToast("Invalid QR — please scan a valid student QR");
            return;
        }
        markAttendance(qrData.substring(sep + 1).trim(), true);
        saveAttendance(); // save when data changes
    }

    public void addDay() {
        table.addColumn("Day " + (table.getTotalColumns() + 1), "0");
        saveAttendance(); // save when data changes
    }

    public void removeLastDay() {
        table.deleteColumn(table.getTotalColumns() - 1);
        saveAttendance(); // save when data changes
    }

    // ── QR ────────────────────────────────────────────────────────────────────
    public void generateQrForAllStudents() {
        QRGenerator qrGenerator = new QRGenerator();
        qrStrings.clear();
        for (int i = 0; i < students.size(); i++) {
            String name      = String.valueOf(students.get(i).get("name"));
            String qrContent = i + "," + name;
            saveQrImage(qrGenerator.generateQRCode(qrContent, name), name);
            qrStrings.add(qrContent);
        }
        saveQrData(); // save when data changes
    }

    private void saveQrImage(Bitmap bitmap, String name) {
        try {
            File file = new File("/storage/emulated/0/Download/" + name + "_QR.png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) { Log.e("DataManager", "saveQrImage: " + e); }
    }

    public String getQrString(int position) { return qrStrings.get(position); }

    // ── SMS ───────────────────────────────────────────────────────────────────
    public void sendAttendanceSms() {
        String json = table.getColumn(table.getTotalColumns() - 1);
        try {
            JSONArray vals = new JSONArray(json);
            for (int i = 0; i < students.size(); i++) {
                HashMap<String, Object> student = students.get(i);
                String name  = String.valueOf(student.get("name"));
                String phone = String.valueOf(student.get("number"));
                if (phone.isEmpty() || name.isEmpty()) { showToast("Invalid data at row " + i); return; }
                if ("1".equals(vals.optString(i))) SmsApi.sendPresentSms(phone, name);
                else SmsApi.sendAbsentSms(phone, name);
            }
            showToast("SMS sent!");
        } catch (Exception e) { Log.e("DataManager", "sendAttendanceSms: " + e); showToast("Failed to send SMS"); }
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    /**
     * Loads all data from disk.
     * Call ONCE at app start (GoatActivity.onCreate).
     * Do NOT call in onResume — DataManager is a singleton and its in-memory
     * state is always the most current. Reloading from disk in onResume would
     * overwrite correct in-memory changes with potentially stale disk data.
     */
    public void loadAll() {
        loadStudentData();
        loadAttendance();
        loadQrData();
        dataLoaded = true;
    }

    /**
     * Loads from disk only if this is the first time (e.g. after process restart).
     * Safe to call from any Activity.onResume — won't overwrite in-memory state
     * unnecessarily.
     */
    public void loadIfNeeded() {
        if (!dataLoaded) loadAll();
    }

    public void saveAll() {
        saveStudentData();
        saveAttendance();
        saveQrData();
    }

    // Student
    public void loadStudentData() {
        String json = readFile("students.json");
        if (json == null || json.trim().isEmpty()) { students = new ArrayList<>(); return; }
        try {
            students = new Gson().fromJson(json, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
            if (students == null) students = new ArrayList<>();
        } catch (Exception e) { Log.e("DataManager", "loadStudentData: " + e); students = new ArrayList<>(); }
    }

    public void saveStudentData() {
        writeFile("students.json", new Gson().toJson(students));
    }

    // QR
    public void loadQrData() {
        qrStrings.clear();
        String json = readFile("qr_datas.json");
        if (json == null || json.trim().isEmpty()) return;
        try {
            qrStrings = new Gson().fromJson(json, new TypeToken<ArrayList<String>>(){}.getType());
            if (qrStrings == null) qrStrings = new ArrayList<>();
        } catch (Exception e) { qrStrings = new ArrayList<>(); }
    }

    public void saveQrData() { writeFile("qr_datas.json", new Gson().toJson(qrStrings)); }

    // Attendance
    public void loadAttendance() {
        attendanceJson = readFile("attendance_table.json");
        if (attendanceJson != null && !attendanceJson.trim().isEmpty()) {
            table.importJSON(attendanceJson);
        } else {
            attendanceJson = "{\"days\":[],\"students\":[]}";
            writeFile("attendance_table.json", attendanceJson);
            table.importJSON(attendanceJson);
        }
    }

    public void saveAttendance() {
        attendanceJson = table.returnJSON();
        writeFile("attendance_table.json", attendanceJson);
        Log.d("DataManager", "saveAttendance → " + attendanceJson);
    }

    // ── Table Helpers ─────────────────────────────────────────────────────────
    public void addRowToTable(String rowName, int columnCount) {
        JSONArray row = new JSONArray();
        row.put(rowName);
        for (int i = 0; i < columnCount; i++) row.put("0");
        table.addRow(row.toString());
    }

    public void deleteRowFromTable(String name) {
        int index = table.findRowByName(name);
        if (index >= 0) table.deleteRow(index);
    }

    // ── File I/O ──────────────────────────────────────────────────────────────

    /**
     * Builds a normalised absolute path so writeFile and readFile always
     * resolve to the exact same location on disk.
     *
     * Root cause of the stale-data bug:
     *   writeFile used  → dir.concat(fileName)      (no slash separator)
     *   readFile used   → dir.concat("/").concat(fileName)
     * If getPackageDataDir() returns a path WITHOUT a trailing slash, those
     * two strings pointed to different files, so reads always returned old data.
     */
    private String getFilePath(String fileName) {
        String dir = FileUtil.getPackageDataDir(appContext);
        if (dir == null) dir = "";
        // Guarantee exactly one slash between directory and file name.
        if (!dir.endsWith("/")) dir = dir + "/";
        return dir + fileName;
    }

    public void writeFile(String fileName, String data) {
        FileUtil.writeFile(getFilePath(fileName), data);
    }

    public String readFile(String fileName) {
        return FileUtil.readFile(getFilePath(fileName));
    }

    // ── Toast ─────────────────────────────────────────────────────────────────
    public void showToast(String message)            { Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show(); }
    public void showToast(Context c, String message) { Toast.makeText(c, message, Toast.LENGTH_SHORT).show(); }
}
