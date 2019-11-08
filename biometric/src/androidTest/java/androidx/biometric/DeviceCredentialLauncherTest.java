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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class DeviceCredentialLauncherTest {
    private static final String TAG = "DeviceCredentialLauncherTest";

    @Mock
    DeviceCredentialHandlerActivity mHandlerActivity;
    @Mock
    FragmentActivity mClientActivity;
    @Mock
    Intent mIntent;
    @Mock
    KeyguardManager mKeyguardManager;
    @Mock
    Runnable mOnLaunchRunnable;

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
    public void testLaunchConfirmation_ReturnsEarly_WithNullActivity() {
        DeviceCredentialLauncher.launchConfirmation(
                TAG, null /* activity */, null /* bundle */, mOnLaunchRunnable);
        verifyZeroInteractions(mOnLaunchRunnable);
    }

    @Test
    public void testLaunchConfirmation_ReturnsEarly_WithNonHandlerActivity() {
        DeviceCredentialLauncher.launchConfirmation(
                TAG, mClientActivity, null /* bundle */, mOnLaunchRunnable);
        verifyZeroInteractions(mClientActivity, mOnLaunchRunnable);
    }

    @Test
    public void testLaunchConfirmation_FinishesAndReturns_WithNoKeyguardManager() {
        mockKeyguardManagerForHandlerActivity(mHandlerActivity, null /* keyguardManagerMock */);

        DeviceCredentialLauncher.launchConfirmation(
                TAG, mHandlerActivity, null /* bundle */, mOnLaunchRunnable);

        verify(mHandlerActivity).handleDeviceCredentialResult(Activity.RESULT_CANCELED);
        verify(mHandlerActivity, never()).startActivityForResult(any(Intent.class), anyInt());
        verifyZeroInteractions(mOnLaunchRunnable);
    }

    @Test
    public void testLaunchConfirmation_FinishesAndReturns_WithNoCDCIntent() {
        mockKeyguardManagerForHandlerActivity(mHandlerActivity, mKeyguardManager);
        when(mKeyguardManager.createConfirmDeviceCredentialIntent(
                nullable(CharSequence.class), nullable(CharSequence.class))).thenReturn(null);

        DeviceCredentialLauncher.launchConfirmation(
                TAG, mHandlerActivity, null /* bundle */, null /* onLaunch */);

        verify(mHandlerActivity).handleDeviceCredentialResult(Activity.RESULT_CANCELED);
        verify(mHandlerActivity, never()).startActivityForResult(any(Intent.class), anyInt());
        verifyZeroInteractions(mOnLaunchRunnable);
    }

    @Test
    public void testLaunchConfirmation_DoesLaunchActivity() {
        final String title = "title";
        final String subtitle = "subtitle";
        final Bundle bundle = new Bundle();
        bundle.putCharSequence(BiometricPrompt.KEY_TITLE, title);
        bundle.putCharSequence(BiometricPrompt.KEY_SUBTITLE, subtitle);
        when(mKeyguardManager.createConfirmDeviceCredentialIntent(
                nullable(CharSequence.class), nullable(CharSequence.class))).thenReturn(mIntent);
        mockKeyguardManagerForHandlerActivity(mHandlerActivity, mKeyguardManager);

        DeviceCredentialLauncher.launchConfirmation(
                TAG, mHandlerActivity, bundle, mOnLaunchRunnable);

        verify(mOnLaunchRunnable).run();
        verify(mKeyguardManager).createConfirmDeviceCredentialIntent(title, subtitle);
        verify(mHandlerActivity).startActivityForResult(eq(mIntent), anyInt());
    }


    @Test
    public void testLaunchConfirmation_DoesLaunchActivity_WithoutBundle() {
        when(mKeyguardManager.createConfirmDeviceCredentialIntent(
                nullable(CharSequence.class), nullable(CharSequence.class))).thenReturn(mIntent);
        mockKeyguardManagerForHandlerActivity(mHandlerActivity, mKeyguardManager);

        DeviceCredentialLauncher.launchConfirmation(
                TAG, mHandlerActivity, null /* bundle */, mOnLaunchRunnable);

        verify(mOnLaunchRunnable).run();
        verify(mHandlerActivity).startActivityForResult(eq(mIntent), anyInt());
    }

    @Test
    public void testLaunchConfirmation_DoesLaunchActivity_WithoutOnLaunch() {
        when(mKeyguardManager.createConfirmDeviceCredentialIntent(
                nullable(CharSequence.class), nullable(CharSequence.class))).thenReturn(mIntent);
        mockKeyguardManagerForHandlerActivity(mHandlerActivity, mKeyguardManager);

        DeviceCredentialLauncher.launchConfirmation(
                TAG, mHandlerActivity, null /* bundle */, null /* onLaunch */);

        verify(mHandlerActivity).startActivityForResult(eq(mIntent), anyInt());
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
