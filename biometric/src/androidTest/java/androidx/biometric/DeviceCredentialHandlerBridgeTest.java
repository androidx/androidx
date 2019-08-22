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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import android.content.DialogInterface;
import android.os.Build;

import androidx.biometric.DeviceCredentialHandlerBridge.DeviceCredentialResult;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceCredentialHandlerBridgeTest {
    private static final BiometricPrompt.AuthenticationCallback AUTH_CALLBACK =
            new BiometricPrompt.AuthenticationCallback() {
            };

    private static final DialogInterface.OnClickListener CLICK_LISTENER = (dialog, which) -> {
    };

    private static final Executor EXECUTOR = runnable -> {
    };

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
    public void testGetInstance_CreatesSingleton() {
        // Calling getInstanceIfNotNull should *not* create the singleton.
        assertThat(DeviceCredentialHandlerBridge.getInstanceIfNotNull(), nullValue());

        // Calling getInstance *should* create the singleton.
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge, notNullValue());
        assertThat(DeviceCredentialHandlerBridge.getInstance(), equalTo(bridge));
        assertThat(DeviceCredentialHandlerBridge.getInstanceIfNotNull(), equalTo(bridge));
    }

    @Test
    public void testClientThemeResId_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getClientThemeResId(), equalTo(0));

        final int resId = 42;
        bridge.setClientThemeResId(resId);
        assertThat(bridge.getClientThemeResId(), equalTo(resId));
    }

    @Test
    @UiThreadTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void testBiometricFragment_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getBiometricFragment(), nullValue());

        final BiometricFragment fragment = BiometricFragment.newInstance();
        bridge.setBiometricFragment(fragment);
        assertThat(bridge.getBiometricFragment(), equalTo(fragment));
    }

    @Test
    @UiThreadTest
    public void testFingerprintFragments_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getFingerprintDialogFragment(), nullValue());
        assertThat(bridge.getFingerprintHelperFragment(), nullValue());

        final FingerprintDialogFragment dialogFragment = FingerprintDialogFragment.newInstance();
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        bridge.setFingerprintFragments(dialogFragment, helperFragment);
        assertThat(bridge.getFingerprintDialogFragment(), equalTo(dialogFragment));
        assertThat(bridge.getFingerprintHelperFragment(), equalTo(helperFragment));
    }

    @Test
    public void testCallbacks_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getExecutor(), nullValue());
        assertThat(bridge.getOnClickListener(), nullValue());
        assertThat(bridge.getAuthenticationCallback(), nullValue());

        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        assertThat(bridge.getExecutor(), equalTo(EXECUTOR));
        assertThat(bridge.getOnClickListener(), equalTo(CLICK_LISTENER));
        assertThat(bridge.getAuthenticationCallback(), equalTo(AUTH_CALLBACK));
    }

    @Test
    @UiThreadTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void testCallbacks_ArePassedToBiometricFragment_WhenSetAfter() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        final BiometricFragment fragment = BiometricFragment.newInstance();
        bridge.setBiometricFragment(fragment);
        assertThat(fragment.mClientExecutor, nullValue());
        assertThat(fragment.mClientNegativeButtonListener, nullValue());
        assertThat(fragment.mClientAuthenticationCallback, nullValue());

        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        assertThat(fragment.mClientExecutor, equalTo(EXECUTOR));
        assertThat(fragment.mClientNegativeButtonListener, equalTo(CLICK_LISTENER));
        assertThat(fragment.mClientAuthenticationCallback, equalTo(AUTH_CALLBACK));
    }

    @Test
    @UiThreadTest
    public void testCallbacks_ArePassedToFingerprintFragments_WhenSetAfter() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        final FingerprintDialogFragment dialogFragment = FingerprintDialogFragment.newInstance();
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        bridge.setFingerprintFragments(dialogFragment, helperFragment);
        assertThat(dialogFragment.mNegativeButtonListener, nullValue());
        assertThat(helperFragment.mExecutor, nullValue());
        assertThat(helperFragment.mClientAuthenticationCallback, nullValue());

        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        assertThat(dialogFragment.mNegativeButtonListener, equalTo(CLICK_LISTENER));
        assertThat(helperFragment.mExecutor, equalTo(EXECUTOR));
        assertThat(helperFragment.mClientAuthenticationCallback, equalTo(AUTH_CALLBACK));
    }

    @Test
    public void testDeviceCredentialResult_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getDeviceCredentialResult(),
                equalTo(DeviceCredentialHandlerBridge.RESULT_NONE));

        final @DeviceCredentialResult int resultBad = DeviceCredentialHandlerBridge.RESULT_ERROR;
        bridge.setDeviceCredentialResult(resultBad);
        assertThat(bridge.getDeviceCredentialResult(), equalTo(resultBad));

        final @DeviceCredentialResult int resultGood = DeviceCredentialHandlerBridge.RESULT_SUCCESS;
        bridge.setDeviceCredentialResult(resultGood);
        assertThat(bridge.getDeviceCredentialResult(), equalTo(resultGood));
    }

    @Test
    @UiThreadTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void testReset_ClearsBiometricFragment_WhenNotIgnoringReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setBiometricFragment(BiometricFragment.newInstance());
        bridge.reset();
        assertThat(bridge.getBiometricFragment(), nullValue());
    }

    @Test
    @UiThreadTest
    public void testReset_ClearsMostState_WhenNotIgnoringReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setFingerprintFragments(
                FingerprintDialogFragment.newInstance(), FingerprintHelperFragment.newInstance());
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.reset();
        assertThat(bridge.getFingerprintDialogFragment(), nullValue());
        assertThat(bridge.getFingerprintHelperFragment(), nullValue());
        assertThat(bridge.getExecutor(), nullValue());
        assertThat(bridge.getOnClickListener(), nullValue());
        assertThat(bridge.getAuthenticationCallback(), nullValue());
        assertThat(DeviceCredentialHandlerBridge.getInstanceIfNotNull(), nullValue());
    }

    @Test
    public void testIgnoreNextReset_PreventsReset_OnceOnly() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.ignoreNextReset();
        bridge.reset();
        assertThat(bridge.getExecutor(), equalTo(EXECUTOR));
        bridge.reset();
        assertThat(bridge.getExecutor(), nullValue());
    }

    @Test
    public void testStartIgnoringReset_PreventsReset_UntilStopIgnoringReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.startIgnoringReset();
        for (int i = 0; i < 5; ++i) {
            bridge.reset();
            assertThat(bridge.getExecutor(), equalTo(EXECUTOR));
        }
        bridge.stopIgnoringReset();
        bridge.reset();
        assertThat(bridge.getExecutor(), nullValue());
    }

    @Test
    public void testStartIgnoringReset_NotAffectedByIgnoreNextReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.startIgnoringReset();
        for (int i = 0; i < 3; ++i) {
            bridge.reset();
            assertThat(bridge.getExecutor(), equalTo(EXECUTOR));
        }

        // Should keep preventing reset until stopIgnoringReset is called.
        bridge.ignoreNextReset();
        for (int i = 0; i < 3; ++i) {
            bridge.reset();
            assertThat(bridge.getExecutor(), equalTo(EXECUTOR));
        }

        bridge.stopIgnoringReset();
        bridge.reset();
        assertThat(bridge.getExecutor(), nullValue());
    }

    @Test
    public void testStartIgnoringReset_OverridesIgnoreNextReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.ignoreNextReset();
        bridge.startIgnoringReset();
        for (int i = 0; i < 5; ++i) {
            bridge.reset();
            assertThat(bridge.getExecutor(), equalTo(EXECUTOR));
        }
        bridge.stopIgnoringReset();
        bridge.reset();
        assertThat(bridge.getExecutor(), nullValue());
    }
}
