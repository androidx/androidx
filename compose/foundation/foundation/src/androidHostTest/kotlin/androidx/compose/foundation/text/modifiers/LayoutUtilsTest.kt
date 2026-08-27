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

package androidx.compose.foundation.text.modifiers

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LayoutUtilsTest {

    @Test
    fun testCalculateAlignmentOffset_ltr() {
        val nodeWidth = 300
        val paragraphWidth = 100

        // Start / Left -> 0
        assertWithMessage("Start LTR")
            .that(
                calculateAlignmentOffset(
                    TextAlign.Start,
                    LayoutDirection.Ltr,
                    nodeWidth,
                    paragraphWidth,
                )
            )
            .isEqualTo(0f)
        assertWithMessage("Left LTR")
            .that(
                calculateAlignmentOffset(
                    TextAlign.Left,
                    LayoutDirection.Ltr,
                    nodeWidth,
                    paragraphWidth,
                )
            )
            .isEqualTo(0f)

        // Center -> (300 - 100) / 2 = 100
        assertWithMessage("Center LTR")
            .that(
                calculateAlignmentOffset(
                    TextAlign.Center,
                    LayoutDirection.Ltr,
                    nodeWidth,
                    paragraphWidth,
                )
            )
            .isEqualTo(100f)

        // End / Right -> 300 - 100 = 200
        assertWithMessage("End LTR")
            .that(
                calculateAlignmentOffset(
                    TextAlign.End,
                    LayoutDirection.Ltr,
                    nodeWidth,
                    paragraphWidth,
                )
            )
            .isEqualTo(200f)
        assertWithMessage("Right LTR")
            .that(
                calculateAlignmentOffset(
                    TextAlign.Right,
                    LayoutDirection.Ltr,
                    nodeWidth,
                    paragraphWidth,
                )
            )
            .isEqualTo(200f)
    }

    @Test
    fun testCalculateAlignmentOffset_rtl() {
        val nodeWidth = 300
        val paragraphWidth = 100

        // Start in RTL -> Right -> 200
        assertWithMessage("Start RTL")
            .that(
                calculateAlignmentOffset(
                    TextAlign.Start,
                    LayoutDirection.Rtl,
                    nodeWidth,
                    paragraphWidth,
                )
            )
            .isEqualTo(200f)

        // Center in RTL -> 100
        assertWithMessage("Center RTL")
            .that(
                calculateAlignmentOffset(
                    TextAlign.Center,
                    LayoutDirection.Rtl,
                    nodeWidth,
                    paragraphWidth,
                )
            )
            .isEqualTo(100f)

        // End in RTL -> Left -> 0
        assertWithMessage("End RTL")
            .that(
                calculateAlignmentOffset(
                    TextAlign.End,
                    LayoutDirection.Rtl,
                    nodeWidth,
                    paragraphWidth,
                )
            )
            .isEqualTo(0f)
    }
}
