package com.ymid.wakeonlan.security;

import static org.junit.Assert.*;

import com.hierynomus.sshj.key.KeyAlgorithm;

import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.signature.Signature;
import net.schmizz.sshj.signature.SignatureECDSA;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.List;

public class KeystoreEcdsaSignatureTest {

    private static final byte[] DATA = "session id + userauth request".getBytes(StandardCharsets.UTF_8);

    private static KeyPair p256() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    /** How sshj puts a signature on the wire (KeyedAuthMethod.putSig): algorithm name, then the encoded signature. */
    private static byte[] wire(Signature signature) {
        return new Buffer.PlainBuffer().putString("ecdsa-sha2-nistp256")
                .putBytes(signature.encode(signature.sign())).getCompactData();
    }

    @Test
    public void signatureIsAcceptedBySshjVerifier() throws Exception {
        KeyPair keys = p256();
        Signature signer = new KeystoreEcdsaSignature();
        signer.initSign(keys.getPrivate());
        signer.update(DATA);
        byte[] wire = wire(signer);

        Signature verifier = new SignatureECDSA.Factory256().create();
        verifier.initVerify(keys.getPublic());
        verifier.update(DATA);
        assertTrue(verifier.verify(wire));
    }

    @Test
    public void verifiesServerHostKeySignaturesLikeSshj() throws Exception {
        KeyPair keys = p256();
        Signature sshj = new SignatureECDSA.Factory256().create();
        sshj.initSign(keys.getPrivate());
        sshj.update(DATA);
        byte[] wire = wire(sshj);

        Signature verifier = new KeystoreEcdsaSignature();
        verifier.initVerify(keys.getPublic());
        verifier.update(DATA);
        assertTrue(verifier.verify(wire));
    }

    @Test
    public void replacesOnlyNistp256AndKeepsOrder() {
        List<Factory.Named<KeyAlgorithm>> original = new DefaultConfig().getKeyAlgorithms();
        List<Factory.Named<KeyAlgorithm>> replaced = KeystoreEcdsaSignature.replaceIn(original);

        assertEquals(original.size(), replaced.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i).getName(), replaced.get(i).getName());
            boolean nistp256 = "ecdsa-sha2-nistp256".equals(original.get(i).getName());
            assertEquals(nistp256, replaced.get(i) != original.get(i));
            if (nistp256) {
                assertTrue(replaced.get(i).create().newSignature() instanceof KeystoreEcdsaSignature);
            }
        }
    }
}
