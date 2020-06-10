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
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.left
import androidx.ui.test.right
import androidx.ui.test.top
import androidx.ui.test.topCenter
import androidx.ui.test.topLeft
import androidx.ui.test.topRight
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.ClickableTestBox.defaultTag
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

        findByTag(defaultTag).doGesture {
            assertThat(left).isEqualTo(0f)
            assertThat(centerX).isEqualTo(1f)
            assertThat(right).isEqualTo(2f)

            assertThat(top).isEqualTo(0f)
            assertThat(centerY).isEqualTo(49.5f)
            assertThat(bottom).isEqualTo(99f)

            assertThat(topLeft).isEqualTo(Offset(0f, 0f))
            assertThat(topCenter).isEqualTo(Offset(1f, 0f))
            assertThat(topRight).isEqualTo(Offset(2f, 0f))
            assertThat(centerLeft).isEqualTo(Offset(0f, 49.5f))
            assertThat(center).isEqualTo(Offset(1f, 49.5f))
            assertThat(centerRight).isEqualTo(Offset(2f, 49.5f))
            assertThat(bottomLeft).isEqualTo(Offset(0f, 99f))
            assertThat(bottomCenter).isEqualTo(Offset(1f, 99f))
            assertThat(bottomRight).isEqualTo(Offset(2f, 99f))
        }
    }
}
