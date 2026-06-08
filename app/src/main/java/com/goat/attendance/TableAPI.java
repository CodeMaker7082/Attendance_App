package com.goat.attendance;

import org.json.JSONArray;
import org.json.JSONObject;

/**
* TableAPI — JNI wrapper for the C++ TableEngine.
*
* Column index convention (used by all column/cell methods):
*   index 0 → first day column (e.g. "Day 1")
* The student name is stored internally as column -1 (row[0] in C++)
* and is never exposed as a column index to Java callers.
*/
public class TableAPI {
	
	static { System.loadLibrary("tableengine"); }
	
	// ── Load / Export ─────────────────────────────────────────────────────────
	
	/** Load from a JSON file on disk. */
	public native void loadJSON(String path);
	
	/**
* Load directly from a JSON string — no file needed.
* Useful when you fetch attendance data from a server.
*
* Example:
*   String json = "{"
*       + "\"days\":[\"Day 1\",\"Day 2\"],"
*       + "\"students\":["
*       + "  {\"name\":\"Alice\",\"attendance\":[1,0]},"
*       + "  {\"name\":\"Bob\",  \"attendance\":[1,1]}"
*       + "]}";
*   api.importJSON(json);
*/	
	public native void importJSON(String jsonStr);
	
	/**
* Returns the full table in the original schema:
*   { "days": [...], "students": [{"name":"...", "attendance":[0,1,...]}, ...] }
*
* Example:
*   String out = api.returnJSON();
*   JSONObject root = new JSONObject(out);
*   JSONArray days = root.getJSONArray("days");
*   JSONArray students = root.getJSONArray("students");
*   for (int i = 0; i < students.length(); i++) {
*       JSONObject s = students.getJSONObject(i);
*       String name = s.getString("name");
*       JSONArray att = s.getJSONArray("attendance");
*   }
*/	
	public native String returnJSON();
	
	// ── Row operations ────────────────────────────────────────────────────────
	
	/**
* Add a new student row. The JSON array must contain:
*   [name, att0, att1, ..., attN]  — all as strings.
*
* Example:
*   JSONArray row = new JSONArray();
*   row.put("Eve");
*   row.put("1");
*   row.put("0");
*   row.put("1");
*   row.put("1");
*   row.put("0");
*   api.addRow(row.toString());
*/	
	public native void addRow(String rowJson);
	
	/**
* Delete the row at the given 0-based index.
*
* Example:
*   api.deleteRow(2);   // removes Carol (index 2)
*/	
	public native void deleteRow(int index);
	
	/**
* Remove all student rows (keeps day columns intact).
*
* Example:
*   api.deleteAllRows();
*   // api.getTotalRows() == 0
*/	
	public native void deleteAllRows();
	
	/**
* Swap two rows by index.
*
* Example:
*   // Move Alice (0) and Bob (1) positions
*   api.swapRows(0, 1);
*/	
	public native void swapRows(int a, int b);
	
	/**
* Get a single student row as a JSON object:
*   {"name":"Alice","attendance":[1,1,0,1,1]}
*
* Returns "null" string if index is out of range.
*
* Example:
*   String rowJson = api.getRow(0);
*   JSONObject student = new JSONObject(rowJson);
*   int day1 = student.getJSONArray("attendance").getInt(0);
*/	
	public native String getRow(int index);
	
	/**
* Find the row index of a student by name. Returns -1 if not found.
*
* Example:
*   int idx = api.findRowByName("Carol");
*   if (idx >= 0) api.deleteRow(idx);
*/	
	public native int findRowByName(String name);
	
	// ── Column operations ─────────────────────────────────────────────────────
	
	/**
* Add a new day column to ALL student rows, filled with defaultValue.
*
* Example — add "Day 6" with everyone absent:
*   api.addColumn("Day 6", "0");
*
* Example — add "Day 6" with everyone present:
*   api.addColumn("Day 6", "1");
*/	
	public native void addColumn(String name, String defaultValue);
	
	/**
* Delete a day column and its data from every row.
* index 0 = first day column ("Day 1").
*
* Example:
*   api.deleteColumn(0);  // deletes "Day 1" from all rows
*/	
	public native void deleteColumn(int index);
	
	/**
* Rename an existing day column.
*
* Example:
*   api.renameColumn(4, "Day 5 (makeup)");
*/	
	public native void renameColumn(int index, String newName);
	
