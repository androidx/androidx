/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.activity.compose

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ActivityResultRegistryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    var launchCount = 0
    val registryOwner = ActivityResultRegistryOwner {
        object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                launchCount++
            }
        }
    }

    @Test
    fun testRegisterForActivityResult() {
        var launcher: ActivityResultLauncher<Intent>? by mutableStateOf(null)
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner
            ) {
                launcher = registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {}
            }
        }
        composeTestRule.runOnIdle {
            launcher?.launch(Intent()) ?: fail("launcher was not composed")
            assertWithMessage("the registry was not invoked")
                .that(launchCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun testRegisterForActivityResultAfterRestoration() {
        val activityScenario: ActivityScenario<ComponentActivity> =
            ActivityScenario.launch(ComponentActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        var launcher: ActivityResultLauncher<Intent>? by mutableStateOf(null)

        activityScenario.onActivity { activity ->
            (activity as ComponentActivity).setContent {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides registryOwner
                ) {
                    launcher = registerForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) {}
                }
            }
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            launcher?.launch(Intent()) ?: fail("launcher was not composed")
            assertWithMessage("the registry was not invoked")
                .that(launchCount)
                .isEqualTo(1)
        }
    }
}
