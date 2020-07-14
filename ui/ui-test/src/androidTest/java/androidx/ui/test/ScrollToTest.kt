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

package androidx.ui.test

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.preferredSize
import androidx.ui.semantics.scrollBy
import androidx.ui.unit.dp
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ScrollToTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun checkSemanticsAction_scrollTo_isCalled() {
        var wasScrollToCalled = false
        val tag = "myTag"

        composeTestRule.setContent {
            Box(Modifier.semantics {
                scrollBy(action = { _, _ ->
                    wasScrollToCalled = true
                    return@scrollBy true
                })
            }) {
                Box(Modifier.testTag(tag))
            }
        }

        runOnIdle {
            Assert.assertTrue(!wasScrollToCalled)
        }

        onNodeWithTag(tag)
            .performScrollTo()

        runOnIdle {
            Assert.assertTrue(wasScrollToCalled)
        }
    }

    @Test
    fun checkSemanticsAction_scrollTo_coordAreCorrect() {
        var currentScrollPositionY = 0.0f
        var currentScrollPositionX = 0.0f
        var elementHeight = 0.0f
        val tag = "myTag"

        val drawRect = @Composable { modifier: Modifier, color: Color ->
            Canvas(modifier.preferredSize(100.dp)) {
                drawRect(color)

                elementHeight = size.height
            }
        }

        composeTestRule.setContent {
            val red = Color(alpha = 0xFF, red = 0xFF, green = 0, blue = 0)
            val blue = Color(alpha = 0xFF, red = 0, green = 0, blue = 0xFF)
            val green = Color(alpha = 0xFF, red = 0, green = 0xFF, blue = 0)

            Box(Modifier.semantics {
                scrollBy(action = { x, y ->
                    currentScrollPositionY += y
                    currentScrollPositionX += x
                    return@scrollBy true
                })
            }) {
                Column {
                    drawRect(Modifier, red)
                    drawRect(Modifier, blue)
                    drawRect(Modifier.testTag(tag), green)
                }
            }
        }

        runOnIdle {
            Truth.assertThat(currentScrollPositionY).isEqualTo(0.0f)
            Truth.assertThat(currentScrollPositionX).isEqualTo(0.0f)
        }

        onNodeWithTag(tag)
            .performScrollTo() // scroll to third element

        runOnIdle {
            val expected = elementHeight * 2
            Truth.assertThat(currentScrollPositionY).isEqualTo(expected)
            Truth.assertThat(currentScrollPositionX).isEqualTo(0.0f)
        }
    }
}
