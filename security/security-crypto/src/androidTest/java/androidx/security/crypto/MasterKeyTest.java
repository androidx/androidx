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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

@MediumTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class MasterKeyTest {
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
        MasterKey masterKey = new MasterKey.Builder(ApplicationProvider.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        assertKeyExists(masterKey.getKeyAlias());
    }

    @Test
    public void testCreateRenamedKey() throws GeneralSecurityException, IOException {
        final String testAlias = "TestKeyAlias";
        MasterKey masterKey =
                new MasterKey.Builder(ApplicationProvider.getApplicationContext(), testAlias)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
        Assert.assertEquals(masterKey.getKeyAlias(), testAlias);
        assertKeyExists(masterKey.getKeyAlias());
    }

    @Test
    public void testCreateKeyWithParamSpec() throws GeneralSecurityException, IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();
        MasterKey masterKey = new MasterKey.Builder(ApplicationProvider.getApplicationContext())
                .setKeyGenParameterSpec(spec)
                .build();
        assertKeyExists(masterKey.getKeyAlias());
    }

    @Test
    public void testCreateKeyWithParamSpecAndAlias() throws GeneralSecurityException,
            IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder("test_key_alias",
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();
        MasterKey masterKey = new MasterKey.Builder(
                ApplicationProvider.getApplicationContext(), "test_key_alias")
                .setKeyGenParameterSpec(spec)
                .build();
        assertKeyExists(masterKey.getKeyAlias());
    }

    @Test
    public void testCreateKeyWithParamSpecWithDifferentAliasFails() throws GeneralSecurityException,
            IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder("test_key_alias",
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new MasterKey.Builder(ApplicationProvider.getApplicationContext())
                    .setKeyGenParameterSpec(spec)
                    .build();
        });
    }

    @Test
    public void testCheckIfKeyIsKeyStoreBacked() throws GeneralSecurityException,
            IOException {
        MasterKey masterKey = new MasterKey.Builder(ApplicationProvider.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        Assert.assertTrue(masterKey.isKeyStoreBacked());
        assertKeyExists(masterKey.getKeyAlias());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testUseOfSchemeAndParamsFails() throws GeneralSecurityException,
            IOException {
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(MasterKeys.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build();

        try {
            MasterKey ignored = new MasterKey.Builder(ApplicationProvider.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .setKeyGenParameterSpec(spec)
                    .build();
            Assert.fail("Could create key with both scheme + KeyGenParameterSpec");
        } catch (IllegalArgumentException iae) {
            // Pass.
        }
    }

    @Test
    public void testCheckGettersAreCallable() throws GeneralSecurityException,
            IOException {
        MasterKey masterKey = new MasterKey.Builder(ApplicationProvider.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        Assert.assertFalse(masterKey.isUserAuthenticationRequired());
        Assert.assertFalse(masterKey.isStrongBoxBacked());
    }

    static void assertKeyExists(String keyAlias) {
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
