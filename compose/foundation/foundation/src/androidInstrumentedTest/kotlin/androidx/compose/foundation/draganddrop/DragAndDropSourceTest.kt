/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.draganddrop

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@MediumTest
class DragAndDropSourceTest {
    @get:Rule val rule = createComposeRule()

    /** Regression test for b/379682458 */
    @Test
    fun dragAndDropSource_doesNotPreventChildInvalidations() {
        var moveBox by mutableStateOf(false)
        val tag = "source"
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(200.toDp())
                        .testTag(tag)
                        .dragAndDropSource { _ -> null }
                        .clip(RectangleShape)
                        .layout { measurable, constraints ->
                            val placeable =
                                measurable.measure(
                                    constraints.copy(maxWidth = Constraints.Infinity)
                                )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(x = if (moveBox) -200 else 0, y = 0)
                            }
                        }
                        .width(400.toDp())
                        .drawBehind {
                            val halfWidth = size.width / 2f
                            // Fill the full height, and half the width, since this will
                            // be translated by the layout modifier above
                            val rectSize = Size(halfWidth, size.height)
                            drawRect(Color.Blue, size = rectSize)
                            drawRect(Color.Red, topLeft = Offset(halfWidth, 0f), size = rectSize)
                        }
                )
            }
        }

        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Blue }

        // Make the layout move the child so that the red box is now visible
        rule.runOnIdle { moveBox = true }

        rule.onNodeWithTag(tag).captureToImage().assertPixels { Color.Red }
    }
}
