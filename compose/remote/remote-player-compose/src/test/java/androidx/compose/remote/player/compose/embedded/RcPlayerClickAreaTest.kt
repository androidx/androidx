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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Document click areas (`ClickArea`) in the embedded player. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerClickAreaTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun clickAreaReportsItsIdAndMetadata() {
        val actions = mutableListOf<Pair<Int, String?>>()

        rule.setRemoteContent(onAction = { id, value -> actions += id to value }) {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                drawRect(
                    paint = RemotePaint { color = Color.Red.rc },
                    topLeft = RemoteOffset(10f.rf, 20f.rf),
                    size = RemoteSize(50f.rf, 30f.rf),
                )
                document.addClickArea(7, "Tap me", 10f, 20f, 60f, 50f, "metadata")
            }
        }
        rule.onNodeWithContentDescription("Tap me").performClick()

        assertThat(actions).containsExactly(7 to "metadata")
    }

    @Test
    fun clickAreaCoversItsBounds() {
        rule.setRemoteContent {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                document.addClickArea(1, "Area", 10f, 20f, 60f, 50f, null)
            }
        }

        // mdpi, so document pixels are dp.
        rule
            .onNodeWithContentDescription("Area")
            .assertLeftPositionInRootIsEqualTo(10.dp)
            .assertTopPositionInRootIsEqualTo(20.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(30.dp)
    }
}
