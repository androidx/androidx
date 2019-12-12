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

package androidx.biometric;

import androidx.annotation.RestrictTo;

/**
 * Interface containing all of the biometric modality agnostic constants. These constants must
 * be kept in sync with the platform BiometricConstants.java
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface BiometricConstants {
    /**
     * The hardware is unavailable. Try again later.
     */
    int ERROR_HW_UNAVAILABLE = 1;

    /**
     * Error state returned when the sensor was unable to process the current image.
     */
    int ERROR_UNABLE_TO_PROCESS = 2;

    /**
     * Error state returned when the current request has been running too long. This is intended to
     * prevent programs from waiting for the biometric sensor indefinitely. The timeout is platform
     * and sensor-specific, but is generally on the order of 30 seconds.
     */
    int ERROR_TIMEOUT = 3;

    /**
     * Error state returned for operations like enrollment; the operation cannot be completed
     * because there's not enough storage remaining to complete the operation.
     */
    int ERROR_NO_SPACE = 4;

    /**
     * The operation was canceled because the biometric sensor is unavailable. For example, this may
     * happen when the user is switched, the device is locked or another pending operation prevents
     * or disables it.
     */
    int ERROR_CANCELED = 5;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int ERROR_UNABLE_TO_REMOVE = 6;

    /**
     * The operation was canceled because the API is locked out due to too many attempts.
     * This occurs after 5 failed attempts, and lasts for 30 seconds.
     */
    int ERROR_LOCKOUT = 7;

    /**
     * Hardware vendors may extend this list if there are conditions that do not fall under one of
     * the above categories. Vendors are responsible for providing error strings for these errors.
     * These messages are typically reserved for internal operations such as enrollment, but may be
     * used to express vendor errors not otherwise covered. Applications are expected to show the
     * error message string if they happen, but are advised not to rely on the message id since they
     * will be device and vendor-specific
     */
    int ERROR_VENDOR = 8;

    /**
     * The operation was canceled because ERROR_LOCKOUT occurred too many times.
     * Biometric authentication is disabled until the user unlocks with strong authentication
     * (PIN/Pattern/Password)
     */
    int ERROR_LOCKOUT_PERMANENT = 9;

    /**
     * The user canceled the operation. Upon receiving this, applications should use alternate
     * authentication (e.g. a password). The application should also provide the means to return to
     * biometric authentication, such as a "use <biometric>" button.
     */
    int ERROR_USER_CANCELED = 10;

    /**
     * The user does not have any biometrics enrolled.
     */
    int ERROR_NO_BIOMETRICS = 11;

    /**
     * The device does not have a biometric sensor.
     */
    int ERROR_HW_NOT_PRESENT = 12;

    /**
     * The user pressed the negative button.
     */
    int ERROR_NEGATIVE_BUTTON = 13;

    /**
     * The device does not have pin, pattern, or password set up.
     */
    int ERROR_NO_DEVICE_CREDENTIAL = 14;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    int ERROR_VENDOR_BASE = 1000;
}
