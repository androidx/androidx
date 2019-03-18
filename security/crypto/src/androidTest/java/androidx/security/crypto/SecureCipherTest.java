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

package androidx.security.crypto;

import androidx.annotation.NonNull;
import androidx.security.SecureConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class SecureCipherTest {

    @Test
    public void testEncryptDecryptSymmetricData() {
        final String keyAlias = "test_signing_key";
        final String original = "It's a secret...";
        try {
            SecureConfig config = SecureConfig.getDefault();
            final SecureCipher cipher = new SecureCipher(config);
            SecureKeyGenerator keyGenerator = SecureKeyGenerator.getInstance(config);
            keyGenerator.generateKey(keyAlias);
            cipher.encrypt(keyAlias, original.getBytes("UTF-8"),
                    new SecureCipher.SecureSymmetricEncryptionListener() {
                        @Override
                        public void encryptionComplete(@NonNull byte[] cipherText,
                                @NonNull byte[] iv) {
                            cipher.decrypt(keyAlias, cipherText, iv,
                                    new SecureCipher.SecureDecryptionListener() {
                                        @Override
                                        public void decryptionComplete(
                                                @NonNull byte[] clearText) {
                                            try {
                                                Assert.assertEquals(
                                                        "Original should match"
                                                                + "encrypted/decrypted data",
                                                        original,
                                                        new String(clearText,
                                                                "UTF-8"));
                                            } catch (UnsupportedEncodingException ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    });

                        }
                    });

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Test
    public void testSignVerifyData() {
        final String keyAlias = "test_signing_key";
        final String original = "It's a secret...";
        try {
            SecureConfig config = SecureConfig.getDefault();
            final SecureCipher cipher = new SecureCipher(config);
            SecureKeyGenerator keyGenerator = SecureKeyGenerator.getInstance(config);
            keyGenerator.generateAsymmetricKeyPair(keyAlias);
            cipher.sign(keyAlias, original.getBytes("UTF-8"),
                    new SecureCipher.SecureSignListener() {
                        @Override
                        public void signComplete(@NonNull byte[] signature) {
                            try {
                                Assert.assertTrue(
                                        "Signature should verify",
                                        cipher.verify(keyAlias,
                                                original.getBytes("UTF-8"),
                                        signature));
                            } catch (UnsupportedEncodingException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

}
