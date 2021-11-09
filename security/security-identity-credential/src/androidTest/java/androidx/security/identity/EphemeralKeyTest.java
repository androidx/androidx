/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.security.identity;

import static junit.framework.TestCase.assertTrue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.security.keystore.KeyProperties;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Collection;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// TODO: For better coverage, use different ECDH and HKDF implementations in test code.
@SuppressWarnings("deprecation")
@MediumTest
@RunWith(AndroidJUnit4.class)
public class EphemeralKeyTest {
    private static final String TAG = "EphemeralKeyTest";

    @Test
    public void createEphemeralKey() throws IdentityCredentialException {
        Context appContext = androidx.test.InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = Util.getIdentityCredentialStore(appContext);

        String credentialName = "ephemeralKeyTest";
        byte[] sessionTranscript = {0x01, 0x02, 0x03};

        store.deleteCredentialByName(credentialName);
        Collection<X509Certificate> certChain = ProvisioningTest.createCredential(store,
                credentialName);
        IdentityCredential credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        assertNotNull(credential);

        // Check we can get both the public and private keys.
        KeyPair ephemeralKeyPair = credential.createEphemeralKeyPair();
        assertNotNull(ephemeralKeyPair);
        assertTrue(ephemeralKeyPair.getPublic().getEncoded().length > 0);
        assertTrue(ephemeralKeyPair.getPrivate().getEncoded().length > 0);

        TestReader reader = new TestReader(
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256,
                ephemeralKeyPair.getPublic(),
                sessionTranscript);

        try {
            credential.setReaderEphemeralPublicKey(reader.getEphemeralPublicKey());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        credential.setSessionTranscript(sessionTranscript);

        // Exchange a couple of messages... this is to test that the nonce/counter
        // state works as expected.
        for (int n = 0; n < 5; n++) {
            // First send a message from the Reader to the Holder...
            byte[] messageToHolder = ("Hello Holder! (serial=" + n + ")").getBytes();
            byte[] encryptedMessageToHolder = reader.encryptMessageToHolder(messageToHolder);
            assertNotEquals(messageToHolder, encryptedMessageToHolder);
            byte[] decryptedMessageToHolder = credential.decryptMessageFromReader(
                    encryptedMessageToHolder);
            assertArrayEquals(messageToHolder, decryptedMessageToHolder);

            // Then from the Holder to the Reader...
            byte[] messageToReader = ("Hello Reader! (serial=" + n + ")").getBytes();
            byte[] encryptedMessageToReader = credential.encryptMessageToReader(messageToReader);
            assertNotEquals(messageToReader, encryptedMessageToReader);
            byte[] decryptedMessageToReader = reader.decryptMessageFromHolder(
                    encryptedMessageToReader);
            assertArrayEquals(messageToReader, decryptedMessageToReader);
        }
    }

    static class TestReader {

        @IdentityCredentialStore.Ciphersuite
        private int mCipherSuite;

        private PublicKey mHolderEphemeralPublicKey;
        private KeyPair mEphemeralKeyPair;
        private SecretKey mSKDevice;
        private SecretKey mSKReader;
        private int mSKDeviceCounter;
        private int mSKReaderCounter;

        private SecureRandom mSecureRandom;

        private boolean mRemoteIsReaderDevice;

        // This is basically the reader-side of what needs to happen for encryption/decryption
        // of messages.. could easily be re-used in an mDL reader application.
        TestReader(@IdentityCredentialStore.Ciphersuite int cipherSuite,
                PublicKey holderEphemeralPublicKey,
                byte[] sessionTranscript) throws IdentityCredentialException {
            mCipherSuite = cipherSuite;
            mHolderEphemeralPublicKey = holderEphemeralPublicKey;
            mSKReaderCounter = 1;
            mSKDeviceCounter = 1;

            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime256v1");
                kpg.initialize(ecSpec);
                mEphemeralKeyPair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error generating ephemeral key", e);
            }

            try {
                KeyAgreement ka = KeyAgreement.getInstance("ECDH");
                ka.init(mEphemeralKeyPair.getPrivate());
                ka.doPhase(mHolderEphemeralPublicKey, true);
                byte[] sharedSecret = ka.generateSecret();

                byte[] sessionTranscriptBytes =
                        Util.cborEncode(Util.cborBuildTaggedByteString(sessionTranscript));
                byte[] salt = MessageDigest.getInstance("SHA-256").digest(sessionTranscriptBytes);

                byte[] info = new byte[] {'S', 'K', 'D', 'e', 'v', 'i', 'c', 'e'};
                byte[] derivedKey = Util.computeHkdf("HmacSha256", sharedSecret, salt, info, 32);
                mSKDevice = new SecretKeySpec(derivedKey, "AES");

                info = new byte[] {'S', 'K', 'R', 'e', 'a', 'd', 'e', 'r'};
                derivedKey = Util.computeHkdf("HmacSha256", sharedSecret, salt, info, 32);
                mSKReader = new SecretKeySpec(derivedKey, "AES");

                mSecureRandom = new SecureRandom();

            } catch (InvalidKeyException
                    | NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error performing key agreement", e);
            }
        }

        PublicKey getEphemeralPublicKey() {
            return mEphemeralKeyPair.getPublic();
        }

        byte[] encryptMessageToHolder(byte[] messagePlaintext) throws IdentityCredentialException {
            byte[] messageCiphertext = null;
            try {
                ByteBuffer iv = ByteBuffer.allocate(12);
                iv.putInt(0, 0x00000000);
                iv.putInt(4, 0x00000000);
                iv.putInt(8, mSKReaderCounter);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec encryptionParameterSpec = new GCMParameterSpec(128, iv.array());
                cipher.init(Cipher.ENCRYPT_MODE, mSKReader, encryptionParameterSpec);
                messageCiphertext = cipher.doFinal(messagePlaintext); // This includes the auth tag
            } catch (BadPaddingException
                    | IllegalBlockSizeException
                    | NoSuchPaddingException
                    | InvalidKeyException
                    | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error encrypting message", e);
            }
            mSKReaderCounter += 1;
            return messageCiphertext;
        }

        byte[] decryptMessageFromHolder(byte[] messageCiphertext)
                throws IdentityCredentialException {
            ByteBuffer iv = ByteBuffer.allocate(12);
            iv.putInt(0, 0x00000000);
            iv.putInt(4, 0x00000001);
            iv.putInt(8, mSKDeviceCounter);
            byte[] plaintext = null;
            try {
                final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, mSKDevice, new GCMParameterSpec(128, iv.array()));
                plaintext = cipher.doFinal(messageCiphertext);
            } catch (BadPaddingException
                    | IllegalBlockSizeException
                    | InvalidAlgorithmParameterException
                    | InvalidKeyException
                    | NoSuchAlgorithmException
                    | NoSuchPaddingException e) {
                e.printStackTrace();
                throw new IdentityCredentialException("Error decrypting message", e);
            }
            mSKDeviceCounter += 1;
            return plaintext;
        }
    }
}
