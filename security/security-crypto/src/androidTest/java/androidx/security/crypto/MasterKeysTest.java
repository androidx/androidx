/*
 * Copyright 2020 The Android Open Source Project
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

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

@SuppressWarnings("deprecation")
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
@RunWith(AndroidJUnit4.class)
public class MasterKeysTest {
    private static final String PREFS_FILE = "test_shared_prefs";
    private static final int KEY_SIZE = 256;

    @Before
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setup() throws Exception {

        final Context context = ApplicationProvider.getApplicationContext();

        // Delete all previous keys and shared preferences.

        String filePath = context.getFilesDir().getParent() + "/shared_prefs/"
                + "__androidx_security__crypto_encrypted_prefs__";
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        SharedPreferences notEncryptedSharedPrefs = context.getSharedPreferences(PREFS_FILE,
                MODE_PRIVATE);
        notEncryptedSharedPrefs.edit().clear().commit();

        filePath = context.getFilesDir().getParent() + "/shared_prefs/"
                + PREFS_FILE;
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        SharedPreferences encryptedSharedPrefs = context.getSharedPreferences("TinkTestPrefs",
                MODE_PRIVATE);
        encryptedSharedPrefs.edit().clear().commit();

        filePath = context.getFilesDir().getParent() + "/shared_prefs/"
                + "TinkTestPrefs";
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        // Delete MasterKeys
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry("_androidx_security_master_key_");
    }

    @Test
    public void testCreateDefaultKey() throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        assertKeyExists(masterKeyAlias);
    }

    @Test
    public void testCreateKeyWithParamSpec() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(MasterKeys.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();
        String masterKeyAlias = MasterKeys.getOrCreate(spec);
        assertKeyExists(masterKeyAlias);
    }

    @Test
    public void testValidateKeyWithAuth() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(MasterKeys.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(60)
                .build();
        // Validate throws if it fails.
        MasterKeys.validate(spec);
    }

    @Test
    public void testCreateWithWrongPurposeFails() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(MasterKeys.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();

        try {
            String masterKeyAlias = MasterKeys.getOrCreate(spec);
            Assert.fail("Key created with wrong purpose: " + masterKeyAlias);
        } catch (IllegalArgumentException iae) {
            // Expected -- Test pass
        }
    }

    @Test
    public void testCreateWithWrongBlockModeFails() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(MasterKeys.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CTR)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();

        try {
            String masterKeyAlias = MasterKeys.getOrCreate(spec);
            Assert.fail("Key created with wrong block mode: " + masterKeyAlias);
        } catch (IllegalArgumentException iae) {
            // Expected -- Test pass
        }
    }

    @Test
    public void testCreateWithWrongPaddingFails() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(MasterKeys.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setKeySize(KEY_SIZE)
                .build();

        try {
            String masterKeyAlias = MasterKeys.getOrCreate(spec);
            Assert.fail("Key created with wrong key size: " + masterKeyAlias);
        } catch (IllegalArgumentException iae) {
            // Expected -- Test pass
        }
    }

    @Test
    public void testCreateWithWrongKeySizeFails() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(MasterKeys.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE * 2)
                .build();

        try {
            String masterKeyAlias = MasterKeys.getOrCreate(spec);
            Assert.fail("Key created with wrong key size: " + masterKeyAlias);
        } catch (IllegalArgumentException iae) {
            // Expected -- Test pass
        }
    }

    @Test
    public void testValidateKeyWithPerUseAuthFails() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(MasterKeys.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(0)
                .build();
        try {
            MasterKeys.validate(spec);
            Assert.fail("KeyGenParamSpec validated with per-use authentication required");
        } catch (IllegalArgumentException iae) {
            // Expected -- Test pass
        }
    }

    private void assertKeyExists(String keyAlias) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            Assert.assertTrue(keyStore.isKeyEntry(keyAlias));
        } catch (Exception e) {
            Assert.fail("Exception checking for key: " + keyAlias);
            throw new RuntimeException(e);
        }
    }
}
