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

package androidx.camera.viewfinder.compose.internal

import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ViewfinderExternalSurfaceTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    @Config(sdk = [29, 30])
    fun surfaceDestroyed_recreatesSurfaceViewWithoutReinvokingOnInit_onApi29To30() =
        assertSurfaceDestroyedInMovableContent(expectRecreated = true)

    @Test
    @Config(sdk = [31, Config.TARGET_SDK])
    fun surfaceDestroyed_retainsSurfaceView_onApi31Plus() =
        assertSurfaceDestroyedInMovableContent(expectRecreated = false)

    @Test
    @Config(sdk = [30, Config.TARGET_SDK])
    fun reusableContentHost_reinvokesOnInitAfterReactivation() {
        var onInitCount = 0
        var active by mutableStateOf(true)

        composeTestRule.setContent {
            ReusableContentHost(active) {
                ViewfinderExternalSurface(modifier = Modifier.size(100.dp)) { onInitCount++ }
            }
        }

        composeTestRule.runOnIdle { assertThat(onInitCount).isEqualTo(1) }

        composeTestRule.runOnIdle { active = false }
        composeTestRule.runOnIdle { active = true }

        composeTestRule.runOnIdle { assertThat(onInitCount).isEqualTo(2) }
    }

    /**
     * Moves a [ViewfinderExternalSurface] within `movableContentOf` while its surface is destroyed,
     * as happens on API 29-30, and asserts that `onInit` isn't re-invoked and that the SurfaceView
     * is replaced by a fresh one with the state's callback if [expectRecreated], or kept otherwise.
     */
    private fun assertSurfaceDestroyedInMovableContent(expectRecreated: Boolean) {
        var onInitCount = 0
        var moveView by mutableStateOf(false)
        lateinit var rootView: View

        composeTestRule.setContent {
            rootView = LocalView.current
            val content = remember {
                movableContentOf {
                    ViewfinderExternalSurface(modifier = Modifier.size(100.dp)) { onInitCount++ }
                }
            }
            Column {
                if (moveView) {
                    content()
                } else {
                    content()
                }
            }
        }

        composeTestRule.runOnIdle { assertThat(onInitCount).isEqualTo(1) }
        val initialSurfaceView = checkNotNull(findFirstSurfaceView(rootView))
        assertThat(shadowOf(initialSurfaceView).fakeSurfaceHolder.callbacks).hasSize(1)

        composeTestRule.runOnUiThread {
            val callbacks = shadowOf(initialSurfaceView).fakeSurfaceHolder.callbacks.toList()
            moveView = true
            for (callback in callbacks) {
                callback.surfaceDestroyed(initialSurfaceView.holder)
            }
        }

        composeTestRule.runOnIdle {
            val currentSurfaceView = checkNotNull(findFirstSurfaceView(rootView))
            if (expectRecreated) {
                // The outgoing SurfaceView's callback is unbound immediately.
                assertThat(shadowOf(initialSurfaceView).fakeSurfaceHolder.callbacks).isEmpty()
                assertThat(currentSurfaceView).isNotSameInstanceAs(initialSurfaceView)
            } else {
                assertThat(currentSurfaceView).isSameInstanceAs(initialSurfaceView)
            }
            assertThat(shadowOf(currentSurfaceView).fakeSurfaceHolder.callbacks).hasSize(1)
            assertThat(onInitCount).isEqualTo(1)
        }
    }

    private fun findFirstSurfaceView(view: View): SurfaceView? {
        if (view is SurfaceView) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findFirstSurfaceView(view.getChildAt(i))?.let {
                    return it
                }
            }
        }
        return null
    }
}
