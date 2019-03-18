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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SecureKeyStoreTest {

    @Test
    public void testKeyDoesNotExist() throws Throwable {
        SecureKeyStore secureKeyStore = SecureKeyStore.getDefault();
        boolean keyExists = secureKeyStore.keyExists("Not_a_real_key");
        Assert.assertFalse("Key has not been created and should not exist!", keyExists);
    }

    @Test
    public void testSymmetricKeyExists() throws Throwable {
        SecureKeyGenerator keyGenerator = SecureKeyGenerator.getDefault();
        keyGenerator.generateKey("symmetric_key");

        SecureKeyStore keyStore = SecureKeyStore.getDefault();
        boolean keyExists = keyStore.keyExists("symmetric_key");

        Assert.assertTrue("Symmetric Key should exist!", keyExists);
    }

    @Test
    public void testDeleteSymmetricKey() throws Throwable {
        SecureKeyStore keyStore = SecureKeyStore.getDefault();
        keyStore.deleteKey("symmetric_key");

        boolean keyExists = keyStore.keyExists("symmetric_key");

        Assert.assertFalse("Symmetric Key should have been deleted!", keyExists);
    }

    @Test
    public void testAsymmetricKeyExists() throws Throwable {
        SecureKeyGenerator keyGenerator = SecureKeyGenerator.getDefault();
        keyGenerator.generateAsymmetricKeyPair("asymmetric_key_pair");

        SecureKeyStore keyStore = SecureKeyStore.getDefault();
        boolean keyExists = keyStore.keyExists("asymmetric_key_pair");

        Assert.assertTrue("Asymmetric Key should exist!", keyExists);
    }

    @Test
    public void testDeleteAsymmetricKey() throws Throwable {
        SecureKeyStore keyStore = SecureKeyStore.getDefault();
        keyStore.deleteKey("asymmetric_key_pair");

        boolean keyExists = keyStore.keyExists("asymmetric_key_pair");

        Assert.assertFalse("Asymmetric Key should have been deleted!", keyExists);
    }

}
