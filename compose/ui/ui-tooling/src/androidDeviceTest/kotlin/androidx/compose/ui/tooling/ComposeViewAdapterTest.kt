/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.tooling

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation
import androidx.compose.ui.tooling.animation.AnimatedContentComposeAnimation
import androidx.compose.ui.tooling.animation.PreviewAnimationClock
import androidx.compose.ui.tooling.animation.TransitionComposeAnimation
import androidx.compose.ui.tooling.animation.UnsupportedComposeAnimation
import androidx.compose.ui.tooling.data.CallGroup
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import androidx.compose.ui.tooling.test.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tolerance, in pixels, for heights computed by adding up dimensions rounded to pixels. */
private const val EXPAND_ROUNDING_TOLERANCE_PX = 2

@MediumTest
@OptIn(UiToolingDataApi::class)
class ComposeViewAdapterTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule =
        androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)

    private lateinit var composeViewAdapter: ComposeViewAdapter

    @Before
    fun setup() {
        composeViewAdapter = activityTestRule.activity.findViewById(R.id.compose_view_adapter)
    }

    @After
    fun tearDown() {
        isAnimationPreviewEnabled = false
        AnimateXAsStateComposeAnimation.testOverrideAvailability(true)
        observedWearReduceMotion = null
        observedWrapperWearReduceMotion = null
        expandAutoCenteringScalingLazyListState = null
    }

    /** Asserts that the given Composable method executes correct and outputs some [ViewInfo]s. */
    private fun assertRendersCorrectly(
        className: String,
        methodName: String,
        previewWrapperProvider: Class<out PreviewWrapperProvider>? = null,
        lookaheadAnimationVisualDebuggingEnabled: Boolean = false,
        lookaheadAnimationVisualDebuggingKeyLabelEnabled: Boolean = false,
        device: String? = null,
    ): List<ViewInfo> {
        initAndWaitForDraw(
            className,
            methodName,
            previewWrapperProvider = previewWrapperProvider,
            lookaheadAnimationVisualDebuggingEnabled = lookaheadAnimationVisualDebuggingEnabled,
            lookaheadAnimationVisualDebuggingKeyLabelEnabled =
                lookaheadAnimationVisualDebuggingKeyLabelEnabled,
            device = device,
        )
        activityTestRule.runOnUiThread { assertTrue(composeViewAdapter.viewInfos.isNotEmpty()) }

        return composeViewAdapter.viewInfos
    }

    /**
     * Initiates the given Composable method and waits for the [ComposeViewAdapter.onDraw] callback.
     */
    private fun initAndWaitForDraw(
        className: String,
        methodName: String,
        designInfoProvidersArgument: String? = null,
        previewWrapperProvider: Class<out PreviewWrapperProvider>? = null,
        lookaheadAnimationVisualDebuggingEnabled: Boolean = false,
        lookaheadAnimationVisualDebuggingKeyLabelEnabled: Boolean = false,
        device: String? = null,
    ) {
        val committedAndDrawn = CountDownLatch(1)
        val committed = AtomicBoolean(false)
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                className,
                methodName,
                debugViewInfos = true,
                lookForDesignInfoProviders = true,
                designInfoProvidersArgument = designInfoProvidersArgument,
                previewWrapperProvider = previewWrapperProvider,
                device = device,
                onCommit = { committed.set(true) },
                onDraw = {
                    if (committed.get()) {
                        committedAndDrawn.countDown()
                    }
                },
                lookaheadAnimationVisualDebuggingEnabled = lookaheadAnimationVisualDebuggingEnabled,
                lookaheadAnimationVisualDebuggingKeyLabelEnabled =
                    lookaheadAnimationVisualDebuggingKeyLabelEnabled,
            )
        }

        // Workaround for a problem described in b/174291742 where onLayout will not be called
        // after composition for the first test in the test suite.
        activityTestRule.runOnUiThread { composeViewAdapter.requestLayout() }

        // Wait for the first draw after the Composable has been committed.
        committedAndDrawn.await()
    }

    @Test
    fun sharedTransitionWithDebuggingRendersCorrectly() {
        val className = "androidx.compose.ui.tooling.SharedTransitionPreviewKt"
        assertRendersCorrectly(className, "PreviewWithSharedElement")

        assertRendersCorrectly(className, "PreviewWithDebuggingEnabled")

        assertRendersCorrectly(
            className,
            "PreviewWithSharedElement",
            lookaheadAnimationVisualDebuggingEnabled = true,
        )

        assertRendersCorrectly(
            className,
            "PreviewWithDebuggingEnabled",
            lookaheadAnimationVisualDebuggingEnabled = true,
        )

        assertRendersCorrectly(
            className,
            "PreviewWithSharedElement",
            lookaheadAnimationVisualDebuggingEnabled = true,
            lookaheadAnimationVisualDebuggingKeyLabelEnabled = true,
        )

        assertRendersCorrectly(
            className,
            "PreviewWithDebuggingEnabled",
            lookaheadAnimationVisualDebuggingEnabled = true,
            lookaheadAnimationVisualDebuggingKeyLabelEnabled = true,
        )
    }

    @Test
    fun sharedTransitionRendersCorrectlyWithDebugging() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SharedTransitionPreviewKt",
            "PreviewWithSharedElement",
            lookaheadAnimationVisualDebuggingEnabled = true,
        )
    }

    @Test
    fun instantiateComposeViewAdapter() {
        val viewInfos =
            assertRendersCorrectly(
                    "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
                    "SimpleComposablePreview",
                )
                .flatMap { it.allChildren() + it }
                .filter { it.fileName == "SimpleComposablePreview.kt" }
                .toList()

        activityTestRule.runOnUiThread {
            assertTrue(viewInfos.isNotEmpty())
            // Verify that valid line numbers are being recorded
            assertTrue(viewInfos.map { it.lineNumber }.all { it > 0 })
            // Verify that this composable has no animations
            assertFalse(composeViewAdapter.hasAnimations())
        }
    }

    @Test
    fun complexTreeStitchLazyColumn() {
        run {
            val viewInfos =
                assertRendersCorrectly(
                    "androidx.compose.ui.tooling.LazyColumnPreviewKt",
                    "ComplexLazyComposablePreview",
                )

            assertEquals(1, viewInfos.size)
            assertEquals(
                """
                |<root>
                .|LazyColumnPreview.kt:37
                ..|LazyColumnPreview.kt:38
                ..|LazyColumnPreview.kt:38
                ...|LazyColumnPreview.kt:41
                ...|LazyColumnPreview.kt:42
                ...|LazyColumnPreview.kt:42
                ....|LazyColumnPreview.kt:42
                ....|LazyColumnPreview.kt:42
                ....|LazyColumnPreview.kt:42
                ....|LazyColumnPreview.kt:42
                ....|LazyColumnPreview.kt:42
                .....|LazyColumnPreview.kt:42
                ....|LazyColumnPreview.kt:42
                .....|LazyColumnPreview.kt:42
                """
                    .trimIndent(),
                viewInfos.toDebugString() { it.fileName == "LazyColumnPreview.kt" }.trimIndent(),
            )
        }
    }

    @Test
    fun animatedVisibilityIsTracked() {
        val clock = PreviewAnimationClock({}, {})

        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestAnimationPreviewKt",
                "AnimatedVisibilityPreview",
            )
            composeViewAdapter.clock = clock
            assertFalse(composeViewAdapter.hasAnimations())
            assertTrue(clock.animationClocks.isEmpty())
        }

        waitFor(1, TimeUnit.SECONDS) {
            // Handle the case where onLayout was called too soon. Calling requestLayout will
            // make sure onLayout will be called again.
            composeViewAdapter.requestLayout()
            composeViewAdapter.hasAnimations()
        }

        activityTestRule.runOnUiThread {
            val animation = clock.animationClocks.values.single().animation
            assertEquals("My Animated Visibility", animation.label)
        }
    }

    @Test
    fun transitionAnimatedVisibilityIsTrackedAsTransition() {
        checkAnimationsAreSubscribed(
            "TransitionAnimatedVisibilityPreview",
            emptyList(),
            listOf("transition.AV"),
        )
    }

    @Test
    fun animatedContentIsSubscribed() {
        checkAnimationsAreSubscribed(
            "AnimatedContentPreview",
            supported = listOf("AnimatedContent"),
        )
    }

    @Test
    fun animatedContentAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "AnimatedContentAndTransitionPreview",
            supported = listOf("checkBoxAnim", "AnimatedContent"),
        )
    }

    @Test
    fun transitionAnimationsAreSubscribedToTheClock() {
        checkAnimationsAreSubscribed("TransitionPreview", emptyList(), listOf("checkBoxAnim"))
    }

    @Test
    fun transitionAnimationsWithSubcomposition() {
        checkAnimationsAreSubscribed(
            "TransitionWithScaffoldPreview",
            emptyList(),
            listOf("checkBoxAnim"),
        )
    }

    @Test
    fun animateXAsStateIsSubscribed() {
        checkAnimationsAreSubscribed(
            "AnimateAsStatePreview",
            supported = listOf("DpAnimation", "IntAnimation"),
        )
    }

    @Test
    fun animateXAsStateIsNotSubscribed() {
        AnimateXAsStateComposeAnimation.testOverrideAvailability(false)
        checkAnimationsAreSubscribed(
            "AllAnimations",
            unsupported = listOf("animateContentSize", "TargetBasedAnimation", "DecayAnimation"),
            supported =
                listOf("checkBoxAnim", "Crossfade", "InfiniteTransition", "AnimatedContent"),
        )
        AnimateXAsStateComposeAnimation.testOverrideAvailability(true)
    }

    @Test
    fun animateContentSizeIsNotSubscribed() {
        checkAnimationsAreSubscribed("AnimateContentSizePreview")
    }

    @Test
    fun animateContentSizeAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "AnimateContentSizeAndTransitionPreview",
            listOf("animateContentSize"),
            listOf("checkBoxAnim"),
        )
    }

    @Test
    fun crossFadeIsSubscribed() {
        checkAnimationsAreSubscribed("CrossFadePreview", emptyList(), listOf("Crossfade"))
    }

    @Test
    fun targetBasedAnimationIsNotSubscribed() {
        checkAnimationsAreSubscribed("TargetBasedAnimationPreview")
    }

    @Test
    fun decayAnimationIsNotSubscribed() {
        checkAnimationsAreSubscribed("DecayAnimationPreview")
    }

    @Test
    fun infiniteTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "InfiniteTransitionPreview",
            supported = listOf("InfiniteTransition"),
        )
    }

    @Test
    fun targetBasedAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "TargetBasedAndTransitionPreview",
            listOf("TargetBasedAnimation"),
            listOf("checkBoxAnim"),
        )
    }

    @Test
    fun decayAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "DecayAndTransitionPreview",
            listOf("DecayAnimation"),
            listOf("checkBoxAnim"),
        )
    }

    @Test
    fun infiniteAndTransitionIsSubscribed() {
        checkAnimationsAreSubscribed(
            "InfiniteAndTransitionPreview",
            supported = listOf("checkBoxAnim", "InfiniteTransition"),
        )
    }

    @Test
    fun unsupportedAreNotSubscribedWhenEnumIsNotAvailable() {
        UnsupportedComposeAnimation.testOverrideAvailability(false)
        checkAnimationsAreSubscribed(
            "AllAnimations",
            unsupported = emptyList(),
            supported =
                listOf(
                    "checkBoxAnim",
                    "Crossfade",
                    "DpAnimation",
                    "IntAnimation",
                    "InfiniteTransition",
                    "AnimatedContent",
                ),
        )
        UnsupportedComposeAnimation.testOverrideAvailability(true)
    }

    @Test
    fun animationsAreOrdered() {
        checkAnimationsAreSubscribed(
            "AnimationOrder",
            emptyList(),
            listOf("transitionOne", "transitionTwo", "transitionThree"),
        )
    }

    @Test
    fun materialAnimationsAreSubscribed() {
        checkAnimationsAreSubscribed(
            "MaterialPreview",
            unsupported = emptyList(),
            supported =
                listOf("ToggleableState", "ColorAnimation", "ColorAnimation", "ColorAnimation"),
        )
    }

    @Test
    fun animatedContentAndAnimateContentSizeWithTrigger() {
        isAnimationPreviewEnabled = true
        checkAnimationsAreSubscribed(
            "AnimatedContentAndAnimateContentSizeWithTrigger",
            supported = listOf("animation"),
            unsupported = listOf("animateContentSize"),
            triggers = listOf("customState"),
        )
    }

    @Test
    fun animatedVisibilityWithTrigger() {
        isAnimationPreviewEnabled = true
        checkAnimationsAreSubscribed(
            "AnimatedVisibilityWithTrigger",
            supported = listOf("animation"),
            triggers = listOf("customState"),
        )
    }

    @Test
    fun animateValueAsStateWithTrigger() {
        isAnimationPreviewEnabled = true
        checkAnimationsAreSubscribed(
            "AnimateValueAsStateWithTrigger",
            supported = listOf("animation1", "animation2"),
            triggers = listOf("customState"),
        )
    }

    @Test
    fun crossFadeWithTrigger() {
        isAnimationPreviewEnabled = true
        checkAnimationsAreSubscribed(
            "CrossFadeWithTrigger",
            supported = listOf("animation"),
            triggers = listOf("customState"),
        )
    }

    @Test
    fun updateTransitionWithTrigger() {
        isAnimationPreviewEnabled = true
        checkAnimationsAreSubscribed(
            "UpdateTransitionWithTrigger",
            supported = listOf("animation"),
            triggers = listOf("customState"),
        )
    }

    @Test
    fun animatedContentExtensionWithTrigger() {
        isAnimationPreviewEnabled = true
        checkAnimationsAreSubscribed(
            "AnimatedContentExtensionWithTrigger",
            supported = listOf("animation"),
            triggers = listOf("customState"),
        )
    }

    @Test
    fun animationsWithManyTriggers() {
        isAnimationPreviewEnabled = true
        checkAnimationsAreSubscribed(
            "AnimationsWithManyTriggers",
            supported = listOf("animation1", "animation2", "animation3"),
            triggers = listOf("customState", "intState", "dataState"),
        )

        composeViewAdapter.clock.triggersToTrack.values
            .first { it.label == "customState" }
            .let {
                assertEquals(true, it.initialState)
                assertEquals(false, it.targetState)
                assertEquals(setOf(true, false), it.states)
            }

        composeViewAdapter.clock.triggersToTrack.values
            .first { it.label == "intState" }
            .let {
                assertEquals(1, it.initialState)
                assertEquals(2, it.targetState)
                assertEquals(setOf(2, 3, 4, 1), it.states)
            }

        composeViewAdapter.clock.triggersToTrack.values
            .first { it.label == "dataState" }
            .let {
                assertEquals(TestTrigger(1), it.initialState)
                assertEquals(TestTrigger(2), it.targetState)
                assertEquals(
                    setOf(TestTrigger(2), TestTrigger(3), TestTrigger(4), TestTrigger(1)),
                    it.states,
                )
            }

        composeViewAdapter.clock.animationClocks.keys
            .first { it.label == "animation1" }
            .let {
                it as TransitionComposeAnimation<*>
                assertEquals(null, it.initialState)
                assertEquals(null, it.targetState)
            }

        composeViewAdapter.clock.animationClocks.keys
            .first { it.label == "animation2" }
            .let {
                it as AnimateXAsStateComposeAnimation<*, *>
                assertEquals(null, it.initialState)
                assertEquals(null, it.targetState)
            }

        composeViewAdapter.clock.animationClocks.keys
            .first { it.label == "animation3" }
            .let {
                it as AnimatedContentComposeAnimation<*>
                assertEquals(null, it.initialState)
                assertEquals(null, it.targetState)
            }
    }

    private fun checkAnimationsAreSubscribed(
        preview: String,
        unsupported: List<String> = emptyList(),
        supported: List<String> = emptyList(),
        triggers: List<String> = emptyList(),
    ) {
        lateinit var clock: PreviewAnimationClock

        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestAnimationPreviewKt",
                preview,
                animationClockStartTime = 0,
            )
        }

        waitFor(5, TimeUnit.SECONDS) {
            // Handle the case where onLayout was called too soon. Calling requestLayout will
            // make sure onLayout will be called again.
            composeViewAdapter.requestLayout()
            composeViewAdapter.clockInitialized
        }

        activityTestRule.runOnUiThread { clock = composeViewAdapter.clock }

        waitFor(5, TimeUnit.SECONDS) {
            // Handle the case where onLayout was called too soon. Calling requestLayout will
            // make sure onLayout will be called again.
            composeViewAdapter.requestLayout()
            composeViewAdapter.hasAnimations()
        }

        activityTestRule.runOnUiThread {
            assertEquals(unsupported, clock.trackedUnsupportedAnimations.map { it.label })
            assertEquals(supported, clock.animationClocks.values.map { it.animation.label })
            assertEquals(triggers, clock.triggersToTrack.values.map { it.label })
        }
    }

    @Test
    fun animationIsFoundWithoutClock() {
        findAnimationWithoutClock("AllAnimations")
    }

    @Test
    fun materialAnimationIsFoundWithoutClock() {
        findAnimationWithoutClock("MaterialPreview")
    }

    private fun findAnimationWithoutClock(preview: String) {
        activityTestRule.runOnUiThread {
            composeViewAdapter.init("androidx.compose.ui.tooling.TestAnimationPreviewKt", preview)
            assertFalse(composeViewAdapter.hasAnimations())
        }

        waitFor(5, TimeUnit.SECONDS) {
            // Handle the case where onLayout was called too soon. Calling requestLayout will
            // make sure onLayout will be called again.
            composeViewAdapter.requestLayout()
            composeViewAdapter.hasAnimations()
        }
        assertTrue(composeViewAdapter.hasAnimations())
    }

    @Test
    fun lineNumberMapping() {
        val viewInfos =
            assertRendersCorrectly(
                    "androidx.compose.ui.tooling.LineNumberPreviewKt",
                    "LineNumberPreview",
                )
                .flatMap { it.allChildren() + it }
                .filter { it.fileName == "LineNumberPreview.kt" }
                .toList()

        activityTestRule.runOnUiThread {
            // Verify all calls, generate the correct line number information
            assertArrayEquals(
                arrayOf(35, 36, 37, 39, 42, 43, 44),
                viewInfos.map { it.lineNumber }.sorted().distinct().toTypedArray(),
            )
        }
    }

    //    @Test
    fun lineNumberLocationMapping() {
        val viewInfos =
            assertRendersCorrectly(
                    "androidx.compose.ui.tooling.LineNumberPreviewKt",
                    "LineNumberPreview",
                )
                .flatMap { it.allChildren() + it }
                .filter { it.location?.let { it.sourceFile == "LineNumberPreview.kt" } == true }
                .toList()

        activityTestRule.runOnUiThread {
            // Verify all calls, generate the correct line number information
            val lines = viewInfos.map { it.location?.lineNumber ?: -1 }.sorted().toTypedArray()
            assertArrayEquals(arrayOf(36, 37, 38, 40, 40, 40, 43, 44, 44, 45, 45), lines)

            // Verify that all calls generate the correct offset information
            val offsets = viewInfos.map { it.location?.offset ?: -1 }.sorted().toTypedArray()
            assertArrayEquals(
                arrayOf(1235, 1272, 1293, 1421, 1421, 1421, 1469, 1491, 1508, 1531, 1548),
                offsets,
            )
        }
    }

    @Test
    fun instantiatePrivateComposeViewAdapter() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "PrivateSimpleComposablePreview",
        )
    }

    @Test
    fun defaultParametersComposableTest1() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview1",
        )
    }

    @Test
    fun defaultParametersComposableTest2() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview2",
        )
    }

    @Test
    fun defaultParametersComposableTest3() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview3",
        )
    }

    /**
     * Verifies the use of inline classes as preview default parameters. Methods with inline classes
     * as parameters will get the name mangled so we need to ensure we invoke correctly the right
     * method.
     */
    @Test
    fun defaultParametersComposableTest4() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview4",
        )
    }

    @Test
    fun PreviewParametersComposableTest() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "PreviewParametersComposablePreview",
        )
    }

    @Test
    fun previewInClass() {
        assertRendersCorrectly("androidx.compose.ui.tooling.TestGroup", "InClassPreview")
    }

    @Test
    fun lifecycleUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "LifecyclePreview",
        )
    }

    @Test
    fun saveableStateRegistryUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "SaveableStateRegistryPreview",
        )
    }

    @Test
    fun onBackPressedDispatcherUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "OnBackPressedDispatcherPreview",
        )
    }

    @Test
    fun activityResultRegistryUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "ActivityResultRegistryPreview",
        )
    }

    @Test
    fun viewModelPreviewRendersCorrectly() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "ViewModelPreview",
        )
    }

    @Test
    fun multipreviewTest() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "Multipreview",
        )
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "MultiPreviews",
        )
    }

    /** Check that no re-composition happens without forcing it. */
    @LargeTest
    @Test
    fun testNoInvalidation() {
        compositionCount.set(0)
        var onDrawCounter = 0
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestInvalidationPreviewKt",
                "CounterPreview",
                onDraw = { onDrawCounter++ },
            )
        }

        // API before 29, might issue an additional draw under testing.
        val expectedDrawCount = if (Build.VERSION.SDK_INT < 29) 2 else 1
        repeat(5) {
            activityTestRule.runOnUiThread {
                assertEquals(1, compositionCount.get())
                assertTrue(
                    "At most, $expectedDrawCount draw is expected ($onDrawCounter happened)",
                    onDrawCounter <= expectedDrawCount,
                )
            }
            Thread.sleep(250)
        }
    }

    @Test
    fun simpleDesignInfoProviderTest() {
        checkDesignInfoList("DesignInfoProviderA", "A", "ObjectA, x=0, y=0")
        checkDesignInfoList("DesignInfoProviderA", "B", "Invalid, x=0, y=0")

        checkDesignInfoList("DesignInfoProviderB", "A", "Invalid, x=0, y=0")
        checkDesignInfoList("DesignInfoProviderB", "B", "ObjectB, x=0, y=0")
    }

    @Test
    fun designInfoProviderCoordinatesTest() {
        val group =
            CallGroup(
                key = null,
                name = null,
                location = null,
                identity = null,
                box = IntRect(10, 20, 100, 200),
                parameters = emptyList(),
                data =
                    listOf(
                        object {
                            @Suppress("UNUSED")
                            fun getDesignInfo(x: Int, y: Int, args: String): String = "x=$x, y=$y"
                        }
                    ),
                children = emptyList(),
                isInline = false,
            )
        val method =
            ComposeViewAdapter::class
                .java
                .getDeclaredMethod("getDesignInfoOrNull", Group::class.java, IntRect::class.java)
                .apply { isAccessible = true }

        val result = method.invoke(composeViewAdapter, group, group.box) as? String
        assertEquals("x=10, y=20", result)
    }

    @Test
    fun testPreviewWrapper() {
        val viewInfos =
            assertRendersCorrectly(
                "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
                "TestWrapperPreview",
                previewWrapperProvider = TestWrapper::class.java,
            )

        activityTestRule.runOnUiThread {
            assertTrue(viewInfos.isNotEmpty())
            // Verify that the wrapper (WrapperContainer) is present.
            val wrapperInfo =
                viewInfos
                    .flatMap { it.allChildren() + it }
                    .find {
                        it.name == "WrapperContainer" && it.fileName == "SimpleComposablePreview.kt"
                    }
            assertTrue("WrapperContainer from wrapper should be present", wrapperInfo != null)

            // Verify it has children (Header, Content, Footer)
            // Content is the SimpleComposablePreview which has a Surface
            // Header and Footer are Text
            assertTrue((wrapperInfo?.children?.size ?: 0) > 0)
        }
    }

    @Test
    fun testPreviewWrapperWithCompositionLocal() {
        val viewInfos =
            assertRendersCorrectly(
                "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
                "TestCompositionLocalWrapperPreview",
                previewWrapperProvider = TestCompositionLocalWrapper::class.java,
            )

        activityTestRule.runOnUiThread {
            assertTrue(viewInfos.isNotEmpty())
            // If the preview rendered successfully without crashing from the
            // IllegalArgumentException we know the CompositionLocal was correctly provided.
            val previewNode =
                viewInfos
                    .flatMap { it.allChildren() + it }
                    .find { it.name == "Text" && it.fileName == "SimpleComposablePreview.kt" }
            assertTrue("TestCompositionLocalWrapperPreview should be present", previewNode != null)
        }
    }

    @Test
    fun subcompositionDesignInfoProviderTest() {
        checkDesignInfoList("ScaffoldDesignInfoProvider", "A", "ObjectA, x=0, y=0")
    }

    @Test
    fun testFakeOnBackPressedDispatcherOwnerExistsInComposeViewAdapter() {
        val composeViewAdapterClass = ComposeViewAdapter::class.java
        val field = composeViewAdapterClass.getDeclaredField("FakeOnBackPressedDispatcherOwner")
        field.isAccessible = true
        val fakeOnBackPressedDispatcherOwner = field.get(composeViewAdapter)

        val actualMethods =
            fakeOnBackPressedDispatcherOwner.javaClass.declaredMethods
                .map { method ->
                    val params = method.parameterTypes.joinToString(",") { it.simpleName }
                    "${method.name}($params): ${method.returnType.simpleName}"
                }
                .toSet()

        val expectedMethods =
            listOf(
                // Back navigation APIs
                "canBackPress(): boolean",
                "onBackPressStarted(String): void",
                "onBackPressProgress(float,String): void",
                "onBackPressCompleted(): void",
                "onBackPressCancelled(): void",
                // Forward navigation APIs
                "canForwardPress(): boolean",
                "onForwardPressStarted(String): void",
                "onForwardPressProgress(float,String): void",
                "onForwardPressCompleted(): void",
                "onForwardPressCancelled(): void",
                // History navigation APIs
                "getHistory(): List",
                "getCurrentIndex(): int",
                "backToState(Object): boolean",
            )

        for (expectedMethod in expectedMethods) {
            assertTrue(
                "Method '$expectedMethod' should be present in FakeOnBackPressedDispatcherOwner",
                actualMethods.contains(expectedMethod),
            )
        }
    }

    /**
     * Initializes [methodName] with the given [device] spec (by default, one that expands
     * vertically) and returns the height of the adapter when measured with an UNSPECIFIED height
     * spec of size [viewportHeightPx], as LayoutLib does in V_SCROLL rendering mode.
     */
    private fun measureExpanded(
        methodName: String,
        viewportHeightPx: Int,
        device: String = EXPAND_VERTICAL_DEVICE,
        heightMode: Int = View.MeasureSpec.UNSPECIFIED,
        className: String = "androidx.compose.ui.tooling.ExpandListsPreviewKt",
    ): Int {
        initAndWaitForDraw(
            className,
            methodName,
            device = device,
        )
        var measuredHeight = -1
        activityTestRule.runOnUiThread {
            composeViewAdapter.measure(
                View.MeasureSpec.makeMeasureSpec(
                    composeViewAdapter.width,
                    View.MeasureSpec.EXACTLY,
                ),
                View.MeasureSpec.makeMeasureSpec(viewportHeightPx, heightMode),
            )
            measuredHeight = composeViewAdapter.measuredHeight
        }
        return measuredHeight
    }

    private fun Dp.toPx(): Int = Density(activityTestRule.activity).run { this@toPx.roundToPx() }

    @Test
    fun expandVertical_longLazyColumnInScaffold_growsToFitAllItems() {
        val expectedHeight =
            EXPAND_ITEM_HEIGHT.toPx() * EXPAND_LONG_LIST_ITEM_COUNT +
                EXPAND_BOTTOM_BAR_HEIGHT.toPx()
        val height =
            measureExpanded("ExpandLongLazyColumnInScaffoldPreview", viewportHeightPx = 100)
        assertEquals(expectedHeight, height)
    }

    @Test
    fun expandVertical_shortLazyColumnInScaffold_shrinksToContent() {
        val expectedHeight =
            EXPAND_ITEM_HEIGHT.toPx() * EXPAND_SHORT_LIST_ITEM_COUNT +
                EXPAND_BOTTOM_BAR_HEIGHT.toPx()
        // With a viewport of 0, a fillMaxSize() Scaffold must not expand to the max height.
        val height = measureExpanded("ExpandShortLazyColumnInScaffoldPreview", viewportHeightPx = 0)
        assertEquals(expectedHeight, height)
    }

    @Test
    fun expandVertical_shortLazyColumnInScaffold_neverSmallerThanViewport() {
        val viewportHeight = EXPAND_ITEM_HEIGHT.toPx() * (EXPAND_SHORT_LIST_ITEM_COUNT + 5)
        val height =
            measureExpanded(
                "ExpandShortLazyColumnInScaffoldPreview",
                viewportHeightPx = viewportHeight,
            )
        assertEquals(viewportHeight, height)
    }

    @Test
    fun expandVertical_verticalScrollColumn_growsToFitAllItems() {
        val expectedHeight = EXPAND_ITEM_HEIGHT.toPx() * EXPAND_LONG_LIST_ITEM_COUNT
        val height = measureExpanded("ExpandVerticalScrollColumnPreview", viewportHeightPx = 100)
        assertEquals(expectedHeight, height)
    }

    @Test
    fun expandVertical_heightIsCappedAtMaxHeight() {
        val height =
            measureExpanded(
                "ExpandLongLazyColumnInScaffoldPreview",
                viewportHeightPx = 100,
                device = "$EXPAND_VERTICAL_DEVICE,maxHeight=300px",
            )
        assertEquals(300, height)
    }

    @Test
    fun expandVertical_heightIsCappedAtMaxHeightInDp() {
        val height =
            measureExpanded(
                "ExpandLongLazyColumnInScaffoldPreview",
                viewportHeightPx = 100,
                device = "$EXPAND_VERTICAL_DEVICE,maxHeight=150dp",
            )
        assertEquals(150.dp.toPx(), height)
    }

    @Test
    fun expandVertical_maxWidthIsIgnored() {
        val expectedHeight = EXPAND_ITEM_HEIGHT.toPx() * EXPAND_LONG_LIST_ITEM_COUNT
        val height =
            measureExpanded(
                "ExpandVerticalScrollColumnPreview",
                viewportHeightPx = 100,
                device = "$EXPAND_VERTICAL_DEVICE,maxWidth=300px",
            )
        assertEquals(expectedHeight, height)
    }

    @Test
    fun noExpand_unspecifiedHeight_isNotExpanded() {
        val viewportHeight = EXPAND_ITEM_HEIGHT.toPx() * 3
        val expandedHeight =
            measureExpanded("ExpandFixedHeightPreview", viewportHeightPx = viewportHeight)
        // When expanded, the adapter is never smaller than the viewport.
        assertEquals(viewportHeight, expandedHeight)
        // Otherwise, it wraps its content, as with any other UNSPECIFIED spec.
        val height =
            measureExpanded(
                "ExpandFixedHeightPreview",
                viewportHeightPx = viewportHeight,
                device = "spec:width=411dp,height=891dp",
            )
        assertEquals(EXPAND_ITEM_HEIGHT.toPx(), height)
    }

    @Test
    fun expandVertical_mediaQueriesSeeTheScreenHeight() {
        val expectedHeight = activityTestRule.activity.resources.configuration.screenHeightDp.dp
        observedMediaQueryWindowHeight = null
        // Use a spec without an explicit height (spec:parent=...) so the fallback screen height is
        // reported.
        measureExpanded(
            "MediaQueryHeightPreview",
            viewportHeightPx = 100,
            device = "spec:parent=pixel_7,expand=vertical",
        )
        activityTestRule.runOnUiThread {
            assertEquals(expectedHeight, observedMediaQueryWindowHeight)
        }

        // When the spec specifies an explicit height, that height takes precedence over the
        // host screen height.
        observedMediaQueryWindowHeight = null
        measureExpanded(
            "MediaQueryHeightPreview",
            viewportHeightPx = 100,
            device = "spec:width=411dp,height=900dp,expand=vertical",
        )
        activityTestRule.runOnUiThread {
            assertEquals(900.dp, observedMediaQueryWindowHeight)
        }
    }

    @Test
    fun expandVertical_exactlySpecIsHonored() {
        val height =
            measureExpanded(
                "ExpandLongLazyColumnInScaffoldPreview",
                viewportHeightPx = 250,
                heightMode = View.MeasureSpec.EXACTLY,
            )
        assertEquals(250, height)
    }

    @Test
    fun expandVertical_repeatedRenderMeasures_retainsExpandedHeightWithoutRecomputing() {
        val expectedHeight = EXPAND_ITEM_HEIGHT.toPx() * EXPAND_LONG_LIST_ITEM_COUNT
        val height = measureExpanded("ExpandVerticalScrollColumnPreview", viewportHeightPx = 100)
        assertEquals(expectedHeight, height)

        // Simulate LayoutLib's subsequent RenderSessionImpl.render() passes, which measure the
        // root view with EXACTLY (pair11) followed by the content root with UNSPECIFIED (pair12).
        // Both passes must return the cached expanded height so calcSize does not add
        // (pair12 - pair11) again or shrink the view back to the viewport height.
        activityTestRule.runOnUiThread {
            val widthSpec =
                View.MeasureSpec.makeMeasureSpec(
                    composeViewAdapter.width,
                    View.MeasureSpec.EXACTLY,
                )
            composeViewAdapter.measure(
                widthSpec,
                View.MeasureSpec.makeMeasureSpec(454, View.MeasureSpec.EXACTLY),
            )
            assertEquals(expectedHeight, composeViewAdapter.measuredHeight)

            composeViewAdapter.measure(
                widthSpec,
                View.MeasureSpec.makeMeasureSpec(454, View.MeasureSpec.UNSPECIFIED),
            )
            assertEquals(expectedHeight, composeViewAdapter.measuredHeight)

            // Measuring with a different width invalidates the cached expanded height, so an
            // EXACTLY height spec is honored and an UNSPECIFIED height spec re-expands for the new
            // width.
            val narrowerWidthSpec =
                View.MeasureSpec.makeMeasureSpec(
                    composeViewAdapter.width / 2,
                    View.MeasureSpec.EXACTLY,
                )
            composeViewAdapter.measure(
                narrowerWidthSpec,
                View.MeasureSpec.makeMeasureSpec(454, View.MeasureSpec.EXACTLY),
            )
            assertEquals(454, composeViewAdapter.measuredHeight)

            composeViewAdapter.measure(
                narrowerWidthSpec,
                View.MeasureSpec.makeMeasureSpec(454, View.MeasureSpec.UNSPECIFIED),
            )
            assertEquals(expectedHeight, composeViewAdapter.measuredHeight)
        }
    }

    @Test
    fun expandVertical_providesWearReduceMotion() {
        observedWearReduceMotion = null
        initAndWaitForDraw(
            "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            "WearReduceMotionPreview",
            device = EXPAND_VERTICAL_DEVICE,
        )
        activityTestRule.runOnUiThread { assertEquals(true, observedWearReduceMotion) }
    }

    @Test
    fun expandVertical_providesWearReduceMotion_withPreviewWrapper() {
        observedWearReduceMotion = null
        observedWrapperWearReduceMotion = null
        initAndWaitForDraw(
            "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            "WearReduceMotionPreview",
            previewWrapperProvider = WearReduceMotionObservingWrapper::class.java,
            device = EXPAND_VERTICAL_DEVICE,
        )
        // The value is provided around the user's PreviewWrapperProvider, so both the wrapper and
        // the preview inside it see it.
        activityTestRule.runOnUiThread {
            assertEquals(true, observedWrapperWearReduceMotion)
            assertEquals(true, observedWearReduceMotion)
        }
    }

    @Test
    fun noExpand_doesNotProvideWearReduceMotion() {
        observedWearReduceMotion = null
        initAndWaitForDraw(
            "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            "WearReduceMotionPreview",
            device = "spec:width=411dp,height=891dp",
        )
        // The system "remove animations" setting is off on test devices.
        activityTestRule.runOnUiThread { assertEquals(false, observedWearReduceMotion) }
    }

    @Test
    fun findWearLocalReduceMotion_returnsNullWithoutWear() {
        // A class loader that can't load anything besides the bootstrap classes.
        val bootstrapClassLoader = ClassLoader.getSystemClassLoader().parent
        assertEquals(null, findWearLocalReduceMotion(bootstrapClassLoader))
    }

    @Test
    fun findWearScalingLazyListStateClass_returnsNullWithoutWear() {
        // A class loader that can't load anything besides the bootstrap classes.
        val bootstrapClassLoader = ClassLoader.getSystemClassLoader().parent
        assertNull(findWearScalingLazyListStateClass(bootstrapClassLoader))
    }

    @Test
    fun expandVertical_scalingLazyColumn_growsToFitAllItems() {
        val height =
            measureExpanded(
                "ExpandScalingLazyColumnPreview",
                viewportHeightPx = 100,
                className = "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            )
        // All the items, unscaled, must fit.
        assertTrue(
            "height=$height",
            height >= EXPAND_ITEM_HEIGHT.toPx() * EXPAND_LONG_LIST_ITEM_COUNT,
        )
        assertTrue("height=$height", height < DEFAULT_EXPAND_MAX_SIZE_PX)
    }

    @Test
    fun expandVertical_autoCenteringScalingLazyColumn_collapsesAutoCenteringSpacers() {
        // With auto centering, a ScalingLazyColumn adds top and bottom spacers that grow with the
        // viewport (~half the viewport each). Expanding vertically collapses those spacers to 0px
        // so the list neither grows to the max height nor leaves the top half of the preview empty:
        // its height matches the same list without auto-centering, plus the two 4.dp item gaps
        // adjacent to the 0px spacer items.
        val noAutoCenteringHeight =
            measureExpanded(
                "ExpandScalingLazyColumnPreview",
                viewportHeightPx = 100,
                className = "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            )
        val autoCenteringHeight =
            measureExpanded(
                "ExpandAutoCenteringScalingLazyColumnPreview",
                viewportHeightPx = 100,
                className = "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            )
        val expectedHeight = noAutoCenteringHeight + 2 * 4.dp.toPx()
        assertTrue(
            "autoCenteringHeight=$autoCenteringHeight expected=$expectedHeight",
            abs(autoCenteringHeight - expectedHeight) <= EXPAND_ROUNDING_TOLERANCE_PX,
        )

        // Even when maxHeight is no larger than the viewport (minHeight >= maxHeight), the
        // auto-centering spacers are still collapsed so the first item starts at the top.
        // expandAutoCenteringScalingLazyListState is not reset here: re-initializing the same
        // preview method reuses the existing composition, which skips the preview's body, so the
        // remembered state set by the previous measurement is the one being collapsed.
        val cappedHeight =
            measureExpanded(
                "ExpandAutoCenteringScalingLazyColumnPreview",
                viewportHeightPx = 300,
                device = "$EXPAND_VERTICAL_DEVICE,maxHeight=300px",
                className = "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            )
        assertEquals(300, cappedHeight)
        activityTestRule.runOnUiThread {
            val layoutInfo = expandAutoCenteringScalingLazyListState!!.layoutInfo
            val firstItem = layoutInfo.visibleItemsInfo.first()
            assertEquals(0, firstItem.index)
            // ScalingLazyListItemInfo.offset is the item's center relative to the viewport center.
            val firstItemTop =
                layoutInfo.viewportSize.height / 2 + firstItem.offset - firstItem.unadjustedSize / 2
            assertTrue(
                "firstItemTop=$firstItemTop",
                abs(firstItemTop - 4.dp.toPx()) <= EXPAND_ROUNDING_TOLERANCE_PX,
            )
        }

        // A 2-item auto-centering list does not overflow at maxHeight even with its spacers intact
        // (topSpacer + items + bottomSpacer == viewportHeight); its spacers must still be collapsed
        // so the first item is positioned at the top.
        expandAutoCenteringScalingLazyListState = null
        val shortHeight =
            measureExpanded(
                "ExpandShortAutoCenteringScalingLazyColumnPreview",
                viewportHeightPx = 300,
                className = "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            )
        assertEquals(300, shortHeight)
        activityTestRule.runOnUiThread {
            val layoutInfo = expandAutoCenteringScalingLazyListState!!.layoutInfo
            val firstItem = layoutInfo.visibleItemsInfo.first()
            assertEquals(0, firstItem.index)
            val firstItemTop =
                layoutInfo.viewportSize.height / 2 + firstItem.offset - firstItem.unadjustedSize / 2
            assertTrue(
                "firstItemTop=$firstItemTop",
                abs(firstItemTop - 4.dp.toPx()) <= EXPAND_ROUNDING_TOLERANCE_PX,
            )
        }
    }

    /** Height of [ExpandTransformingLazyColumnWithPaddingPreview]'s content, padding included. */
    private fun transformingLazyColumnWithPaddingHeight(): Int =
        EXPAND_LIST_TOP_PADDING.toPx() +
            EXPAND_ITEM_HEIGHT.toPx() * EXPAND_LONG_LIST_ITEM_COUNT +
            EXPAND_LIST_BOTTOM_PADDING.toPx()

    @Test
    fun expandVertical_transformingLazyColumn_showsAllContentPadding() {
        // Measuring the list at smaller heights during the search scrolls it to its end. That
        // state must not make the list look like it fits before its bottom content padding (e.g.
        // the space reserved for an EdgeButton) is entirely visible.
        val height =
            measureExpanded(
                "ExpandTransformingLazyColumnWithPaddingPreview",
                viewportHeightPx = 100,
                className = "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            )
        val expectedHeight = transformingLazyColumnWithPaddingHeight()
        assertTrue(
            "height=$height expected=$expectedHeight",
            abs(height - expectedHeight) <= EXPAND_ROUNDING_TOLERANCE_PX,
        )
    }

    @Test
    fun expandVertical_autoCenteringListNextToTransformingLazyColumn_fitsBoth() {
        // Collapsing the auto-centering spacers of the ScalingLazyColumn must not prevent the
        // taller TransformingLazyColumn next to it from expanding until its bottom content padding
        // is visible.
        val height =
            measureExpanded(
                "ExpandAutoCenteringAndTransformingLazyColumnsPreview",
                viewportHeightPx = 100,
                className = "androidx.compose.ui.tooling.ExpandWearListsPreviewKt",
            )
        val expectedHeight = transformingLazyColumnWithPaddingHeight()
        assertTrue(
            "height=$height expected=$expectedHeight",
            abs(height - expectedHeight) <= EXPAND_ROUNDING_TOLERANCE_PX,
        )
    }

    private fun checkDesignInfoList(
        methodName: String,
        customArgument: String,
        expectedResult: String,
    ) {
        initAndWaitForDraw(
            "androidx.compose.ui.tooling.DesignInfoProviderComposableKt",
            methodName,
            customArgument,
        )

        activityTestRule.runOnUiThread {
            assertTrue(composeViewAdapter.designInfoList.isNotEmpty())
        }

        assertEquals(1, composeViewAdapter.designInfoList.size)
        assertEquals(expectedResult, composeViewAdapter.designInfoList[0])
    }

    /**
     * Waits for a given condition to be satisfied within a given timeout. Fails the test when
     * timing out. The condition is evaluated on the UI thread.
     */
    private fun waitFor(timeout: Long, timeUnit: TimeUnit, conditionExpression: () -> Boolean) {
        val conditionSatisfied = AtomicBoolean(false)
        val now = System.nanoTime()
        val timeoutNanos = timeUnit.toNanos(timeout)
        while (!conditionSatisfied.get()) {
            activityTestRule.runOnUiThread { conditionSatisfied.set(conditionExpression()) }
            if ((System.nanoTime() - now) > timeoutNanos) {
                // Some previews are expected not to have animations.
                return
            }
            Thread.sleep(200)
        }
    }

    @Test
    fun testMediaQueryPreviewRendersCorrectly() {
        val viewInfos =
            assertRendersCorrectly(
                "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
                "TestMediaQueryPreview",
            )
        activityTestRule.runOnUiThread {
            assertTrue(viewInfos.isNotEmpty())
            val previewNode =
                viewInfos
                    .flatMap { it.allChildren() + it }
                    .find { it.name == "Text" && it.fileName == "SimpleComposablePreview.kt" }
            assertTrue("TestMediaQueryPreview should be present", previewNode != null)
        }
    }

    @Test
    fun testMediaQueryPreviewWithDeviceSpecRendersCorrectly() {
        val viewInfos =
            assertRendersCorrectly(
                "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
                "TestTabletopMediaQueryPreview",
                device = "spec:width=673dp,height=841dp,windowPosture=tabletop",
            )
        activityTestRule.runOnUiThread {
            assertTrue(viewInfos.isNotEmpty())
            val previewNode =
                viewInfos
                    .flatMap { it.allChildren() + it }
                    .find { it.name == "Text" && it.fileName == "SimpleComposablePreview.kt" }
            assertTrue("TestTabletopMediaQueryPreview should be present", previewNode != null)
        }
    }

    companion object {
        class TestActivity : Activity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.compose_adapter_test)
            }
        }
    }
}
