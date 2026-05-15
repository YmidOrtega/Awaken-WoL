package com.ymid.wakeonlan.persistence.migrations;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class MigrationFrom7To8 extends Migration {

    public MigrationFrom7To8() {
        super(7, 8);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE Devices ADD COLUMN ssh_auth_type TEXT DEFAULT 'password'");
        database.execSQL("ALTER TABLE Devices ADD COLUMN ssh_key_alias TEXT");
        database.execSQL("ALTER TABLE Devices ADD COLUMN group_name TEXT");
        database.execSQL("ALTER TABLE Devices ADD COLUMN wan_ip TEXT");
        database.execSQL("ALTER TABLE Devices ADD COLUMN wan_port INTEGER");
    }
}
