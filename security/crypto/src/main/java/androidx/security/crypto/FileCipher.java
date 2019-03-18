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

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.security.SecureConfig;
import androidx.security.context.SecureContextCompat;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Class used to create and read encrypted files. Provides implementations
 * of EncryptedFileInput/Output Streams.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FileCipher {

    private String mFileName;
    private String mKeyPairAlias;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    private SecureContextCompat.EncryptedFileInputStreamListener mListener;

    SecureConfig mSecureConfig;

    public FileCipher(@NonNull String fileName, @NonNull FileInputStream fileInputStream,
                      @NonNull SecureConfig secureConfig, @NonNull Executor executor,
                      @NonNull SecureContextCompat.EncryptedFileInputStreamListener listener)
            throws IOException {
        mFileName = fileName;
        mFileInputStream = fileInputStream;
        mSecureConfig = secureConfig;
        EncryptedFileInputStream encryptedFileInputStream =
                new EncryptedFileInputStream(mFileInputStream);
        setEncryptedFileInputStreamListener(listener);
        encryptedFileInputStream.decrypt(listener);
    }

    public FileCipher(@NonNull String keyPairAlias, @NonNull FileOutputStream fileOutputStream,
                      @NonNull SecureConfig secureConfig) {
        mKeyPairAlias = keyPairAlias;
        mFileOutputStream = new EncryptedFileOutputStream(mFileName, mKeyPairAlias,
                fileOutputStream);
        mSecureConfig = secureConfig;
    }

    /**
     * @param listener the listener to call back on
     */
    public void setEncryptedFileInputStreamListener(
            @NonNull SecureContextCompat.EncryptedFileInputStreamListener listener) {
        mListener = listener;
    }

    /**
     * @return  the file output stream
     */
    @NonNull
    public FileOutputStream getFileOutputStream() {
        return mFileOutputStream;
    }

    /**
     * @return the file input stream
     */
    @NonNull
    public FileInputStream getFileInputStream() {
        return mFileInputStream;
    }

    /**
     * Encrypted file output stream
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class EncryptedFileOutputStream extends FileOutputStream {

        private static final String TAG = "EncryptedFOS";

        FileOutputStream mFileOutputStream;
        private String mKeyPairAlias;

        EncryptedFileOutputStream(String name, String keyPairAlias,
                FileOutputStream fileOutputStream) {
            super(new FileDescriptor());
            this.mKeyPairAlias = keyPairAlias;
            this.mFileOutputStream = fileOutputStream;
        }

        String getAsymKeyPairAlias() {
            return this.mKeyPairAlias;
        }

        @Override
        public void write(@NonNull byte[] b) {
            SecureKeyStore secureKeyStore = SecureKeyStore.getDefault();
            if (!secureKeyStore.keyExists(getAsymKeyPairAlias())) {
                SecureKeyGenerator keyGenerator = SecureKeyGenerator.getDefault();
                keyGenerator.generateAsymmetricKeyPair(getAsymKeyPairAlias());
            }
            SecureKeyGenerator secureKeyGenerator = SecureKeyGenerator.getDefault();
            final EphemeralSecretKey secretKey = secureKeyGenerator.generateEphemeralDataKey();
            final SecureCipher secureCipher = SecureCipher
                    .getDefault(mSecureConfig.getBiometricKeyAuthCallback());
            final Pair<byte[], byte[]> encryptedData =
                    secureCipher.encryptEphemeralData(secretKey, b);
            secureCipher.encryptAsymmetric(getAsymKeyPairAlias(),
                    secretKey.getEncoded(),
                    new SecureCipher.SecureAsymmetricEncryptionListener() {
                        public void encryptionComplete(byte[] encryptedEphemeralKey) {
                            byte[] encodedData = secureCipher.encodeEphemeralData(
                            getAsymKeyPairAlias().getBytes(), encryptedEphemeralKey,
                            encryptedData.first, encryptedData.second);
                            secretKey.destroy();
                            try {
                                mFileOutputStream.write(encodedData);
                            } catch (IOException e) {
                            Log.e(TAG, "Failed to write secure file.");
                            e.printStackTrace();
                        }
                    }
                });
        }

        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must write all data "
                    + "simultaneously. Call #write(byte[]).");
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must write all data "
                    + "simultaneously. Call #write(byte[]).");
        }

        @Override
        public void close() throws IOException {
            mFileOutputStream.close();
        }

        @NonNull
        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, you must write all data "
                    + "simultaneously. Call #write(byte[]).");
        }

        @Override
        protected void finalize() throws IOException {
            super.finalize();
        }

        @Override
        public void flush() throws IOException {
            mFileOutputStream.flush();
        }
    }


    /**
     * Encrypted file input stream
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class EncryptedFileInputStream extends FileInputStream {

        // Was 25 characters, truncating to fix compile error
        private static final String TAG = "EncryptedFIS";

        private FileInputStream mFileInputStream;
        byte[] mDecryptedData;
        private int mReadStatus = 0;

        EncryptedFileInputStream(FileInputStream fileInputStream) {
            super(new FileDescriptor());
            this.mFileInputStream = fileInputStream;
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must read all data "
                    + "simultaneously. Call #read(byte[]).");
        }

        void decrypt(final SecureContextCompat.EncryptedFileInputStreamListener listener)
                throws IOException {
            final EncryptedFileInputStream thisStream = this;
            if (this.mDecryptedData == null) {
                try {
                    byte[] encodedData = new byte[mFileInputStream.available()];
                    mReadStatus = mFileInputStream.read(encodedData);
                    SecureCipher secureCipher = SecureCipher.getDefault(
                            mSecureConfig.getBiometricKeyAuthCallback());
                    secureCipher.decryptEncodedData(encodedData,
                            new SecureCipher.SecureDecryptionListener() {
                                public void decryptionComplete(byte[] clearText) {
                                    thisStream.mDecryptedData = clearText;
                                    //Binder.clearCallingIdentity();
                                    listener.onEncryptedFileInput(thisStream);
                                }
                            });
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        private void destroyCache() {
            if (mDecryptedData != null) {
                Arrays.fill(mDecryptedData, (byte) 0);
                mDecryptedData = null;
            }
        }

        @Override
        public int read(@NonNull byte[] b) {
            System.arraycopy(mDecryptedData, 0, b, 0, mDecryptedData.length);
            return mReadStatus;
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must read all data "
                    + "simultaneously. Call #read(byte[]).");
        }

        @Override
        public long skip(long n) throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must read all data "
                    + "simultaneously. Call #read(byte[]).");
        }

        @Override
        public int available() {
            return mDecryptedData.length;
        }

        @Override
        public void close() throws IOException {
            destroyCache();
            mFileInputStream.close();
        }

        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, you must read all data "
                    + "simultaneously. Call #read(byte[]).");
        }

        @Override
        protected void finalize() throws IOException {
            destroyCache();
            super.finalize();
        }

        @Override
        public synchronized void mark(int readlimit) {
            throw new UnsupportedOperationException("For encrypted files, you must read all data "
                    + "simultaneously. Call #read(byte[]).");
        }

        @Override
        public synchronized void reset() throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must read all data "
                    + "simultaneously. Call #read(byte[]).");
        }

        @Override
        public boolean markSupported() {
            return false;
        }

    }

}
