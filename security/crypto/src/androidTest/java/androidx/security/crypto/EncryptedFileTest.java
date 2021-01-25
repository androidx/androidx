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

import static androidx.security.crypto.MasterKey.KEYSTORE_PATH_URI;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class EncryptedFileTest {
    private Context mContext;
    private MasterKey mMasterKey;

    @Before
    public void setup() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                "__androidx_security_crypto_encrypted_file_pref__", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();

        SharedPreferences customSharedPreferences = mContext.getSharedPreferences(
                "CUSTOMPREFNAME", Context.MODE_PRIVATE);
        customSharedPreferences.edit().clear().commit();

        // Delete old keys for testing
        String filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + "__androidx_security_crypto_encrypted_file_pref__";
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        filePath = mContext.getFilesDir().getParent() + "nothing_to_see_here";
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        File dataFile = new File(mContext.getFilesDir(), "nothing_to_see_here");
        dataFile.delete();

        dataFile = new File(mContext.getFilesDir(), "nothing_to_see_here_custom");
        dataFile.delete();

        dataFile = new File(mContext.getFilesDir(), "tink_test_file");
        dataFile.delete();

        // Delete MasterKeys
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS);

        mMasterKey = new MasterKey.Builder(mContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
    }

    @Test
    public void testWriteReadEncryptedFile() throws Exception {
        final String fileContent = "Don't tell anyone...";
        final String fileName = "nothing_to_see_here";

        // Write

        EncryptedFile encryptedFile = new EncryptedFile.Builder(mContext,
                new File(mContext.getFilesDir(),
                fileName), mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();

        OutputStream outputStream = encryptedFile.openFileOutput();
        outputStream.write(fileContent.getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();

        FileInputStream rawStream = mContext.openFileInput(fileName);
        ByteArrayOutputStream rawByteArrayOutputStream = new ByteArrayOutputStream();
        int rawNextByte = rawStream.read();
        while (rawNextByte != -1) {
            rawByteArrayOutputStream.write(rawNextByte);
            rawNextByte = rawStream.read();
        }
        byte[] rawCipherText = rawByteArrayOutputStream.toByteArray();
        System.out.println("Raw CipherText = " + new String(rawCipherText,
                UTF_8));
        rawStream.close();

        InputStream inputStream = encryptedFile.openFileInput();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int nextByte = inputStream.read();
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte);
            nextByte = inputStream.read();
        }

        byte[] plainText = byteArrayOutputStream.toByteArray();

        System.out.println("Decrypted Data: " + new String(plainText,
                UTF_8));

        Assert.assertEquals(
                "Contents should be equal, data was encrypted.",
                fileContent, new String(plainText, "UTF-8"));
        inputStream.close();


        EncryptedFile existingFileInputCheck = new EncryptedFile.Builder(mContext,
                new File(mContext.getFilesDir(), "FAKE_FILE"), mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();
        boolean inputFailed = false;
        try {
            existingFileInputCheck.openFileInput();
        } catch (IOException ex) {
            inputFailed = true;
        }
        Assert.assertTrue("File should have failed opening.", inputFailed);

        EncryptedFile existingFileOutputCheck = new EncryptedFile.Builder(mContext,
                new File(mContext.getFilesDir(), fileName), mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();
        boolean outputFailed = false;
        try {
            existingFileOutputCheck.openFileOutput();
        } catch (IOException ex) {
            outputFailed = true;
        }
        Assert.assertTrue("File should have failed writing.", outputFailed);

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteReadEncryptedFileWithAlias() throws Exception {
        final String fileContent = "Don't tell anyone...";
        final String fileName = "nothing_to_see_here";

        // Write

        EncryptedFile encryptedFile = new EncryptedFile.Builder(new File(mContext.getFilesDir(),
                        fileName), mContext, mMasterKey.getKeyAlias(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();

        OutputStream outputStream = encryptedFile.openFileOutput();
        outputStream.write(fileContent.getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();

        FileInputStream rawStream = mContext.openFileInput(fileName);
        ByteArrayOutputStream rawByteArrayOutputStream = new ByteArrayOutputStream();
        int rawNextByte = rawStream.read();
        while (rawNextByte != -1) {
            rawByteArrayOutputStream.write(rawNextByte);
            rawNextByte = rawStream.read();
        }
        byte[] rawCipherText = rawByteArrayOutputStream.toByteArray();
        System.out.println("Raw CipherText = " + new String(rawCipherText,
                UTF_8));
        rawStream.close();

        InputStream inputStream = encryptedFile.openFileInput();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int nextByte = inputStream.read();
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte);
            nextByte = inputStream.read();
        }

        byte[] plainText = byteArrayOutputStream.toByteArray();

        System.out.println("Decrypted Data: " + new String(plainText,
                UTF_8));

        Assert.assertEquals(
                "Contents should be equal, data was encrypted.",
                fileContent, new String(plainText, "UTF-8"));
        inputStream.close();


        EncryptedFile existingFileInputCheck = new EncryptedFile.Builder(
                new File(mContext.getFilesDir(), "FAKE_FILE"), mContext,
                mMasterKey.getKeyAlias(), EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();
        boolean inputFailed = false;
        try {
            existingFileInputCheck.openFileInput();
        } catch (IOException ex) {
            inputFailed = true;
        }
        Assert.assertTrue("File should have failed opening.", inputFailed);

        EncryptedFile existingFileOutputCheck = new EncryptedFile.Builder(
                new File(mContext.getFilesDir(), fileName), mContext, mMasterKey.getKeyAlias(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();
        boolean outputFailed = false;
        try {
            existingFileOutputCheck.openFileOutput();
        } catch (IOException ex) {
            outputFailed = true;
        }
        Assert.assertTrue("File should have failed writing.", outputFailed);

    }

    @Test
    public void testWriteReadEncryptedFileCustomPrefs() throws Exception {
        final String fileContent = "Don't tell anyone...!!!!!";
        final String fileName = "nothing_to_see_here_custom";

        // Write
        EncryptedFile encryptedFile = new EncryptedFile.Builder(mContext,
                new File(mContext.getFilesDir(),
                fileName), mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .setKeysetAlias("CustomKEYALIAS")
                .setKeysetPrefName("CUSTOMPREFNAME")
                .build();

        OutputStream outputStream = encryptedFile.openFileOutput();
        outputStream.write(fileContent.getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();

        FileInputStream rawStream = mContext.openFileInput(fileName);
        ByteArrayOutputStream rawByteArrayOutputStream = new ByteArrayOutputStream();
        int rawNextByte = rawStream.read();
        while (rawNextByte != -1) {
            rawByteArrayOutputStream.write(rawNextByte);
            rawNextByte = rawStream.read();
        }
        byte[] rawCipherText = rawByteArrayOutputStream.toByteArray();
        System.out.println("Raw CipherText = " + new String(rawCipherText,
                UTF_8));
        rawStream.close();

        InputStream inputStream = encryptedFile.openFileInput();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int nextByte = inputStream.read();
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte);
            nextByte = inputStream.read();
        }

        byte[] plainText = byteArrayOutputStream.toByteArray();

        System.out.println("Decrypted Data: " + new String(plainText,
                UTF_8));

        Assert.assertEquals(
                "Contents should be equal, data was encrypted.",
                fileContent, new String(plainText, "UTF-8"));
        inputStream.close();

        SharedPreferences sharedPreferences = mContext.getSharedPreferences("CUSTOMPREFNAME",
                Context.MODE_PRIVATE);
        boolean containsKeyset = sharedPreferences.contains("CustomKEYALIAS");
        Assert.assertTrue("Keyset should have existed.", containsKeyset);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void tinkTest() throws Exception {
        final String fileContent = "Don't tell anyone...";
        final String fileName = "tink_test_file";
        File file = new File(mContext.getFilesDir(), fileName);

        // Write
        EncryptedFile encryptedFile = new EncryptedFile.Builder(mContext, file, mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();

        OutputStream outputStream = encryptedFile.openFileOutput();
        outputStream.write(fileContent.getBytes(UTF_8));
        outputStream.flush();
        outputStream.close();

        StreamingAeadConfig.register();
        KeysetHandle streadmingAeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate())
                .withSharedPref(mContext,
                        "__androidx_security_crypto_encrypted_file_keyset__",
                        "__androidx_security_crypto_encrypted_file_pref__")
                .withMasterKeyUri(KEYSTORE_PATH_URI + mMasterKey.getKeyAlias())
                .build().getKeysetHandle();

        StreamingAead streamingAead = com.google.crypto.tink.streamingaead.StreamingAeadFactory
                .getPrimitive(streadmingAeadKeysetHandle);

        FileInputStream fileInputStream = new FileInputStream(file);
        InputStream inputStream = streamingAead.newDecryptingStream(fileInputStream,
                file.getName().getBytes(UTF_8));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int nextByte = inputStream.read();
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte);
            nextByte = inputStream.read();
        }

        byte[] plainText = byteArrayOutputStream.toByteArray();

        System.out.println("Decrypted Data: " + new String(plainText,
                UTF_8));

        Assert.assertEquals(
                "Contents should be equal, data was encrypted.",
                fileContent, new String(plainText, "UTF-8"));
        inputStream.close();
    }

}
