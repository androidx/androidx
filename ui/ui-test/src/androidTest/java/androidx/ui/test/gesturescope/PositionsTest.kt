/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test.gesturescope

import androidx.test.filters.MediumTest
import androidx.ui.geometry.Offset
import androidx.ui.test.percentOffset
import androidx.ui.test.bottom
import androidx.ui.test.bottomCenter
import androidx.ui.test.bottomLeft
import androidx.ui.test.bottomRight
import androidx.ui.test.center
import androidx.ui.test.centerLeft
import androidx.ui.test.centerRight
import androidx.ui.test.centerX
import androidx.ui.test.centerY
import androidx.ui.test.createComposeRule
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.height
import androidx.ui.test.left
import androidx.ui.test.right
import androidx.ui.test.top
import androidx.ui.test.topCenter
import androidx.ui.test.topLeft
import androidx.ui.test.topRight
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.ClickableTestBox.defaultTag
import androidx.ui.test.width
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@MediumTest
class PositionsTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun testCornersEdgesAndCenter() {
        composeTestRule.setContent { ClickableTestBox(width = 3f, height = 100f) }

        onNodeWithTag(defaultTag).performGesture {
            assertThat(width).isEqualTo(3)
            assertThat(height).isEqualTo(100)

            assertThat(left).isEqualTo(0f)
            assertThat(centerX).isEqualTo(1.5f)
            assertThat(right).isEqualTo(2f)

            assertThat(top).isEqualTo(0f)
            assertThat(centerY).isEqualTo(50f)
            assertThat(bottom).isEqualTo(99f)

            assertThat(topLeft).isEqualTo(Offset(0f, 0f))
            assertThat(topCenter).isEqualTo(Offset(1.5f, 0f))
            assertThat(topRight).isEqualTo(Offset(2f, 0f))
            assertThat(centerLeft).isEqualTo(Offset(0f, 50f))
            assertThat(center).isEqualTo(Offset(1.5f, 50f))
            assertThat(centerRight).isEqualTo(Offset(2f, 50f))
            assertThat(bottomLeft).isEqualTo(Offset(0f, 99f))
            assertThat(bottomCenter).isEqualTo(Offset(1.5f, 99f))
            assertThat(bottomRight).isEqualTo(Offset(2f, 99f))
        }
    }

    @Test
    fun testRelativeOffset() {
        composeTestRule.setContent { ClickableTestBox() }

        onNodeWithTag(defaultTag).performGesture {
            assertThat(percentOffset(.1f, .1f)).isEqualTo(Offset(10f, 10f))
            assertThat(percentOffset(-.2f, 0f)).isEqualTo(Offset(-20f, 0f))
            assertThat(percentOffset(.25f, -.5f)).isEqualTo(Offset(25f, -50f))
            assertThat(percentOffset(0f, .5f)).isEqualTo(Offset(0f, 50f))
            assertThat(percentOffset(2f, -2f)).isEqualTo(Offset(200f, -200f))
        }
    }
}
