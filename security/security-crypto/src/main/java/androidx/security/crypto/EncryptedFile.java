/*
 * Copyright 2018 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.crypto.tink.KeyTemplate;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;

/**
 * Class used to create and read encrypted files.
 * <br />
 * <br />
 * <b>WARNING</b>: The encrypted file should not be backed up with Auto Backup. When restoring the
 * file it is likely the key used to encrypt it will no longer be present. You should exclude all
 * <code>EncryptedFile</code>s from backup using
 * <a href="https://developer.android.com/guide/topics/data/autobackup#IncludingFiles">backup rules</a>.
 * Be aware that if you are not explicitly calling <code>setKeysetPrefName()</code> there is also a
 * silently-created default preferences file created at
 * <pre>
 *     ApplicationProvider
 *          .getApplicationContext()
 *          .getFilesDir()
 *          .getParent() + "/shared_prefs/__androidx_security_crypto_encrypted_file_pref__"
 * </pre>
 *
 * This preferences file (or any others created with a custom specified location) also should be
 * excluded from backups.
 * <br />
 * <br />
 * Basic use of the class:
 *
 * <pre>
 *  MasterKey masterKey = new MasterKey.Builder(context)
 *      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
 *      .build();
 *
 *  File file = new File(context.getFilesDir(), "secret_data");
 *  EncryptedFile encryptedFile = EncryptedFile.Builder(
 *      context,
 *      file,
 *      masterKey,
 *      EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
 *  ).build();
 *
 *  // write to the encrypted file
 *  FileOutputStream encryptedOutputStream = encryptedFile.openFileOutput();
 *
 *  // read the encrypted file
 *  FileInputStream encryptedInputStream = encryptedFile.openFileInput();
 * </pre>
 */
public final class EncryptedFile {

    private static final String KEYSET_PREF_NAME =
            "__androidx_security_crypto_encrypted_file_pref__";
    private static final String KEYSET_ALIAS =
            "__androidx_security_crypto_encrypted_file_keyset__";

    final File mFile;
    final Context mContext;
    final String mMasterKeyAlias;
    final StreamingAead mStreamingAead;

    EncryptedFile(
            @NonNull File file,
            @NonNull String masterKeyAlias,
            @NonNull StreamingAead streamingAead,
            @NonNull Context context) {
        mFile = file;
        mContext = context;
        mMasterKeyAlias = masterKeyAlias;
        mStreamingAead = streamingAead;
    }

    /**
     * The encryption scheme to encrypt files.
     */
    public enum FileEncryptionScheme {
        /**
         * The file content is encrypted using StreamingAead with AES-GCM, with the file name as
         * associated data.
         *
         * <p>For more information please see the Tink documentation:
         *
         * <p><a href="https://google.github.io/tink/javadoc/tink/1.7.0/com/google/crypto/tink/streamingaead/AesGcmHkdfStreamingKeyManager.html">AesGcmHkdfStreamingKeyManager</a>.aes256GcmHkdf4KBTemplate()
         */
        AES256_GCM_HKDF_4KB("AES256_GCM_HKDF_4KB");

        private final String mKeyTemplateName;

        FileEncryptionScheme(String keyTemplateName) {
            mKeyTemplateName = keyTemplateName;
        }

        KeyTemplate getKeyTemplate() throws GeneralSecurityException {
            return KeyTemplates.get(mKeyTemplateName);
        }
    }

    /**
     * Builder class to configure EncryptedFile
     */
    public static final class Builder {
        private static final Object sLock = new Object();

        /**
         * Builder for an EncryptedFile.
         *
         * <p>If the <code>masterKeyAlias</code> used here is for a key that is not yet
         * created, this method will not be thread safe. Use the alternate signature that is not
         * deprecated for multi-threaded contexts.
         *
         * @deprecated Use {@link #Builder(Context, File, MasterKey, FileEncryptionScheme)} instead.
         */
        @Deprecated
        public Builder(@NonNull File file,
                @NonNull Context context,
                @NonNull String masterKeyAlias,
                @NonNull FileEncryptionScheme fileEncryptionScheme) {
            mFile = file;
            mFileEncryptionScheme = fileEncryptionScheme;
            mContext = context.getApplicationContext();
            mMasterKeyAlias = masterKeyAlias;
        }

        /**
         * Builder for an EncryptedFile.
         */
        // [StreamFiles]: Because the contents of EncryptedFile are encrypted the use of
        // a FileDescriptor or Streams are intentionally not supported for the following reasons:
        // - The encrypted content is tightly coupled to the current installation of the app. If
        // the app is uninstalled, even if the data remained (such as being stored in a public
        // directory or another DocumentProvider) it would be (intentionally) unrecoverable.
        // - If the API did accept either an already opened FileDescriptor or a stream, then it
        // would be possible for the developer to inadvertently commingle encrypted and plain
        // text data, which, due to the way the API is structured, could render both encrypted
        // and unencrypted data irrecoverable.
        @SuppressLint("StreamFiles")
        public Builder(@NonNull Context context,
                @NonNull File file,
                @NonNull MasterKey masterKey,
                @NonNull FileEncryptionScheme fileEncryptionScheme) {
            mFile = file;
            mFileEncryptionScheme = fileEncryptionScheme;
            mContext = context.getApplicationContext();
            mMasterKeyAlias = masterKey.getKeyAlias();
        }

        // Required parameters
        File mFile;
        final FileEncryptionScheme mFileEncryptionScheme;
        final Context mContext;
        final String mMasterKeyAlias;

        // Optional parameters
        String mKeysetPrefName = KEYSET_PREF_NAME;
        String mKeysetAlias = KEYSET_ALIAS;