	/**
* Get all attendance values for one day column as a JSON int array.
* Returns "null" string if index is out of range.
*
* Example:
*   String colJson = api.getColumn(0);  // Day 1 attendance for all students
*   JSONArray vals = new JSONArray(colJson);
*   // vals = ["1","1","0","1"]
*/	
	public native String getColumn(int index);
	
	// ── Cell operations ───────────────────────────────────────────────────────
	
	/**
* Get a single attendance cell value as a string ("0" or "1").
* rowIndex = student index, colIndex = day column index (0-based).
*
* Example:
*   String val = api.getCell(1, 2);  // Bob, Day 3
*/	
	public native String getCell(int rowIndex, int colIndex);
	
	/**
* Set a single attendance cell value.
*
* Example:
*   api.setCell(1, 2, "1");  // Mark Bob present on Day 3
*/	
	public native void setCell(int rowIndex, int colIndex, String value);
	
	// ── Aggregates ────────────────────────────────────────────────────────────
	
	/** Total number of student rows. */
	public native int getTotalRows();
	
	/** Total number of day columns. */
	public native int getTotalColumns();
	
	/**
* Sum of attendance values (0/1) for one student — i.e. days present.
* Returns -1 if rowIndex is out of range.
*
* Example:
*   int davidDays = api.getTotalAttendance(3); // David attended all 5 → returns 5
*/	
	public native int getTotalAttendance(int rowIndex);
	
	// =========================================================================
	// USAGE EXAMPLES (call from Activity / Fragment / ViewModel)
	// =========================================================================
	
	public static void runExamples(TableAPI api) {
		
		// ── 1. Load from file ─────────────────────────────────────────────────
		api.loadJSON("/data/user/0/com.goat.attendance/files/attendance.json");
		
		// ── 2. Export — must match input schema ───────────────────────────────
		String out = api.returnJSON();
		// out == {"days":["Day 1",...,"Day 5"],
		//         "students":[{"name":"Alice","attendance":[1,1,0,1,1]}, ...]}
		
		// ── 3. Add a student ──────────────────────────────────────────────────
		JSONArray newRow = new JSONArray();
		newRow.put("Eve");
		newRow.put("1"); newRow.put("1"); newRow.put("1"); newRow.put("0"); newRow.put("1");
		api.addRow(newRow.toString());
		// getTotalRows() == 5
		
		// ── 4. Add a day column ───────────────────────────────────────────────
		api.addColumn("Day 6", "0");   // everyone absent by default
		api.setCell(0, 5, "1");        // Alice was present on Day 6
		// getTotalColumns() == 6
		
		// ── 5. Get one student's data ─────────────────────────────────────────
		String rowJson = api.getRow(0);
		// {"name":"Alice","attendance":[1,1,0,1,1,1]}
		
		// ── 6. Find student by name ───────────────────────────────────────────
		int carolIdx = api.findRowByName("Carol");
		if (carolIdx >= 0) {
			int carolDays = api.getTotalAttendance(carolIdx); // 3
		}
		
		// ── 7. Get a full day column ──────────────────────────────────────────
		String day1Col = api.getColumn(0);
		// ["1","1","0","1","1"]  (Alice,Bob,Carol,David,Eve on Day 1)
		
		// ── 8. Rename a column ────────────────────────────────────────────────
		api.renameColumn(5, "Day 6 (extra)");
		
		// ── 9. Read / write individual cell ──────────────────────────────────
		String val = api.getCell(1, 1);  // Bob, Day 2  → "0"
		api.setCell(1, 1, "1");          // mark Bob present
		
		// ── 10. Swap rows ─────────────────────────────────────────────────────
		api.swapRows(0, 3);  // swap Alice ↔ David
		
		// ── 11. Delete column ─────────────────────────────────────────────────
		api.deleteColumn(5); // removes Day 6 from all rows
		
		// ── 12. Delete student ────────────────────────────────────────────────
		api.deleteRow(carolIdx);
		
		// ── 13. Import from JSON string (e.g. from HTTP response) ─────────────
		String serverJson = "{"
		+ "\"days\":[\"Mon\",\"Tue\",\"Wed\"],"
		+ "\"students\":["
		+ "  {\"name\":\"Zara\",\"attendance\":[1,1,0]},"
		+ "  {\"name\":\"Leo\", \"attendance\":[0,1,1]}"
		+ "]}";
		api.importJSON(serverJson);
		
		// ── 14. Wipe everything ───────────────────────────────────────────────
		api.deleteAllRows();
		// getTotalRows() == 0, columns still intact
	}
}
