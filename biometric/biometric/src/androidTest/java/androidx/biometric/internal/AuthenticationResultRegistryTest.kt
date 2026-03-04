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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
