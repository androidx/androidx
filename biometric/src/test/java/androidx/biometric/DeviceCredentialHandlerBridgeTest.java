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

import android.content.DialogInterface;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.biometric.DeviceCredentialHandlerBridge.DeviceCredentialResult;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class DeviceCredentialHandlerBridgeTest {
    private static final BiometricPrompt.AuthenticationCallback AUTH_CALLBACK =
            new BiometricPrompt.AuthenticationCallback() {
            };

    private static final DialogInterface.OnClickListener CLICK_LISTENER =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            };

    private static final Executor EXECUTOR = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
        }
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
        assertThat(DeviceCredentialHandlerBridge.getInstanceIfNotNull()).isNull();

        // Calling getInstance *should* create the singleton.
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge).isNotNull();
        assertThat(DeviceCredentialHandlerBridge.getInstance()).isEqualTo(bridge);
        assertThat(DeviceCredentialHandlerBridge.getInstanceIfNotNull()).isEqualTo(bridge);
    }

    @Test
    public void testClientThemeResId_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getClientThemeResId()).isEqualTo(0);

        final int resId = 42;
        bridge.setClientThemeResId(resId);
        assertThat(bridge.getClientThemeResId()).isEqualTo(resId);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testBiometricFragment_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getBiometricFragment()).isNull();

        final BiometricFragment fragment = BiometricFragment.newInstance();
        bridge.setBiometricFragment(fragment);
        assertThat(bridge.getBiometricFragment()).isEqualTo(fragment);
    }

    @Test
    public void testFingerprintFragments_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getFingerprintDialogFragment()).isNull();
        assertThat(bridge.getFingerprintHelperFragment()).isNull();

        final FingerprintDialogFragment dialogFragment = FingerprintDialogFragment.newInstance();
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        bridge.setFingerprintFragments(dialogFragment, helperFragment);
        assertThat(bridge.getFingerprintDialogFragment()).isEqualTo(dialogFragment);
        assertThat(bridge.getFingerprintHelperFragment()).isEqualTo(helperFragment);
    }

    @Test
    public void testCallbacks_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getExecutor()).isNull();
        assertThat(bridge.getOnClickListener()).isNull();
        assertThat(bridge.getAuthenticationCallback()).isNull();

        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        assertThat(bridge.getExecutor()).isEqualTo(EXECUTOR);
        assertThat(bridge.getOnClickListener()).isEqualTo(CLICK_LISTENER);
        assertThat(bridge.getAuthenticationCallback()).isEqualTo(AUTH_CALLBACK);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testCallbacks_ArePassedToBiometricFragment_WhenSetAfter() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        final BiometricFragment fragment = BiometricFragment.newInstance();
        bridge.setBiometricFragment(fragment);
        assertThat(fragment.mClientExecutor).isNull();
        assertThat(fragment.mClientNegativeButtonListener).isNull();
        assertThat(fragment.mClientAuthenticationCallback).isNull();

        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        assertThat(fragment.mClientExecutor).isEqualTo(EXECUTOR);
        assertThat(fragment.mClientNegativeButtonListener).isEqualTo(CLICK_LISTENER);
        assertThat(fragment.mClientAuthenticationCallback).isEqualTo(AUTH_CALLBACK);
    }

    @Test
    public void testCallbacks_ArePassedToFingerprintFragments_WhenSetAfter() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        final FingerprintDialogFragment dialogFragment = FingerprintDialogFragment.newInstance();
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        bridge.setFingerprintFragments(dialogFragment, helperFragment);
        assertThat(dialogFragment.mNegativeButtonListener).isNull();
        assertThat(helperFragment.mExecutor).isNull();
        assertThat(helperFragment.mClientAuthenticationCallback).isNull();

        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        assertThat(dialogFragment.mNegativeButtonListener).isEqualTo(CLICK_LISTENER);
        assertThat(helperFragment.mExecutor).isEqualTo(EXECUTOR);
        assertThat(helperFragment.mClientAuthenticationCallback).isEqualTo(AUTH_CALLBACK);
    }

    @Test
    public void testDeviceCredentialResult_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.getDeviceCredentialResult())
                .isEqualTo(DeviceCredentialHandlerBridge.RESULT_NONE);

        final @DeviceCredentialResult int resultBad = DeviceCredentialHandlerBridge.RESULT_ERROR;
        bridge.setDeviceCredentialResult(resultBad);
        assertThat(bridge.getDeviceCredentialResult()).isEqualTo(resultBad);

        final @DeviceCredentialResult int resultGood = DeviceCredentialHandlerBridge.RESULT_SUCCESS;
        bridge.setDeviceCredentialResult(resultGood);
        assertThat(bridge.getDeviceCredentialResult()).isEqualTo(resultGood);
    }

    @Test
    public void testConfirmingDeviceCredential_CanSetAndGet() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        assertThat(bridge.isConfirmingDeviceCredential()).isFalse();

        bridge.setConfirmingDeviceCredential(true);
        assertThat(bridge.isConfirmingDeviceCredential()).isTrue();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testReset_ClearsBiometricFragment_WhenNotIgnoringReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setBiometricFragment(BiometricFragment.newInstance());
        bridge.reset();
        assertThat(bridge.getBiometricFragment()).isNull();
    }

    @Test
    public void testReset_ClearsState_WhenNotIgnoringReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setClientThemeResId(1);
        bridge.setFingerprintFragments(
                FingerprintDialogFragment.newInstance(), FingerprintHelperFragment.newInstance());
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.setDeviceCredentialResult(DeviceCredentialHandlerBridge.RESULT_SUCCESS);
        bridge.setConfirmingDeviceCredential(true);

        bridge.reset();
        assertThat(bridge.getClientThemeResId()).isEqualTo(0);
        assertThat(bridge.getFingerprintDialogFragment()).isNull();
        assertThat(bridge.getFingerprintHelperFragment()).isNull();
        assertThat(bridge.getExecutor()).isNull();
        assertThat(bridge.getOnClickListener()).isNull();
        assertThat(bridge.getAuthenticationCallback()).isNull();
        assertThat(bridge.getDeviceCredentialResult()).isEqualTo(
                DeviceCredentialHandlerBridge.RESULT_NONE);
        assertThat(bridge.isConfirmingDeviceCredential()).isFalse();
        assertThat(DeviceCredentialHandlerBridge.getInstanceIfNotNull()).isNull();
    }

    @Test
    public void testIgnoreNextReset_PreventsReset_OnceOnly() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.ignoreNextReset();
        bridge.reset();
        assertThat(bridge.getExecutor()).isEqualTo(EXECUTOR);
        bridge.reset();
        assertThat(bridge.getExecutor()).isNull();
    }

    @Test
    public void testStartIgnoringReset_PreventsReset_UntilStopIgnoringReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.startIgnoringReset();
        for (int i = 0; i < 5; ++i) {
            bridge.reset();
            assertThat(bridge.getExecutor()).isEqualTo(EXECUTOR);
        }
        bridge.stopIgnoringReset();
        bridge.reset();
        assertThat(bridge.getExecutor()).isNull();
    }

    @Test
    public void testStartIgnoringReset_NotAffectedByIgnoreNextReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.startIgnoringReset();
        for (int i = 0; i < 3; ++i) {
            bridge.reset();
            assertThat(bridge.getExecutor()).isEqualTo(EXECUTOR);
        }

        // Should keep preventing reset until stopIgnoringReset is called.
        bridge.ignoreNextReset();
        for (int i = 0; i < 3; ++i) {
            bridge.reset();
            assertThat(bridge.getExecutor()).isEqualTo(EXECUTOR);
        }

        bridge.stopIgnoringReset();
        bridge.reset();
        assertThat(bridge.getExecutor()).isNull();
    }

    @Test
    public void testStartIgnoringReset_OverridesIgnoreNextReset() {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setCallbacks(EXECUTOR, CLICK_LISTENER, AUTH_CALLBACK);
        bridge.ignoreNextReset();
        bridge.startIgnoringReset();
        for (int i = 0; i < 5; ++i) {
            bridge.reset();
            assertThat(bridge.getExecutor()).isEqualTo(EXECUTOR);
        }
        bridge.stopIgnoringReset();
        bridge.reset();
        assertThat(bridge.getExecutor()).isNull();
    }
}
