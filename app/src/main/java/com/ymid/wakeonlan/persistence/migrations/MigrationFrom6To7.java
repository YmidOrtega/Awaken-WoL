package com.ymid.wakeonlan.persistence.migrations;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class MigrationFrom6To7 extends Migration {

    public MigrationFrom6To7() {
        super(6, 7);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS ActionLog (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "device_name TEXT NOT NULL, " +
            "action_type TEXT NOT NULL, " +
            "timestamp INTEGER NOT NULL)"
        );
    }
}
