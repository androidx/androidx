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
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateRulers
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// There is a bug on x86 L emulators where 35f == NaN is true
@MediumTest
@RunWith(Parameterized::class)
class RulerTest(val useIndividualRulers: Boolean) {
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
                Box(Modifier.size(100.toDp(), 150.toDp()).provideRulers()) {
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
                Row(Modifier.size(100.toDp(), 150.toDp()).provideRulers()) {
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
                Box(Modifier.size(100.toDp(), 150.toDp()).provideRulers(7f, 5f)) {
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
                    Box(Modifier.size(100.toDp(), 150.toDp()).provideRelativeRuler()) {
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
                    Box(Modifier.size(100.toDp(), 150.toDp()).provideRelativeRuler()) {
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
                    Box(Modifier.size(100.toDp(), 150.toDp()).provideRelativeRuler()) {
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
                    Modifier.size(100.toDp(), 150.toDp())
                        .provideRulers(horz = { horzValue }, vert = { vertValue })
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
                    Modifier.size(100.toDp(), 150.toDp())
                        .provideRulers(
                            horz = { if (setRulers) 53f else null },
                            vert = { if (setRulers) 35f else null },
                        )
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
                    Modifier.size(100.toDp(), 150.toDp())
                        .provideRulers(
                            horz = { if (setRulers) 53f else null },
                            vert = { if (setRulers) 35f else null },
                        )
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
                Box(Modifier.size(100.toDp(), 150.toDp()).provideRulers()) {
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
                    Modifier.provideRulers(
                        horz = { null },
                        vert = { -coordinates.positionInRoot().x },
                    )
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

    @Test
    fun readOnlyImportantRulers() {
        var horzRead = false
        var vertRead = false
        rule.setContent {
            Box(
                Modifier.fillMaxSize()
                    .provideRulers(
                        horz = {
                            horzRead = true
                            35f
                        },
                        vert = {
                            vertRead = true
                            53f
                        },
                    )
            ) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            val x = horizontalRuler.current(Float.NaN)
                            if (x.isNaN()) {
                                p.place(0, 0)
                            } else {
                                p.place(x.roundToInt(), 0)
                            }
                        }
                    }
                )
            }
        }
        rule.waitForIdle()
        assertThat(horzRead).isTrue()
        assertThat(vertRead).isEqualTo(!useIndividualRulers)
    }

    @Test
    fun repeatedReadsWithoutValueChange_stillInvalidateOnChange() {
        val verticalKeylineValues = mutableFloatListOf()
        var vertValue by mutableFloatStateOf(35f)
        var replaceTrigger by mutableIntStateOf(0)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp(), 150.toDp())
                        .provideRulers(horz = { 53f }, vert = { vertValue })
                ) {
                    Box(
                        Modifier.align(AbsoluteAlignment.TopLeft)
                            .offset(x = 25.toDp(), y = 50.toDp())
                            .requiredSize(50.toDp())
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    replaceTrigger // re-place without changing the ruler value
                                    verticalKeylineValues += verticalRuler.current(0f)
                                    p.place(0, 0)
                                }
                            }
                    )
                }
            }
        }
        rule.waitForIdle()
        repeat(20) {
            replaceTrigger++
            rule.waitForIdle()
        }
        assertThat(verticalKeylineValues.size).isEqualTo(21)
        assertThat(verticalKeylineValues.last()).isEqualTo(10f)

        // A single value change re-places the reader exactly once with the new value.
        vertValue = 34f
        rule.waitForIdle()
        assertThat(verticalKeylineValues.size).isEqualTo(22)
        assertThat(verticalKeylineValues.last()).isEqualTo(9f)

        // The reader is re-registered and observes subsequent changes.
        vertValue = 33f
        rule.waitForIdle()
        assertThat(verticalKeylineValues.size).isEqualTo(23)
        assertThat(verticalKeylineValues.last()).isEqualTo(8f)
    }

    @Test
    fun removedReader_notReplacedOnValueChange() {
        var readerPlacements = 0
        var vertValue by mutableFloatStateOf(35f)
        var showReader by mutableStateOf(true)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp(), 150.toDp())
                        .provideRulers(horz = { 53f }, vert = { vertValue })
                ) {
                    if (showReader) {
                        Box(
                            Modifier.requiredSize(50.toDp()).layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    readerPlacements++
                                    verticalRuler.current(0f)
                                    p.place(0, 0)
                                }
                            }
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        assertThat(readerPlacements).isEqualTo(1)
        showReader = false
        rule.waitForIdle()
        vertValue = 34f
        rule.waitForIdle()
        assertThat(readerPlacements).isEqualTo(1)
    }

    fun Modifier.provideRulers(horz: Float = 53f, vert: Float = 35f) =
        if (useIndividualRulers) {
            layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(
                    p.width,
                    p.height,
                    isRulerProvided = { it == verticalRuler || it == horizontalRuler },
                    rulerProvider = { ruler ->
                        if (ruler == horizontalRuler) {
                            ruler.provides(horz)
                        } else if (ruler == verticalRuler) {
                            ruler.provides(vert)
                        }
                    },
                ) {
                    p.place(0, 0)
                }
            }
        } else {
            layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(
                    p.width,
                    p.height,
                    rulers = {
                        verticalRuler.provides(vert)
                        horizontalRuler.provides(horz)
                    },
                ) {
                    p.place(0, 0)
                }
            }
        }

    fun Modifier.provideRulers(horz: RulerScope.() -> Float?, vert: RulerScope.() -> Float?) =
        if (useIndividualRulers) {
            layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(
                    p.width,
                    p.height,
                    isRulerProvided = { it == verticalRuler || it == horizontalRuler },
                    rulerProvider = { ruler ->
                        if (ruler == horizontalRuler) {
                            val value = horz()
                            if (value != null) {
                                ruler.provides(value)
                            }
                        } else if (ruler == verticalRuler) {
                            val value = vert()
                            if (value != null) {
                                ruler.provides(value)
                            }
                        }
                    },
                ) {
                    p.place(0, 0)
                }
            }
        } else {
            layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(
                    p.width,
                    p.height,
                    rulers = {
                        val h = horz()
                        if (h != null) {
                            horizontalRuler.provides(h)
                        }
                        val v = vert()
                        if (v != null) {
                            verticalRuler.provides(v)
                        }
                    },
                ) {
                    p.place(0, 0)
                }
            }
        }

    fun Modifier.provideRelativeRuler(vert: Float = 0f) =
        if (useIndividualRulers) {
            layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(
                    p.width,
                    p.height,
                    isRulerProvided = { it == verticalRuler },
                    rulerProvider = { ruler ->
                        if (ruler == verticalRuler) {
                            verticalRuler.providesRelative(vert)
                        }
                    },
                ) {
                    p.place(0, 0)
                }
            }
        } else {
            layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, p.height, rulers = { verticalRuler.providesRelative(vert) }) {
                    p.place(0, 0)
                }
            }
        }

    @Test
    fun sideEffectRulersInvalidate() {
        if (!useIndividualRulers) return

        var stateValue by mutableFloatStateOf(10f)
        var readValue1 = 0f
        var readValue2 = 0f

        val ruler1 = verticalRuler
        val ruler2 = horizontalRuler

        rule.setContent {
            Box(
                Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(
                        p.width,
                        p.height,
                        isRulerProvided = { it == ruler1 || it == ruler2 },
                        rulerProvider = { ruler ->
                            ruler1.provides(stateValue)
                            ruler2.provides(stateValue)
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
                            readValue1 = ruler1.current(0f)
                            p.place(0, 0)
                        }
                    }
                )
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            readValue2 = ruler2.current(0f)
                            p.place(0, 0)
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(readValue1).isEqualTo(10f)
        assertThat(readValue2).isEqualTo(10f)

        stateValue = 20f
        rule.waitForIdle()

        assertThat(readValue1).isEqualTo(20f)
        assertThat(readValue2).isEqualTo(20f)
    }

    @Test
    fun sideEffectRulersInvalidate_coordinateChange() {
        if (!useIndividualRulers) return

        var offset by mutableStateOf(0)
        var readValue1 = 0f
        var readValue2 = 0f

        val ruler1 = verticalRuler
        val ruler2 = horizontalRuler

        rule.setContent {
            Box(
                Modifier.offset { IntOffset(offset, 0) }
                    .layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            isRulerProvided = { it == ruler1 || it == ruler2 },
                            rulerProvider = { ruler ->
                                ruler1.provides(coordinates.positionInParent().x)
                                ruler2.provides(coordinates.positionInParent().x)
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
                            readValue1 = ruler1.current(0f)
                            p.place(0, 0)
                        }
                    }
                )
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            readValue2 = ruler2.current(0f)
                            p.place(0, 0)
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(readValue1).isEqualTo(0f)
        assertThat(readValue2).isEqualTo(0f)

        offset = 10
        rule.waitForIdle()

        assertThat(readValue1).isEqualTo(10f)
        assertThat(readValue2).isEqualTo(10f)
    }

    @Test
    fun nestedSideEffectRulersInvalidate() {
        if (!useIndividualRulers) return

        var stateValue by mutableFloatStateOf(10f)
        var stateValue2 by mutableFloatStateOf(10f)
        var readA by mutableStateOf(false)
        var readB by mutableStateOf(false)
        var readC by mutableStateOf(false)
        var readValueA = 0f
        var readValueB = 0f
        var readValueC = 0f

        val rulerA = verticalRuler
        val rulerB = horizontalRuler
        val rulerC = VerticalRuler()

        rule.setContent {
            Box(
                Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(
                        p.width,
                        p.height,
                        isRulerProvided = { it == rulerA || it == rulerB || it == rulerC },
                        rulerProvider = { ruler ->
                            if (ruler == rulerA) {
                                rulerA.provides(stateValue)
                                rulerB.provides(stateValue)
                            } else if (ruler == rulerB || ruler == rulerC) {
                                rulerB.provides(stateValue2)
                                rulerC.provides(stateValue2)
                            }
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
                            if (readA) {
                                readValueA = rulerA.current(0f)
                            }
                            p.place(0, 0)
                        }
                    }
                )
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            if (readB) {
                                readValueB = rulerB.current(0f)
                            }
                            p.place(0, 0)
                        }
                    }
                )
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            if (readC) {
                                readValueC = rulerC.current(0f)
                            }
                            p.place(0, 0)
                        }
                    }
                )
            }
        }

        // 1. Read A. (Provides A=10, B=10)
        readA = true
        rule.waitForIdle()
        assertThat(readValueA).isEqualTo(10f)

        // 2. Stop reading A, invalidate A (stateValue = 20)
        readA = false
        stateValue = 20f
        rule.waitForIdle()

        // 3. Read B. B is not in values, runs B's provider (provides B=10, C=10)
        readB = true
        rule.waitForIdle()
        assertThat(readValueB).isEqualTo(10f)

        // 4. Stop reading B, read C. C is in values (10)
        readB = false
        readC = true
        rule.waitForIdle()
        assertThat(readValueC).isEqualTo(10f)

        // 5. Read A again. A's provider runs (provides A=20, B=20)
        readA = true
        rule.waitForIdle()
        assertThat(readValueA).isEqualTo(20f)

        // 6. Invalidate A again (stateValue = 30) -> should invalidate A, B, C
        stateValue = 30f
        // 7. Change B's provider state (stateValue2 = 40)
        stateValue2 = 40f
        rule.waitForIdle()

        // 8. Verify C is updated to 40 (meaning it was invalidated and re-evaluated)
        assertThat(readValueC).isEqualTo(40f)
    }

    @Test
    fun invalidateRulersRecalculatesNonStateValue() {
        val node = NonStateRulerNode(verticalRuler, useIndividualRulers)
        node.rulerValue = 10f
        val readValues = mutableFloatListOf()
        rule.setContent {
            Box(Modifier.then(NonStateRulerElement(node))) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            readValues += verticalRuler.current(Float.NaN)
                            p.place(0, 0)
                        }
                    }
                )
            }
        }
        rule.waitForIdle()
        assertThat(readValues.size).isEqualTo(1)
        assertThat(readValues[0]).isEqualTo(10f)

        // Changing a non-state value is not observed, so nothing is relaid out.
        rule.runOnIdle { node.rulerValue = 20f }
        rule.waitForIdle()
        assertThat(readValues.size).isEqualTo(1)

        rule.runOnIdle { node.invalidateRulers() }
        rule.waitForIdle()
        assertThat(readValues.size).isEqualTo(2)
        assertThat(readValues[1]).isEqualTo(20f)
    }

    @Test
    fun invalidateRulersWhenDetachedDoesNotCrash() {
        val node = NonStateRulerNode(verticalRuler, useIndividualRulers)
        node.invalidateRulers()
    }

    @Test
    fun invalidateRulersUnchangedValueDoesNotRelayoutReaders() {
        // Provided rulers are lazily calculated, so readers are always relaid out.
        if (useIndividualRulers) return

        val node = NonStateRulerNode(verticalRuler, useIndividualRulers)
        node.rulerValue = 10f
        val readValues = mutableFloatListOf()
        rule.setContent {
            Box(Modifier.then(NonStateRulerElement(node))) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            readValues += verticalRuler.current(Float.NaN)
                            p.place(0, 0)
                        }
                    }
                )
            }
        }
        rule.waitForIdle()
        assertThat(readValues.size).isEqualTo(1)

        rule.runOnIdle { node.invalidateRulers() }
        rule.waitForIdle()
        assertThat(readValues.size).isEqualTo(1)
    }

    @Test
    fun invalidateRulersInLookaheadScope() {
        val node = NonStateRulerNode(verticalRuler, useIndividualRulers)
        node.rulerValue = 10f
        val lookaheadValues = mutableFloatListOf()
        val readValues = mutableFloatListOf()
        rule.setContent {
            LookaheadScope {
                Box(Modifier.then(NonStateRulerElement(node))) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            val lookingAhead = isLookingAhead
                            layout(p.width, p.height) {
                                val value = verticalRuler.current(Float.NaN)
                                if (lookingAhead) {
                                    lookaheadValues += value
                                } else {
                                    readValues += value
                                }
                                p.place(0, 0)
                            }
                        }
                    )
                }
            }
        }
        rule.waitForIdle()
        assertThat(lookaheadValues.size).isAtLeast(1)
        assertThat(lookaheadValues.last()).isEqualTo(10f)
        assertThat(readValues.size).isAtLeast(1)
        assertThat(readValues.last()).isEqualTo(10f)

        rule.runOnIdle { node.rulerValue = 20f }
        rule.waitForIdle()
        assertThat(lookaheadValues.last()).isEqualTo(10f)
        assertThat(readValues.last()).isEqualTo(10f)

        rule.runOnIdle { node.invalidateRulers() }
        rule.waitForIdle()
        assertThat(lookaheadValues.last()).isEqualTo(20f)
        assertThat(readValues.last()).isEqualTo(20f)
    }

    /**
     * Checks that when an inner ruler provider stops providing a value, readers underneath fall
     * back to the outer provider's value and switch back when the inner provider resumes.
     */
    @Test
    fun nestedProviders_innerOverridesOuterAndFallsBackWhenRemoved() {
        var provideInner by mutableStateOf(true)
        var innerRead = Float.NaN
        var outerRead = Float.NaN

        rule.setContent {
            Box(Modifier.provideRulers(vert = 10f)) {
                Box(
                    Modifier.provideRulers(
                        horz = { null },
                        vert = { if (provideInner) 50f else null },
                    )
                ) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                innerRead = verticalRuler.current(Float.NaN)
                                p.place(0, 0)
                            }
                        }
                    )
                }
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            outerRead = verticalRuler.current(Float.NaN)
                            p.place(0, 0)
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(innerRead).isEqualTo(50f)
        assertThat(outerRead).isEqualTo(10f)

        provideInner = false
        rule.waitForIdle()
        assertThat(innerRead).isEqualTo(10f)
        assertThat(outerRead).isEqualTo(10f)

        provideInner = true
        rule.waitForIdle()
        assertThat(innerRead).isEqualTo(50f)
        assertThat(outerRead).isEqualTo(10f)
    }

    /**
     * Checks that detaching an inner ruler-providing modifier notifies child readers to fall back
     * to the outer provider, and re-attaching notifies them again.
     */
    @Test
    fun nestedProviders_innerModifierRemoved_notifiesReaders() {
        var hasInnerModifier by mutableStateOf(true)
        var innerRead = Float.NaN

        rule.setContent {
            Box(Modifier.provideRulers(vert = 10f)) {
                Box(if (hasInnerModifier) Modifier.provideRulers(vert = 50f) else Modifier) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                innerRead = verticalRuler.current(Float.NaN)
                                p.place(0, 0)
                            }
                        }
                    )
                }
            }
        }

        rule.waitForIdle()
        assertThat(innerRead).isEqualTo(50f)

        hasInnerModifier = false
        rule.waitForIdle()
        assertThat(innerRead).isEqualTo(10f)

        hasInnerModifier = true
        rule.waitForIdle()
        assertThat(innerRead).isEqualTo(50f)
    }

    /**
     * Checks that a reader that originally fell back to the root gets notified when a provider
     * under the root later starts providing that ruler.
     */
    @Test
    fun nestedProviders_addedUnderRoot_notifiesReader() {
        var provide by mutableStateOf(false)
        var readValue = Float.NaN

        rule.setContent {
            Box(Modifier.provideRulers(horz = { null }, vert = { if (provide) 42f else null })) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            readValue = verticalRuler.current(-1f)
                            p.place(0, 0)
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(readValue).isEqualTo(-1f)

        provide = true
        rule.waitForIdle()
        assertThat(readValue).isEqualTo(42f)
    }

    /**
     * Checks that rulers on a single `LayoutNode` flow inward to later modifiers, the node's
     * `MeasurePolicy`, and children, never outward to earlier modifiers.
     */
    @Test
    fun sameLayoutNode_modifierOrderAndMeasurePolicy() {
        var outerModifierRead = Float.NaN
        var innerModifierRead = Float.NaN
        var measurePolicyRead = Float.NaN
        var childVertRead = Float.NaN
        var childHorzRead = Float.NaN

        rule.setContent {
            Layout(
                modifier =
                    Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                outerModifierRead = verticalRuler.current(Float.NaN)
                                p.place(0, 0)
                            }
                        }
                        .provideRulers(horz = 5f, vert = 10f)
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                innerModifierRead = verticalRuler.current(Float.NaN)
                                p.place(0, 0)
                            }
                        },
                content = {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                childVertRead = verticalRuler.current(Float.NaN)
                                childHorzRead = horizontalRuler.current(Float.NaN)
                                p.place(0, 0)
                            }
                        }
                    )
                },
            ) { measurables, constraints ->
                val p = measurables.first().measure(constraints)
                if (useIndividualRulers) {
                    layout(
                        p.width,
                        p.height,
                        isRulerProvided = { it == horizontalRuler },
                        rulerProvider = { ruler ->
                            if (ruler == horizontalRuler) {
                                horizontalRuler.provides(20f)
                            }
                        },
                    ) {
                        measurePolicyRead = verticalRuler.current(Float.NaN)
                        p.place(0, 0)
                    }
                } else {
                    layout(p.width, p.height, rulers = { horizontalRuler.provides(20f) }) {
                        measurePolicyRead = verticalRuler.current(Float.NaN)
                        p.place(0, 0)
                    }
                }
            }
        }

        rule.waitForIdle()
        assertThat(outerModifierRead).isNaN()
        assertThat(innerModifierRead).isEqualTo(10f)
        assertThat(measurePolicyRead).isEqualTo(10f)
        assertThat(childVertRead).isEqualTo(10f)
        assertThat(childHorzRead).isEqualTo(20f)
    }

    /**
     * Checks that `RectRulers.innermostOf` and `RectRulers.outermostOf` combine two rect rulers,
     * adjust for the reader's offset, and update when an underlying ruler value changes.
     */
    @Test
    fun rectRulers_providesAndInnermostOutermost() {
        val rect1 = RectRulers()
        val rect2 = RectRulers()
        val inner = RectRulers.innermostOf(rect1, rect2)
        val outer = RectRulers.outermostOf(rect1, rect2)

        var rect2Left by mutableFloatStateOf(30f)
        var innerLeft = Float.NaN
        var innerTop = Float.NaN
        var innerRight = Float.NaN
        var innerBottom = Float.NaN
        var outerLeft = Float.NaN
        var outerTop = Float.NaN
        var outerRight = Float.NaN
        var outerBottom = Float.NaN

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(300.toDp()).layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        if (useIndividualRulers) {
                            layout(
                                p.width,
                                p.height,
                                isRulerProvided = {
                                    it == rect1.left ||
                                        it == rect1.top ||
                                        it == rect1.right ||
                                        it == rect1.bottom ||
                                        it == rect2.left ||
                                        it == rect2.top ||
                                        it == rect2.right ||
                                        it == rect2.bottom
                                },
                                rulerProvider = { ruler ->
                                    when (ruler) {
                                        rect1.left -> rect1.left.provides(10f)
                                        rect1.top -> rect1.top.provides(20f)
                                        rect1.right -> rect1.right.provides(100f)
                                        rect1.bottom -> rect1.bottom.provides(200f)
                                        rect2.left -> rect2.left.provides(rect2Left)
                                        rect2.top -> rect2.top.provides(10f)
                                        rect2.right -> rect2.right.provides(80f)
                                        rect2.bottom -> rect2.bottom.provides(250f)
                                        else -> {}
                                    }
                                },
                            ) {
                                p.place(0, 0)
                            }
                        } else {
                            layout(
                                p.width,
                                p.height,
                                rulers = {
                                    rect1.left.provides(10f)
                                    rect1.top.provides(20f)
                                    rect1.right.provides(100f)
                                    rect1.bottom.provides(200f)
                                    rect2.left.provides(rect2Left)
                                    rect2.top.provides(10f)
                                    rect2.right.provides(80f)
                                    rect2.bottom.provides(250f)
                                },
                            ) {
                                p.place(0, 0)
                            }
                        }
                    }
                ) {
                    Box(
                        Modifier.absoluteOffset(x = 5.toDp(), y = 5.toDp()).layout {
                            measurable,
                            constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                innerLeft = inner.left.current(Float.NaN)
                                innerTop = inner.top.current(Float.NaN)
                                innerRight = inner.right.current(Float.NaN)
                                innerBottom = inner.bottom.current(Float.NaN)
                                outerLeft = outer.left.current(Float.NaN)
                                outerTop = outer.top.current(Float.NaN)
                                outerRight = outer.right.current(Float.NaN)
                                outerBottom = outer.bottom.current(Float.NaN)
                                p.place(0, 0)
                            }
                        }
                    )
                }
            }
        }

        rule.waitForIdle()
        assertThat(innerLeft).isEqualTo(25f)
        assertThat(innerTop).isEqualTo(15f)
        assertThat(innerRight).isEqualTo(75f)
        assertThat(innerBottom).isEqualTo(195f)
        assertThat(outerLeft).isEqualTo(5f)
        assertThat(outerTop).isEqualTo(5f)
        assertThat(outerRight).isEqualTo(95f)
        assertThat(outerBottom).isEqualTo(245f)

        rect2Left = 5f
        rule.waitForIdle()
        assertThat(innerLeft).isEqualTo(5f)
        assertThat(outerLeft).isEqualTo(0f)
    }

    /**
     * Checks that a `VerticalRuler` provided with `providesRelative` in RTL mirrors from the right
     * edge and updates readers when the provider's width changes.
     */
    @Test
    fun providesRelative_rtlWidthChange_updatesRulerValue() {
        var width by mutableIntStateOf(100)
        var readValue = Float.NaN

        rule.setContent {
            with(LocalDensity.current) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.size(width.toDp(), 100.toDp()).provideRelativeRuler(20f)) {
                        Box(
                            Modifier.align(AbsoluteAlignment.TopLeft).layout {
                                measurable,
                                constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    readValue = verticalRuler.current(Float.NaN)
                                    p.place(0, 0)
                                }
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        assertThat(readValue).isEqualTo(80f)

        width = 150
        rule.waitForIdle()
        assertThat(readValue).isEqualTo(130f)
    }

    /**
     * Checks that removing the `rulers` lambda from `layout(...)` notifies child readers to fall
     * back to the default value, and adding it back notifies them again.
     */
    @Test
    fun rulersLambdaRemoved_notifiesReaders() {
        if (useIndividualRulers) return

        var useRulers by mutableStateOf(true)
        var readValue = Float.NaN

        rule.setContent {
            Box(
                Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    if (useRulers) {
                        layout(p.width, p.height, rulers = { verticalRuler.provides(10f) }) {
                            p.place(0, 0)
                        }
                    } else {
                        layout(p.width, p.height) { p.place(0, 0) }
                    }
                }
            ) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            readValue = verticalRuler.current(-1f)
                            p.place(0, 0)
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(readValue).isEqualTo(10f)

        useRulers = false
        rule.waitForIdle()
        assertThat(readValue).isEqualTo(-1f)

        useRulers = true
        rule.waitForIdle()
        assertThat(readValue).isEqualTo(10f)
    }

    /**
     * Checks that switching a node between eager `rulers = { ... }` and lazy `isRulerProvided` /
     * `rulerProvider` notifies child readers of the updated values.
     */
    @Test
    fun switchBetweenRulersAndLazyProvider_notifiesReaders() {
        var useLazy by mutableStateOf(false)
        var readValue = Float.NaN

        rule.setContent {
            Box(
                Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    if (useLazy) {
                        layout(
                            p.width,
                            p.height,
                            isRulerProvided = { it == verticalRuler },
                            rulerProvider = { verticalRuler.provides(20f) },
                        ) {
                            p.place(0, 0)
                        }
                    } else {
                        layout(p.width, p.height, rulers = { verticalRuler.provides(10f) }) {
                            p.place(0, 0)
                        }
                    }
                }
            ) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            readValue = verticalRuler.current(Float.NaN)
                            p.place(0, 0)
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(readValue).isEqualTo(10f)

        useLazy = true
        rule.waitForIdle()
        assertThat(readValue).isEqualTo(20f)

        useLazy = false
        rule.waitForIdle()
        assertThat(readValue).isEqualTo(10f)
    }

    /**
     * Checks that when a ruler provider is placed early for alignment (`isPlacingForAlignment`),
     * its rulers are skipped during the alignment pass but captured once real placement runs.
     */
    @Test
    fun providerPlacedForAlignment_stillCapturesRulersOnCommit() {
        val readValues = mutableFloatListOf()
        rule.setContent {
            Row {
                Box(Modifier.alignByBaseline().provideRulers(vert = 42f)) {
                    Text(
                        "Hello",
                        Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                readValues += verticalRuler.current(Float.NaN)
                                p.place(0, 0)
                            }
                        },
                    )
                }
                Text("World", Modifier.alignByBaseline())
            }
        }

        rule.waitForIdle()
        assertThat(readValues.size).isGreaterThan(1)
        assertThat(readValues[0]).isNaN()
        assertThat(readValues.last()).isEqualTo(42f)
    }

    /**
     * Checks that lookahead and main passes keep separate ruler values and state-read tracking so
     * changing one pass's ruler value does not overwrite the other.
     */
    @Test
    fun lookaheadAndMainPass_differentRulerValues() {
        var lookaheadState by mutableFloatStateOf(100f)
        var mainState by mutableFloatStateOf(50f)
        val lookaheadValues = mutableFloatListOf()
        val mainValues = mutableFloatListOf()

        rule.setContent {
            LookaheadScope {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        val lookingAhead = isLookingAhead
                        if (useIndividualRulers) {
                            layout(
                                p.width,
                                p.height,
                                isRulerProvided = { it == verticalRuler },
                                rulerProvider = {
                                    verticalRuler.provides(
                                        if (lookingAhead) lookaheadState else mainState
                                    )
                                },
                            ) {
                                p.place(0, 0)
                            }
                        } else {
                            layout(
                                p.width,
                                p.height,
                                rulers = {
                                    verticalRuler.provides(
                                        if (lookingAhead) lookaheadState else mainState
                                    )
                                },
                            ) {
                                p.place(0, 0)
                            }
                        }
                    }
                ) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            val lookingAhead = isLookingAhead
                            layout(p.width, p.height) {
                                val value = verticalRuler.current(Float.NaN)
                                if (lookingAhead) {
                                    lookaheadValues += value
                                } else {
                                    mainValues += value
                                }
                                p.place(0, 0)
                            }
                        }
                    )
                }
            }
        }

        rule.waitForIdle()
        assertThat(lookaheadValues.last()).isEqualTo(100f)
        assertThat(mainValues.last()).isEqualTo(50f)

        mainState = 60f
        rule.waitForIdle()
        assertThat(lookaheadValues.last()).isEqualTo(100f)
        assertThat(mainValues.last()).isEqualTo(60f)

        lookaheadState = 120f
        rule.waitForIdle()
        assertThat(lookaheadValues.last()).isEqualTo(120f)
        assertThat(mainValues.last()).isEqualTo(60f)
    }

    /**
     * Checks that a lazy `rulerProvider` reading `coordinates` re-evaluates and notifies child
     * readers when an ancestor moves it, including transitioning between unprovided and provided.
     */
    @Test
    fun lazyProvider_coordinatesBecomesProvidedOnMove() {
        if (!useIndividualRulers) return

        var offset by mutableIntStateOf(0)
        var readValue = Float.NaN

        rule.setContent {
            Box(Modifier.offset { IntOffset(offset, 0) }) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(
                            p.width,
                            p.height,
                            isRulerProvided = { it == verticalRuler },
                            rulerProvider = {
                                if (coordinates.positionInRoot().x > 50f) {
                                    verticalRuler.provides(99f)
                                }
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
                                readValue = verticalRuler.current(Float.NaN)
                                p.place(0, 0)
                            }
                        }
                    )
                }
            }
        }

        rule.waitForIdle()
        assertThat(readValue).isNaN()

        offset = 100
        rule.waitForIdle()
        assertThat(readValue).isEqualTo(99f)

        offset = 0
        rule.waitForIdle()
        assertThat(readValue).isNaN()
    }

    /**
     * Checks that a reader can read different rulers from different ancestors and updates
     * independently when either ancestor changes or stops providing its ruler.
     */
    @Test
    fun multipleProviders_readerReadsFromDifferentAncestors() {
        var vertValue by mutableFloatStateOf(10f)
        var horzValue by mutableFloatStateOf(20f)
        var provideHorz by mutableStateOf(true)
        var readVert = Float.NaN
        var readHorz = Float.NaN

        rule.setContent {
            Box(Modifier.provideRulers(horz = { null }, vert = { vertValue })) {
                Box(
                    Modifier.provideRulers(
                        horz = { if (provideHorz) horzValue else null },
                        vert = { null },
                    )
                ) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                readVert = verticalRuler.current(Float.NaN)
                                readHorz = horizontalRuler.current(Float.NaN)
                                p.place(0, 0)
                            }
                        }
                    )
                }
            }
        }

        rule.waitForIdle()
        assertThat(readVert).isEqualTo(10f)
        assertThat(readHorz).isEqualTo(20f)

        vertValue = 15f
        rule.waitForIdle()
        assertThat(readVert).isEqualTo(15f)
        assertThat(readHorz).isEqualTo(20f)

        horzValue = 25f
        rule.waitForIdle()
        assertThat(readVert).isEqualTo(15f)
        assertThat(readHorz).isEqualTo(25f)

        provideHorz = false
        rule.waitForIdle()
        assertThat(readVert).isEqualTo(15f)
        assertThat(readHorz).isNaN()
    }

    private class NonStateRulerNode(val ruler: VerticalRuler, val useIndividualRulers: Boolean) :
        Modifier.Node(), LayoutModifierNode {
        var rulerValue = 0f

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val p = measurable.measure(constraints)
            return if (useIndividualRulers) {
                layout(
                    p.width,
                    p.height,
                    isRulerProvided = { it == ruler },
                    rulerProvider = { ruler.provides(rulerValue) },
                ) {
                    p.place(0, 0)
                }
            } else {
                layout(p.width, p.height, rulers = { ruler.provides(rulerValue) }) {
                    p.place(0, 0)
                }
            }
        }
    }

    private data class NonStateRulerElement(val node: NonStateRulerNode) :
        ModifierNodeElement<NonStateRulerNode>() {
        override fun create(): NonStateRulerNode = node

        override fun update(node: NonStateRulerNode) {}
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useIndividualRulers={0}")
        fun params() = arrayOf(false, true)
    }
}
