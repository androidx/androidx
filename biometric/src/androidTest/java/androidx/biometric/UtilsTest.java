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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UtilsTest {
    private static final String TAG = "UtilsTest";

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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testLaunchDeviceCredentialConfirmation_ReturnsEarly_WithNullActivity() {
        final Runnable onLaunch = mock(Runnable.class);

        Utils.launchDeviceCredentialConfirmation(
                TAG, null /* activity */, null /* bundle */, onLaunch);

        verifyZeroInteractions(onLaunch);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testLaunchDeviceCredentialConfirmation_ReturnsEarly_WithNonHandlerActivity() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        final Runnable onLaunch = mock(Runnable.class);

        Utils.launchDeviceCredentialConfirmation(TAG, activity, null /* bundle */, onLaunch);

        verifyZeroInteractions(activity, onLaunch);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testLaunchDeviceCredentialConfirmation_FinishesAndReturns_WithNoKeyguardManager() {
        final DeviceCredentialHandlerActivity handlerActivity =
                mock(DeviceCredentialHandlerActivity.class);
        final Runnable onLaunch = mock(Runnable.class);

        mockKeyguardManagerForHandlerActivity(handlerActivity, null);

        Utils.launchDeviceCredentialConfirmation(TAG, handlerActivity, null /* bundle */, onLaunch);

        verify(handlerActivity).handleDeviceCredentialResult(Activity.RESULT_CANCELED);
        verify(handlerActivity, never()).startActivityForResult(any(Intent.class), anyInt());
        verifyZeroInteractions(onLaunch);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testLaunchDeviceCredentialConfirmation_FinishesAndReturns_WithNoCDCIntent() {
        final DeviceCredentialHandlerActivity handlerActivity =
                mock(DeviceCredentialHandlerActivity.class);
        final Runnable onLaunch = mock(Runnable.class);
        final KeyguardManager keyguardManager = mock(KeyguardManager.class);

        mockKeyguardManagerForHandlerActivity(handlerActivity, keyguardManager);
        when(keyguardManager.createConfirmDeviceCredentialIntent(
                nullable(CharSequence.class), nullable(CharSequence.class))).thenReturn(null);

        Utils.launchDeviceCredentialConfirmation(
                TAG, handlerActivity, null /* bundle */, null /* onLaunch */);

        verify(handlerActivity).handleDeviceCredentialResult(Activity.RESULT_CANCELED);
        verify(handlerActivity, never()).startActivityForResult(any(Intent.class), anyInt());
        verifyZeroInteractions(onLaunch);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testLaunchDeviceCredentialConfirmation_DoesLaunchActivity() {
        final DeviceCredentialHandlerActivity handlerActivity =
                mock(DeviceCredentialHandlerActivity.class);
        final Runnable onLaunch = mock(Runnable.class);
        final KeyguardManager keyguardManager = mock(KeyguardManager.class);
        final Intent intent = mock(Intent.class);
        final String title = "title";
        final String subtitle = "subtitle";
        final Bundle bundle = new Bundle();
        bundle.putCharSequence(BiometricPrompt.KEY_TITLE, title);
        bundle.putCharSequence(BiometricPrompt.KEY_SUBTITLE, subtitle);

        when(keyguardManager.createConfirmDeviceCredentialIntent(
                nullable(CharSequence.class), nullable(CharSequence.class))).thenReturn(intent);
        mockKeyguardManagerForHandlerActivity(handlerActivity, keyguardManager);

        Utils.launchDeviceCredentialConfirmation(TAG, handlerActivity, bundle, onLaunch);

        verify(onLaunch).run();
        verify(keyguardManager).createConfirmDeviceCredentialIntent(title, subtitle);
        verify(handlerActivity).startActivityForResult(eq(intent), anyInt());
    }


    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testLaunchDeviceCredentialConfirmation_DoesLaunchActivity_WithoutBundle() {
        final DeviceCredentialHandlerActivity handlerActivity =
                mock(DeviceCredentialHandlerActivity.class);
        final Runnable onLaunch = mock(Runnable.class);
        final KeyguardManager keyguardManager = mock(KeyguardManager.class);
        final Intent intent = mock(Intent.class);

        when(keyguardManager.createConfirmDeviceCredentialIntent(
                nullable(CharSequence.class), nullable(CharSequence.class))).thenReturn(intent);
        mockKeyguardManagerForHandlerActivity(handlerActivity, keyguardManager);

        Utils.launchDeviceCredentialConfirmation(TAG, handlerActivity, null /* bundle */, onLaunch);

        verify(onLaunch).run();
        verify(handlerActivity).startActivityForResult(eq(intent), anyInt());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testLaunchDeviceCredentialConfirmation_DoesLaunchActivity_WithoutOnLaunch() {
        final DeviceCredentialHandlerActivity handlerActivity =
                mock(DeviceCredentialHandlerActivity.class);
        final KeyguardManager keyguardManager = mock(KeyguardManager.class);
        final Intent intent = mock(Intent.class);

        when(keyguardManager.createConfirmDeviceCredentialIntent(
                nullable(CharSequence.class), nullable(CharSequence.class))).thenReturn(intent);
        mockKeyguardManagerForHandlerActivity(handlerActivity, keyguardManager);

        Utils.launchDeviceCredentialConfirmation(TAG, handlerActivity, null /* bundle */,
                null /* onLaunch */);

        verify(handlerActivity).startActivityForResult(eq(intent), anyInt());
    }

    @Test
    public void testMaybeFinishHandler_FinishesActivity_WhenHandlerActivityNotFinishing() {
        final DeviceCredentialHandlerActivity handlerActivity =
                mock(DeviceCredentialHandlerActivity.class);
        when(handlerActivity.isFinishing()).thenReturn(false);

        Utils.maybeFinishHandler(handlerActivity);

        verify(handlerActivity).finish();
    }

    @Test
    public void testMaybeFinishHandler_DoesNotFinishActivity_WhenHandlerActivityIsFinishing() {
        final DeviceCredentialHandlerActivity handlerActivity =
                mock(DeviceCredentialHandlerActivity.class);
        when(handlerActivity.isFinishing()).thenReturn(true);

        Utils.maybeFinishHandler(handlerActivity);

        verify(handlerActivity, never()).finish();
    }

    @Test
    public void testMaybeFinishHandler_DoesNotFinishActivity_WhenGivenNonHandlerActivity() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.isFinishing()).thenReturn(false);

        Utils.maybeFinishHandler(activity);

        verify(activity, never()).finish();
    }

    private static void mockKeyguardManagerForHandlerActivity(
            @NonNull DeviceCredentialHandlerActivity activityMock,
            @Nullable KeyguardManager keyguardManagerMock) {
        when(activityMock.getSystemService(anyString())).thenReturn(keyguardManagerMock);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when(activityMock.getSystemServiceName(KeyguardManager.class))
                    .thenReturn("KeyguardManager");
            when(activityMock.getSystemService(KeyguardManager.class))
                    .thenReturn(keyguardManagerMock);
        }
    }
}
