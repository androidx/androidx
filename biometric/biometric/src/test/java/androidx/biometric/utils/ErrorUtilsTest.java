/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.biometric.utils;

import static com.google.common.truth.Truth.assertThat;

import androidx.biometric.BiometricPrompt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ErrorUtilsTest {
    @Test
    public void testToKnownErrorCode_ReturnsOriginalErrors_ForKnownErrors() {
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_HW_UNAVAILABLE)).isEqualTo(
                BiometricPrompt.ERROR_HW_UNAVAILABLE);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_UNABLE_TO_PROCESS)).isEqualTo(
                BiometricPrompt.ERROR_UNABLE_TO_PROCESS);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_TIMEOUT)).isEqualTo(
                BiometricPrompt.ERROR_TIMEOUT);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_NO_SPACE)).isEqualTo(
                BiometricPrompt.ERROR_NO_SPACE);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_CANCELED)).isEqualTo(
                BiometricPrompt.ERROR_CANCELED);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_LOCKOUT)).isEqualTo(
                BiometricPrompt.ERROR_LOCKOUT);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_VENDOR)).isEqualTo(
                BiometricPrompt.ERROR_VENDOR);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_LOCKOUT_PERMANENT)).isEqualTo(
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_USER_CANCELED)).isEqualTo(
                BiometricPrompt.ERROR_USER_CANCELED);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_NO_BIOMETRICS)).isEqualTo(
                BiometricPrompt.ERROR_NO_BIOMETRICS);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_HW_NOT_PRESENT)).isEqualTo(
                BiometricPrompt.ERROR_HW_NOT_PRESENT);
        assertThat(ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_NEGATIVE_BUTTON)).isEqualTo(
                BiometricPrompt.ERROR_NEGATIVE_BUTTON);
        assertThat(
                ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL)).isEqualTo(
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL);
        assertThat(ErrorUtils.toKnownErrorCode(
                BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED)).isEqualTo(
                BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED);
        assertThat(ErrorUtils.toKnownErrorCode(
                BiometricPrompt.ERROR_IDENTITY_CHECK_NOT_ACTIVE)).isEqualTo(
                BiometricPrompt.ERROR_IDENTITY_CHECK_NOT_ACTIVE);
        assertThat(ErrorUtils.toKnownErrorCode(
                BiometricPrompt.ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON)).isEqualTo(
                BiometricPrompt.ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON);
    }

    @Test
    public void testToKnownErrorCode_ReturnsHWUnavailable_ForSomeHiddenErrors() {
        assertThat(
                ErrorUtils.toKnownErrorCode(BiometricPrompt.ERROR_NOT_ENABLED_FOR_APPS)).isEqualTo(
                BiometricPrompt.ERROR_HW_UNAVAILABLE);
        assertThat(ErrorUtils.toKnownErrorCode(
                BiometricPrompt.ERROR_SENSOR_PRIVACY_ENABLED)).isEqualTo(
                BiometricPrompt.ERROR_HW_UNAVAILABLE);
    }

    @Test
    public void testToKnownErrorCode_ReturnsErrorVendor_ForUnknownErrors() {
        assertThat(ErrorUtils.toKnownErrorCode(-1)).isEqualTo(BiometricPrompt.ERROR_VENDOR);
        assertThat(ErrorUtils.toKnownErrorCode(1337)).isEqualTo(BiometricPrompt.ERROR_VENDOR);
    }

    @Test
    public void testIsLockoutError_ReturnsTrue_ForLockoutErrors() {
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_LOCKOUT)).isTrue();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_LOCKOUT_PERMANENT)).isTrue();
    }

    @Test
    public void testIsLockoutError_ReturnsFalse_ForNonLockoutErrors() {
        assertThat(ErrorUtils.isLockoutError(-1)).isFalse();
        assertThat(ErrorUtils.isLockoutError(1337)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_HW_UNAVAILABLE)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_TIMEOUT)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_NO_SPACE)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_CANCELED)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_VENDOR)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_USER_CANCELED)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_NO_BIOMETRICS)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_HW_NOT_PRESENT)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_NEGATIVE_BUTTON)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED))
                .isFalse();
        assertThat(
                ErrorUtils.isLockoutError(
                        BiometricPrompt.ERROR_IDENTITY_CHECK_NOT_ACTIVE)).isFalse();
        assertThat(ErrorUtils.isLockoutError(BiometricPrompt.ERROR_NOT_ENABLED_FOR_APPS)).isFalse();
        assertThat(ErrorUtils.isLockoutError(
                BiometricPrompt.ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON)).isFalse();
    }

    @Test
    public void testGetFingerprintErrorString_ReturnsEmpty_ForNullContext() {
        assertThat(ErrorUtils.getFingerprintErrorString(
                null /* context */, BiometricPrompt.ERROR_CANCELED)).isEmpty();
    }
}
