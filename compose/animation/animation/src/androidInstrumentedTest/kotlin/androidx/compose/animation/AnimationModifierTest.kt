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

package androidx.compose.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.math.roundToInt
import kotlin.random.Random
import leakcanary.DetectLeaksAfterTestSuccess
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AnimationModifierTest {
    val rule = createComposeRule()
    // Detect leaks BEFORE and AFTER compose rule work
    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(rule)

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun animateContentSizeTest() {
        val startWidth = 100
        val endWidth = 150
        val startHeight = 400
        val endHeight = 200
        var width by mutableStateOf(startWidth)
        var height by mutableStateOf(startHeight)

        var density = 0f
        val testModifier by mutableStateOf(TestModifier())
        var animationStartSize: IntSize? = null
        var animationEndSize: IntSize? = null

        val frameDuration = 16
        val animDuration = 10 * frameDuration

        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(
                testModifier
                    .animateContentSize(tween(animDuration, easing = LinearOutSlowInEasing)) {
                        startSize,
                        endSize ->
                        animationStartSize = startSize
                        animationEndSize = endSize
                    }
                    .requiredSize(width.dp, height.dp)
            )
            density = LocalDensity.current.density
        }

        rule.runOnUiThread {
            width = endWidth
            height = endHeight
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        for (i in 0..animDuration step frameDuration) {
            val fraction = LinearOutSlowInEasing.transform(i / animDuration.toFloat())
            assertEquals(
                density * (startWidth * (1 - fraction) + endWidth * fraction),
                testModifier.width.toFloat(),
                1f
            )

            assertEquals(
                density * (startHeight * (1 - fraction) + endHeight * fraction),
                testModifier.height.toFloat(),
                1f
            )

            if (i == animDuration) {
                assertNotNull(animationStartSize)
                assertEquals(animationStartSize!!.width.toFloat(), startWidth * density, 1f)
                assertEquals(animationStartSize!!.height.toFloat(), startHeight * density, 1f)
            } else {
                assertNull(animationEndSize)
            }

            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }
    }

    @Test
    fun testAlignmentInAnimateContentSize_underLtr() {
        assertAlignmentInAnimateContentSize(LayoutDirection.Ltr)
    }

    @Test
    fun testAlignmentInAnimateContentSize_underRtl() {
        assertAlignmentInAnimateContentSize(LayoutDirection.Rtl)
    }

    @Test
    fun testAnimatedContentSizeInLookahead() {
        val lookaheadSizes = mutableListOf<IntSize>()
        var size by mutableStateOf(IntSize(400, 600))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Box(
                        Modifier.layout { measurable, constraints ->
                                measurable.measure(constraints).run {
                                    if (isLookingAhead) {
                                        lookaheadSizes.add(IntSize(width, height))
                                    }
                                    layout(width, height) { place(0, 0) }
                                }
                            }
                            .animateContentSize()
                            .size(size.width.dp, size.height.dp)
                    ) {
                        Box(Modifier.size(20.dp))
                    }
                }
            }
        }

        repeat(8) {
            size = IntSize(Random.nextInt(200, 600), Random.nextInt(100, 800))
            lookaheadSizes.clear()
            rule.runOnIdle {
                assertTrue(lookaheadSizes.isNotEmpty())
                lookaheadSizes.forEach { assertEquals(size, it) }
            }
        }
    }

    @Test
    fun testInspectorValue() {
        rule.setContent {
            Modifier.animateContentSize()
                .any {
                    it as InspectableValue
                    if (it.nameFallback == "animateContentSize") {
                        assertThat(it.valueOverride, nullValue())
                        assertThat(
                            it.inspectableElements.map { it.name }.toList(),
                            `is`(listOf("animationSpec", "alignment", "finishedListener"))
                        )
                        true
                    } else {
                        false
                    }
                }
                .also { assertTrue(it) }
        }
    }

    @Test
    fun properFinalStateAfterReAttach() =
        with(rule.density) {
            // Tests that animateContentSize is able to recover (end at its proper target size)
            // after
            // being interrupted with movableContent
            val totalSizePx = 300

            val smallSizePx = 100
            val largeSizePx = 200
            val isExpanded = mutableStateOf(false)

            val containerAOffset = Offset.Zero
            val containerBOffset = Offset(100f, 100f)
            val isAtContainerA = mutableStateOf(true)

            val frameDuration = 16
            val animDuration = 10 * frameDuration

            val testModifier by mutableStateOf(TestModifier())

            rule.setContent {
                val animatedBox = remember {
                    movableContentOf {
                        Box(
                            modifier =
                                Modifier.wrapContentSize()
                                    .then(testModifier)
                                    .animateContentSize(tween(animDuration, easing = LinearEasing))
                        ) {
                            val size =
                                if (isExpanded.value) {
                                    largeSizePx.toDp()
                                } else {
                                    smallSizePx.toDp()
                                }
                            Box(Modifier.requiredSize(size))
                        }
                    }
                }

                Box(Modifier.size(totalSizePx.toDp())) {
                    Box(Modifier.offset { containerAOffset.round() }) {
                        if (isAtContainerA.value) {
                            animatedBox()
                        }
                    }
                    Box(Modifier.offset { containerBOffset.round() }) {
                        if (!isAtContainerA.value) {
                            animatedBox()
                        }
                    }
                }
            }

            rule.waitForIdle()
            rule.mainClock.autoAdvance = false

            isExpanded.value = true
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()

            // Animate towards halfway the animation
            rule.mainClock.advanceTimeBy(animDuration / 2L)
            rule.waitForIdle()

            assertEquals(150, testModifier.width)
            assertEquals(150, testModifier.height)

            // Move container, this should cause a re-attach in `animateContentSize` node, after
            // this,
            // if we let the animation run until it finishes, the final size should match the
            // expected
            // size.
            // Note that this test intentionally doesn't cover the behavior of the remaining
            // animation
            // as this change does not address that.
            isAtContainerA.value = !isAtContainerA.value
            rule.mainClock.autoAdvance = true
            rule.waitForIdle()

            assertEquals(largeSizePx, testModifier.width)
            assertEquals(largeSizePx, testModifier.height)
        }

    /**
     * Verifies Alignment behavior when used with animateContentSize.
     *
     * @param layoutDirection LayoutDirection applied to the Compose UI, this is also used to
     *   manually verify alignment values with [Alignment.align].
     */
    private fun assertAlignmentInAnimateContentSize(layoutDirection: LayoutDirection) {
        val alignmentList = listOf(Alignment.TopStart, Alignment.Center, Alignment.BottomEnd)

        val startWidth = 100
        val endWidth = 150
        val startHeight = 400
        val endHeight = 200
        var width by mutableStateOf(startWidth)
        var height by mutableStateOf(startHeight)

        val density = rule.density.density

        val frameDuration = 16
        val animDuration = 10 * frameDuration

        val positionInRootByBoxIndex = mutableMapOf<Int, Offset>()

        @Composable
        fun AnimateBoxSizeWithAlignment(alignment: Alignment, index: Int) {
            Box(
                Modifier.animateContentSize(
                        animationSpec = tween(animDuration, easing = LinearOutSlowInEasing),
                        alignment = alignment
                    )
                    .onPlaced { positionInRootByBoxIndex[index] = it.positionInRoot() }
                    .requiredSize(width.dp, height.dp)
            )
        }

        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                alignmentList.forEachIndexed { index, alignment ->
                    AnimateBoxSizeWithAlignment(index = index, alignment = alignment)
                }
            }
        }

        rule.runOnUiThread {
            width = endWidth
            height = endHeight
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        val size = with(rule.density) { IntSize(endWidth.dp.roundToPx(), endHeight.dp.roundToPx()) }

        for (i in 0..animDuration step frameDuration) {
            val fraction = LinearOutSlowInEasing.transform(i / animDuration.toFloat())
            val expectedWidth = density * (startWidth * (1 - fraction) + endWidth * fraction)
            val expectedHeight = density * (startHeight * (1 - fraction) + endHeight * fraction)
            val space = IntSize(expectedWidth.roundToInt(), expectedHeight.roundToInt())

            // Test all boxes at the current frame
            for (alignIndex in alignmentList.indices) {
                val expectedPosition =
                    alignmentList[alignIndex].align(size, space, layoutDirection).toOffset()
                val positionInRoot = positionInRootByBoxIndex[alignIndex]!!
                assertEquals(expectedPosition.x, positionInRoot.x, 1f)
                assertEquals(expectedPosition.y, positionInRoot.y, 1f)
            }

            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }
    }
}

internal class TestModifier : LayoutModifier {
    var width: Int = 0
    var height: Int = 0
    var lookaheadSize: IntSize? = null
        private set

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        if (isLookingAhead) lookaheadSize = IntSize(placeable.width, placeable.height)
        width = placeable.width
        height = placeable.height
        return layout(width, height) { placeable.place(0, 0) }
    }
}
