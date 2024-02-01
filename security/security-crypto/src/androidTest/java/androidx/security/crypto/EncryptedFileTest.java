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

import static org.junit.Assert.assertTrue;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;

import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.ArrayList;

@MediumTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class EncryptedFileTest {
    private static final String KEYSET_ALIAS = "__androidx_security_crypto_encrypted_file_keyset__";
    private static final String PREFS_FILE = "__androidx_security_crypto_encrypted_file_pref__";
    private static final String CUSTOM_PREF_NAME = "CUSTOMPREFNAME";

    private Context mContext;
    private MasterKey mMasterKey;

    /**
     * Enum for all test files used in the test suite. Each file will be deleted if it exists
     * before each test is ran.
     */
    private enum TestFileName {
        NOTHING_TO_SEE_HERE("nothing_to_see_here"),
        NOTHING_TO_SEE_HERE_CUSTOM("nothing_to_see_here_custom"),
        TINK_TEST_FILE("tink_test_file"),
        NON_EXISTING("non-existing.data"),
        ENCRYPTED_FILE_1("encrypted_file_1"),
        ENCRYPTED_FILE_2("encrypted_file_2");

        private final String mText;

        TestFileName(final String text) {
            mText = text;
        }

        @NonNull
        @Override
        public String toString() {
            return mText;
        }
    }

    @Before
    public void setup() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                PREFS_FILE, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();

        SharedPreferences customSharedPreferences = mContext.getSharedPreferences(
                CUSTOM_PREF_NAME, Context.MODE_PRIVATE);
        customSharedPreferences.edit().clear().commit();

        String appFolderPath = mContext.getFilesDir().getParent();

        // Delete old keys stored in preferences file
        String prefsFilePath = appFolderPath + "/shared_prefs/" + PREFS_FILE;
        File prefFile = new File(prefsFilePath);
        prefFile.delete();

        // Delete test files
        for (TestFileName fileName : TestFileName.values()) {
            File dataFile = new File(mContext.getFilesDir(), fileName.toString());
            dataFile.delete();
        }

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
        final String fileName = TestFileName.NOTHING_TO_SEE_HERE.toString();

        // Write
        EncryptedFile encryptedFile = new EncryptedFile.Builder(mContext,
                new File(mContext.getFilesDir(),
                        fileName), mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();

        OutputStream outputStream = encryptedFile.openFileOutput();
        outputStream.write(fileContent.getBytes(UTF_8));
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
                fileContent, new String(plainText, UTF_8));
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
        assertTrue("File should have failed opening.", inputFailed);

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
        assertTrue("File should have failed writing.", outputFailed);

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWriteReadEncryptedFileWithAlias() throws Exception {
        final String fileContent = "Don't tell anyone...";
        final String fileName = TestFileName.NOTHING_TO_SEE_HERE.toString();

        // Write
        EncryptedFile encryptedFile = new EncryptedFile.Builder(new File(mContext.getFilesDir(),
                fileName), mContext, mMasterKey.getKeyAlias(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();

        OutputStream outputStream = encryptedFile.openFileOutput();
        outputStream.write(fileContent.getBytes(UTF_8));
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
                fileContent, new String(plainText, UTF_8));
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
        assertTrue("File should have failed opening.", inputFailed);

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
        assertTrue("File should have failed writing.", outputFailed);
    }

    @Test
    public void testReadNonExistingFileThrows() throws Exception {
        final File nonExisting = new File(mContext.getFilesDir(),
                TestFileName.NON_EXISTING.toString());
        if (nonExisting.exists()) {
            assertTrue(nonExisting.delete());
        }
        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                mContext,
                nonExisting,
                mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();

        Assert.assertThrows(FileNotFoundException.class, encryptedFile::openFileInput);
    }

    @Test
    public void testWriteReadEncryptedFileCustomPrefs() throws Exception {
        final String fileContent = "Don't tell anyone...!!!!!";
        final String fileName = TestFileName.NOTHING_TO_SEE_HERE_CUSTOM.toString();

        // Write
        EncryptedFile encryptedFile = new EncryptedFile.Builder(mContext,
                new File(mContext.getFilesDir(),
                        fileName), mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .setKeysetAlias("CustomKEYALIAS")
                .setKeysetPrefName(CUSTOM_PREF_NAME)
                .build();

        OutputStream outputStream = encryptedFile.openFileOutput();
        outputStream.write(fileContent.getBytes(UTF_8));
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
                fileContent, new String(plainText, UTF_8));
        inputStream.close();

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(CUSTOM_PREF_NAME,
                Context.MODE_PRIVATE);
        boolean containsKeyset = sharedPreferences.contains("CustomKEYALIAS");
        assertTrue("Keyset should have existed.", containsKeyset);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void tinkTest() throws Exception {
        final String fileContent = "Don't tell anyone...";
        final String fileName = TestFileName.TINK_TEST_FILE.toString();
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
        KeysetHandle streamingAeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate())
                .withSharedPref(mContext,
                        KEYSET_ALIAS,
                        PREFS_FILE)
                .withMasterKeyUri(MasterKey.KEYSTORE_PATH_URI + mMasterKey.getKeyAlias())
                .build().getKeysetHandle();

        StreamingAead streamingAead = com.google.crypto.tink.streamingaead.StreamingAeadFactory
                .getPrimitive(streamingAeadKeysetHandle);

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
                fileContent, new String(plainText, UTF_8));
        inputStream.close();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void multiThreadFileCreate() throws Exception {
        final int fileSize = 2 << 14;
        final int fileCount = 10;
        ArrayList<File> files = new ArrayList<>();
        final File directory = mContext.getFilesDir();

        for (int i = 0; i < fileCount; i++) {
            File file = new File(directory + "/multiThreadFileCreate-file-" + i);
            file.delete(); // Clear out file from previous run if it exists
            files.add(file);
        }

        // Inlining what should just be Assume.assumeTrue(SDK_INT >= M) because AndroidStudio's
        // static analysis can't understand that.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            throw new AssumptionViolatedException("API v23 or higher is required to run this test");
        }

        final String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        ArrayList<Thread> threads = new ArrayList<>();
        ArrayList<Exception> exceptions = new ArrayList<>();

        // Create a thread for each encrypted file
        for (File file : files) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                                file,
                                mContext,
                                masterKeyAlias,
                                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                        ).build();

                        try (OutputStream outputStream = encryptedFile.openFileOutput()) {
                            byte[] buffer = new byte[fileSize];
                            outputStream.write(buffer, 0, buffer.length);
                            outputStream.flush();
                        }
                    } catch (Exception e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                    }
                }
            };

            threads.add(thread);
        }

        // Start each thread
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for each thread to finish
        for (Thread thread : threads) {
            thread.join();
        }

        if (exceptions.size() > 0) {
            System.err.println(exceptions.size() + " errors were thrown during file encryption");

            for (Exception exception : exceptions) {
                exception.printStackTrace();
            }

            System.err.println("Throwing the first exception");
            throw exceptions.get(0);
        }

        // Decrypt files serially
        for (File file : files) {
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    file,
                    mContext,
                    masterKeyAlias,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            try (InputStream inputStream = encryptedFile.openFileInput()) {
                byte[] buffer = new byte[fileSize];
                inputStream.read(buffer); // Will throw IOException if decryption fails
            }
        }
    }
}
