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

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.StringRes;
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

    private static final int AUTH_MODALITY_NONE = 0;
    private static final int AUTH_MODALITY_CREDENTIAL = 1;
    private static final int AUTH_MODALITY_UNKNOWN_BIOMETRIC = 1 << 1;
    private static final int AUTH_MODALITY_FINGERPRINT = 1 << 2;
    private static final int AUTH_MODALITY_FACE = 1 << 3;

    @IntDef(flag = true, value = {
        AUTH_MODALITY_NONE,
        AUTH_MODALITY_CREDENTIAL,
        AUTH_MODALITY_UNKNOWN_BIOMETRIC,
        AUTH_MODALITY_FINGERPRINT,
        AUTH_MODALITY_FACE
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface AuthModalities {}

    /**
     * Provides localized strings for an application that uses {@link BiometricPrompt} to
     * authenticate the user.
     */
    public static class Strings {
        /**
         * The framework strings instance. Non-null on Android 12 (API 31) and above.
         */
        @Nullable private final android.hardware.biometrics.BiometricManager.Strings mStrings;

        /**
         * The compatibility strings instance. Non-null on Android 11 (API 30) and below.
         */
        @Nullable private final StringsCompat mStringsCompat;

        @RequiresApi(Build.VERSION_CODES.S)
        Strings(@NonNull android.hardware.biometrics.BiometricManager.Strings strings) {
            mStrings = strings;
            mStringsCompat = null;
        }

        Strings(@NonNull StringsCompat stringsCompat) {
            mStrings = null;
            mStringsCompat = stringsCompat;
        }

        /**
         * Provides a localized string that can be used as the label for a button that invokes
         * {@link BiometricPrompt}.
         *
         * <p>When possible, this method should use the given authenticator requirements to more
         * precisely specify the authentication type that will be used. For example, if
         * <strong>Class 3</strong> biometric authentication is requested on a device with a
         * <strong>Class 3</strong> fingerprint sensor and a <strong>Class 2</strong> face sensor,
         * the returned string should indicate that fingerprint authentication will be used.
         *
         * <p>This method should also try to specify which authentication method(s) will be used in
         * practice when multiple authenticators meet the given requirements. For example, if
         * biometric authentication is requested on a device with both face and fingerprint sensors
         * but the user has selected face as their preferred method, the returned string should
         * indicate that face authentication will be used.
         *
         * <p>This method may return {@code null} if none of the requested authenticator types are
         * available, but this should <em>not</em> be relied upon for checking the status of
         * authenticators. Instead, use {@link #canAuthenticate(int)}.
         *
         * @return The label for a button that invokes {@link BiometricPrompt} for authentication.
         */
        @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
        @Nullable
        public CharSequence getButtonLabel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mStrings != null) {
                return Api31Impl.getButtonLabel(mStrings);
            } else if (mStringsCompat != null) {
                return mStringsCompat.getButtonLabel();
            } else {
                Log.e(TAG, "Failure in Strings.getButtonLabel(). No available string provider.");
                return null;
            }
        }

        /**
         * Provides a localized string that can be shown while the user is authenticating with
         * {@link BiometricPrompt}.
         *
         * <p>When possible, this method should use the given authenticator requirements to more
         * precisely specify the authentication type that will be used. For example, if
         * <strong>Class 3</strong> biometric authentication is requested on a device with a
         * <strong>Class 3</strong> fingerprint sensor and a <strong>Class 2</strong> face sensor,
         * the returned string should indicate that fingerprint authentication will be used.
         *
         * <p>This method should also try to specify which authentication method(s) will be used in
         * practice when multiple authenticators meet the given requirements. For example, if
         * biometric authentication is requested on a device with both face and fingerprint sensors
         * but the user has selected face as their preferred method, the returned string should
         * indicate that face authentication will be used.
         *
         * <p>This method may return {@code null} if none of the requested authenticator types are
         * available, but this should <em>not</em> be relied upon for checking the status of
         * authenticators. Instead, use {@link #canAuthenticate(int)}.
         *
         * @return A message to be shown on {@link BiometricPrompt} during authentication.
         */
        @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
        @Nullable
        public CharSequence getPromptMessage() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mStrings != null) {
                return Api31Impl.getPromptMessage(mStrings);
            } else if (mStringsCompat != null) {
                return mStringsCompat.getPromptMessage();
            } else {
                Log.e(TAG, "Failure in Strings.getPromptMessage(). No available string provider.");
                return null;
            }
        }

        /**
         * Provides a localized string that can be shown as the title for an app setting that
         * allows authentication with {@link BiometricPrompt}.
         *
         * <p>When possible, this method should use the given authenticator requirements to more
         * precisely specify the authentication type that will be used. For example, if
         * <strong>Class 3</strong> biometric authentication is requested on a device with a
         * <strong>Class 3</strong> fingerprint sensor and a <strong>Class 2</strong> face sensor,
         * the returned string should indicate that fingerprint authentication will be used.
         *
         * <p>This method should <em>not</em> try to specify which authentication method(s) will be
         * used in practice when multiple authenticators meet the given requirements. For example,
         * if biometric authentication is requested on a device with both face and fingerprint
         * sensors, the returned string should indicate that either face or fingerprint
         * authentication may be used, regardless of whether the user has enrolled or selected
         * either as their preferred method.
         *
         * <p>This method may return {@code null} if none of the requested authenticator types are
         * supported by the system, but this should <em>not</em> be relied upon for checking the
         * status of authenticators. Instead, use {@link #canAuthenticate(int)} or
         * {@link android.content.pm.PackageManager#hasSystemFeature(String)}.
         *
         * @return The name for a setting that allows authentication with {@link BiometricPrompt}.
         */
        @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
        @Nullable
        public CharSequence getSettingName() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mStrings != null) {
                return Api31Impl.getSettingName(mStrings);
            } else if (mStringsCompat != null) {
                return mStringsCompat.getSettingName();
            } else {
                Log.e(TAG, "Failure in Strings.getSettingName(). No available string provider.");
                return null;
            }
        }
    }

    /**
     * Compatibility wrapper for the {@link Strings} class on Android 11 (API 30) and below.
     */
    private class StringsCompat {
        @NonNull private final Resources mResources;
        @AuthenticatorTypes private final int mAuthenticators;
        @AuthModalities private final int mPossibleModalities;

        StringsCompat(
                @NonNull Resources resources,
                @AuthenticatorTypes int authenticators,
                boolean isFingerprintSupported,
                boolean isFaceSupported,
                boolean isIrisSupported,
                boolean isDeviceSecured) {

            mResources = resources;
            mAuthenticators = authenticators;

            @AuthModalities int possibleModalities =
                    isDeviceSecured && AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)
                            ? AUTH_MODALITY_CREDENTIAL
                            : AUTH_MODALITY_NONE;

            if (AuthenticatorUtils.isSomeBiometricAllowed(authenticators)) {
                if (isFingerprintSupported) {
                    possibleModalities |= AUTH_MODALITY_FINGERPRINT;
                }
                if (isFaceSupported) {
                    possibleModalities |= AUTH_MODALITY_FACE;
                }
                if (isIrisSupported) {
                    possibleModalities |= AUTH_MODALITY_UNKNOWN_BIOMETRIC;
                }
            }
            mPossibleModalities = possibleModalities;
        }

        /**
         * Provides a localized string that can be used as the label for a button that invokes
         * {@link BiometricPrompt}.
         *
         * This is a backwards-compatible implementation of the {@link Strings#getButtonLabel()}
         * method for Android 11 (API 30) and below.
         *
         * @return The label for a button that invokes {@link BiometricPrompt} for authentication.
         */
        @Nullable
        CharSequence getButtonLabel() {
            @AuthenticatorTypes final int biometricAuthenticators =
                    AuthenticatorUtils.getBiometricAuthenticators(mAuthenticators);
            if (canAuthenticate(biometricAuthenticators) == BIOMETRIC_SUCCESS) {
                switch (mPossibleModalities & ~AUTH_MODALITY_CREDENTIAL) {
                    case AUTH_MODALITY_FINGERPRINT:
                        // Fingerprint is the only supported and available biometric.
                        return mResources.getString(R.string.use_fingerprint_label);
                    case AUTH_MODALITY_FACE:
                        // Face is the only supported and available biometric.
                        return mResources.getString(R.string.use_face_label);
                    default:
                        // 1+ biometric types are supported and available.
                        return mResources.getString(R.string.use_biometric_label);
                }
            }

            if ((mPossibleModalities & AUTH_MODALITY_CREDENTIAL) != 0) {
                // Only screen lock is supported and available.
                return mResources.getString(R.string.use_screen_lock_label);
            }

            // Authentication is not supported or not available.
            return null;
        }

        /**
         * Provides a localized string that can be shown while the user is authenticating with
         * {@link BiometricPrompt}.
         *
         * This is a backwards-compatible implementation of the {@link Strings#getPromptMessage()}
         * method for Android 11 (API 30) and below.
         *
         * @return A message to be shown on {@link BiometricPrompt} during authentication.
         */
        @Nullable
        CharSequence getPromptMessage() {
            @AuthenticatorTypes final int biometricAuthenticators =
                    AuthenticatorUtils.getBiometricAuthenticators(mAuthenticators);

            if (canAuthenticate(biometricAuthenticators) == BIOMETRIC_SUCCESS) {
                @StringRes final int messageRes;
                switch (mPossibleModalities & ~AUTH_MODALITY_CREDENTIAL) {
                    case AUTH_MODALITY_FINGERPRINT:
                        // Fingerprint is the only supported and available biometric.
                        messageRes = AuthenticatorUtils.isDeviceCredentialAllowed(mAuthenticators)
                                ? R.string.fingerprint_or_screen_lock_prompt_message
                                : R.string.fingerprint_prompt_message;
                        break;

                    case AUTH_MODALITY_FACE:
                        // Face is the only supported and available biometric.
                        messageRes = AuthenticatorUtils.isDeviceCredentialAllowed(mAuthenticators)
                                ? R.string.face_or_screen_lock_prompt_message
                                : R.string.face_prompt_message;
                        break;

                    default:
                        // 1+ biometric types are supported and available.
                        messageRes = AuthenticatorUtils.isDeviceCredentialAllowed(mAuthenticators)
                                ? R.string.biometric_or_screen_lock_prompt_message
                                : R.string.biometric_prompt_message;
                        break;
                }

                return mResources.getString(messageRes);
            }

            if ((mPossibleModalities & AUTH_MODALITY_CREDENTIAL) != 0) {
                // Only screen lock is supported and available.
                return mResources.getString(R.string.screen_lock_prompt_message);
            }

            // Authentication is not supported or not available.
            return null;
        }

        /**
         * Provides a localized string that can be shown as the title for an app setting that
         * allows authentication with {@link BiometricPrompt}.
         *
         * This is a backwards-compatible implementation of the {@link Strings#getSettingName()}
         * method for Android 11 (API 30) and below.
         *
         * @return The name for a setting that allows authentication with {@link BiometricPrompt}.
         */
        @Nullable
        CharSequence getSettingName() {
            CharSequence settingName;
            switch (mPossibleModalities) {
                case AUTH_MODALITY_NONE:
                    // Authentication is not supported.
                    settingName = null;
                    break;

                case AUTH_MODALITY_CREDENTIAL:
                    // Only screen lock is supported.
                    settingName = mResources.getString(R.string.use_screen_lock_label);
                    break;

                case AUTH_MODALITY_UNKNOWN_BIOMETRIC:
                    // Only an unknown biometric type(s) is supported.
                    settingName = mResources.getString(R.string.use_biometric_label);
                    break;

                case AUTH_MODALITY_FINGERPRINT:
                    // Only fingerprint is supported.
                    settingName = mResources.getString(R.string.use_fingerprint_label);
                    break;

                case AUTH_MODALITY_FACE:
                    // Only face is supported.
                    settingName = mResources.getString(R.string.use_face_label);
                    break;

                default:
                    if ((mPossibleModalities & AUTH_MODALITY_CREDENTIAL) == 0) {
                        // 2+ biometric types are supported (but not screen lock).
                        settingName = mResources.getString(R.string.use_biometric_label);
                    } else {
                        switch (mPossibleModalities & ~AUTH_MODALITY_CREDENTIAL) {
                            case AUTH_MODALITY_FINGERPRINT:
                                // Only fingerprint and screen lock are supported.
                                settingName = mResources.getString(
                                        R.string.use_fingerprint_or_screen_lock_label);
                                break;

                            case AUTH_MODALITY_FACE:
                                // Only face and screen lock are supported.
                                settingName = mResources.getString(
                                        R.string.use_face_or_screen_lock_label);
                                break;

                            default:
                                // 1+ biometric types and screen lock are supported.
                                settingName = mResources.getString(
                                        R.string.use_biometric_or_screen_lock_label);
                                break;
                        }
                    }
                    break;
            }
            return settingName;
        }
    }

    /**
     * An injector for various class and method dependencies. Used for testing.
     */
    @VisibleForTesting
    interface Injector {
        /**
         * Provides the application {@link Resources} object.
         *
         * @return An instance of {@link Resources}.
         */
        @NonNull
        Resources getResources();

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
         * Checks if the current device has a hardware sensor that may be used for face
         * authentication.
         *
         * @return Whether the device has a face sensor.
         */
        boolean isFaceHardwarePresent();

        /**
         * Checks if the current device has a hardware sensor that may be used for iris
         * authentication.
         *
         * @return Whether the device has an iris sensor.
         */
        boolean isIrisHardwarePresent();

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
        @NonNull
        public Resources getResources() {
            return mContext.getResources();
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
        public boolean isFaceHardwarePresent() {
            return PackageUtils.hasSystemFeatureFace(mContext);
        }

        @Override
        public boolean isIrisHardwarePresent() {
            return PackageUtils.hasSystemFeatureIris(mContext);
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
     * The framework fingerprint manager. Should be non-null on Android 10 (API 29) and below.
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
        mBiometricManager = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? injector.getBiometricManager()
                : null;
        mFingerprintManager = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                ? injector.getFingerprintManager()
                : null;
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

        // Credential authentication is always possible if the device is secured. Conversely, no
        // form of authentication is possible if the device is not secured.
        if (AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            return mInjector.isDeviceSecuredWithCredential()
                    ? BIOMETRIC_SUCCESS
                    : BIOMETRIC_ERROR_NONE_ENROLLED;
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
                    ? canAuthenticateWithFingerprintOrUnknownBiometric()
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
                            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
                                    ? canAuthenticateWithCrypto.invoke(mBiometricManager, crypto)
                                    : null;
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
        return canAuthenticateWithFingerprintOrUnknownBiometric();
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
     * Checks if the user can authenticate with fingerprint or with a biometric sensor for which
     * there is no platform method to check availability.
     *
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with fingerprint. Otherwise,
     * returns an error code indicating why the user can't authenticate, or
     * {@link #BIOMETRIC_STATUS_UNKNOWN} if it is unknown whether the user can authenticate.
     */
    @AuthenticationStatus
    private int canAuthenticateWithFingerprintOrUnknownBiometric() {
        // If the device is not secured, authentication is definitely not possible. Use
        // FingerprintManager to distinguish between the "no hardware" and "none enrolled" cases.
        if (!mInjector.isDeviceSecuredWithCredential()) {
            return canAuthenticateWithFingerprint();
        }

        // Check for definite availability of fingerprint. Otherwise, return "unknown" to allow for
        // non-fingerprint biometrics (e.g. iris) that may be available via BiometricPrompt.
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
     * Produces an instance of the {@link Strings} class, which provides localized strings for an
     * application, given a set of allowed authenticator types.
     *
     * @param authenticators A bit field representing the types of {@link Authenticators} that may
     *                       be used for authentication.
     * @return A {@link Strings} collection for the given allowed authenticator types.
     */
    @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
    @Nullable
    public Strings getStrings(@AuthenticatorTypes int authenticators) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (mBiometricManager == null) {
                Log.e(TAG, "Failure in getStrings(). BiometricManager was null.");
                return null;
            }
            return new Strings(Api31Impl.getStrings(mBiometricManager, authenticators));
        }

        final StringsCompat stringsCompat = new StringsCompat(
                mInjector.getResources(),
                authenticators,
                mInjector.isFingerprintHardwarePresent(),
                mInjector.isFaceHardwarePresent(),
                mInjector.isIrisHardwarePresent(),
                mInjector.isDeviceSecuredWithCredential());

        return new Strings(stringsCompat);
    }


    /**
     * Nested class to avoid verification errors for methods introduced in Android 12 (API 31).
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private static class Api31Impl {
        // Prevent instantiation.
        private Api31Impl() {}

        /**
         * Gets an instance of the framework
         * {@link android.hardware.biometrics.BiometricManager.Strings} class.
         *
         * @param biometricManager An instance of
         *                         {@link android.hardware.biometrics.BiometricManager}.
         * @param authenticators   A bit field representing the types of {@link Authenticators} that
         *                         may be used for authentication.
         * @return An instance of {@link android.hardware.biometrics.BiometricManager.Strings}.
         */
        @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
        @NonNull
        static android.hardware.biometrics.BiometricManager.Strings getStrings(
                @NonNull android.hardware.biometrics.BiometricManager biometricManager,
                @AuthenticatorTypes int authenticators) {
            return biometricManager.getStrings(authenticators);
        }

        /**
         * Calls {@link android.hardware.biometrics.BiometricManager.Strings#getButtonLabel()} for
         * the given framework strings instance.
         *
         * @param strings An instance of
         *                {@link android.hardware.biometrics.BiometricManager.Strings}.
         * @return The result of
         * {@link android.hardware.biometrics.BiometricManager.Strings#getButtonLabel()}.
         */
        @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
        @Nullable
        static CharSequence getButtonLabel(
                @NonNull android.hardware.biometrics.BiometricManager.Strings strings) {
            return strings.getButtonLabel();
        }

        /**
         * Calls {@link android.hardware.biometrics.BiometricManager.Strings#getPromptMessage()} for
         * the given framework strings instance.
         *
         * @param strings An instance of
         *                {@link android.hardware.biometrics.BiometricManager.Strings}.
         * @return The result of
         * {@link android.hardware.biometrics.BiometricManager.Strings#getPromptMessage()}.
         */
        @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
        @Nullable
        static CharSequence getPromptMessage(
                @NonNull android.hardware.biometrics.BiometricManager.Strings strings) {
            return strings.getPromptMessage();
        }

        /**
         * Calls {@link android.hardware.biometrics.BiometricManager.Strings#getSettingName()} for
         * the given framework strings instance.
         *
         * @param strings An instance of
         *                {@link android.hardware.biometrics.BiometricManager.Strings}.
         * @return The result of
         * {@link android.hardware.biometrics.BiometricManager.Strings#getSettingName()}.
         */
        @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
        @Nullable
        static CharSequence getSettingName(
                @NonNull android.hardware.biometrics.BiometricManager.Strings strings) {
            return strings.getSettingName();
        }
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
