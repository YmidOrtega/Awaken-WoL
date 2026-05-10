package com.ymid.wakeonlan.persistence;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import java.io.File;

import com.ymid.wakeonlan.persistence.migrations.MigrationFrom1To2;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom2To3;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom3To4;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom4To5;
import com.ymid.wakeonlan.persistence.migrations.MigrationFrom5To6;

public class DatabaseInstanceManager {

    private static final String DB_NAME = "database-name";
    private static final String TAG = "DatabaseInstanceManager";

    private static AppDatabase INSTANCE;

    public static synchronized AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            SQLiteDatabase.loadLibs(context);
            migrateToEncryptedIfNeeded(context);

            String passphrase = DatabaseKeyManager.getOrCreatePassphrase(context);
            byte[] passphraseBytes = passphrase.getBytes();
            SupportFactory factory = new SupportFactory(passphraseBytes);

            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)
                    .allowMainThreadQueries()
                    .openHelperFactory(factory)
                    .addMigrations(
                            new MigrationFrom1To2(),
                            new MigrationFrom2To3(),
                            new MigrationFrom3To4(),
                            new MigrationFrom4To5(),
                            new MigrationFrom5To6()
                    )
                    .build();
        }
        return INSTANCE;
    }

    /**
     * Si la BD existe y es texto plano (no cifrada), la cifra in-place con SQLCipher.
     * Esto corre una sola vez — en instalaciones nuevas no hace nada.
     */
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

            // Abre la BD en texto plano y exporta una copia cifrada
            SQLiteDatabase plainDb = SQLiteDatabase.openDatabase(
                    dbFile.getAbsolutePath(), "", null, SQLiteDatabase.OPEN_READWRITE);
            plainDb.rawExecSQL(String.format(
                    "ATTACH DATABASE '%s' AS encrypted KEY '%s'",
                    tempFile.getAbsolutePath(), passphrase));
            plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted')");
            plainDb.rawExecSQL("DETACH DATABASE encrypted");
            plainDb.close();

            // Reemplaza la BD original con la cifrada
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

    /**
     * Detecta si la BD ya está cifrada intentando abrirla sin clave.
     * Una BD SQLite en texto plano empieza con el header "SQLite format 3".
     */
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