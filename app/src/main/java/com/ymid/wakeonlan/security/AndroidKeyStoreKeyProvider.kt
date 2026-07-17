package com.ymid.wakeonlan.security

import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.common.KeyType
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

class AndroidKeyStoreKeyProvider(private val alias: String) : KeyProvider {

    private val entry: KeyStore.PrivateKeyEntry by lazy {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.getEntry(alias, null) as KeyStore.PrivateKeyEntry
    }

    override fun getPrivate(): PrivateKey = entry.privateKey

    override fun getPublic(): PublicKey = entry.certificate.publicKey

    override fun getType(): KeyType = KeyType.fromKey(entry.certificate.publicKey)
}
