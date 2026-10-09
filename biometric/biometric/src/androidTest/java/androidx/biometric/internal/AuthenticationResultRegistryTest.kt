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

import android.R
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultLauncher
import androidx.biometric.TestActivity
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.registerForAuthenticationResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationResultRegistryTest {
    @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)
    private var lastResult: AuthenticationResult? = null
    private lateinit var launcher: AuthenticationResultLauncher

    @Before
    fun setUp() {
        activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        activityRule.scenario.onActivity { activity ->
            val registry = AuthenticationResultRegistry()
            launcher =
                registry.register(
                    context = activity,
                    lifecycleOwner = activity,
                    viewModelStoreOwner = activity,
                    confirmCredentialActivityLauncher = {},
                    resultCallback = { result -> lastResult = result },
                )
        }
    }

    @Test
    fun launcherLaunch_showsPromptWithCorrectInfo() {
        activityRule.scenario.moveToState(Lifecycle.State.STARTED)

        activityRule.scenario.onActivity { activity ->
            val request =
                AuthenticationRequest.biometricRequest(title = "Test Title") {
                    setSubtitle("Test Subtitle")
                }
            launcher.launch(request)

            val viewModel = ViewModelProvider(activity)[AuthenticationViewModel::class.java]
            assertThat(viewModel.title).isEqualTo("Test Title")
            assertThat(viewModel.subtitle).isEqualTo("Test Subtitle")
        }
    }

    @Test
    fun launcherLaunch_beforeStarted_launchesWhenStarted() {
        activityRule.scenario.onActivity {
            val request =
                AuthenticationRequest.biometricRequest(title = "Queued Title") {
                    setSubtitle("Queued Subtitle")
                }
            launcher.launch(request)
        }

        activityRule.scenario.moveToState(Lifecycle.State.STARTED)

        activityRule.scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[AuthenticationViewModel::class.java]
            assertThat(viewModel.title).isEqualTo("Queued Title")
            assertThat(viewModel.subtitle).isEqualTo("Queued Subtitle")
        }
    }

    @Test
    fun launcherLaunch_thenCancelBeforeStarted_doesNotLaunch() {
        activityRule.scenario.onActivity {
            val request =
                AuthenticationRequest.biometricRequest(title = "Canceled Title") {
                    setSubtitle("Canceled Subtitle")
                }
            launcher.launch(request)
            launcher.cancel()
        }

        activityRule.scenario.moveToState(Lifecycle.State.STARTED)

        activityRule.scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[AuthenticationViewModel::class.java]
            assertThat(viewModel.title).isNull()
            assertThat(viewModel.isPromptShowing).isFalse()
        }
    }

    @Test
    fun launcherLaunch_afterStopped_launchesWhenRestarted() {
        activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        activityRule.scenario.onActivity {
            val request =
                AuthenticationRequest.biometricRequest(title = "Restarted Title") {
                    setSubtitle("Restarted Subtitle")
                }
            launcher.launch(request)
        }

        activityRule.scenario.moveToState(Lifecycle.State.STARTED)

        activityRule.scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[AuthenticationViewModel::class.java]
            assertThat(viewModel.title).isEqualTo("Restarted Title")
            assertThat(viewModel.subtitle).isEqualTo("Restarted Subtitle")
        }
    }

    @Test
    fun fragmentRegisterForAuthenticationResult_asFieldInitializer_doesNotThrowAndLaunches() {
        class FieldInitFragment : Fragment() {
            var fragmentResult: AuthenticationResult? = null
            val fragmentLauncher: AuthenticationResultLauncher = registerForAuthenticationResult {
                fragmentResult = it
            }
        }

        // Instantiate the Fragment before attaching it to an Activity (requireContext() would
        // throw here). Construction happens on the main thread, as it does in a real app, because
        // registerForAuthenticationResult registers a lifecycle observer.
        lateinit var fragment: FieldInitFragment
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            fragment = FieldInitFragment()
        }

        activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        activityRule.scenario.onActivity { activity ->
            activity.supportFragmentManager
                .beginTransaction()
                .add(fragment, "field_init")
                .commitNow()

            val request =
                AuthenticationRequest.biometricRequest(title = "Fragment Title") {
                    setSubtitle("Fragment Subtitle")
                }
            fragment.fragmentLauncher.launch(request)

            val fragmentViewModel = ViewModelProvider(fragment)[AuthenticationViewModel::class.java]
            val activityViewModel = ViewModelProvider(activity)[AuthenticationViewModel::class.java]
            assertThat(fragmentViewModel.title).isEqualTo("Fragment Title")
            assertThat(fragmentViewModel.subtitle).isEqualTo("Fragment Subtitle")
            // Verify ViewModel repositories are scoped per ViewModelStoreOwner, not global
            // singletons
            assertThat(activityViewModel.title).isNull()
        }
    }

    @Test
    fun launcherLaunch_withDefaultCancelButton() {
        activityRule.scenario.moveToState(Lifecycle.State.STARTED)

        activityRule.scenario.onActivity { activity ->
            val request = AuthenticationRequest.biometricRequest(title = "Test Title") {}
            launcher.launch(request)

            val defaultCancelButtonText = activity.getString(R.string.cancel)

            val viewModel = ViewModelProvider(activity)[AuthenticationViewModel::class.java]
            assertThat(viewModel.singleFallbackOptionText).isEqualTo(defaultCancelButtonText)
        }
    }

    @Test
    fun launcherLaunch_withMultipleFallbacks() {
        activityRule.scenario.moveToState(Lifecycle.State.STARTED)

        val fallback1Text = "Fallback 1"
        val fallback2Text = "Fallback 2"
        val fallback1 = AuthenticationRequest.Biometric.Fallback.CustomOption(fallback1Text)
        val fallback2 = AuthenticationRequest.Biometric.Fallback.CustomOption(fallback2Text)
        val fallbackList = arrayOf(fallback1, fallback2)

        activityRule.scenario.onActivity { activity ->
            val request =
                AuthenticationRequest.biometricRequest(
                    title = "Title",
                    authFallbacks = fallbackList,
                ) {}

            launcher.launch(request)

            val viewModel = ViewModelProvider(activity)[AuthenticationViewModel::class.java]

            if (fallbackList.toList().multipleFallbackOptionsValid()) {
                fallbackList.forEachIndexed { index, option ->
                    assertThat(viewModel.multipleFallbackOptionList?.get(index)).isEqualTo(option)
                }
            } else {
                assertThat(viewModel.singleFallbackOption).isEqualTo(fallback1)
            }
        }
    }
}
