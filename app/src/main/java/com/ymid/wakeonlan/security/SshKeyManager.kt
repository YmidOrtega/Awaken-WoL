package com.ymid.wakeonlan.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.util.UUID

object SshKeyManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val EC_CURVE = "secp256r1"
    private const val ECDSA_KEY_TYPE = "ecdsa-sha2-nistp256"
    private const val ECDSA_CURVE_NAME = "nistp256"
    private const val EC_COORDINATE_LENGTH = 32
    const val ALIAS_PREFIX = "aweken_ssh_"

    @JvmStatic
    fun generateKeyPair(): String = generateKeyPair("${ALIAS_PREFIX}${UUID.randomUUID()}")

    @JvmStatic
    fun generateKeyPair(deviceId: Int): String {
        return generateKeyPair(aliasFor(deviceId))
    }

    @JvmStatic
    fun generateKeyPair(alias: String): String {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
            .apply { initialize(spec) }
            .generateKeyPair()

        return alias
    }

    @JvmStatic
    fun getOpenSshPublicKey(alias: String): String? = try {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        when (val pubKey = entry?.certificate?.publicKey) {
            is ECPublicKey ->
                "$ECDSA_KEY_TYPE ${Base64.encodeToString(encodeSshEcdsa(pubKey), Base64.NO_WRAP)} aweken"
            is RSAPublicKey ->
                "ssh-rsa ${Base64.encodeToString(encodeSshRsa(pubKey), Base64.NO_WRAP)} aweken"
            else -> null
        }
    } catch (_: Exception) { null }

    @JvmStatic
    fun aliasFor(deviceId: Int) = "$ALIAS_PREFIX$deviceId"

    @JvmStatic
    fun deleteKeyPair(alias: String) {
        runCatching {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (ks.containsAlias(alias)) ks.deleteEntry(alias)
        }
    }

    private fun encodeSshEcdsa(key: ECPublicKey): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { dos ->
            write(dos, ECDSA_KEY_TYPE.toByteArray())
            write(dos, ECDSA_CURVE_NAME.toByteArray())
            write(dos, encodeUncompressedPoint(key.w))
        }
        return baos.toByteArray()
    }

    private fun encodeUncompressedPoint(point: ECPoint): ByteArray {
        return byteArrayOf(0x04) +
                toFixedLength(point.affineX, EC_COORDINATE_LENGTH) +
                toFixedLength(point.affineY, EC_COORDINATE_LENGTH)
    }

    private fun toFixedLength(value: BigInteger, length: Int): ByteArray {
        val raw = value.toByteArray()
        if (raw.size == length) return raw

        val result = ByteArray(length)
        if (raw.size > length) {
            System.arraycopy(raw, raw.size - length, result, 0, length)
        } else {
            System.arraycopy(raw, 0, result, length - raw.size, raw.size)
        }
        return result
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
