/*
 * Copyright 2025 The Android Open Source Project
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

package android.hardware.fingerprint;

import android.os.CancellationSignal;
import android.os.Handler;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * Stubs for FingerprintManager.
 */
public class FingerprintManager {
    /** stub */
    public void authenticate(@Nullable CryptoObject crypto, @Nullable CancellationSignal cancel,
            int flags, @NonNull AuthenticationCallback callback, @Nullable Handler handler) {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean hasEnrolledFingerprints() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public boolean isHardwareDetected() {
        throw new UnsupportedOperationException();
    }

    /** stub */
    public static final class CryptoObject {
        /** stub */
        public CryptoObject(@NonNull Signature signature) {
        }

        /** stub */
        public CryptoObject(@NonNull Cipher cipher) {
        }

        /** stub */
        public CryptoObject(@NonNull Mac mac) {
        }

        /** stub */
        @Nullable
        public Signature getSignature() {
            throw new UnsupportedOperationException();
        }

        /** stub */
        @Nullable
        public Cipher getCipher() {
            throw new UnsupportedOperationException();
        }

        /** stub */
        @Nullable
        public Mac getMac() {
            throw new UnsupportedOperationException();
        }
    }

    /** stub */
    public static class AuthenticationResult {
        /** stub */
        @NonNull
        public CryptoObject getCryptoObject() {
            throw new UnsupportedOperationException();
        }
    }

    /** stub */
    public abstract static class AuthenticationCallback {
        /** stub */
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            throw new UnsupportedOperationException();
        }

        /** stub */
        public void onAuthenticationHelp(int helpCode, @NonNull CharSequence helpString) {
            throw new UnsupportedOperationException();
        }

        /** stub */
        public void onAuthenticationSucceeded(@NonNull AuthenticationResult result) {
            throw new UnsupportedOperationException();
        }

        /** stub */
        public void onAuthenticationFailed() {
            throw new UnsupportedOperationException();
        }
    }

}



