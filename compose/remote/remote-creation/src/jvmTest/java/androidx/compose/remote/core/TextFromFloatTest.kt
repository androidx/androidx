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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.Color
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.creation.Rc
import org.junit.Test

class TextFromFloatTest : BaseLayoutTest() {

    @Test
    fun testTextFromFloatArguments() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.fillMaxSize().background(Color.YELLOW)) {
                        val value = 1234.5678f

                        // Default formatting
                        text(createTextFromFloat(value, 0, 2, Rc.TextFromFloat.OPTIONS_NONE))

                        // Padding after with zeros
                        text(createTextFromFloat(value, 0, 5, Rc.TextFromFloat.PAD_AFTER_ZERO))

                        // Padding before with zeros
                        text(createTextFromFloat(value, 6, 2, Rc.TextFromFloat.PAD_PRE_ZERO))

                        // Grouping by 3
                        text(createTextFromFloat(1234567.89f, 0, 2, Rc.TextFromFloat.GROUPING_BY3))

                        // Grouping by 3 with custom separator (Comma Period)
                        text(
                            createTextFromFloat(
                                1234567.89f,
                                0,
                                2,
                                Rc.TextFromFloat.GROUPING_BY3 or
                                    Rc.TextFromFloat.SEPARATOR_COMMA_PERIOD,
                            )
                        )

                        // Negative with parentheses
                        text(
                            createTextFromFloat(
                                -123.45f,
                                0,
                                2,
                                Rc.TextFromFloat.OPTIONS_NEGATIVE_PARENTHESES,
                            )
                        )

                        // Rounding
                        text(createTextFromFloat(123.456f, 0, 2, Rc.TextFromFloat.OPTIONS_ROUNDING))

                        // Full format (Float.toString)
                        text(createTextFromFloat(value, 0, 0, Rc.TextFromFloat.FULL_FORMAT))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(800, 800, 7, RcProfiles.PROFILE_ANDROIDX, "TextFromFloatArgs", ops)
    }
}
