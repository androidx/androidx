/*
 * Copyright 2026 The Android Open Source Project
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


import androidx.annotation.RestrictTo;

/**
 * Interface containing all of the biometric modality agnostic constants.
 *
 * NOTE: The error messages must be consistent with
 * {@link android.hardware.biometrics.BiometricConstants}
 */
interface BiometricConstants {

    /**
     * There is no error, and the user can successfully authenticate.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int BIOMETRIC_SUCCESS = 0;

    /**
     * The hardware is unavailable. Try again later.
     */
    int ERROR_HW_UNAVAILABLE = 1;

    /**
     * The sensor was unable to process the current image.
     */
    int ERROR_UNABLE_TO_PROCESS = 2;

    /**
     * The current operation has been running too long and has timed out.
     *
     * <p>This is intended to prevent programs from waiting for the biometric sensor indefinitely.
     * The timeout is platform and sensor-specific, but is generally on the order of ~30 seconds.
     */
    int ERROR_TIMEOUT = 3;

    /**
     * The operation can't be completed because there is not enough device storage remaining.
     */
    int ERROR_NO_SPACE = 4;

    /**
     * The operation was canceled because the biometric sensor is unavailable. This may happen when
     * the user is switched, the device is locked, or another pending operation prevents it.
     */
    int ERROR_CANCELED = 5;

    /**
     * The operation was canceled because the API is locked out due to too many attempts. This
     * occurs after 5 failed attempts, and lasts for 30 seconds.
     */
    int ERROR_LOCKOUT = 7;

    /**
     * The operation failed due to a vendor-specific error.
     *
     * <p>This error code may be used by hardware vendors to extend this list to cover errors that
     * don't fall under one of the other predefined categories. Vendors are responsible for
     * providing the strings for these errors.
     *
     * <p>These messages are typically reserved for internal operations such as enrollment but may
     * be used to express any error that is not otherwise covered. In this case, applications are
     * expected to show the error message, but they are advised not to rely on the message ID, since
     * this may vary by vendor and device.
     */
    int ERROR_VENDOR = 8;

    /**
     * The operation was canceled because {@link #ERROR_LOCKOUT} occurred too many times. Biometric
     * authentication is disabled until the user unlocks with their device credential (i.e. PIN,
     * pattern, or password).
     */
    int ERROR_LOCKOUT_PERMANENT = 9;

    /**
     * The user canceled the operation.
     *
     * <p>Upon receiving this, applications should use alternate authentication, such as a password.
     * The application should also provide the user a way of returning to biometric authentication,
     * such as a button.
     */
    int ERROR_USER_CANCELED = 10;

    /**
     * The user does not have any biometrics enrolled.
     */
    int ERROR_NO_BIOMETRICS = 11;

    /**
     * The device does not have the required authentication hardware.
     */
    int ERROR_HW_NOT_PRESENT = 12;

    /**
     * Indicates that the user pressed the negative button.
     *
     * **Note:** This constant is not used for results of type [AuthenticationResult.Error]
     * in the new API. Instead, negative button clicks are delivered via
     * [AuthenticationResult.CustomFallbackSelected].
     */
    int ERROR_NEGATIVE_BUTTON = 13;

    /**
     * The device does not have pin, pattern, or password set up.
     */
    int ERROR_NO_DEVICE_CREDENTIAL = 14;

    /**
     * A security vulnerability has been discovered with one or more hardware sensors. The
     * affected sensor(s) are unavailable until a security update has addressed the issue.
     */
    int ERROR_SECURITY_UPDATE_REQUIRED = 15;

    /**
     * The privacy setting has been enabled and will block use of the sensor.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int ERROR_SENSOR_PRIVACY_ENABLED = 18;

    /**
     * Identity Check is currently not active.
     *
     * This device either doesn't have this feature enabled, or it's not considered in a
     * high-risk environment that requires extra security measures for accessing sensitive data.
     */
    int ERROR_IDENTITY_CHECK_NOT_ACTIVE = 20;

    /**
     * Biometrics is not allowed to verify the user in apps. It's for internal use only. This
     * error code, introduced in API 35, was previously covered by ERROR_HW_UNAVAILABLE and
     * doesn't need to be public. Therefore, for backward compatibility, this error will be
     * converted to ERROR_HW_UNAVAILABLE.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int ERROR_NOT_ENABLED_FOR_APPS = 21;

    /**
     * The user pressed the more options button on {@link PromptContentViewWithMoreOptionsButton}
     * set by {@link BiometricPrompt.PromptInfo.Builder#setContentView}
     */
    int ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON = 22;


    /**
     * Authentication type reported by {@link BiometricPrompt.AuthenticationResult} when the user
     * authenticated via an unknown method.
     *
     * <p>This value may be returned on older Android versions due to partial incompatibility
     * with a newer API. It does NOT necessarily imply that the user authenticated with a method
     * other than those represented by {@link #AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL} and
     * {@link #AUTHENTICATION_RESULT_TYPE_BIOMETRIC}.
     */
    int AUTHENTICATION_RESULT_TYPE_UNKNOWN = -1;

    /**
     * Authentication type reported by {@link BiometricPrompt.AuthenticationResult} when the user
     * authenticated by entering their device PIN, pattern, or password.
     */
    int AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL = 1;

    /**
     * Authentication type reported by {@link BiometricPrompt.AuthenticationResult} when the user
     * authenticated by presenting some form of biometric (e.g. fingerprint or face).
     */
    int AUTHENTICATION_RESULT_TYPE_BIOMETRIC = 2;

    /**
     * An icon representing a password. Added in
     * {@link android.os.Build.VERSION_CODES_FULL#BAKLAVA_1}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int ICON_TYPE_PASSWORD = 0;

    /**
     * An icon representing a QR code. Added in
     * {@link android.os.Build.VERSION_CODES_FULL#BAKLAVA_1}
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int ICON_TYPE_QR_CODE = 1;

    /**
     * An icon representing a user account. Added in
     * {@link android.os.Build.VERSION_CODES_FULL#BAKLAVA_1}
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int ICON_TYPE_ACCOUNT = 2;

    /**
     * A generic icon.Added in {@link android.os.Build.VERSION_CODES_FULL#BAKLAVA_1}
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int ICON_TYPE_GENERIC = 3;

    /**
     * The maximum amount of fallback options that can be added to the prompt
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int MAX_FALLBACK_OPTIONS = 4;
}
