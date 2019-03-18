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


package androidx.security.context;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.security.SecureConfig;
import androidx.security.crypto.FileCipher;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * Class that provides access to an encrypted file input and output streams, as well as encrypted
 * SharedPreferences.
 */
public class SecureContextCompat {

    private static final String TAG = "SecureContextCompat";

    private Context mContext;
    private SecureConfig mSecureConfig;

    private static final String DEFAULT_FILE_ENCRYPTION_KEY = "default_encryption_key";

    /**
     * A listener for getting access to encrypted files. Required when the key requires a
     * Biometric Prompt.
     */
    public interface EncryptedFileInputStreamListener {
        /**
         * @param inputStream
         */
        void onEncryptedFileInput(@NonNull FileInputStream inputStream);
    }

    public SecureContextCompat(@NonNull Context context) {
        this(context, SecureConfig.getDefault());
    }

    public SecureContextCompat(@NonNull Context context, @NonNull SecureConfig secureConfig) {
        mContext = context;
        mSecureConfig = secureConfig;
    }

    /**
     * Open an encrypted private file associated with this Context's application
     * package for reading.
     *
     * @param name The name of the file to open; can not contain path separators.
     * @throws IOException
     */
    public void openEncryptedFileInput(@NonNull String name,
                                       @NonNull Executor executor,
            @NonNull EncryptedFileInputStreamListener listener) throws IOException {
        new FileCipher(name, mContext.openFileInput(name), mSecureConfig, executor, listener);
    }

    /**
     * Open a private encrypted file associated with this Context's application package for
     * writing. Creates the file if it doesn't already exist.
     * <p>
     * The written file will be encrypted with the default keyPairAlias.
     *
     * @param name The name of the file to open; can not contain path separators.
     * @param mode Operating mode.
     * @return The resulting {@link FileOutputStream}.
     * @throws IOException
     */
    @NonNull
    public FileOutputStream openEncryptedFileOutput(@NonNull String name, int mode)
            throws IOException {
        return openEncryptedFileOutput(name, mode, DEFAULT_FILE_ENCRYPTION_KEY);
    }

    /**
     * Open a private encrypted file associated with this Context's application package for
     * writing. Creates the file if it doesn't already exist.
     * <p>
     * The written file will be encrypted with the specified keyPairAlias.
     *
     * @param name         The name of the file to open; can not contain path separators.
     * @param mode         Operating mode.
     * @param keyPairAlias The alias of the KeyPair used for encryption, the KeyPair will be
     *                     created if it does not exist.
     * @return The resulting {@link FileOutputStream}.
     * @throws IOException
     */
    @NonNull
    public FileOutputStream openEncryptedFileOutput(@NonNull String name, int mode,
            @NonNull String keyPairAlias)
            throws IOException {
        FileCipher fileCipher = new FileCipher(keyPairAlias,
                mContext.openFileOutput(name, mode), mSecureConfig);
        return fileCipher.getFileOutputStream();
    }


    /**
     * Checks to see if the device is locked.
     *
     * @return true if the device is locked, false otherwise.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public boolean deviceLocked() {
        KeyguardManager keyGuardManager =
                (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        return keyGuardManager.isDeviceLocked();
    }

}
