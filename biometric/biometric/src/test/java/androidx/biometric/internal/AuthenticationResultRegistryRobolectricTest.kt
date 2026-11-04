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

package androidx.biometric.internal

import android.app.Activity
import android.app.Application
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.internal.viewmodel.AuthenticationViewModelFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class AuthenticationResultRegistryRobolectricTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val sharedViewModelStore = ViewModelStore()
    private val viewModelStoreOwner =
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = sharedViewModelStore
        }

    @After
    fun tearDown() {
        sharedViewModelStore.clear()
    }

    @Test
    fun unregister_afterCompletedAuthAndStopStart_doesNotInterceptSecondLauncherResult() {
        val lifecycleOwner = TestLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        var launcher1Result: AuthenticationResult? = null
        val registry1 = AuthenticationResultRegistry()
        val launcher1 =
            registry1.register(
                context = context,
                lifecycleOwner = lifecycleOwner,
                viewModelStoreOwner = viewModelStoreOwner,
                confirmCredentialActivityLauncher = {},
                callbackExecutor = { it.run() },
                resultCallback = { result -> launcher1Result = result },
            )

        var launcher2ResultCount = 0
        val registry2 = AuthenticationResultRegistry()
        val launcher2 =
            registry2.register(
                context = context,
                lifecycleOwner = lifecycleOwner,
                viewModelStoreOwner = viewModelStoreOwner,
                confirmCredentialActivityLauncher = {},
                callbackExecutor = { it.run() },
                resultCallback = { launcher2ResultCount++ },
            )

        val request =
            AuthenticationRequest.biometricRequest(title = "Auth") {
                setSubtitle("Subtitle")
            }
        val viewModel =
            ViewModelProvider(viewModelStoreOwner, AuthenticationViewModelFactory())
                .get(AuthenticationViewModel::class.java)

        launcher2.launch(request)
        viewModel.setAuthenticationResult(BiometricPrompt.AuthenticationResult(null, 0))
        assertThat(launcher2ResultCount).isEqualTo(1)
        registry2.unregister()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        launcher1.launch(request)
        viewModel.setAuthenticationResult(BiometricPrompt.AuthenticationResult(null, 0))

        assertThat(launcher1Result).isInstanceOf(AuthenticationResult.Success::class.java)
        assertThat(launcher2ResultCount).isEqualTo(1)
    }

    @Test
    fun unregister_beforeStarted_clearsPendingRequestAndIgnoresSubsequentLaunch() {
        val lifecycleOwner = TestLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val registry = AuthenticationResultRegistry()
        val launcher =
            registry.register(
                context = context,
                lifecycleOwner = lifecycleOwner,
                viewModelStoreOwner = viewModelStoreOwner,
                confirmCredentialActivityLauncher = {},
                callbackExecutor = { it.run() },
                resultCallback = {},
            )

        val request = AuthenticationRequest.biometricRequest(title = "Queued Title") {}
        launcher.launch(request)
        registry.unregister()
        launcher.launch(request)

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val viewModel =
            ViewModelProvider(viewModelStoreOwner, AuthenticationViewModelFactory())
                .get(AuthenticationViewModel::class.java)
        assertThat(viewModel.title).isNull()
        assertThat(viewModel.isPromptShowing).isFalse()
    }

    @Test
    fun register_withExplicitKey_reconnectsAfterUnregisterBeforeDestroyOnConfigChange() {
        val activity1 =
            Robolectric.buildActivity(ConfigChangeTestActivity::class.java).setup().get()
        val lifecycle1 = TestLifecycleOwner()
        lifecycle1.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val explicitKey = -42
        var recreatedResult: AuthenticationResult? = null

        // Advance generateNextHandlerKey() first so positional keys would not match
        val transientRegistry = AuthenticationResultRegistry()
        transientRegistry.register(
            context = activity1,
            lifecycleOwner = lifecycle1,
            viewModelStoreOwner = viewModelStoreOwner,
            confirmCredentialActivityLauncher = {},
            callbackExecutor = { it.run() },
            resultCallback = {},
        )
        transientRegistry.unregister()

        val registry1 = AuthenticationResultRegistry()
        val launcher1 =
            registry1.register(
                context = activity1,
                lifecycleOwner = lifecycle1,
                viewModelStoreOwner = viewModelStoreOwner,
                confirmCredentialActivityLauncher = {},
                callbackExecutor = { it.run() },
                resultCallback = {},
                key = explicitKey,
            )

        launcher1.launch(AuthenticationRequest.biometricRequest(title = "Rotate Auth") {})

        // Unregister before ON_DESTROY (matching Fragment ComposeView detach order)
        activity1.simulatingConfigChange = true
        registry1.unregister()
        lifecycle1.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        val activity2 =
            Robolectric.buildActivity(ConfigChangeTestActivity::class.java).setup().get()
        val lifecycle2 = TestLifecycleOwner()
        lifecycle2.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val registry2 = AuthenticationResultRegistry()
        registry2.register(
            context = activity2,
            lifecycleOwner = lifecycle2,
            viewModelStoreOwner = viewModelStoreOwner,
            confirmCredentialActivityLauncher = {},
            callbackExecutor = { it.run() },
            resultCallback = { result -> recreatedResult = result },
            key = explicitKey,
        )

        val viewModel =
            ViewModelProvider(viewModelStoreOwner, AuthenticationViewModelFactory())
                .get(AuthenticationViewModel::class.java)
        viewModel.setAuthenticationResult(BiometricPrompt.AuthenticationResult(null, 0))

        assertThat(recreatedResult).isInstanceOf(AuthenticationResult.Success::class.java)
    }

    private class ConfigChangeTestActivity : Activity() {
        var simulatingConfigChange: Boolean = false

        override fun isChangingConfigurations(): Boolean =
            simulatingConfigChange || super.isChangingConfigurations()
    }
}
