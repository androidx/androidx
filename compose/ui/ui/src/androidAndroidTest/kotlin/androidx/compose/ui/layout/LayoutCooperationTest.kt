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

package androidx.compose.ui.layout

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LayoutCooperationTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun whenConstraintsChangeButSizeDoesNot() = with(rule.density) {
        val size = 48
        var initialOuterSize by mutableStateOf((size / 2).toDp())
        rule.setContent {
            Box(
                Modifier
                    .size(initialOuterSize)
                    .testTag("outer")) {
                Box(
                    Modifier
                        .requiredSize(size.toDp())
                        .background(Color.Yellow))
            }
        }

        rule.runOnIdle {
            initialOuterSize = size.toDp()
        }

        rule.onNodeWithTag("outer").captureToImage().assertPixels(IntSize(size, size)) {
            Color.Yellow
        }
    }

    @Test
    fun relayoutSkippingModifiersDoesntBreakCooperation() {
        with(rule.density) {
            val containerSize = 100
            val width = 50
            val widthDp = width.toDp()
            val height = 40
            val heightDp = height.toDp()
            var offset by mutableStateOf(0)
            rule.setContent {
                Layout(content = {
                    Box(Modifier.requiredSize(widthDp, heightDp)) {
                        Box(Modifier.testTag("child"))
                    }
                }) { measurables, _ ->
                    val placeable =
                        measurables.first().measure(Constraints.fixed(containerSize, containerSize))
                    layout(containerSize, containerSize) {
                        placeable.place(offset, offset)
                    }
                }
            }

            var expectedTop = ((containerSize - height) / 2).toDp()
            var expectedLeft = ((containerSize - width) / 2).toDp()
            rule.onNodeWithTag("child")
                .assertTopPositionInRootIsEqualTo(expectedTop)
                .assertLeftPositionInRootIsEqualTo(expectedLeft)

            rule.runOnIdle {
                offset = 10
            }

            expectedTop += offset.toDp()
            expectedLeft += offset.toDp()
            rule.onNodeWithTag("child")
                .assertTopPositionInRootIsEqualTo(expectedTop)
                .assertLeftPositionInRootIsEqualTo(expectedLeft)
        }
    }
}
