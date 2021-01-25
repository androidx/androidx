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

import static android.content.Context.MODE_PRIVATE;

import static androidx.security.crypto.MasterKey.KEYSTORE_PATH_URI;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKeyManager;
import com.google.crypto.tink.daead.AesSivKeyManager;
import com.google.crypto.tink.daead.DeterministicAeadConfig;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.subtle.Base64;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class EncryptedSharedPreferencesTest {

    private Context mContext;
    private MasterKey mMasterKey;

    private static final String PREFS_FILE = "test_shared_prefs";

    private static final String KEY_KEYSET_ALIAS =
            "__androidx_security_crypto_encrypted_prefs_key_keyset__";
    private static final String VALUE_KEYSET_ALIAS =
            "__androidx_security_crypto_encrypted_prefs_value_keyset__";

    @Before
    public void setup() throws Exception {

        mContext = ApplicationProvider.getApplicationContext();

        // Delete all previous keys and shared preferences.

        String filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + "__androidx_security__crypto_encrypted_prefs__";
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        SharedPreferences notEncryptedSharedPrefs = mContext.getSharedPreferences(PREFS_FILE,
                MODE_PRIVATE);
        notEncryptedSharedPrefs.edit().clear().commit();

        filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + PREFS_FILE;
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        SharedPreferences encryptedSharedPrefs = mContext.getSharedPreferences("TinkTestPrefs",
                MODE_PRIVATE);
        encryptedSharedPrefs.edit().clear().commit();

        filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + "TinkTestPrefs";
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        // Delete MasterKeys
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry("_androidx_security_master_key_");

        mMasterKey = new MasterKey.Builder(mContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
    }

    @Test
    public void testWriteSharedPrefs() throws Exception {

        SharedPreferences sharedPreferences = EncryptedSharedPreferences
                .create(mContext,
                        PREFS_FILE,
                        mMasterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // String Test
        final String stringTestKey = "StringTest";
        final String stringTestValue = "THIS IS A TEST STRING";
        editor.putString(stringTestKey, stringTestValue);


        final SharedPreferences.OnSharedPreferenceChangeListener listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                            String key) {
                        Assert.assertEquals(stringTestValue,
                                sharedPreferences.getString(stringTestKey, null));

                    }
                };

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        editor.commit();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);


        // String Set Test
        String stringSetTestKey = "StringSetTest";
        Set<String> stringSetValue = new ArraySet<>();
        stringSetValue.add("Test1");
        stringSetValue.add("Test2");
        editor.putStringSet(stringSetTestKey, stringSetValue);


        // Int Test
        String intTestKey = "IntTest";
        int intTestValue = 1000;
        editor.putInt(intTestKey, intTestValue);

        // Long Test
        String longTestKey = "LongTest";
        long longTestValue = 500L;
        editor.putLong(longTestKey, longTestValue);

        // Boolean Test
        String booleanTestKey = "BooleanTest";
        boolean booleanTestValue = true;
        editor.putBoolean(booleanTestKey, booleanTestValue);

        // Float Test
        String floatTestKey = "FloatTest";
        float floatTestValue = 250.5f;
        editor.putFloat(floatTestKey, floatTestValue);

        // Null Key Test
        String nullKey = null;
        String nullStringValue = "NULL_KEY";
        editor.putString(nullKey, nullStringValue);

        editor.commit();

        // String Test Assertion
        Assert.assertEquals(stringTestKey + " has the wrong value",
                stringTestValue,
                sharedPreferences.getString(stringTestKey, null));

        // StringSet Test Assertion
        Set<String> stringSetPrefsValue = sharedPreferences.getStringSet(stringSetTestKey, null);
        String stringSetTestValue = null;
        if (!stringSetPrefsValue.isEmpty()) {
            stringSetTestValue = stringSetPrefsValue.iterator().next();
        }
        Assert.assertEquals(stringSetTestKey + " has the wrong value",
                ((ArraySet<String>) stringSetValue).valueAt(0),
                stringSetTestValue);

        // Int Test Assertion
        Assert.assertEquals(intTestKey + " has the wrong value",
                intTestValue,
                sharedPreferences.getInt(intTestKey, 0));

        // Long Test Assertion
        Assert.assertEquals(longTestKey + " has the wrong value",
                longTestValue,
                sharedPreferences.getLong(longTestKey, 0L));

        // Boolean Test Assertion
        Assert.assertEquals(booleanTestKey + " has the wrong value",
                booleanTestValue,
                sharedPreferences.getBoolean(booleanTestKey, false));

        // Float Test Assertion
        Assert.assertEquals(floatTestValue,
                sharedPreferences.getFloat(floatTestKey, 0.0f),
                0.0f);

        // Null Key Test Assertion
        Assert.assertEquals(nullKey + " has the wrong value",
                nullStringValue,
                sharedPreferences.getString(nullKey, null));

        Assert.assertTrue(nullKey + " should exist", sharedPreferences.contains(nullKey));

        // Test Remove
        editor.remove(nullKey);
        editor.apply();

        Assert.assertEquals(nullKey + " should have been removed.",
                null,
                sharedPreferences.getString(nullKey, null));

        Assert.assertFalse(nullKey + " should not exist",
                sharedPreferences.contains(nullKey));

        // Null String Key and value Test Assertion
        editor.putString(null, null);
        editor.putStringSet(null, null);
        editor.commit();
        Assert.assertEquals(null + " should not have a value",
                null,
                sharedPreferences.getString(null, null));

        // Null StringSet Key and value Test Assertion

        Assert.assertEquals(null + " should not have a value",
                null,
                sharedPreferences.getStringSet(null, null));

        // Test overwriting keys
        String twiceKey = "KeyTwice";
        String twiceVal1 = "FirstVal";
        String twiceVal2 = "SecondVal";
        editor.putString(twiceKey, twiceVal1);
        editor.commit();

        Assert.assertEquals(twiceVal1 + " should be the value",
                twiceVal1,
                sharedPreferences.getString(twiceKey, null));

        editor.putString(twiceKey, twiceVal2);
        editor.commit();

        Assert.assertEquals(twiceVal2 + " should be the value",
                twiceVal2,
                sharedPreferences.getString(twiceKey, null));

        // Test getAll
        Map<String, ?> all = sharedPreferences.getAll();

        Assert.assertTrue("Get all should have supplied " + twiceKey,
                all.containsKey(twiceKey));

        Assert.assertFalse("Get all should have removed " + KEY_KEYSET_ALIAS,
                all.containsKey(KEY_KEYSET_ALIAS));

        Assert.assertFalse("Get all should have removed " + VALUE_KEYSET_ALIAS,
                all.containsKey(VALUE_KEYSET_ALIAS));

        System.out.println("All entries " + all);

        // Test using reserved keys

        boolean exceptionThrown = false;

        // try overwriting keyset
        try {
            editor.putString(KEY_KEYSET_ALIAS, "Not a keyset");
        } catch (SecurityException ex) {
            exceptionThrown = true;
        }

        Assert.assertTrue("Access to " + KEY_KEYSET_ALIAS + " should have been blocked.",
                exceptionThrown);
        exceptionThrown = false;

        // try removing keyset
        try {
            editor.remove(KEY_KEYSET_ALIAS);
        } catch (SecurityException ex) {
            exceptionThrown = true;
        }

        Assert.assertTrue("Access to " + KEY_KEYSET_ALIAS + " should have been blocked.",
                exceptionThrown);
        exceptionThrown = false;

        // try calling contains
        try {
            sharedPreferences.contains(VALUE_KEYSET_ALIAS);
        } catch (SecurityException ex) {
            exceptionThrown = true;
        }

        Assert.assertTrue("Access to " + VALUE_KEYSET_ALIAS + " should have been blocked.",
                exceptionThrown);
        exceptionThrown = false;

        // try calling get
        try {
            sharedPreferences.getString(VALUE_KEYSET_ALIAS, null);
        } catch (SecurityException ex) {
            exceptionThrown = true;
        }

        Assert.assertTrue("Access to " + VALUE_KEYSET_ALIAS + " should have been blocked.",
                exceptionThrown);

        // test clear

        editor.clear();
        editor.commit();

        editor.putString("New Data", "New");
        editor.commit();

        Assert.assertEquals("Data should be equal", "New",
                sharedPreferences.getString("New Data", null));

        Assert.assertEquals("Data should not exist", null,
                sharedPreferences.getString(twiceKey, null));

        editor.clear();
        editor.commit();

        // test clear after put with apply
        editor.putString("New Data", "New");
        editor.apply();
        editor.clear();
        editor.apply();

        Assert.assertEquals("Get all size should be equal", 0,
                sharedPreferences.getAll().size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteSharedPrefsTink() throws Exception {
        String tinkTestPrefs = "TinkTestPrefs";
        String testKey = "TestKey";
        String testValue = "TestValue";

        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences
                .create(mContext,
                        tinkTestPrefs,
                        mMasterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

        SharedPreferences.Editor encryptedEditor = encryptedSharedPreferences.edit();
        encryptedEditor.putString(testKey, testValue);
        encryptedEditor.commit();

        // Set up Tink
        DeterministicAeadConfig.register();
        AeadConfig.register();

        KeysetHandle daeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(AesSivKeyManager.aes256SivTemplate())
                .withSharedPref(mContext,
                        "__androidx_security_crypto_encrypted_prefs_key_keyset__", tinkTestPrefs)
                .withMasterKeyUri(KEYSTORE_PATH_URI + "_androidx_security_master_key_")
                .build().getKeysetHandle();

        DeterministicAead deterministicAead =
                daeadKeysetHandle.getPrimitive(DeterministicAead.class);
        byte[] encryptedKey = deterministicAead.encryptDeterministically(testKey.getBytes(UTF_8),
                tinkTestPrefs.getBytes());
        String encodedKey = Base64.encode(encryptedKey);
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(tinkTestPrefs,
                MODE_PRIVATE);

        boolean keyExists = sharedPreferences.contains(encodedKey);
        Assert.assertTrue("Key should exist if Tink is compatible.", keyExists);

        KeysetHandle aeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
                .withSharedPref(mContext,
                        "__androidx_security_crypto_encrypted_prefs_value_keyset__", tinkTestPrefs)
                .withMasterKeyUri(KEYSTORE_PATH_URI + "_androidx_security_master_key_")
                .build().getKeysetHandle();
        Aead aead = aeadKeysetHandle.getPrimitive(Aead.class);
        String encryptedValue = sharedPreferences.getString(encodedKey, null);
        byte[] cipherText = Base64.decode(encryptedValue);
        ByteBuffer values = ByteBuffer.wrap(aead.decrypt(cipherText, encodedKey.getBytes(UTF_8)));
        values.getInt(); // throw type away, we know its a String
        int length = values.getInt();
        ByteBuffer stringSlice = values.slice();
        stringSlice.limit(length);
        String actualValue = UTF_8.decode(stringSlice).toString();
        Assert.assertEquals("String should have been equal to original",
                actualValue,
                testValue);
    }

    @Test
    public void testReentrantCallbackCalls() throws Exception {
        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences
                .create(mContext,
                        PREFS_FILE,
                        mMasterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

        encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                            String key) {
                        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
                    }
                });

        encryptedSharedPreferences.registerOnSharedPreferenceChangeListener(
                (sharedPreferences, key) -> {
                    // No-op
                });

        SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
        editor.putString("someKey", "someValue");
        editor.apply();
    }

}
