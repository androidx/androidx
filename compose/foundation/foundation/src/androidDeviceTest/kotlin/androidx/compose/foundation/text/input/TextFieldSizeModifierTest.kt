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

package androidx.compose.foundation.text.input

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TEST_FONT
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldSizeModifierTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    var flagValue = true

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun setup() {
        flagValue = ComposeFoundationFlags.isBasicTextFieldSizeOptimizationEnabled
        ComposeFoundationFlags.isBasicTextFieldSizeOptimizationEnabled = true
    }

    @OptIn(ExperimentalFoundationApi::class)
    @After
    fun cleanup() {
        ComposeFoundationFlags.isBasicTextFieldSizeOptimizationEnabled = flagValue
    }

    @Test
    fun singleLine_fixedHeight_ignoresTextContent() {
        var heightWithShortText = 0
        var heightWithLongText = 0

        rule.setContent {
            BasicTextField(
                rememberTextFieldState("text"),
                Modifier.onSizeChanged { heightWithShortText = it.height },
                lineLimits = SingleLine,
            )
            BasicTextField(
                rememberTextFieldState("text".repeat(100)),
                Modifier.onSizeChanged { heightWithLongText = it.height },
                lineLimits = SingleLine,
            )
        }

        rule.runOnIdle {
            assertThat(heightWithShortText).isGreaterThan(0)
            assertThat(heightWithLongText).isEqualTo(heightWithShortText)
        }
    }

    @Test
    fun multiLine_clampsHeight_betweenMinAndMax() {
        var heightTwoLines = 0
        var heightFiveLines = 0
        var heightTenLines = 0

        rule.setContent {
            BasicTextField(
                rememberTextFieldState("1\n2"),
                Modifier.onSizeChanged { heightTwoLines = it.height },
                lineLimits = MultiLine(2, 5),
            )
            BasicTextField(
                rememberTextFieldState("1\n".repeat(5)),
                Modifier.onSizeChanged { heightFiveLines = it.height },
                lineLimits = MultiLine(2, 5),
            )
            BasicTextField(
                rememberTextFieldState("1\n".repeat(10)),
                Modifier.onSizeChanged { heightTenLines = it.height },
                lineLimits = MultiLine(2, 5),
            )
        }

        rule.runOnIdle {
            // 10 lines of text should be clamped to 5 lines height
            assertThat(heightTenLines).isEqualTo(heightFiveLines)
            // 5 lines should be strictly taller than 2 lines
            assertThat(heightFiveLines).isGreaterThan(heightTwoLines)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun asyncFontLoad_updatesHeight() {
        val testDispatcher = UnconfinedTestDispatcher()
        val resolver = createFontFamilyResolver(context, testDispatcher)

        val typefaceDeferred = CompletableDeferred<Typeface>()
        val asyncLoader =
            object : AndroidFont.TypefaceLoader {
                override fun loadBlocking(context: Context, font: AndroidFont): Typeface =
                    TODO("Not yet implemented")

                override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface {
                    return typefaceDeferred.await()
                }
            }
        val fontFamily =
            FontFamily(
                object :
                    AndroidFont(FontLoadingStrategy.Async, asyncLoader, FontVariation.Settings()) {
                    override val weight: FontWeight = FontWeight.Bold
                    override val style: FontStyle = FontStyle.Italic
                },
                TEST_FONT,
            )

        val heights = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalFontFamilyResolver provides resolver,
                LocalDensity provides Density(1.0f, 1f),
            ) {
                BasicTextField(
                    state = rememberTextFieldState("1\n2\n3"),
                    lineLimits = MultiLine(minHeightInLines = 1),
                    textStyle =
                        TextStyle.Default.copy(
                            fontFamily = fontFamily,
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        ),
                    modifier = Modifier.onSizeChanged { heights.add(it.height) },
                )
            }
        }

        val before = heights.toList()
        // Complete the font load with a different font that likely has different metrics
        typefaceDeferred.complete(Typeface.create("cursive", Typeface.BOLD_ITALIC))

        rule.runOnIdle {
            // We expect at least one new measurement after font resolution
            assertThat(heights.size).isGreaterThan(before.size)
            // And the height should have changed
            assertThat(heights.distinct().size).isGreaterThan(before.distinct().size)
        }
    }

    @Test
    fun multiLine_parentForcesTightHeight_smallerThanMinLines() {
        var height = 0

        rule.setContent {
            Layout(
                content = {
                    BasicTextField(
                        state = remember { TextFieldState("1\n2\n3\n4\n5") },
                        lineLimits = MultiLine(minHeightInLines = 3),
                        modifier = Modifier.requiredWidth(200.dp),
                    )
                }
            ) { measurables, constraints ->
                val childConstraints =
                    androidx.compose.ui.unit.Constraints.fixed(constraints.maxWidth, 40)
                val placeable = measurables[0].measure(childConstraints)
                height = placeable.height
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            }
        }

        rule.runOnIdle { assertThat(height).isEqualTo(40) }
    }

    @Test
    fun multiLine_parentForcesMaxHeight_smallerThanMaxLines() {
        var height = 0

        rule.setContent {
            Layout(
                content = {
                    BasicTextField(
                        state = remember { TextFieldState("1\n2\n3\n4\n5") },
                        lineLimits = MultiLine(maxHeightInLines = 5),
                        modifier = Modifier.requiredWidth(200.dp),
                    )
                }
            ) { measurables, constraints ->
                val childConstraints = constraints.copy(maxHeight = 40)
                val placeable = measurables[0].measure(childConstraints)
                height = placeable.height
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            }
        }

        rule.runOnIdle { assertThat(height).isEqualTo(40) }
    }

    @Test
    fun emptyText_hasNonZeroMinSize() {
        var size = IntSize.Zero

        rule.setContent {
            BasicTextField(
                state = remember { TextFieldState("") },
                modifier = Modifier.onSizeChanged { size = it },
            )
        }

        rule.runOnIdle {
            assertThat(size.width).isGreaterThan(0)
            assertThat(size.height).isGreaterThan(0)
        }
    }
}
