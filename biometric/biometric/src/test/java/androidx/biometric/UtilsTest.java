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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.fragment.app.FragmentActivity;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@LargeTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class UtilsTest {
    @Mock
    DeviceCredentialHandlerActivity mHandlerActivity;
    @Mock
    FragmentActivity mFragmentActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        // Ensure the bridge is fully reset after running each test.
        final DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        if (bridge != null) {
            bridge.stopIgnoringReset();
            bridge.reset();
        }
    }

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

    @Test
    public void testMaybeFinishHandler_FinishesActivity_WhenHandlerActivityNotFinishing() {
        when(mHandlerActivity.isFinishing()).thenReturn(false);
        Utils.maybeFinishHandler(mHandlerActivity);
        verify(mHandlerActivity).finish();
    }

    @Test
    public void testMaybeFinishHandler_DoesNotFinishActivity_WhenHandlerActivityIsFinishing() {
        when(mHandlerActivity.isFinishing()).thenReturn(true);
        Utils.maybeFinishHandler(mHandlerActivity);
        verify(mHandlerActivity, never()).finish();
    }

    @Test
    public void testMaybeFinishHandler_DoesNotFinishActivity_WhenGivenNonHandlerActivity() {
        when(mFragmentActivity.isFinishing()).thenReturn(false);
        Utils.maybeFinishHandler(mFragmentActivity);
        verify(mFragmentActivity, never()).finish();
    }

    @Test
    public void testIsConfirmingDeviceCredential_ReturnsFalse_WhenBridgeIsNull() {
        assertThat(Utils.isConfirmingDeviceCredential()).isFalse();
    }

    @Test
    public void testIsConfirmingDeviceCredential_ReturnsFalse_WhenBridgeValueIsFalse() {
        DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstance();
        bridge.setConfirmingDeviceCredential(false);
        assertThat(Utils.isConfirmingDeviceCredential()).isFalse();
    }

    @Test
    public void testIsConfirmingDeviceCredential_ReturnsTrue_WhenBridgeValueIsTrue() {
        DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstance();
        bridge.setConfirmingDeviceCredential(true);
        assertThat(Utils.isConfirmingDeviceCredential()).isTrue();
    }
}
