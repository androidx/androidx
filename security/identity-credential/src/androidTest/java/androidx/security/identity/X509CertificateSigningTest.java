/*
 * Copyright 2019 The Android Open Source Project
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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@SuppressWarnings("deprecation")
@MediumTest
@RunWith(AndroidJUnit4.class)
public class X509CertificateSigningTest {

    private static final String TAG = "X509CertificateSigningTest";

    @Test
    public void testSigning() {
        Context appContext = androidx.test.InstrumentationRegistry.getTargetContext();

        String keyToSignAlias = "testKeyToSign";
        String keyToSignWithAlias = "testKeyToSignWith";

        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            ks.deleteEntry(keyToSignAlias);
            ks.deleteEntry(keyToSignWithAlias);
            assertFalse(ks.containsAlias(keyToSignAlias));
            assertFalse(ks.containsAlias(keyToSignWithAlias));

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

            kpg.initialize(new KeyGenParameterSpec.Builder(
                    keyToSignAlias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512).build());
            KeyPair keyToSignKeyPair = kpg.generateKeyPair();

            kpg.initialize(new KeyGenParameterSpec.Builder(
                    keyToSignWithAlias,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512).build());
            KeyPair keyToSignWithKeyPair = kpg.generateKeyPair();

            assertTrue(ks.containsAlias(keyToSignAlias));
            assertTrue(ks.containsAlias(keyToSignWithAlias));

            X509Certificate cert = Util.signPublicKeyWithPrivateKey(keyToSignAlias,
                    keyToSignWithAlias);
            assertNotNull(cert);
            Log.d(TAG, "Cert:\n--\n" + cert.toString() + "\n--\n");

            String filename = "ic_cert.bin";
            AtomicFile file = new AtomicFile(appContext.getFileStreamPath(filename));
            FileOutputStream outputStream = null;
            try {
                outputStream = file.startWrite();
                outputStream.write(cert.getEncoded());
                outputStream.close();
                file.finishWrite(outputStream);
            } catch (IOException e) {
                if (outputStream != null) {
                    file.failWrite(outputStream);
                }
                e.printStackTrace();
                assertTrue(false);
            }


            // Check |cert| is for |keyToSignAlias|
            assertArrayEquals(keyToSignKeyPair.getPublic().getEncoded(),
                    cert.getPublicKey().getEncoded());

            // Check |cert| was signed by |keyToSignWithAlias|
            cert.verify(keyToSignWithKeyPair.getPublic());   // Throws if verification fails.

        } catch (IOException
                | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | KeyStoreException
                | CertificateException
                | InvalidKeyException
                | SignatureException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        //assertTrue(false);
    }

}
