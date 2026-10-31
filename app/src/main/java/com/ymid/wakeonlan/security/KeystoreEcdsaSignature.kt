package com.ymid.wakeonlan.security

import com.hierynomus.sshj.key.KeyAlgorithm
import com.hierynomus.sshj.key.KeyAlgorithms
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.signature.Signature
import net.schmizz.sshj.signature.SignatureECDSA
import java.security.PrivateKey
import java.security.PublicKey

/**
 * ecdsa-sha2-nistp256 signatures that work with keys held in the Android Keystore.
 *
 * sshj asks Bouncy Castle for the signature by name, and Bouncy Castle cannot use a key that never
 * leaves the Keystore ("no encoding for EC private key"). Without a named provider, Android picks
 * the one that accepts the key. Encoding and verifying the server's host key stay with sshj.
 */
class KeystoreEcdsaSignature : Signature {

    private val sshj = SignatureECDSA.Factory256().create()
    private val signer = java.security.Signature.getInstance("SHA256withECDSA")
    private var signing = false

    override fun getSignatureName(): String = sshj.signatureName

    override fun initSign(key: PrivateKey) {
        signer.initSign(key)
        signing = true
    }

    override fun initVerify(key: PublicKey) {
        sshj.initVerify(key)
        signing = false
    }

    override fun update(data: ByteArray) = update(data, 0, data.size)

    override fun update(data: ByteArray, off: Int, len: Int) =
        if (signing) signer.update(data, off, len) else sshj.update(data, off, len)

    override fun sign(): ByteArray = signer.sign()

    override fun encode(signature: ByteArray): ByteArray = sshj.encode(signature)

    override fun verify(signature: ByteArray): Boolean = sshj.verify(signature)

    private class SignatureFactory : Factory.Named<Signature> {
        override fun create(): Signature = KeystoreEcdsaSignature()
        override fun getName(): String = KeyType.ECDSA256.toString()
    }

    companion object {
        /** The same list with ecdsa-sha2-nistp256 signed by the Keystore, in the same position. */
        @JvmStatic
        fun replaceIn(algorithms: List<Factory.Named<KeyAlgorithm>>): List<Factory.Named<KeyAlgorithm>> =
            algorithms.map {
                if (it.name == KeyType.ECDSA256.toString()) {
                    KeyAlgorithms.Factory(it.name, SignatureFactory(), KeyType.ECDSA256)
                } else {
                    it
                }
            }
    }
}
