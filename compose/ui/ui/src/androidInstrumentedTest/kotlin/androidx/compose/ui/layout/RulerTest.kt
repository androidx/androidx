/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.activity.ComponentActivity
import androidx.collection.mutableFloatListOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// There is a bug on x86 L emulators where 35f == NaN is true
@MediumTest
@RunWith(AndroidJUnit4::class)
class RulerTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val verticalRuler = VerticalRuler()
    private val horizontalRuler = HorizontalRuler()

    @Test
    fun valueWithNoProvider() {
        val keylineValues = mutableFloatListOf()
        rule.setContent {
            Box(
                Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(p.width, p.height) { keylineValues += verticalRuler.current(Float.NaN) }
                }
            )
        }
        rule.waitForIdle()
        assertThat(keylineValues.size).isEqualTo(1)
        assertThat(keylineValues[0]).isNaN()
    }

    @Test
    fun valueWithNoProviderDefaultValue() {
        val keylineValues = mutableFloatListOf()
        rule.setContent {
            Box(
                Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(p.width, p.height) { keylineValues += verticalRuler.current(100f) }
                }
            )
        }
        rule.waitForIdle()
        assertThat(keylineValues.size).isEqualTo(1)
        assertThat(keylineValues[0]).isEqualTo(100f)
    }

    @Test
    fun valueWithinLayout() {
        val verticalKeylineValues = mutableFloatListOf()
        val horizontalKeylineValues = mutableFloatListOf()
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                verticalRuler.provides(35f)
                                horizontalRuler.provides(53f)
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                ) {
                    Box(
                        Modifier.offset(x = 25.toDp(), y = 50.toDp())
                            .requiredSize(50.toDp())
                            .background(Color.Blue)
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    horizontalKeylineValues += horizontalRuler.current(Float.NaN)
                                    verticalKeylineValues += verticalRuler.current(Float.NaN)
                                    p.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(1)
        assertThat(horizontalKeylineValues[0]).isEqualTo(3f)
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isEqualTo(10f)
    }

    @Test
    fun alignmentLinesTest() {
        val verticalKeylineValues = mutableFloatListOf()
        val horizontalKeylineValues = mutableFloatListOf()
        rule.setContent {
            with(LocalDensity.current) {
                Row(
                    Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                verticalRuler.provides(35f)
                                horizontalRuler.provides(53f)
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                ) {
                    Text(
                        "Hello",
                        Modifier.alignByBaseline()
                            .requiredSize(50.toDp())
                            .background(Color.Blue)
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    horizontalKeylineValues += horizontalRuler.current(Float.NaN)
                                    verticalKeylineValues += verticalRuler.current(Float.NaN)
                                    p.place(0, 0)
                                }
                            },
                    )
                    Text("World")
                }
            }
        }
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isGreaterThan(1)
        assertThat(horizontalKeylineValues[0]).isNaN()
        assertThat(horizontalKeylineValues.last()).isNotNaN()
        assertThat(verticalKeylineValues.size).isGreaterThan(1)
        assertThat(verticalKeylineValues[0]).isNaN()
        assertThat(verticalKeylineValues.last()).isNotNaN()
    }

    @Test
    fun valueNotWithinLayout() {
        val verticalKeylineValues = mutableFloatListOf()
        val horizontalKeylineValues = mutableFloatListOf()
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                verticalRuler.provides(5f)
                                horizontalRuler.provides(7f)
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                ) {
                    Box(
                        Modifier.offset(x = 25.toDp(), y = 50.toDp())
                            .requiredSize(50.toDp())
                            .background(Color.Blue)
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    horizontalKeylineValues += horizontalRuler.current(Float.NaN)
                                    verticalKeylineValues += verticalRuler.current(Float.NaN)
                                    p.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(1)
        assertThat(horizontalKeylineValues[0]).isLessThan(0f)
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isLessThan(0)
    }

    @Test
    fun valueRtl() {
        val verticalKeylineValues = mutableFloatListOf()
        rule.setContent {
            with(LocalDensity.current) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(
                                p.width,
                                p.height,
                                rulers = { verticalRuler.providesRelative(0f) },
                            ) {
                                p.place(0, 0)
                            }
                        }
                    ) {
                        // Make sure that the layout direction of the place where the ruler is
                        // defined is used, not the layout direction where it is consumed
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Ltr
                        ) {
                            Box(
                                Modifier.align(AbsoluteAlignment.TopLeft)
                                    .offset(x = 25.toDp(), y = 50.toDp())
                                    .requiredSize(50.toDp())
                                    .background(Color.Blue)
                                    .layout { measurable, constraints ->
                                        val p = measurable.measure(constraints)
                                        layout(p.width, p.height) {
                                            verticalKeylineValues += verticalRuler.current(0f)
                                            p.place(0, 0)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isEqualTo(75f)
    }

    @Test
    fun valueLtr() {
        val verticalKeylineValues = mutableFloatListOf()
        rule.setContent {
            with(LocalDensity.current) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(
                                p.width,
                                p.height,
                                rulers = { verticalRuler.providesRelative(0f) },
                            ) {
                                p.place(0, 0)
                            }
                        }
                    ) {
                        // Make sure that the layout direction of the place where the ruler is
                        // defined is used, not the layout direction where it is consumed
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Rtl
                        ) {
                            Box(
                                Modifier.align(AbsoluteAlignment.TopLeft)
                                    .absoluteOffset(x = 25.toDp(), y = 50.toDp())
                                    .requiredSize(50.toDp())
                                    .background(Color.Blue)
                                    .layout { measurable, constraints ->
                                        val p = measurable.measure(constraints)
                                        layout(p.width, p.height) {
                                            verticalKeylineValues += verticalRuler.current(0f)
                                            p.place(0, 0)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isEqualTo(-25f)
    }

    @Test
    fun valueLayoutDirectionChanges() {
        val verticalKeylineValues = mutableFloatListOf()
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            with(LocalDensity.current) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Box(
                        Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(
                                p.width,
                                p.height,
                                rulers = { verticalRuler.providesRelative(0f) },
                            ) {
                                p.place(0, 0)
                            }
                        }
                    ) {
                        // Make sure that the layout direction of the place where the ruler is
                        // defined is used, not the layout direction where it is consumed
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Rtl
                        ) {
                            Box(
                                Modifier.align(AbsoluteAlignment.TopLeft)
                                    .absoluteOffset(x = 25.toDp(), y = 50.toDp())
                                    .requiredSize(50.toDp())
                                    .background(Color.Blue)
                                    .layout { measurable, constraints ->
                                        val p = measurable.measure(constraints)
                                        layout(p.width, p.height) {
                                            verticalKeylineValues += verticalRuler.current(0f)
                                            p.place(0, 0)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isEqualTo(-25f)
        layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        assertThat(verticalKeylineValues.size).isEqualTo(2)
        assertThat(verticalKeylineValues[1]).isEqualTo(75f)
    }

    @Test
    fun updateStateValue() {
        val verticalKeylineValues = mutableFloatListOf()
        val horizontalKeylineValues = mutableFloatListOf()
        var vertValue by mutableFloatStateOf(35f)
        var horzValue by mutableFloatStateOf(53f)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                verticalRuler.provides(vertValue)
                                horizontalRuler.provides(horzValue)
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                ) {
                    Box(
                        Modifier.align(AbsoluteAlignment.TopLeft)
                            .offset(x = 25.toDp(), y = 50.toDp())
                            .requiredSize(50.toDp())
                            .background(Color.Blue)
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    horizontalKeylineValues += horizontalRuler.current(0f)
                                    verticalKeylineValues += verticalRuler.current(0f)
                                    p.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(1)
        assertThat(horizontalKeylineValues[0]).isEqualTo(3f)
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isEqualTo(10f)
        vertValue = 34f
        horzValue = 52f
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(2)
        assertThat(horizontalKeylineValues[1]).isEqualTo(2f)
        assertThat(verticalKeylineValues.size).isEqualTo(2)
        assertThat(verticalKeylineValues[1]).isEqualTo(9f)
    }

    @Test
    fun rulerValueRemoved() {
        val verticalKeylineValues = mutableFloatListOf()
        val horizontalKeylineValues = mutableFloatListOf()
        var setRulers by mutableStateOf(true)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                if (setRulers) {
                                    verticalRuler.provides(35f)
                                    horizontalRuler.provides(53f)
                                }
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                ) {
                    Box(
                        Modifier.align(AbsoluteAlignment.TopLeft)
                            .offset(x = 25.toDp(), y = 50.toDp())
                            .requiredSize(50.toDp())
                            .background(Color.Blue)
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    horizontalKeylineValues += horizontalRuler.current(Float.NaN)
                                    verticalKeylineValues += verticalRuler.current(Float.NaN)
                                    p.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(1)
        assertThat(horizontalKeylineValues[0]).isEqualTo(3f)
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isEqualTo(10f)
        setRulers = false
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(2)
        assertThat(horizontalKeylineValues[1]).isNaN()
        assertThat(verticalKeylineValues.size).isEqualTo(2)
        assertThat(verticalKeylineValues[1]).isNaN()
    }

    @Test
    fun rulerValueAdded() {
        val verticalKeylineValues = mutableFloatListOf()
        val horizontalKeylineValues = mutableFloatListOf()
        var setRulers by mutableStateOf(false)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                if (setRulers) {
                                    verticalRuler.provides(35f)
                                    horizontalRuler.provides(53f)
                                }
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                ) {
                    Box(
                        Modifier.align(AbsoluteAlignment.TopLeft)
                            .offset(x = 25.toDp(), y = 50.toDp())
                            .requiredSize(50.toDp())
                            .background(Color.Blue)
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    horizontalKeylineValues += horizontalRuler.current(Float.NaN)
                                    verticalKeylineValues += verticalRuler.current(Float.NaN)
                                    p.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(1)
        assertThat(horizontalKeylineValues[0]).isNaN()
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isNaN()
        setRulers = true
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(2)
        assertThat(horizontalKeylineValues[1]).isEqualTo(3f)
        assertThat(verticalKeylineValues.size).isEqualTo(2)
        assertThat(verticalKeylineValues[1]).isEqualTo(10f)
    }

    @Test
    fun rulerChildMoved() {
        val verticalKeylineValues = mutableFloatListOf()
        val horizontalKeylineValues = mutableFloatListOf()
        var offset by mutableStateOf(IntOffset.Zero)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp(), 150.toDp()).layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                verticalRuler.provides(35f)
                                horizontalRuler.provides(53f)
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                ) {
                    Box(
                        Modifier.align(AbsoluteAlignment.TopLeft)
                            .offset { offset }
                            .offset(x = 25.toDp(), y = 50.toDp())
                            .requiredSize(50.toDp())
                            .background(Color.Blue)
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    horizontalKeylineValues += horizontalRuler.current(0f)
                                    verticalKeylineValues += verticalRuler.current(0f)
                                    p.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(1)
        assertThat(horizontalKeylineValues[0]).isEqualTo(3f)
        assertThat(verticalKeylineValues.size).isEqualTo(1)
        assertThat(verticalKeylineValues[0]).isEqualTo(10f)
        offset = IntOffset(1, 2)
        rule.waitForIdle()
        assertThat(horizontalKeylineValues.size).isEqualTo(2)
        assertThat(horizontalKeylineValues[1]).isEqualTo(1f)
        assertThat(verticalKeylineValues.size).isEqualTo(2)
        assertThat(verticalKeylineValues[1]).isEqualTo(9f)
    }

    @Test
    fun rulerMovesWithLayout() {
        var offset by mutableIntStateOf(0)
        var rulerValue = 0f
        rule.setContent {
            Box(Modifier.offset { IntOffset(offset, 0) }) {
                Box(
                    Modifier.layout { m, constraints ->
                        val p = m.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            rulers = {
                                val position = coordinates.positionInRoot().x
                                verticalRuler.provides(-position)
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                ) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                rulerValue = verticalRuler.current(Float.NaN)
                            }
                        }
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(rulerValue).isWithin(0.01f).of(0f)
        offset = 100
        rule.waitForIdle()
        assertThat(rulerValue).isWithin(0.01f).of(-100f)
    }

    @Test
    fun verticalDerivedRuler() {
        var rulerValue = 0f

        val myRuler = VerticalRuler.derived { 10f }
        rule.setContent {
            Box(
                Modifier.fillMaxSize().layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(p.width, p.height) { rulerValue = myRuler.current(Float.NaN) }
                }
            )
        }
        rule.runOnIdle { assertThat(rulerValue).isWithin(0.01f).of(10f) }
    }

    @Test
    fun horizontalDerivedRuler() {
        var rulerValue = 0f

        val myRuler = HorizontalRuler.derived { 10f }
        rule.setContent {
            Box(
                Modifier.fillMaxSize().layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(p.width, p.height) { rulerValue = myRuler.current(Float.NaN) }
                }
            )
        }
        rule.runOnIdle { assertThat(rulerValue).isWithin(0.01f).of(10f) }
    }
}
