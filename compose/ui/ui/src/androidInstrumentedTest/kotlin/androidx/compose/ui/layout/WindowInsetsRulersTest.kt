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
package androidx.compose.ui.layout

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.collection.mutableObjectListOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable.PlacementScope
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.CaptionBar
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.DisplayCutout
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.Ime
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.MandatorySystemGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.NavigationBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SafeContent
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SafeDrawing
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SafeGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.StatusBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SystemGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.TappableElement
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.Waterfall
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requestRemeasure
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.ActivityWithInsets
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@SdkSuppress(minSdkVersion = 30)
@RunWith(JUnit4::class)
class WindowInsetsRulersTest {
    @get:Rule val rule = createAndroidComposeRule<ActivityWithInsets>()

    private lateinit var composeView: AndroidComposeView
    private var insetsRect: IntRect? = null
    private var sourceRect: IntRect? = null
    private var targetRect: IntRect? = null
    private var isVisible = false
    private var isAnimating = false
    private var fraction = 0f
    private var durationMillis = 0L
    private var alpha = 0f
    private var maximumRect: IntRect? = null
    private var contentWidth = 0
    private var contentHeight = 0
    private val displayCutoutRects = mutableObjectListOf<IntRect?>()

    @Before
    fun setup() {
        rule.runOnUiThread { rule.activity.enableEdgeToEdge() }
        // Don't let the normal rulers through. We only want the sendOnApplyWindowInsets() to have
        // an effect.
        val contentView = rule.activity.findViewById<View>(android.R.id.content)
        contentView.setOnApplyWindowInsetsListener { _, _ -> android.view.WindowInsets.CONSUMED }
    }

