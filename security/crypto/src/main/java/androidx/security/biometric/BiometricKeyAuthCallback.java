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
package androidx.security.biometric;

import androidx.annotation.NonNull;
import androidx.security.crypto.SecureCipher;

import java.security.Signature;

import javax.crypto.Cipher;



/**
 * Callback for Biometric Key auth.
 *
 */
public abstract class BiometricKeyAuthCallback {

    /**
     * Statuses of Biometric auth
     */
    public enum BiometricStatus {
        SUCCESS(0),
        FAILED(1),
        ERROR(2);

        private final int mType;

        BiometricStatus(int type) {
            this.mType = type;
        }

        /**
         * @return the mType
         */
        public int getType() {
            return this.mType;
        }

        /**
         * @return the status that matches the id
         */
        @NonNull
        public static BiometricStatus fromId(int id) {
            switch (id) {
                case 0:
                    return SUCCESS;
                case 1:
                    return FAILED;
                case 2:
                    return ERROR;
            }
            return ERROR;
        }
    }

    /**
     *
     */
    public abstract void onAuthenticationSucceeded();

    /**
     *
     */
    public abstract void onAuthenticationError(int errorCode, @NonNull CharSequence errString);

    /**
     *
     */
    public abstract void onAuthenticationFailed();

    /**
     * @param message the message to send
     */
    public abstract void onMessage(@NonNull String message);

    /**
     * @param cipher The cipher to authenticate
     * @param listener The listener to call back
     */
    public abstract void authenticateKey(@NonNull Cipher cipher,
            @NonNull SecureCipher.SecureAuthListener listener);

    /**
     * @param signature The signature to authenticate
     * @param listener The listener to call back
     */
    public abstract void authenticateKey(@NonNull Signature signature,
            @NonNull SecureCipher.SecureAuthListener listener);

}
