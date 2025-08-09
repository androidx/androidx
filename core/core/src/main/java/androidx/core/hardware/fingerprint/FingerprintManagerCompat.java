/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package androidx.core.hardware.fingerprint;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;

import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * A class that coordinates access to the fingerprint hardware.
 * <p>
 * On platforms before {@link android.os.Build.VERSION_CODES#M}, this class behaves as there would
 * be no fingerprint hardware available.
 *
 * @deprecated Use {@code androidx.biometrics.BiometricPrompt} instead.
 */
@SuppressWarnings({"deprecation", "unused"})
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FingerprintManagerCompat {

    private final Context mContext;

    /** Get a {@link FingerprintManagerCompat} instance for a provided context. */
    public static @NonNull FingerprintManagerCompat from(@NonNull Context context) {
        return new FingerprintManagerCompat(context);
    }

    private FingerprintManagerCompat(Context context) {
        mContext = context;
    }

    /**
     * Determine if there is at least one fingerprint enrolled.
     *
     * @return true if at least one fingerprint is enrolled, false otherwise
     */
    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public boolean hasEnrolledFingerprints() {
        final FingerprintManager fp = getFingerprintManagerOrNull(mContext);
        return (fp != null) && Api23Impl.hasEnrolledFingerprints(fp);
    }

    /**
     * Determine if fingerprint hardware is present and functional.
     *
     * @return true if hardware is present and functional, false otherwise.
     */
    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public boolean isHardwareDetected() {
        final FingerprintManager fp = getFingerprintManagerOrNull(mContext);
        return (fp != null) && Api23Impl.isHardwareDetected(fp);
    }

    /**
     * Request authentication of a crypto object. This call warms up the fingerprint hardware
     * and starts scanning for a fingerprint. It terminates when
     * {@link AuthenticationCallback#onAuthenticationError(int, CharSequence)} or
     * {@link AuthenticationCallback#onAuthenticationSucceeded(AuthenticationResult)} is called, at
     * which point the object is no longer valid. The operation can be canceled by using the
     * provided cancel object.
     *
     * @param crypto object associated with the call or null if none required.
     * @param flags optional flags; should be 0
     * @param cancel an object that can be used to cancel authentication
     * @param callback an object to receive authentication events
     * @param handler an optional handler for events
     * @deprecated Use
     * {@link #authenticate(CryptoObject, int, CancellationSignal, AuthenticationCallback, Handler)}
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public void authenticate(@Nullable CryptoObject crypto, int flags,
            androidx.core.os.@Nullable CancellationSignal cancel,
            @NonNull AuthenticationCallback callback,
            @Nullable Handler handler) {
        authenticate(crypto, flags,
                cancel != null ? (CancellationSignal) cancel.getCancellationSignalObject() : null,
                callback, handler);
    }

    /**
     * Request authentication of a crypto object. This call warms up the fingerprint hardware
     * and starts scanning for a fingerprint. It terminates when
     * {@link AuthenticationCallback#onAuthenticationError(int, CharSequence)} or
     * {@link AuthenticationCallback#onAuthenticationSucceeded(AuthenticationResult)} is called, at
     * which point the object is no longer valid. The operation can be canceled by using the
     * provided cancel object.
     *
     * @param crypto object associated with the call or null if none required.
     * @param flags optional flags; should be 0
     * @param cancel an object that can be used to cancel authentication
     * @param callback an object to receive authentication events
     * @param handler an optional handler for events
     */
    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public void authenticate(@Nullable CryptoObject crypto, int flags,
            @Nullable CancellationSignal cancel, @NonNull AuthenticationCallback callback,
            @Nullable Handler handler) {
        final FingerprintManager fp = getFingerprintManagerOrNull(mContext);
        if (fp != null) {
            Api23Impl.authenticate(fp, wrapCryptoObject(crypto), cancel, flags,
                    wrapCallback(callback), handler);
        }
    }

    private static @Nullable FingerprintManager getFingerprintManagerOrNull(
            @NonNull Context context) {
        return Api23Impl.getFingerprintManagerOrNull(context);
    }

    private static FingerprintManager.CryptoObject wrapCryptoObject(CryptoObject cryptoObject) {
        return Api23Impl.wrapCryptoObject(cryptoObject);
    }

    static CryptoObject unwrapCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        return Api23Impl.unwrapCryptoObject(cryptoObject);
    }

    private static FingerprintManager.AuthenticationCallback wrapCallback(
            final AuthenticationCallback callback) {
        return new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                callback.onAuthenticationError(errMsgId, errString);
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                callback.onAuthenticationHelp(helpMsgId, helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                callback.onAuthenticationSucceeded(new AuthenticationResult(
                        unwrapCryptoObject(Api23Impl.getCryptoObject(result))));
            }

            @Override
            public void onAuthenticationFailed() {
                callback.onAuthenticationFailed();
            }
        };
    }

    /**
     * A wrapper class for the crypto objects supported by FingerprintManager. Currently the
     * framework supports {@link Signature} and {@link Cipher} objects.
     */
    public static class CryptoObject {

        private final Signature mSignature;
        private final Cipher mCipher;
        private final Mac mMac;

        public CryptoObject(@NonNull Signature signature) {
            mSignature = signature;
            mCipher = null;
            mMac = null;

        }

        public CryptoObject(@NonNull Cipher cipher) {
            mCipher = cipher;
            mSignature = null;
            mMac = null;
        }

        public CryptoObject(@NonNull Mac mac) {
            mMac = mac;
            mCipher = null;
            mSignature = null;
        }

        /**
         * Get {@link Signature} object.
         * @return {@link Signature} object or null if this doesn't contain one.
         */
        public @Nullable Signature getSignature() { return mSignature; }

        /**
         * Get {@link Cipher} object.
         * @return {@link Cipher} object or null if this doesn't contain one.
         */
        public @Nullable Cipher getCipher() { return mCipher; }

        /**
         * Get {@link Mac} object.
         * @return {@link Mac} object or null if this doesn't contain one.
         */
        public @Nullable Mac getMac() { return mMac; }
    }

    /**
     * Container for callback data from {@link FingerprintManagerCompat#authenticate(CryptoObject,
     *     int, CancellationSignal, AuthenticationCallback, Handler)}.
     */
    public static final class AuthenticationResult {
        private final CryptoObject mCryptoObject;

        public AuthenticationResult(@NonNull CryptoObject crypto) {
            mCryptoObject = crypto;
        }

        /**
         * Obtain the crypto object associated with this transaction
         * @return crypto object provided to {@link FingerprintManagerCompat#authenticate(
         *         CryptoObject, int, CancellationSignal, AuthenticationCallback, Handler)}.
         */
        public @NonNull CryptoObject getCryptoObject() { return mCryptoObject; }
    }

    /**
     * Callback structure provided to {@link FingerprintManagerCompat#authenticate(CryptoObject,
     * int, CancellationSignal, AuthenticationCallback, Handler)}. Users of {@link
     * FingerprintManagerCompat#authenticate(CryptoObject, int, CancellationSignal,
     * AuthenticationCallback, Handler) } must provide an implementation of this for listening to
     * fingerprint events.
     */
    public static abstract class AuthenticationCallback {
        /**
         * Called when an unrecoverable error has been encountered and the operation is complete.
         * No further callbacks will be made on this object.
         * @param errMsgId An integer identifying the error message
         * @param errString A human-readable error string that can be shown in UI
         */
        public void onAuthenticationError(int errMsgId, @NonNull CharSequence errString) { }

        /**
         * Called when a recoverable error has been encountered during authentication. The help
         * string is provided to give the user guidance for what went wrong, such as
         * "Sensor dirty, please clean it."
         * @param helpMsgId An integer identifying the error message
         * @param helpString A human-readable string that can be shown in UI
         */
        public void onAuthenticationHelp(int helpMsgId, @NonNull CharSequence helpString) { }

        /**
         * Called when a fingerprint is recognized.
         * @param result An object containing authentication-related data
         */
        public void onAuthenticationSucceeded(@NonNull AuthenticationResult result) { }

        /**
         * Called when a fingerprint is valid but not recognized.
         */
        public void onAuthenticationFailed() { }
    }

    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
        static boolean hasEnrolledFingerprints(Object fingerprintManager) {
            return ((FingerprintManager) fingerprintManager).hasEnrolledFingerprints();
        }

        @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
        static boolean isHardwareDetected(Object fingerprintManager) {
            return ((FingerprintManager) fingerprintManager).isHardwareDetected();
        }

        @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
        static void authenticate(Object fingerprintManager, Object crypto,
                CancellationSignal cancel, int flags, Object callback, Handler handler) {
            ((FingerprintManager) fingerprintManager).authenticate(
                    (FingerprintManager.CryptoObject) crypto, cancel, flags,
                    (FingerprintManager.AuthenticationCallback) callback, handler);
        }

        static FingerprintManager.CryptoObject getCryptoObject(Object authenticationResult) {
            return ((FingerprintManager.AuthenticationResult) authenticationResult)
                    .getCryptoObject();
        }

        public static FingerprintManager getFingerprintManagerOrNull(Context context) {
            if (Build.VERSION.SDK_INT == 23) {
                return context.getSystemService(FingerprintManager.class);
            } else if (context.getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
                return context.getSystemService(FingerprintManager.class);
            } else {
                return null;
            }
        }

        public static FingerprintManager.CryptoObject wrapCryptoObject(CryptoObject cryptoObject) {
            if (cryptoObject == null) {
                return null;
            } else if (cryptoObject.getCipher() != null) {
                return new FingerprintManager.CryptoObject(cryptoObject.getCipher());
            } else if (cryptoObject.getSignature() != null) {
                return new FingerprintManager.CryptoObject(cryptoObject.getSignature());
            } else if (cryptoObject.getMac() != null) {
                return new FingerprintManager.CryptoObject(cryptoObject.getMac());
            } else {
                return null;
            }
        }

        public static CryptoObject unwrapCryptoObject(Object cryptoObjectObj) {
            FingerprintManager.CryptoObject cryptoObject =
                    (FingerprintManager.CryptoObject) cryptoObjectObj;
            if (cryptoObject == null) {
                return null;
            } else if (cryptoObject.getCipher() != null) {
                return new CryptoObject(cryptoObject.getCipher());
            } else if (cryptoObject.getSignature() != null) {
                return new CryptoObject(cryptoObject.getSignature());
            } else if (cryptoObject.getMac() != null) {
                return new CryptoObject(cryptoObject.getMac());
            } else {
                return null;
            }
        }
    }
}
