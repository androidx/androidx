/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.compose.unaryPlus
import androidx.test.filters.MediumTest
import androidx.ui.graphics.Color
import androidx.ui.test.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class TextColorsTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun textColorForBackgroundUsesCorrectValues() {
        val colors = lightColorPalette(
            primary = Color(0),
            onPrimary = Color(1),
            secondary = Color(2),
            onSecondary = Color(3),
            background = Color(4),
            onBackground = Color(5),
            surface = Color(6),
            onSurface = Color(7),
            error = Color(8),
            onError = Color(9)
        )
        composeTestRule.setContent {
            MaterialTheme(colors = colors) {
                assertEquals(
                    +textColorForBackground((+MaterialTheme.colors()).primary),
                    (+MaterialTheme.colors()).onPrimary
                )
                assertEquals(
                    +textColorForBackground((+MaterialTheme.colors()).secondary),
                    (+MaterialTheme.colors()).onSecondary
                )
                assertEquals(
                    +textColorForBackground((+MaterialTheme.colors()).background),
                    (+MaterialTheme.colors()).onBackground
                )
                assertEquals(
                    +textColorForBackground((+MaterialTheme.colors()).surface),
                    (+MaterialTheme.colors()).onSurface
                )
                assertNull(+textColorForBackground(Color(100)))
            }
        }
    }
}