    private fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            content()
        }
    }

    private fun setSimpleRulerContent(rulerState: State<WindowInsetsRulers>) {
        setContent {
            Box(
                Modifier.fillMaxSize()
                    .onPlaced {
                        contentWidth = it.size.width
                        contentHeight = it.size.height
                    }
                    .rulerToRect(rulerState.value) { rect ->
                        insetsRect = rect

                        sourceRect = null
                        targetRect = null
                        isAnimating = false

                        isVisible = false
                        fraction = 0f
                        alpha = 1f
                        durationMillis = 0L
                        maximumRect = null
                        displayCutoutRects.clear()

                        val rulers = rulerState.value
                        if (rulers == DisplayCutout) {
                            val cutouts = getDisplayCutoutBounds()
                            cutouts.forEach { cutoutRulers ->
                                displayCutoutRects.add(readRulers(cutoutRulers))
                            }
                        }
                        val animationProperties = rulers.getAnimation(this)
                        sourceRect = readRulers(animationProperties.source)
                        targetRect = readRulers(animationProperties.target)
                        isAnimating = animationProperties.isAnimating
                        isVisible = animationProperties.isVisible
                        fraction = animationProperties.fraction
                        alpha = animationProperties.alpha
                        durationMillis = animationProperties.durationMillis
                        maximumRect = readRulers(rulers.maximum)
                    }
            )
        }
    }

    private fun sendOnApplyWindowInsets(insets: WindowInsetsCompat) {
        val view = composeView.parent as View
        rule.runOnIdle { composeView.insetsListener.onApplyWindowInsets(view, insets) }
    }

    private fun startAnimation(
        animation: WindowInsetsAnimationCompat,
        type: Int,
        low: Insets,
        high: Insets,
        target: Insets,
    ) {
        val view = composeView.parent as View
        rule.runOnIdle {
            val insetsListener = composeView.insetsListener
            insetsListener.onPrepare(animation)
            insetsListener.onApplyWindowInsets(view, createInsets(type to target))
            insetsListener.onStart(animation, BoundsCompat(low, high))
        }
    }

    private fun progressAnimation(
        animation: WindowInsetsAnimationCompat,
        insets: WindowInsetsCompat,
    ) {
        val view = composeView.parent as View
        rule.runOnIdle {
            val insetsListener = composeView.insetsListener
            insetsListener.onProgress(insets, mutableListOf(animation))
            insetsListener.onApplyWindowInsets(view, insets)
        }
    }

    private fun endAnimation(animation: WindowInsetsAnimationCompat, insets: WindowInsetsCompat) {
        val view = composeView.parent as View
        rule.runOnIdle {
            val insetsListener = composeView.insetsListener
            insetsListener.onEnd(animation)
            insetsListener.onApplyWindowInsets(view, insets)
        }
    }

    private fun assertNotAnimating(rulers: Any, updatedRulers: Any = rulers) {
        val message = "$rulers / $updatedRulers"
        assertWithMessage(message).that(sourceRect).isNull()
        assertWithMessage(message).that(targetRect).isNull()
        assertWithMessage(message).that(isAnimating).isFalse()
        assertWithMessage(message).that(fraction).isEqualTo(0f)
        assertWithMessage(message).that(durationMillis).isEqualTo(0L)
    }

    @Test
    fun noRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val rulerState = mutableStateOf(CaptionBar)

        setSimpleRulerContent(rulerState)

        // Send no insets
        sendOnApplyWindowInsets(createInsets())
        val normalRulersList =
            listOf(
                TappableElement,
                StatusBars,
                Ime,
                NavigationBars,
                CaptionBar,
                MandatorySystemGestures,
                SystemGestures,
                DisplayCutout,
            )
        normalRulersList.forEach { rulers ->
            rulerState.value = rulers
            rule.waitForIdle()
            assertWithMessage(rulers.toString()).that(insetsRect).isNull()
            assertWithMessage("$rulers maximum").that(maximumRect).isNull()
        }
    }

    @Test
    fun onlyMaximumRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val rulerState = mutableStateOf(CaptionBar)

        setSimpleRulerContent(rulerState)

        // Send one maximum ruler
        sendOnApplyWindowInsets(
            WindowInsetsCompat.Builder()
                .setInsetsIgnoringVisibility(Type.captionBar(), Insets.of(1, 0, 0, 0))
                .build()
        )
        rule.waitForIdle()
        val fullSizeRect = IntRect(0, 0, contentWidth, contentHeight)
        assertThat(insetsRect).isEqualTo(fullSizeRect)
        assertThat(maximumRect).isEqualTo(IntRect(1, 0, contentWidth, contentHeight))

        rulerState.value = NavigationBars
        rule.waitForIdle()
        assertThat(insetsRect).isEqualTo(fullSizeRect)
        assertThat(maximumRect).isEqualTo(fullSizeRect)
    }

    @Test
    fun startAndStopProvidingRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val rulerState = mutableStateOf(CaptionBar)

        setSimpleRulerContent(rulerState)

        // Send no insets
        sendOnApplyWindowInsets(createInsets())
        rule.waitForIdle()

        // Send caption bar insets
        sendOnApplyWindowInsets(createInsets(Type.captionBar() to Insets.of(1, 0, 0, 0)))
        rule.waitForIdle()

        assertThat(insetsRect).isEqualTo(IntRect(1, 0, contentWidth, contentHeight))

        // Send no insets
        sendOnApplyWindowInsets(createInsets())
        rule.waitForIdle()

        // Not null now that rulers have been provided once.
        assertThat(insetsRect).isEqualTo(IntRect(0, 0, contentWidth, contentHeight))
    }

    @Test
    fun normalRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val rulerState = mutableStateOf(CaptionBar)
        setSimpleRulerContent(rulerState)
        val normalRulersList =
            listOf(
                TappableElement,
                StatusBars,
                Ime,
                NavigationBars,
                CaptionBar,
                MandatorySystemGestures,
                SystemGestures,
                DisplayCutout,
            )
        normalRulersList.forEach { visibleRulers ->
            rulerState.value = visibleRulers
            rule.waitForIdle()
            InsetsRulerTypes.forEach { (rulers, type) ->
                val insets = createInsets(type to Insets.of(1, 2, 3, 5))
                sendOnApplyWindowInsets(insets)
                rule.runOnIdle {
                    val isRulerVisible =
                        visibleRulers === rulers ||
                            (rulers === Waterfall && visibleRulers == DisplayCutout)
                    val expectedRect =
                        if (isRulerVisible) {
                            IntRect(1, 2, contentWidth - 3, contentHeight - 5)
                        } else {
                            IntRect(0, 0, contentWidth, contentHeight)
                        }
                    val message = "$visibleRulers / $rulers"
                    assertWithMessage(message).that(insetsRect).isEqualTo(expectedRect)
                    if (visibleRulers === Ime) {
                        assertWithMessage(message).that(maximumRect).isNull()
                    } else {
                        assertWithMessage(message).that(maximumRect).isEqualTo(expectedRect)
                    }
                    assertNotAnimating(rulers)
                    assertWithMessage(message).that(isVisible).isEqualTo(isRulerVisible)
                }
            }
        }
    }

    @Test
    fun maximum() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val rulerState = mutableStateOf(CaptionBar)
        setSimpleRulerContent(rulerState)
        val maximumRulersList =
            listOf(
                TappableElement,
                StatusBars,
                NavigationBars,
                CaptionBar,
                MandatorySystemGestures,
                SystemGestures,
                DisplayCutout,
            )
        maximumRulersList.forEach { rulers ->
            rulerState.value = rulers
            rule.waitForIdle()

            val type = InsetsRulerTypes[rulers]!!
            val expectedInsets = Insets.of(1, 2, 3, 5)
            val insets = createInsetsIgnoringVisibility(type to expectedInsets)
            sendOnApplyWindowInsets(insets)
            rule.runOnIdle {
                assertWithMessage(rulers.toString())
                    .that(insetsRect)
                    .isEqualTo(IntRect(0, 0, contentWidth, contentHeight))
                assertWithMessage(rulers.toString())
                    .that(maximumRect)
                    .isEqualTo(IntRect(1, 2, contentWidth - 3, contentHeight - 5))
            }
        }
    }

    @Test
    fun waterfallRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val rulerState = mutableStateOf(Waterfall)
        setSimpleRulerContent(rulerState)
        rule.waitForIdle()

        InsetsRulerTypes.forEach { (rulers, type) ->
            val insets = createInsets(type to Insets.of(1, 2, 3, 5))
            sendOnApplyWindowInsets(insets)
            rule.runOnIdle {
                val expectedRect =
                    if (rulers === Waterfall) {
                        IntRect(1, 2, contentWidth - 3, contentHeight - 5)
                    } else {
                        IntRect(0, 0, contentWidth, contentHeight)
                    }
                val message = "Waterfall / $rulers"
                assertWithMessage(message).that(insetsRect).isEqualTo(expectedRect)
                assertNotAnimating(rulers)
                assertWithMessage(message).that(isVisible).isEqualTo(rulers === Waterfall)
            }
        }
    }

    /** Make sure that when the display cutout is set that it includes the rects for each side. */
    @Test
    fun displayCutoutRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        setSimpleRulerContent(mutableStateOf(DisplayCutout))

        val insets = createInsets(Type.displayCutout() to Insets.of(1, 2, 3, 5))
        sendOnApplyWindowInsets(insets)
        rule.runOnIdle {
            assertThat(displayCutoutRects.size).isEqualTo(4)
            assertThat(displayCutoutRects.any { it == null }).isFalse()
            assertThat(displayCutoutRects.asList())
                .containsExactly(
                    IntRect(0, 0, 1, contentHeight),
                    IntRect(0, 0, contentWidth, 2),
                    IntRect(contentWidth - 3, 0, contentWidth, contentHeight),
                    IntRect(0, contentHeight - 5, contentWidth, contentHeight),
                )
        }
    }

    @Test
    fun mergedRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val mergedRulersMap =
            mapOf(
                SafeGestures to
                    listOf(
                        Type.systemGestures(),
                        Type.mandatorySystemGestures(),
                        Type.tappableElement(),
                        WaterfallType,
                    ),
                SafeDrawing to
                    listOf(
                        Type.captionBar(),
                        Type.ime(),
                        Type.navigationBars(),
                        Type.statusBars(),
                        Type.tappableElement(),
                        Type.displayCutout(),
                        WaterfallType,
                    ),
                SafeContent to
                    listOf(
                        Type.systemGestures(),
                        Type.mandatorySystemGestures(),
                        Type.tappableElement(),
                        Type.captionBar(),
                        Type.ime(),
                        Type.navigationBars(),
                        Type.statusBars(),
                        Type.displayCutout(),
                        WaterfallType,
                    ),
            )

        val rulerState = mutableStateOf(SafeGestures)
        setSimpleRulerContent(rulerState)

        mergedRulersMap.forEach { gestureRulers, includedTypes ->
            rulerState.value = gestureRulers
            rule.waitForIdle()
            InsetsRulerTypes.forEach { (rulers, type) ->
                val insets = createInsets(type to Insets.of(1, 2, 3, 5))
                sendOnApplyWindowInsets(insets)
                rule.runOnIdle {
                    val expectedRect =
                        if (type in includedTypes) {
                            IntRect(1, 2, contentWidth - 3, contentHeight - 5)
                        } else {
                            IntRect(0, 0, contentWidth, contentHeight)
                        }
                    val message = "$gestureRulers / $rulers}"
                    assertWithMessage(message).that(insetsRect).isEqualTo(expectedRect)
                    assertNotAnimating(rulers)
                    assertWithMessage(message).that(isVisible).isEqualTo(type in includedTypes)
                }
            }
        }
    }

    @Test
    fun animatingRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val rulerState = mutableStateOf(Ime)
        setSimpleRulerContent(rulerState)

        val animatableRulersList =
            listOf(CaptionBar, Ime, NavigationBars, StatusBars, TappableElement)
        animatableRulersList.forEach { animatableRulers ->
            rulerState.value = animatableRulers
            rule.waitForIdle()

            val type = InsetsRulerTypes[animatableRulers]!!
            val insets = createInsets(type to Insets.of(10, 20, 30, 50))
            // set the initial insets
            sendOnApplyWindowInsets(insets)

            val animationInterpolator = AccelerateDecelerateInterpolator()
            val animation = WindowInsetsAnimationCompat(type, animationInterpolator, 1000L)
            animation.alpha = 1f
            val targetInsets = Insets.of(0, 0, 0, 0)
            val sourceInsets = Insets.of(10, 20, 30, 50)

            startAnimation(animation, type, targetInsets, sourceInsets, targetInsets)
            rule.runOnIdle { assertAnimating(animatableRulers, sourceInsets, 0f) }

            animation.fraction = 0.25f
            animation.alpha = lerp(1f, 0f, animation.interpolatedFraction)
            progressAnimation(
                animation,
                createInsets(
                    type to lerp(sourceInsets, targetInsets, animation.interpolatedFraction)
                ),
            )
            rule.runOnIdle {
                assertAnimating(animatableRulers, sourceInsets, animation.interpolatedFraction)
            }
            animation.fraction = 0.75f
            animation.alpha = lerp(1f, 0f, animation.interpolatedFraction)
            progressAnimation(
                animation,
                createInsets(
                    type to lerp(sourceInsets, targetInsets, animation.interpolatedFraction)
                ),
            )
            rule.runOnIdle {
                assertAnimating(animatableRulers, sourceInsets, animation.interpolatedFraction)
            }

            animation.fraction = 1f
            animation.alpha = 0f
            endAnimation(animation, createInsets(type to Insets.of(0, 0, 0, 0)))
            rule.runOnIdle {
                val expectedRect = IntRect(0, 0, contentWidth, contentHeight)
                assertWithMessage(animatableRulers.toString())
                    .that(insetsRect)
                    .isEqualTo(expectedRect)
                assertNotAnimating(animatableRulers)
                assertWithMessage(animatableRulers.toString()).that(isVisible).isFalse()
            }
        }
    }

    private fun lerp(a: Insets, b: Insets, fraction: Float): Insets {
        val left = (a.left + fraction * (b.left - a.left)).toInt()
        val top = (a.top + fraction * (b.top - a.top)).toInt()
        val right = (a.right + fraction * (b.right - a.right)).toInt()
        val bottom = (a.bottom + fraction * (b.bottom - a.bottom)).toInt()
        return Insets.of(left, top, right, bottom)
    }

    private fun assertAnimating(
        animatableRulers: WindowInsetsRulers,
        source: Insets,
        expectedFraction: Float,
    ) {
        val target = Insets.of(0, 0, 0, 0)
        val insets = lerp(source, target, expectedFraction)
        val expectedRect =
            IntRect(
                insets.left,
                insets.top,
                contentWidth - insets.right,
                contentHeight - insets.bottom,
            )
        val rulerName = animatableRulers.toString()
        assertWithMessage(rulerName).that(insetsRect).isEqualTo(expectedRect)
        val expectedAlpha = 1f - expectedFraction

        val expectedSourceRect =
            IntRect(
                source.left,
                source.top,
                contentWidth - source.right,
                contentHeight - source.bottom,
            )
        assertWithMessage(rulerName).that(sourceRect).isEqualTo(expectedSourceRect)
        val expectedTargetRect =
            IntRect(
                target.left,
                target.top,
                contentWidth - target.right,
                contentHeight - target.bottom,
            )
        assertWithMessage(rulerName).that(targetRect).isEqualTo(expectedTargetRect)
        assertWithMessage(rulerName).that(isAnimating).isTrue()
        assertWithMessage(rulerName).that(fraction).isWithin(0.01f).of(expectedFraction)
        assertWithMessage(rulerName).that(durationMillis).isEqualTo(1000L)
        assertWithMessage(rulerName).that(alpha).isWithin(0.01f).of(expectedAlpha)

        assertWithMessage(rulerName).that(isVisible).isTrue()
    }

    private fun assertAnimatingMerged(
        animatableRulers: WindowInsetsRulers,
        source: Insets,
        expectedFraction: Float,
    ) {
        val target = Insets.of(0, 0, 0, 0)
        val insets = lerp(source, target, expectedFraction)
        val expectedRect =
            IntRect(
                insets.left,
                insets.top,
                contentWidth - insets.right,
                contentHeight - insets.bottom,
            )
        val rulerName = animatableRulers.toString()
        assertWithMessage(rulerName).that(insetsRect).isEqualTo(expectedRect)
        val maximumRect =
            IntRect(
                source.left,
                source.top,
                contentWidth - source.right,
                contentHeight - source.bottom,
            )
        assertWithMessage(rulerName).that(maximumRect).isEqualTo(maximumRect)

        assertWithMessage(rulerName).that(sourceRect).isNull()
        assertWithMessage(rulerName).that(targetRect).isNull()
        assertWithMessage(rulerName).that(isAnimating).isTrue()
        assertWithMessage(rulerName).that(fraction).isWithin(0.0001f).of(0f)
        assertWithMessage(rulerName).that(durationMillis).isEqualTo(0L)
        assertWithMessage(rulerName).that(alpha).isEqualTo(1f)
        assertWithMessage(rulerName).that(isVisible).isTrue()
    }

    @Test
    fun animateMergedRulers() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        // display cutout and waterfall aren't animatable
        val mergedRulersMap =
            mapOf(
                SafeGestures to
                    listOf(
                        Type.systemGestures(),
                        Type.mandatorySystemGestures(),
                        Type.tappableElement(),
                        WaterfallType,
                    ),
                SafeDrawing to
                    listOf(
                        Type.captionBar(),
                        Type.ime(),
                        Type.navigationBars(),
                        Type.statusBars(),
                        Type.tappableElement(),
                        Type.displayCutout(),
                        WaterfallType,
                    ),
                SafeContent to
                    listOf(
                        Type.systemGestures(),
                        Type.mandatorySystemGestures(),
                        Type.tappableElement(),
                        Type.captionBar(),
                        Type.ime(),
                        Type.navigationBars(),
                        Type.statusBars(),
                        Type.displayCutout(),
                        WaterfallType,
                    ),
            )

        val rulerState = mutableStateOf(SafeGestures)
        setSimpleRulerContent(rulerState)

        mergedRulersMap.forEach { mergedRulers, includedTypes ->
            rulerState.value = mergedRulers
            rule.waitForIdle()
            InsetsRulerTypes.forEach { (rulers, type) ->
                if (rulers !== Waterfall) {
                    // set the initial insets
                    val insets = createInsets(type to Insets.of(10, 20, 30, 50))
                    sendOnApplyWindowInsets(insets)

                    val shouldBeAnimating = type in includedTypes

                    val animationInterpolator = AccelerateDecelerateInterpolator()
                    val animation = WindowInsetsAnimationCompat(type, animationInterpolator, 1000L)
                    animation.alpha = 1f
                    val targetInsets = Insets.of(0, 0, 0, 0)
                    val sourceInsets = Insets.of(10, 20, 30, 50)

                    startAnimation(animation, type, targetInsets, sourceInsets, targetInsets)
                    rule.waitForIdle()
                    rule.runOnIdle {
                        assertWithMessage("$mergedRulers / $rulers")
                            .that(isVisible)
                            .isEqualTo(type in includedTypes)
                        assertWithMessage("$mergedRulers / $rulers")
                            .that(isAnimating)
                            .isEqualTo(shouldBeAnimating)
                        assertWithMessage("$mergedRulers / $rulers").that(sourceRect).isNull()
                        assertWithMessage("$mergedRulers / $rulers").that(targetRect).isNull()
                    }
                    animation.fraction = 0.25f
                    animation.alpha = 0.75f
                    progressAnimation(
                        animation,
                        createInsets(type to lerp(sourceInsets, targetInsets, 0.25f)),
                    )
                    rule.runOnIdle {
                        assertWithMessage("$mergedRulers / $rulers")
                            .that(isVisible)
                            .isEqualTo(type in includedTypes)
                        assertWithMessage("$mergedRulers / $rulers")
                            .that(isAnimating)
                            .isEqualTo(shouldBeAnimating)
                        assertWithMessage("$mergedRulers / $rulers").that(sourceRect).isNull()
                        assertWithMessage("$mergedRulers / $rulers").that(targetRect).isNull()
                    }
                    animation.fraction = 0.75f
                    animation.alpha = 0.25f
                    progressAnimation(
                        animation,
                        createInsets(type to lerp(sourceInsets, targetInsets, 0.75f)),
                    )
                    rule.runOnIdle {
                        assertWithMessage("$mergedRulers / $rulers")
                            .that(isVisible)
                            .isEqualTo(type in includedTypes)
                        assertWithMessage("$mergedRulers / $rulers")
                            .that(isAnimating)
                            .isEqualTo(shouldBeAnimating)
                        assertWithMessage("$mergedRulers / $rulers").that(sourceRect).isNull()
                        assertWithMessage("$mergedRulers / $rulers").that(targetRect).isNull()
                    }

                    animation.fraction = 1f
                    animation.alpha = 0f
                    endAnimation(animation, createInsets())
                    rule.runOnIdle {
                        val expectedRect = IntRect(0, 0, contentWidth, contentHeight)
                        assertWithMessage("$mergedRulers / $rulers")
                            .that(insetsRect)
                            .isEqualTo(expectedRect)
                        assertNotAnimating(mergedRulers, rulers)
                        assertWithMessage("$mergedRulers / $rulers").that(isVisible).isFalse()
                    }
                }
            }
        }
    }

    @Test
    fun rulersInCenteredDialog() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        var boxInsetsRect: IntRect? = null
        var dialogInsetsRect: IntRect? = null
        lateinit var coordinates: LayoutCoordinates

        lateinit var composeView: AndroidComposeView
        lateinit var dialogComposeView: AndroidComposeView
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            Box(
                Modifier.fillMaxSize()
                    .onPlaced { coordinates = it }
                    .rulerToRect(SafeDrawing) { boxInsetsRect = it }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false,
                        ),
                ) {
                    dialogComposeView = LocalView.current as AndroidComposeView
                    with(LocalDensity.current) {
                        Box(
                            Modifier.size(50.toDp()).rulerToRect(SafeContent) {
                                dialogInsetsRect = it
                            }
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            val insets =
                createInsets(
                    Type.displayCutout() to Insets.of(5, 0, 0, 0),
                    Type.statusBars() to Insets.of(0, 7, 0, 0),
                    Type.navigationBars() to Insets.of(0, 0, 11, 0),
                    Type.tappableElement() to Insets.of(0, 0, 0, 13),
                )
            val view = composeView.parent as View
            composeView.insetsListener.onApplyWindowInsets(view, insets)
            val dialogView = dialogComposeView.parent as View
            dialogComposeView.insetsListener.onApplyWindowInsets(dialogView, createInsets())
        }

        rule.runOnIdle {
            val width = coordinates.size.width
            val height = coordinates.size.height
            assertThat(boxInsetsRect).isEqualTo(IntRect(5, 7, width - 11, height - 13))
            assertThat(dialogInsetsRect).isEqualTo(IntRect(0, 0, 50, 50))
        }
    }

    @Test
    fun rulersInFullScreenDialog() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        var boxInsetsRect: IntRect? = null
        var dialogInsetsRect: IntRect? = null
        lateinit var coordinates: LayoutCoordinates

        lateinit var composeView: AndroidComposeView
        lateinit var dialogComposeView: AndroidComposeView
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            Box(
                Modifier.fillMaxSize()
                    .onPlaced { coordinates = it }
                    .rulerToRect(SafeDrawing) { boxInsetsRect = it }
            ) {
                Dialog(
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false,
                        ),
                ) {
                    dialogComposeView = LocalView.current as AndroidComposeView
                    Box(Modifier.fillMaxSize().rulerToRect(SafeContent) { dialogInsetsRect = it })
                }
            }
        }
        rule.runOnIdle {
            val insets =
                createInsets(
                    Type.displayCutout() to Insets.of(5, 0, 0, 0),
                    Type.statusBars() to Insets.of(0, 7, 0, 0),
                    Type.navigationBars() to Insets.of(0, 0, 11, 0),
                    Type.tappableElement() to Insets.of(0, 0, 0, 13),
                )
            val view = composeView.parent as View
            composeView.insetsListener.onApplyWindowInsets(view, insets)
            val dialogView = dialogComposeView.parent as View
            dialogComposeView.insetsListener.onApplyWindowInsets(dialogView, insets)
        }

        rule.runOnIdle {
            val width = coordinates.size.width
            val height = coordinates.size.height
            assertThat(boxInsetsRect).isEqualTo(IntRect(5, 7, width - 11, height - 13))
            assertThat(dialogInsetsRect).isEqualTo(IntRect(5, 7, width - 11, height - 13))
        }
    }

    private fun createInsets(vararg insetValues: Pair<Int, Insets>): WindowInsetsCompat {
        val builder = WindowInsetsCompat.Builder()
        insetValues.forEach { (type, insets) ->
            if (type != Type.displayCutout() && type != WaterfallType) {
                builder.setInsets(type, insets)
                if (type != Type.ime()) {
                    builder.setInsetsIgnoringVisibility(type, insets)
                }
                builder.setVisible(
                    type,
                    insets.left != 0 || insets.top != 0 || insets.right != 0 || insets.bottom != 0,
                )
            }
        }
        val map = mapOf(*insetValues)
        val displayCutoutInsets = map[Type.displayCutout()]
        if (displayCutoutInsets != null) {
            val left =
                if (displayCutoutInsets.left == 0) null
                else android.graphics.Rect(0, 0, displayCutoutInsets.left, contentHeight)
            val top =
                if (displayCutoutInsets.top == 0) null
                else android.graphics.Rect(0, 0, contentWidth, displayCutoutInsets.top)
            val right =
                if (displayCutoutInsets.right == 0) null
                else
                    android.graphics.Rect(
                        contentWidth - displayCutoutInsets.right,
                        0,
                        contentWidth,
                        contentHeight,
                    )
            val bottom =
                if (displayCutoutInsets.bottom == 0) null
                else
                    android.graphics.Rect(
                        0,
                        contentHeight - displayCutoutInsets.bottom,
                        contentWidth,
                        contentHeight,
                    )
            val cutout =
                DisplayCutoutCompat(
                    displayCutoutInsets,
                    left,
                    top,
                    right,
                    bottom,
                    Insets.of(0, 0, 0, 0),
                )
            builder.setInsets(Type.displayCutout(), displayCutoutInsets)
            builder.setInsetsIgnoringVisibility(Type.displayCutout(), displayCutoutInsets)
            builder.setVisible(Type.displayCutout(), true)
            builder.setDisplayCutout(cutout)
        } else {
            val waterfallInsets = map[WaterfallType]
            if (waterfallInsets != null) {
                val displayCutout =
                    DisplayCutoutCompat(waterfallInsets, null, null, null, null, waterfallInsets)
                builder.setInsets(Type.displayCutout(), waterfallInsets)
                builder.setInsetsIgnoringVisibility(Type.displayCutout(), waterfallInsets)
                builder.setVisible(Type.displayCutout(), true)
                builder.setDisplayCutout(displayCutout)
            }
        }
        val windowInsets = builder.build()
        val method =
            windowInsets::class
                .java
                .getDeclaredMethod("setOverriddenInsets", Array<Insets>::class.java)
        method.isAccessible = true
        val lastTypeMask = 1 shl 8
        val insets = arrayOfNulls<Insets>(9)
        var typeMask = 1
        while (typeMask <= lastTypeMask) {
            insets[indexOf(typeMask)] = map[typeMask] ?: Insets.of(0, 0, 0, 0)
            typeMask = typeMask shl 1
        }
        method.invoke(windowInsets, insets)
        return windowInsets
    }

    @Test
    fun innermostOf() {
        Assume.assumeTrue(ComposeUiFlags.areWindowInsetsRulersEnabled)
        val rulerState = mutableStateOf(WindowInsetsRulers.innermostOf(CaptionBar, NavigationBars))
        setSimpleRulerContent(rulerState)
        rule.waitForIdle()

        val type = Type.navigationBars()
        val insets = createInsets(type to Insets.of(10, 20, 30, 50))
        // set the initial insets
        sendOnApplyWindowInsets(insets)

        rule.runOnIdle {
            assertThat(sourceRect).isNull()
            assertThat(targetRect).isNull()
            assertThat(insetsRect).isEqualTo(IntRect(10, 20, contentWidth - 30, contentHeight - 50))
            assertThat(maximumRect)
                .isEqualTo(IntRect(10, 20, contentWidth - 30, contentHeight - 50))
            assertThat(isVisible).isTrue()
            assertThat(isAnimating).isFalse()
        }

        val animationInterpolator = AccelerateDecelerateInterpolator()
        val animation = WindowInsetsAnimationCompat(type, animationInterpolator, 1000L)
        animation.alpha = 1f
        val targetInsets = Insets.of(0, 0, 0, 0)
        val sourceInsets = Insets.of(10, 20, 30, 50)

        startAnimation(animation, type, targetInsets, sourceInsets, targetInsets)

        rule.runOnIdle { assertAnimatingMerged(rulerState.value, sourceInsets, 0f) }

        animation.fraction = 0.25f
        animation.alpha = 0.75f
        progressAnimation(animation, createInsets(type to lerp(sourceInsets, targetInsets, 0.25f)))
        rule.runOnIdle { assertAnimatingMerged(rulerState.value, sourceInsets, 0.25f) }
        animation.fraction = 0.75f
        animation.alpha = 0.25f
        progressAnimation(animation, createInsets(type to lerp(sourceInsets, targetInsets, 0.75f)))
        rule.runOnIdle { assertAnimatingMerged(rulerState.value, sourceInsets, 0.75f) }

        animation.fraction = 1f
        animation.alpha = 0f
        endAnimation(animation, createInsets(type to Insets.of(0, 0, 0, 0)))
        rule.runOnIdle {
            val expectedRect = IntRect(0, 0, contentWidth, contentHeight)
            assertWithMessage(rulerState.value.toString()).that(insetsRect).isEqualTo(expectedRect)
            assertNotAnimating(rulerState.value)
            assertWithMessage(rulerState.value.toString()).that(isVisible).isFalse()
        }
    }

    private fun createInsetsIgnoringVisibility(
        vararg insetValues: Pair<Int, Insets>
    ): WindowInsetsCompat {
        val builder = WindowInsetsCompat.Builder()
        for (type in NormalInsetsTypes) {
            val targetInsets =
                insetValues.firstOrNull { it.first == type }?.second ?: Insets.of(0, 0, 0, 0)
            builder.setInsetsIgnoringVisibility(type, targetInsets)
            builder.setVisible(type, false)
            builder.setInsets(type, Insets.of(0, 0, 0, 0))
        }
        return builder.build()
    }

    private fun indexOf(type: Int): Int =
        when (type) {
            Type.statusBars() -> 0
            Type.navigationBars() -> 1
            Type.captionBar() -> 2
            Type.ime() -> 3
            Type.systemGestures() -> 4
            Type.mandatorySystemGestures() -> 5
            Type.tappableElement() -> 6
            Type.displayCutout() -> 7
            1 shl 8 -> 8
            else -> -1
        }

    private fun PlacementScope.readRulers(rulers: RectRulers): IntRect? {
        val left = rulers.left.current(-1f).roundToInt()
        val top = rulers.top.current(-1f).roundToInt()
        val right = rulers.right.current(-1f).roundToInt()
        val bottom = rulers.bottom.current(-1f).roundToInt()
        if (left == -1 || top == -1 || right == -1 || bottom == -1) {
            return null
        }
        return IntRect(left, top, right, bottom)
    }

    private fun Modifier.rulerToRect(
        ruler: WindowInsetsRulers,
        block: PlacementScope.(IntRect?) -> Unit,
    ): Modifier = layout { m, c ->
        val p = m.measure(c)
        layout(p.width, p.height) {
            p.place(0, 0)
            block(readRulers(ruler.current))
        }
    }

    private class MyLayoutModifierElement(
        val measurePolicy: MeasureScope.(Measurable, Constraints, DelegatableNode) -> MeasureResult
    ) : ModifierNodeElement<MyLayoutModifierNode>() {
        override fun create(): MyLayoutModifierNode = MyLayoutModifierNode(measurePolicy)

        override fun hashCode(): Int = measurePolicy.hashCode()

        override fun equals(other: Any?): Boolean =
            other is MyLayoutModifierElement &&
                other.measurePolicy.hashCode() == measurePolicy.hashCode()

        override fun update(node: MyLayoutModifierNode) {
            node.measurePolicy = measurePolicy
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "layoutWithDelegatableNode"
            value = measurePolicy
        }
    }

    private class MyLayoutModifierNode(
        measurePolicy: MeasureScope.(Measurable, Constraints, DelegatableNode) -> MeasureResult
    ) : Modifier.Node(), LayoutModifierNode {
        var measurePolicy:
            MeasureScope.(Measurable, Constraints, DelegatableNode) -> MeasureResult =
            measurePolicy
            set(value) {
                if (value.hashCode() != field.hashCode()) {
                    field = value
                    requestRemeasure()
                }
            }

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            return measurePolicy.invoke(this, measurable, constraints, this@MyLayoutModifierNode)
        }
    }

    companion object {
        const val WaterfallType = -1
        val InsetsRulerTypes =
            mapOf(
                TappableElement to Type.tappableElement(),
                StatusBars to Type.statusBars(),
                Ime to Type.ime(),
                SystemGestures to Type.systemGestures(),
                MandatorySystemGestures to Type.mandatorySystemGestures(),
                DisplayCutout to Type.displayCutout(),
                NavigationBars to Type.navigationBars(),
                CaptionBar to Type.captionBar(),
                Waterfall to WaterfallType,
            )

        val NormalInsetsTypes =
            intArrayOf(
                Type.tappableElement(),
                Type.statusBars(),
                Type.systemGestures(),
                Type.mandatorySystemGestures(),
                Type.navigationBars(),
                Type.captionBar(),
                Type.displayCutout(),
            )
    }
}
