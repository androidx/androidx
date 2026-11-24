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

@file:Suppress("DEPRECATION")

package androidx.xr.compose.subspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.session
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SpatialExternalSurfaceHemisphere] and [SpatialExternalSurfaceSphere]. */
@RunWith(AndroidJUnit4::class)
class SpatialExternalSurfaceTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialExternalSurfaceHemisphere_whenDisposed_removesPassthroughOpacityChangedListener() {
        composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        val fakeSceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().first()
        val initialListenerCount =
            fakeSceneRuntime.spatialEnvironment.passthroughOpacityChangedListenerMap.size
        var showSurface by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                if (showSurface) {
                    SpatialExternalSurfaceHemisphere(stereoMode = StereoMode.SideBySide) {}
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(fakeSceneRuntime.spatialEnvironment.passthroughOpacityChangedListenerMap)
            .hasSize(initialListenerCount + 1)

        showSurface = false
        composeTestRule.waitForIdle()

        assertThat(fakeSceneRuntime.spatialEnvironment.passthroughOpacityChangedListenerMap)
            .hasSize(initialListenerCount)
    }

    @Test
    fun spatialExternalSurfaceSphere_whenDisposed_removesPassthroughOpacityChangedListener() {
        composeTestRule.configureFakeSession()
        val session = assertNotNull(composeTestRule.session)
        val fakeSceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().first()
        val initialListenerCount =
            fakeSceneRuntime.spatialEnvironment.passthroughOpacityChangedListenerMap.size
        var showSurface by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                if (showSurface) {
                    SpatialExternalSurfaceSphere(stereoMode = StereoMode.Mono) {}
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(fakeSceneRuntime.spatialEnvironment.passthroughOpacityChangedListenerMap)
            .hasSize(initialListenerCount + 1)

        showSurface = false
        composeTestRule.waitForIdle()

        assertThat(fakeSceneRuntime.spatialEnvironment.passthroughOpacityChangedListenerMap)
            .hasSize(initialListenerCount)
    }
}
