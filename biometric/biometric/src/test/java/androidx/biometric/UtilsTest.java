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

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class UtilsTest {
    @Test
    public void testIsUnknownError_ReturnsFalse_ForKnownErrors() {
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_HW_UNAVAILABLE)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_TIMEOUT)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_NO_SPACE)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_CANCELED)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_LOCKOUT)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_VENDOR)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_LOCKOUT_PERMANENT)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_USER_CANCELED)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_NO_BIOMETRICS)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_HW_NOT_PRESENT)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_NEGATIVE_BUTTON)).isFalse();
        assertThat(Utils.isUnknownError(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL)).isFalse();
    }

    @Test
    public void testIsUnknownError_ReturnsTrue_ForUnknownErrors() {
        assertThat(Utils.isUnknownError(-1)).isTrue();
        assertThat(Utils.isUnknownError(1337)).isTrue();
    }
}
