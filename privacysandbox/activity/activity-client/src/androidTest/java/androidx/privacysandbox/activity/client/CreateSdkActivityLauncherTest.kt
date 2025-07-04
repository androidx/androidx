/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.activity.client

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.os.Binder
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class CreateSdkActivityLauncherTest(private val useManagedSdkActivityLauncher: Boolean) {
    // Lifecycle-aware activity is used in the tests as both types of LocalSdkActivityLauncher
    // (managed/unmanaged) can be created for it.
    @get:Rule
    var lifecycleAwareActivityScenarioRule =
        ActivityScenarioRule(TestLifecycleAwareActivity::class.java)

    @get:Rule var intentsRule = IntentsRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useManagedSdkActivityLauncher={0}")
        fun data(): Array<Any> = arrayOf(arrayOf(true), arrayOf(false))
    }

    private val sdkSandboxActivityMatcher =
        hasAction(`is`("android.app.sdksandbox.action.START_SANDBOXED_ACTIVITY"))

    @Before
    fun setUp() {
        // Intercepts intent to start sandboxed activity and immediately return a result.
        // This allows us to avoid loading and setting up an SDK just for checking if activities
        // are
        // launched.
        intending(sdkSandboxActivityMatcher)
            .respondWith(ActivityResult(Activity.RESULT_OK, Intent()))
    }

    @Test
    fun returnedLauncher_launchesActivitiesWhenAllowed() = runBlocking {
        val launcher =
            lifecycleAwareActivityScenarioRule.withActivity {
                this.createSdkActivityLauncher { true }
            }

        val result = launcher.launchSdkActivity(Binder())

        assertThat(result).isTrue()
        intended(sdkSandboxActivityMatcher, times(1))
    }

    @Test
    fun returnedLauncher_rejectsActivityLaunchesAccordingToPredicate() = runBlocking {
        val launcher =
            lifecycleAwareActivityScenarioRule.withActivity {
                this.createSdkActivityLauncher { false }
            }

        val result = launcher.launchSdkActivity(Binder())

        assertThat(result).isFalse()
        intended(sdkSandboxActivityMatcher, times(0))
    }

    @Test
    fun returnedLauncher_rejectsActivityLaunchesWhenDisposed() = runBlocking {
        val launcher =
            lifecycleAwareActivityScenarioRule.withActivity {
                this.createSdkActivityLauncher { true }
            }
        launcher.dispose()

        val result = launcher.launchSdkActivity(Binder())

        assertThat(result).isFalse()
        intended(sdkSandboxActivityMatcher, times(0))
    }

    @Test
    fun returnedManagedLauncher_disposeCanBeCalledMultipleTimes() = runBlocking {
        val launcher =
            lifecycleAwareActivityScenarioRule.withActivity {
                this.createSdkActivityLauncher { true }
            }
        launcher.dispose()

        val result = launcher.launchSdkActivity(Binder())
        launcher.dispose()
        launcher.dispose()

        assertThat(result).isFalse()
        intended(sdkSandboxActivityMatcher, times(0))
    }

    @Test
    fun returnedManagedLauncher_rejectsActivityLaunchesWhenHostActivityIsDestroyed() = runBlocking {
        assumeTrue(useManagedSdkActivityLauncher)

        val launcher =
            lifecycleAwareActivityScenarioRule.withActivity {
                this.createManagedSdkActivityLauncher { true }
            }
        lifecycleAwareActivityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)

        val result = launcher.launchSdkActivity(Binder())

        assertThat(result).isFalse()
        intended(sdkSandboxActivityMatcher, times(0))
    }

    @Test
    fun returnedManagedLauncher_rejectsActivityLaunchesWhenHostActivityWasAlreadyDestroyed() =
        runBlocking {
            assumeTrue(useManagedSdkActivityLauncher)

            val activity = lifecycleAwareActivityScenarioRule.withActivity { this }
            lifecycleAwareActivityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)
            val launcher = activity.createManagedSdkActivityLauncher { true }

            val result = launcher.launchSdkActivity(Binder())

            assertThat(result).isFalse()
            intended(sdkSandboxActivityMatcher, times(0))
        }

    private fun <T> T.createSdkActivityLauncher(
        allowLaunch: () -> Boolean
    ): LocalSdkActivityLauncher where T : Activity, T : LifecycleOwner =
        if (useManagedSdkActivityLauncher) this.createManagedSdkActivityLauncher(allowLaunch)
        else this.createUnmanagedSdkActivityLauncher(allowLaunch)
}
