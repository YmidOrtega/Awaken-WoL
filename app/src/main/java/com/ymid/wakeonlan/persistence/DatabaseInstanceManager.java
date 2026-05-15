package com.ymid.wakeonlan.persistence;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.io.File;

import com.ymid.wakeonlan.persistence.migrations.MigrationFrom1To2;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom2To3;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom3To4;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom4To5;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom5To6;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom6To7;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom7To8;

public class DatabaseInstanceManager {

    private static final String DB_NAME = "database-name";
    private static final String TAG = "DatabaseInstanceManager";

    private static AppDatabase INSTANCE;

    public static synchronized AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            System.loadLibrary("sqlcipher");

            migrateToEncryptedIfNeeded(context);

            String passphrase = DatabaseKeyManager.getOrCreatePassphrase(context);
            byte[] passphraseBytes = passphrase.getBytes();

            SupportOpenHelperFactory factory = new SupportOpenHelperFactory(passphraseBytes);

            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                    .allowMainThreadQueries()
                    .openHelperFactory(factory)
                    .addMigrations(
                            new MigrationFrom1To2(),
                            new MigrationFrom2To3(),
                            new MigrationFrom3To4(),
                            new MigrationFrom4To5(),
                            new MigrationFrom5To6(),
                            new MigrationFrom6To7(),
                            new MigrationFrom7To8()
                    )
                    .build();
        }
        return INSTANCE;
    }

    private static void migrateToEncryptedIfNeeded(Context context) {
        File dbFile = context.getDatabasePath(DB_NAME);
        if (!dbFile.exists()) return;

        if (isAlreadyEncrypted(dbFile)) {
            Log.d(TAG, "Database is already encrypted, skipping migration.");
            return;
        }

        Log.i(TAG, "Plaintext database detected, encrypting...");
        try {
            String passphrase = DatabaseKeyManager.getOrCreatePassphrase(context);
            File tempFile = new File(dbFile.getParent(), DB_NAME + "_temp_encrypted");

            SQLiteDatabase plainDb = SQLiteDatabase.openOrCreateDatabase(
                    dbFile, "", null, null, null);

            plainDb.execSQL(String.format(
                    "ATTACH DATABASE '%s' AS encrypted KEY '%s'",
                    tempFile.getAbsolutePath(), passphrase));
            plainDb.execSQL("SELECT sqlcipher_export('encrypted')");
            plainDb.execSQL("DETACH DATABASE encrypted");
            plainDb.close();

            if (!dbFile.delete()) {
                throw new RuntimeException("Could not delete plaintext database");
            }
            if (!tempFile.renameTo(dbFile)) {
                throw new RuntimeException("Could not rename encrypted database");
            }

            Log.i(TAG, "Database encryption migration completed successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt existing database", e);
            throw new RuntimeException("Database encryption migration failed", e);
        }
    }

    private static boolean isAlreadyEncrypted(File dbFile) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(dbFile, "r")) {
            byte[] header = new byte[16];
            raf.read(header);
            String headerStr = new String(header);
            return !headerStr.startsWith("SQLite format 3");
        } catch (Exception e) {
            return false;
        }
    }
}
