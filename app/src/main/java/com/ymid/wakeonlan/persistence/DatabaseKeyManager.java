package com.ymid.wakeonlan.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import android.util.Base64;

import java.security.SecureRandom;

public class DatabaseKeyManager {

    private static final String PREFS_FILE = "db_key_prefs";
    private static final String KEY_DB_PASSPHRASE = "db_passphrase";

    public static String getOrCreatePassphrase(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            String passphrase = prefs.getString(KEY_DB_PASSPHRASE, null);
            if (passphrase == null) {
                passphrase = generatePassphrase();
                prefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply();
            }
            return passphrase;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get or create DB passphrase", e);
        }
    }

    private static String generatePassphrase() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}