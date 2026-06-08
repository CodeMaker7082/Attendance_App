package com.goat.attendance;

public class TableManager {

    private static TableAPI table;

    public static TableAPI getTable() {
        if (table == null) {
            table = new TableAPI();
        }
        return table;
    }
}