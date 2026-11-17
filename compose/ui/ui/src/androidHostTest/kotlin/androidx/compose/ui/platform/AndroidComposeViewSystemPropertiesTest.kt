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

package androidx.compose.ui.platform

import android.os.Build
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemProperties
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@LooperMode(LooperMode.Mode.PAUSED)
class AndroidComposeViewSystemPropertiesTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun layoutBoundsChangedFromBackgroundThread_updatesOnViewThread() {
        ShadowSystemProperties.override("debug.layout", "false")
        lateinit var view: AndroidComposeView
        rule.setContent { view = LocalView.current as AndroidComposeView }
        rule.waitForIdle()
        // The system properties callback is registered outside the composition frame.
        shadowOf(Looper.getMainLooper()).idle()
        val callback =
            requireNotNull(
                ReflectionHelpers.getStaticField<Runnable?>(
                    AndroidComposeView::class.java,
                    "systemPropertiesChangedRunnable",
                )
            )
        val executor = Executors.newSingleThreadExecutor()
        try {
            rule.runOnIdle {
                assertThat(view.showLayoutBounds).isFalse()
                ShadowSystemProperties.override("debug.layout", "true")
                // Keep the view thread busy until the background callback has completed.
                executor.submit(callback).get(5, TimeUnit.SECONDS)
                assertThat(view.showLayoutBounds).isFalse()
            }
            rule.runOnIdle { assertThat(view.showLayoutBounds).isTrue() }
        } finally {
            executor.shutdownNow()
            ShadowSystemProperties.override("debug.layout", "false")
        }
    }
}