        /**
         * @param keysetPrefName The SharedPreferences file to store the keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setKeysetPrefName(@NonNull String keysetPrefName) {
            mKeysetPrefName = keysetPrefName;
            return this;
        }

        /**
         * @param keysetAlias The alias in the SharedPreferences file to store the keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setKeysetAlias(@NonNull String keysetAlias) {
            mKeysetAlias = keysetAlias;
            return this;
        }

        /**
         * @return An EncryptedFile with the specified parameters.
         */
        @NonNull
        public EncryptedFile build() throws GeneralSecurityException, IOException {
            StreamingAeadConfig.register();

            AndroidKeysetManager.Builder keysetManagerBuilder = new AndroidKeysetManager.Builder()
                    .withKeyTemplate(mFileEncryptionScheme.getKeyTemplate())
                    .withSharedPref(mContext, mKeysetAlias, mKeysetPrefName)
                    .withMasterKeyUri(KEYSTORE_PATH_URI + mMasterKeyAlias);

            // Building the keyset manager involves shared pref filesystem operations. To control
            // access to this global state in multi-threaded contexts we need to ensure mutual
            // exclusion of the build() function.
            AndroidKeysetManager androidKeysetManager;
            synchronized (sLock) {
                androidKeysetManager = keysetManagerBuilder.build();
            }

            KeysetHandle streamingAeadKeysetHandle = androidKeysetManager.getKeysetHandle();
            StreamingAead streamingAead =
                    streamingAeadKeysetHandle.getPrimitive(StreamingAead.class);

            return new EncryptedFile(mFile, mKeysetAlias, streamingAead, mContext);
        }
    }

    /**
     * Opens a FileOutputStream for writing that automatically encrypts the data based on the
     * provided settings.
     *
     * <p>Please ensure that the same master key and keyset are  used to decrypt or it
     * will cause failures.
     *
     * @return The FileOutputStream that encrypts all data.
     * @throws GeneralSecurityException when a bad master key or keyset has been used
     * @throws IOException              when the file already exists or is not available for writing
     */
    @NonNull
    public FileOutputStream openFileOutput()
            throws GeneralSecurityException, IOException {
        if (mFile.exists()) {
            throw new IOException("output file already exists, please use a new file: "
                    + mFile.getName());
        }
        FileOutputStream fileOutputStream = new FileOutputStream(mFile);
        OutputStream encryptingStream = mStreamingAead.newEncryptingStream(fileOutputStream,
                mFile.getName().getBytes(UTF_8));
        return new EncryptedFileOutputStream(fileOutputStream.getFD(), encryptingStream);
    }

    /**
     * Opens a FileInputStream that reads encrypted files based on the previous settings.
     *
     * <p>Please ensure that the same master key and keyset are  used to decrypt or it
     * will cause failures.
     *
     * @return The input stream to read previously encrypted data.
     * @throws GeneralSecurityException when a bad master key or keyset has been used
     * @throws FileNotFoundException    when the file was not found
     * @throws IOException              when other I/O errors occur
     */
    @NonNull
    public FileInputStream openFileInput()
            throws GeneralSecurityException, IOException, FileNotFoundException {
        if (!mFile.exists()) {
            throw new FileNotFoundException("file doesn't exist: " + mFile.getName());
        }
        FileInputStream fileInputStream = new FileInputStream(mFile);
        InputStream decryptingStream = mStreamingAead.newDecryptingStream(fileInputStream,
                mFile.getName().getBytes(UTF_8));
        return new EncryptedFileInputStream(fileInputStream.getFD(), decryptingStream);
    }

    /**
     * Encrypted file output stream
     */
    private static final class EncryptedFileOutputStream extends FileOutputStream {

        private final OutputStream mEncryptedOutputStream;

        EncryptedFileOutputStream(FileDescriptor descriptor, OutputStream encryptedOutputStream) {
            super(descriptor);
            mEncryptedOutputStream = encryptedOutputStream;
        }

        @Override
        public void write(@NonNull byte[] b) throws IOException {
            mEncryptedOutputStream.write(b);
        }

        @Override
        public void write(int b) throws IOException {
            mEncryptedOutputStream.write(b);
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            mEncryptedOutputStream.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            mEncryptedOutputStream.close();
        }

        @NonNull
        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, please open the "
                    + "relevant FileInput/FileOutputStream.");
        }

        @Override
        public void flush() throws IOException {
            mEncryptedOutputStream.flush();
        }

    }

    /**
     * Encrypted file input stream
     */
    private static final class EncryptedFileInputStream extends FileInputStream {

        private final InputStream mEncryptedInputStream;

        private final Object mLock = new Object();

        EncryptedFileInputStream(FileDescriptor descriptor,
                InputStream encryptedInputStream) {
            super(descriptor);
            mEncryptedInputStream = encryptedInputStream;
        }

        @Override
        public int read() throws IOException {
            return mEncryptedInputStream.read();
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            return mEncryptedInputStream.read(b);
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            return mEncryptedInputStream.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return mEncryptedInputStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return mEncryptedInputStream.available();
        }

        @Override
        public void close() throws IOException {
            mEncryptedInputStream.close();
        }

        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, please open the "
                    + "relevant FileInput/FileOutputStream.");
        }

        @Override
        public void mark(int readLimit) {
            synchronized (mLock) {
                mEncryptedInputStream.mark(readLimit);
            }
        }

        @Override
        public void reset() throws IOException {
            synchronized (mLock) {
                mEncryptedInputStream.reset();
            }
        }

        @Override
        public boolean markSupported() {
            return mEncryptedInputStream.markSupported();
        }

    }

}
