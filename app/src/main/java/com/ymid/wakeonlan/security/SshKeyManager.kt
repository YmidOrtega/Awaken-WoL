package com.ymid.wakeonlan.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.RSAPublicKey

object SshKeyManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_SIZE = 2048
    const val ALIAS_PREFIX = "aweken_ssh_"

    fun generateKeyPair(deviceId: Int): String {
        val alias = aliasFor(deviceId)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(KEY_SIZE)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER)
            .apply { initialize(spec) }
            .generateKeyPair()

        return alias
    }

    fun getOpenSshPublicKey(alias: String): String? = try {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry ?: return null
        val pubKey = entry.certificate.publicKey as RSAPublicKey
        val wireBytes = encodeSshRsa(pubKey)
        "ssh-rsa ${Base64.encodeToString(wireBytes, Base64.NO_WRAP)} aweken"
    } catch (_: Exception) { null }

    fun aliasFor(deviceId: Int) = "$ALIAS_PREFIX$deviceId"

    fun deleteKeyPair(alias: String) {
        runCatching {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (ks.containsAlias(alias)) ks.deleteEntry(alias)
        }
    }

    private fun encodeSshRsa(key: RSAPublicKey): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { dos ->
            write(dos, "ssh-rsa".toByteArray())
            write(dos, key.publicExponent.toByteArray())
            write(dos, key.modulus.toByteArray())
        }
        return baos.toByteArray()
    }

    private fun write(dos: DataOutputStream, bytes: ByteArray) {
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }
}
