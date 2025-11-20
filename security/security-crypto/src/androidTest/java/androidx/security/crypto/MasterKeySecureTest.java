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

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * These are tests that require the device to have a lockscreen enabled.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class MasterKeySecureTest {
    private static final String PREFS_FILE = "test_shared_prefs";

    @Before
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setup() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();

        KeyguardManager keyguardManager = context.getSystemService(KeyguardManager.class);
        Assume.assumeTrue(keyguardManager != null);
        // You need to enable screen lock to pass this assumption
        Assume.assumeTrue(keyguardManager.isDeviceSecure());

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
    public void testCreateKeyWithAuthenicationRequired() throws GeneralSecurityException,
            IOException {
        MasterKey masterKey = new MasterKey.Builder(ApplicationProvider.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setUserAuthenticationRequired(true, 10)
                .build();
        MasterKeyTest.assertKeyExists(masterKey.getKeyAlias());
        Assert.assertTrue(masterKey.isUserAuthenticationRequired());
        Assert.assertEquals(masterKey.getUserAuthenticationValidityDurationSeconds(), 10);
    }
}
