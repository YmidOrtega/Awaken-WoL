package com.ymid.wakeonlan.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.schmizz.sshj.common.Buffer
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.MessageDigest
import java.security.PublicKey

class TofuHostKeyVerifier(context: Context) : HostKeyVerifier {

    class Mismatch(
        val host: String,
        val port: Int,
        val storedFingerprint: String,
        val presentedFingerprint: String
    )

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy { knownHostsPrefs(appContext) }

    @Volatile
    var mismatch: Mismatch? = null
        private set

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val entryKey = entryKey(hostname, port)
        val keyType = KeyType.fromKey(key).toString()
        val fingerprint = fingerprint(key)

        val stored = prefs.getString(entryKey, null)
        if (stored == null) {
            prefs.edit().putString(entryKey, "$keyType $fingerprint").apply()
            return true
        }

        val storedFingerprint = stored.substringAfterLast(' ')
        if (storedFingerprint == fingerprint) {
            return true
        }

        mismatch = Mismatch(hostname, port, storedFingerprint, fingerprint)
        return false
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> {
        val stored = prefs.getString(entryKey(hostname, port), null) ?: return emptyList()
        val keyType = stored.substringBefore(' ', missingDelimiterValue = "")
        return if (keyType.isEmpty()) emptyList() else listOf(keyType)
    }

    companion object {
        private const val PREFS_FILE = "ssh_known_hosts"

        /** Removes the pinned key so the next connection trusts the newly presented one. */
        @JvmStatic
        fun forget(context: Context, hostname: String, port: Int) {
            knownHostsPrefs(context.applicationContext).edit()
                .remove(entryKey(hostname, port))
                .apply()
        }

        private fun entryKey(hostname: String, port: Int) = "$hostname:$port"

        private fun knownHostsPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        private fun fingerprint(key: PublicKey): String {
            val wireBytes = Buffer.PlainBuffer().putPublicKey(key).compactData
            val digest = MessageDigest.getInstance("SHA-256").digest(wireBytes)
            return "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }
}
