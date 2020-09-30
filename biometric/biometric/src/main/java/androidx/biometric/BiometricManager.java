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

package androidx.biometric;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A class that provides system information related to biometrics (e.g. fingerprint, face, etc.).
 *
 * <p>On devices running Android 10 (API 29) and above, this will query the framework's version of
 * {@link android.hardware.biometrics.BiometricManager}. On Android 9.0 (API 28) and prior
 * versions, this will query {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
 *
 * @see BiometricPrompt To prompt the user to authenticate with their biometric.
 */
@SuppressWarnings("deprecation")
public class BiometricManager {
    private static final String TAG = "BiometricManager";

    /**
     * The user can successfully authenticate.
     */
    public static final int BIOMETRIC_SUCCESS = 0;

    /**
     * Unable to determine whether the user can authenticate.
     *
     * <p>This status code may be returned on older Android versions due to partial incompatibility
     * with a newer API. Applications that wish to enable biometric authentication on affected
     * devices may still call {@code BiometricPrompt#authenticate()} after receiving this status
     * code but should be prepared to handle possible errors.
     */
    public static final int BIOMETRIC_STATUS_UNKNOWN = -1;

    /**
     * The user can't authenticate because the specified options are incompatible with the current
     * Android version.
     */
    public static final int BIOMETRIC_ERROR_UNSUPPORTED = -2;

    /**
     * The user can't authenticate because the hardware is unavailable. Try again later.
     */
    public static final int BIOMETRIC_ERROR_HW_UNAVAILABLE = 1;

    /**
     * The user can't authenticate because no biometric or device credential is enrolled.
     */
    public static final int BIOMETRIC_ERROR_NONE_ENROLLED = 11;

    /**
     * The user can't authenticate because there is no suitable hardware (e.g. no biometric sensor
     * or no keyguard).
     */
    public static final int BIOMETRIC_ERROR_NO_HARDWARE = 12;

    /**
     * The user can't authenticate because a security vulnerability has been discovered with one or
     * more hardware sensors. The affected sensor(s) are unavailable until a security update has
     * addressed the issue.
     */
    public static final int BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED = 15;

    /**
     * A status code that may be returned when checking for biometric authentication.
     */
    @IntDef({
        BIOMETRIC_SUCCESS,
        BIOMETRIC_STATUS_UNKNOWN,
        BIOMETRIC_ERROR_UNSUPPORTED,
        BIOMETRIC_ERROR_HW_UNAVAILABLE,
        BIOMETRIC_ERROR_NONE_ENROLLED,
        BIOMETRIC_ERROR_NO_HARDWARE,
        BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface AuthenticationStatus {}

    /**
     * Types of authenticators, defined at a level of granularity supported by
     * {@link BiometricManager} and {@link BiometricPrompt}.
     *
     * <p>Types may combined via bitwise OR into a single integer representing multiple
     * authenticators (e.g. {@code DEVICE_CREDENTIAL | BIOMETRIC_WEAK}).
     *
     * @see #canAuthenticate(int)
     * @see BiometricPrompt.PromptInfo.Builder#setAllowedAuthenticators(int)
     */
    public interface Authenticators {
        /**
         * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the
         * requirements for <strong>Class 3</strong> (formerly <strong>Strong</strong>), as defined
         * by the Android CDD.
         */
        int BIOMETRIC_STRONG = 0x000F;

        /**
         * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the
         * requirements for <strong>Class 2</strong> (formerly <strong>Weak</strong>), as defined by
         * the Android CDD.
         *
         * <p>Note that this is a superset of {@link #BIOMETRIC_STRONG} and is defined such that
         * {@code BIOMETRIC_STRONG | BIOMETRIC_WEAK == BIOMETRIC_WEAK}.
         */
        int BIOMETRIC_WEAK = 0x00FF;

        /**
         * The non-biometric credential used to secure the device (i.e. PIN, pattern, or password).
         * This should typically only be used in combination with a biometric auth type, such as
         * {@link #BIOMETRIC_WEAK}.
         */
        int DEVICE_CREDENTIAL = 1 << 15;
    }

    /**
     * A bitwise combination of authenticator types defined in {@link Authenticators}.
     */
    @IntDef(flag = true, value = {
        Authenticators.BIOMETRIC_STRONG,
        Authenticators.BIOMETRIC_WEAK,
        Authenticators.DEVICE_CREDENTIAL
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface AuthenticatorTypes {}

    /**
     * An injector for various class and method dependencies. Used for testing.
     */
    @VisibleForTesting
    interface Injector {
        /**
         * Provides the framework biometric manager that may be used on Android 10 (API 29) and
         * above.
         *
         * @return An instance of {@link android.hardware.biometrics.BiometricManager}.
         */
        @RequiresApi(Build.VERSION_CODES.Q)
        @Nullable
        android.hardware.biometrics.BiometricManager getBiometricManager();

        /**
         * Provides the fingerprint manager that may be used on Android 9.0 (API 28) and below.
         *
         * @return An instance of
         * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
         */
        @Nullable
        androidx.core.hardware.fingerprint.FingerprintManagerCompat getFingerprintManager();

        /**
         * Checks if the current device is capable of being secured with a lock screen credential
         * (i.e. PIN, pattern, or password).
         */
        boolean isDeviceSecurable();

        /**
         * Checks if the current device is secured with a lock screen credential (i.e. PIN, pattern,
         * or password).
         *
         * @return Whether the device is secured with a lock screen credential.
         */
        boolean isDeviceSecuredWithCredential();

        /**
         * Checks if the current device has a hardware sensor that may be used for fingerprint
         * authentication.
         *
         * @return Whether the device has a fingerprint sensor.
         */
        boolean isFingerprintHardwarePresent();

        /**
         * Checks if all biometric sensors on the device are known to meet or exceed the security
         * requirements for <strong>Class 3</strong> (formerly <strong>Strong</strong>).
         *
         * @return Whether all biometrics are known to be <strong>Class 3</strong> or stronger.
         */
        boolean isStrongBiometricGuaranteed();
    }

    /**
     * Provides the default class and method dependencies that will be used in production.
     */
    private static class DefaultInjector implements Injector {
        @NonNull private final Context mContext;

        /**
         * Creates a default injector from the given context.
         *
         * @param context The application or activity context.
         */
        DefaultInjector(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        @RequiresApi(Build.VERSION_CODES.Q)
        @Nullable
        public android.hardware.biometrics.BiometricManager getBiometricManager() {
            return Api29Impl.create(mContext);
        }

        @Override
        @Nullable
        public androidx.core.hardware.fingerprint.FingerprintManagerCompat getFingerprintManager() {
            return androidx.core.hardware.fingerprint.FingerprintManagerCompat.from(mContext);
        }

        @Override
        public boolean isDeviceSecurable() {
            return KeyguardUtils.getKeyguardManager(mContext) != null;
        }

        @Override
        public boolean isDeviceSecuredWithCredential() {
            return KeyguardUtils.isDeviceSecuredWithCredential(mContext);
        }

        @Override
        public boolean isFingerprintHardwarePresent() {
            return PackageUtils.hasSystemFeatureFingerprint(mContext);
        }

        @Override
        public boolean isStrongBiometricGuaranteed() {
            return DeviceUtils.canAssumeStrongBiometrics(mContext, Build.MODEL);
        }
    }

    /**
     * The injector for class and method dependencies used by this manager.
     */
    @NonNull private final Injector mInjector;

    /**
     * The framework biometric manager. Should be non-null on Android 10 (API 29) and above.
     */
    @Nullable private final android.hardware.biometrics.BiometricManager mBiometricManager;

    /**
     * The framework fingerprint manager. Should be non-null on Android 9.0 (API 28) and below.
     */
    @Nullable private final androidx.core.hardware.fingerprint.FingerprintManagerCompat
            mFingerprintManager;

    /**
     * Creates a {@link BiometricManager} instance from the given context.
     *
     * @param context The application or activity context.
     * @return An instance of {@link BiometricManager}.
     */
    @NonNull
    public static BiometricManager from(@NonNull Context context) {
        return new BiometricManager(new DefaultInjector(context));
    }

    /**
     * Creates a {@link BiometricManager} instance with the given injector.
     *
     * @param injector An injector for class and method dependencies.
     */
    @VisibleForTesting
    BiometricManager(@NonNull Injector injector) {
        mInjector = injector;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mBiometricManager = injector.getBiometricManager();
            mFingerprintManager = null;
        } else {
            mBiometricManager = null;
            mFingerprintManager = injector.getFingerprintManager();
        }
    }

    /**
     * Checks if the user can authenticate with biometrics. This requires at least one biometric
     * sensor to be present, enrolled, and available on the device.
     *
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with biometrics. Otherwise,
     * returns an error code indicating why the user can't authenticate, or
     * {@link #BIOMETRIC_STATUS_UNKNOWN} if it is unknown whether the user can authenticate.
     *
     * @deprecated Use {@link #canAuthenticate(int)} instead.
     */
    @Deprecated
    @AuthenticationStatus
    public int canAuthenticate() {
        return canAuthenticate(Authenticators.BIOMETRIC_WEAK);
    }

    /**
     * Checks if the user can authenticate with an authenticator that meets the given requirements.
     * This requires at least one of the specified authenticators to be present, enrolled, and
     * available on the device.
     *
     * <p>Note that not all combinations of authenticator types are supported prior to Android 11
     * (API 30). Specifically, {@code DEVICE_CREDENTIAL} alone is unsupported prior to API 30, and
     * {@code BIOMETRIC_STRONG | DEVICE_CREDENTIAL} is unsupported on API 28-29. Developers that
     * wish to check for the presence of a PIN, pattern, or password on these versions should
     * instead use {@link KeyguardManager#isDeviceSecure()}.
     *
     * @param authenticators A bit field representing the types of {@link Authenticators} that may
     *                       be used for authentication.
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with an allowed
     * authenticator. Otherwise, returns {@link #BIOMETRIC_STATUS_UNKNOWN} or an error code
     * indicating why the user can't authenticate.
     */
    @AuthenticationStatus
    public int canAuthenticate(@AuthenticatorTypes int authenticators) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (mBiometricManager == null) {
                Log.e(TAG, "Failure in canAuthenticate(). BiometricManager was null.");
                return BIOMETRIC_ERROR_HW_UNAVAILABLE;
            }
            return Api30Impl.canAuthenticate(mBiometricManager, authenticators);
        }
        return canAuthenticateCompat(authenticators);
    }

    /**
     * Checks if the user can authenticate with an authenticator that meets the given requirements.
     *
     * <p>This method attempts to emulate the behavior of {@link #canAuthenticate(int)} on devices
     * running Android 10 (API 29) and below.
     *
     * @param authenticators A bit field representing the types of {@link Authenticators} that may
     *                       be used for authentication.
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with the given set of allowed
     * authenticators. Otherwise, returns an error code indicating why the user can't authenticate,
     * or {@link #BIOMETRIC_STATUS_UNKNOWN} if it is unknown whether the user can authenticate.
     */
    @AuthenticationStatus
    private int canAuthenticateCompat(@AuthenticatorTypes int authenticators) {
        if (!AuthenticatorUtils.isSupportedCombination(authenticators)) {
            return BIOMETRIC_ERROR_UNSUPPORTED;
        }

        // Match the framework's behavior for an empty authenticator set on API 30.
        if (authenticators == 0) {
            return BIOMETRIC_ERROR_NO_HARDWARE;
        }

        // Authentication is impossible if the device can't be secured.
        if (!mInjector.isDeviceSecurable()) {
            return BIOMETRIC_ERROR_NO_HARDWARE;
        }

        // No authenticators are enrolled if the device is not secured.
        if (!mInjector.isDeviceSecuredWithCredential()) {
            return BIOMETRIC_ERROR_NONE_ENROLLED;
        }

        // Credential authentication is always possible if the device is secured.
        if (AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            return BIOMETRIC_SUCCESS;
        }

        // The class of some non-fingerprint biometrics can be checked on API 29.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            return AuthenticatorUtils.isWeakBiometricAllowed(authenticators)
                    ? canAuthenticateWithWeakBiometricOnApi29()
                    : canAuthenticateWithStrongBiometricOnApi29();
        }

        // Non-fingerprint biometrics may be invoked but can't be checked on API 28.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Having fingerprint hardware is a prerequisite, since BiometricPrompt internally
            // calls FingerprintManager#getErrorString() on API 28 (b/151443237).
            return mInjector.isFingerprintHardwarePresent()
                    ? canAuthenticateWithFingerprintOrUnknown()
                    : BIOMETRIC_ERROR_NO_HARDWARE;
        }

        // No non-fingerprint biometric APIs exist prior to API 28.
        return canAuthenticateWithFingerprint();
    }

    /**
     * Checks if the user can authenticate with a <strong>Class 3</strong> (formerly
     * <strong>Strong</strong>) or better biometric sensor on a device running Android 10 (API 29).
     *
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with a
     * <strong>Class 3</strong> or better biometric sensor. Otherwise, returns an error code
     * indicating why the user can't authenticate, or {@link #BIOMETRIC_STATUS_UNKNOWN} if it is
     * unknown whether the user can authenticate.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @AuthenticationStatus
    private int canAuthenticateWithStrongBiometricOnApi29() {
        // Use the hidden canAuthenticate(CryptoObject) method if it exists.
        final Method canAuthenticateWithCrypto = Api29Impl.getCanAuthenticateWithCryptoMethod();
        if (canAuthenticateWithCrypto != null) {
            final android.hardware.biometrics.BiometricPrompt.CryptoObject crypto =
                    CryptoObjectUtils.wrapForBiometricPrompt(
                            CryptoObjectUtils.createFakeCryptoObject());
            if (crypto != null) {
                try {
                    final Object result =
                            canAuthenticateWithCrypto.invoke(mBiometricManager, crypto);
                    if (result instanceof Integer) {
                        return (int) result;
                    }
                    Log.w(TAG, "Invalid return type for canAuthenticate(CryptoObject).");
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    Log.w(TAG, "Failed to invoke canAuthenticate(CryptoObject).", e);
                }
            }
        }

        // Check if we can use canAuthenticate() as a proxy for canAuthenticate(BIOMETRIC_STRONG).
        @AuthenticationStatus final int result = canAuthenticateWithWeakBiometricOnApi29();
        if (mInjector.isStrongBiometricGuaranteed() || result != BIOMETRIC_SUCCESS) {
            return result;
        }

        // If all else fails, check if fingerprint authentication is available.
        return canAuthenticateWithFingerprintOrUnknown();
    }

    /**
     * Checks if the user can authenticate with a <strong>Class 2</strong> (formerly
     * <strong>Weak</strong>) or better biometric sensor on a device running Android 10 (API 29).
     *
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with a
     * <strong>Class 2</strong> or better biometric sensor. Otherwise, returns an error code
     * indicating why the user can't authenticate.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @AuthenticationStatus
    private int canAuthenticateWithWeakBiometricOnApi29() {
        if (mBiometricManager == null) {
            Log.e(TAG, "Failure in canAuthenticate(). BiometricManager was null.");
            return BIOMETRIC_ERROR_HW_UNAVAILABLE;
        }
        return Api29Impl.canAuthenticate(mBiometricManager);
    }

    /**
     * Checks if the user can authenticate with fingerprint, falling back to
     * {@link #BIOMETRIC_STATUS_UNKNOWN} for any error condition.
     *
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with fingerprint, or
     * {@link #BIOMETRIC_STATUS_UNKNOWN} otherwise.
     */
    @AuthenticationStatus
    private int canAuthenticateWithFingerprintOrUnknown() {
        return canAuthenticateWithFingerprint() == BIOMETRIC_SUCCESS
                ? BIOMETRIC_SUCCESS
                : BIOMETRIC_STATUS_UNKNOWN;
    }

    /**
     * Checks if the user can authenticate with fingerprint.
     *
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with fingerprint.
     * Otherwise, returns an error code indicating why the user can't authenticate.
     */
    @AuthenticationStatus
    private int canAuthenticateWithFingerprint() {
        if (mFingerprintManager == null) {
            Log.e(TAG, "Failure in canAuthenticate(). FingerprintManager was null.");
            return BIOMETRIC_ERROR_HW_UNAVAILABLE;
        }
        if (!mFingerprintManager.isHardwareDetected()) {
            return BIOMETRIC_ERROR_NO_HARDWARE;
        }
        if (!mFingerprintManager.hasEnrolledFingerprints()) {
            return BIOMETRIC_ERROR_NONE_ENROLLED;
        }
        return BIOMETRIC_SUCCESS;
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        // Prevent instantiation.
        private Api30Impl() {}

        /**
         * Calls {@link android.hardware.biometrics.BiometricManager#canAuthenticate(int)} for the
         * given biometric manager and set of allowed authenticators.
         *
         * @param biometricManager An instance of
         *                         {@link android.hardware.biometrics.BiometricManager}.
         * @param authenticators   A bit field representing the types of {@link Authenticators} that
         *                         may be used for authentication.
         * @return The result of
         * {@link android.hardware.biometrics.BiometricManager#canAuthenticate(int)}.
         */
        @AuthenticationStatus
        static int canAuthenticate(
                @NonNull android.hardware.biometrics.BiometricManager biometricManager,
                @AuthenticatorTypes int authenticators) {
            return biometricManager.canAuthenticate(authenticators);
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 10 (API 29).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static class Api29Impl {
        // Prevent instantiation.
        private Api29Impl() {}

        /**
         * Gets an instance of the framework
         * {@link android.hardware.biometrics.BiometricManager} class.
         *
         * @param context The application or activity context.
         * @return An instance of {@link android.hardware.biometrics.BiometricManager}.
         */
        @Nullable
        static android.hardware.biometrics.BiometricManager create(@NonNull Context context) {
            return context.getSystemService(android.hardware.biometrics.BiometricManager.class);
        }

        /**
         * Calls {@link android.hardware.biometrics.BiometricManager#canAuthenticate()} for the
         * given biometric manager.
         *
         * @param biometricManager An instance of
         *                         {@link android.hardware.biometrics.BiometricManager}.
         * @return The result of
         * {@link android.hardware.biometrics.BiometricManager#canAuthenticate()}.
         */
        @AuthenticationStatus
        static int canAuthenticate(
                @NonNull android.hardware.biometrics.BiometricManager biometricManager) {
            return biometricManager.canAuthenticate();
        }

        /**
         * Checks for and returns the hidden {@link android.hardware.biometrics.BiometricManager}
         * method {@code canAuthenticate(CryptoObject)} via reflection.
         *
         * @return The method {@code BiometricManager#canAuthenticate(CryptoObject)}, if present.
         */
        @SuppressWarnings("JavaReflectionMemberAccess")
        @Nullable
        static Method getCanAuthenticateWithCryptoMethod() {
            try {
                return android.hardware.biometrics.BiometricManager.class.getMethod(
                        "canAuthenticate",
                        android.hardware.biometrics.BiometricPrompt.CryptoObject.class);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    }
}
