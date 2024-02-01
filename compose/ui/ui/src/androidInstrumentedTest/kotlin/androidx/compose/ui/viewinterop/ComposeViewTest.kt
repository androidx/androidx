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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeViewTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @FlakyTest(bugId = 256017578)
    @Test
    fun composeViewComposedContent() {
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity)
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello, World!", Modifier.testTag("text"))
            }
        }
        Espresso.onView(instanceOf(ComposeView::class.java))
            .check(matches(isDisplayed()))
            .check { view, _ ->
                view as ViewGroup
                assertTrue("has children", view.childCount > 0)
                if (Build.VERSION.SDK_INT >= 23) {
                    assertEquals(
                        "androidx.compose.ui.platform.ComposeView",
                        view.getAccessibilityClassName()
                    )
                }
            }

        rule.onNodeWithTag("text").assertTextEquals("Hello, World!")
    }

    @Test
    fun composeDifferentViewContent() {
        val id = View.generateViewId()
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity).also { it.id = id }
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.onActivity { activity ->
            val composeView: ComposeView = activity.findViewById(id)
            composeView.setContent {
                BasicText("World", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("World")
    }

    @Test
    fun compositionStrategyDisposed() {
        rule.activityRule.scenario.onActivity { activity ->
            var installed = false
            var disposed = false
            val testView = TestComposeView(activity)
            val strategy = object : ViewCompositionStrategy {
                override fun installFor(view: AbstractComposeView): () -> Unit {
                    installed = true
                    assertSame("correct view provided", testView, view)
                    return { disposed = true }
                }
            }
            testView.setViewCompositionStrategy(strategy)
            assertTrue("strategy should be installed", installed)
            assertFalse("strategy should not be disposed", disposed)
            testView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            assertTrue("strategy should be disposed", disposed)
        }
    }

    @Test
    fun disposeOnDetachedDefaultStrategy() {
        rule.activityRule.scenario.onActivity { activity ->
            val testView = TestComposeView(activity)
            assertFalse("should not have composition yet", testView.hasComposition)
            activity.setContentView(testView)
            assertTrue("composition should be created", testView.hasComposition)
            activity.setContentView(View(activity))
            assertFalse("composition should have been disposed on detach", testView.hasComposition)
        }
    }

    @Test
    fun disposeOnLifecycleDestroyedStrategy() {
        var composeViewCapture: ComposeView? = null
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity).also {
                it.setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity)
                )
                composeViewCapture = it
            }
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        assertNotNull("composeViewCapture should not be null", composeViewCapture)
        assertTrue(
            "ComposeView should not have a composition",
            composeViewCapture?.hasComposition == false
        )
    }

    @Test
    fun disposeOnViewTreeLifecycleDestroyedStrategy_setBeforeAttached() {
        var composeViewCapture: ComposeView? = null
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity).also {
                it.setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                composeViewCapture = it
            }
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        assertNotNull("composeViewCapture should not be null", composeViewCapture)
        assertTrue(
            "ComposeView should not have a composition",
            composeViewCapture?.hasComposition == false
        )
    }

    @Test
    fun disposeOnViewTreeLifecycleDestroyedStrategy_setAfterAttached() {
        var composeViewCapture: ComposeView? = null
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity)
            composeViewCapture = composeView

            activity.setContentView(composeView)
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        assertNotNull("composeViewCapture should not be null", composeViewCapture)
        assertTrue(
            "ComposeView should not have a composition",
            composeViewCapture?.hasComposition == false
        )
    }

    @Ignore("Disable Broken test: b/187962859")
    @Test
    fun paddingsAreNotIgnored() {
        var globalBounds = Rect.Zero
        val latch = CountDownLatch(1)
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity)
            composeView.setPadding(10, 20, 30, 40)
            activity.setContentView(composeView, ViewGroup.LayoutParams(100, 100))
            composeView.setContent {
                Box(
                    Modifier
                        .testTag("box")
                        .fillMaxSize()
                        .onGloballyPositioned {
                            val position = IntArray(2)
                            composeView.getLocationOnScreen(position)
                            globalBounds = it
                                .boundsInWindow()
                                .translate(
                                    -position[0].toFloat(), -position[1].toFloat()
                                )
                            latch.countDown()
                        }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(Rect(10f, 20f, 70f, 60f), globalBounds)
    }

    @Test
    fun boundsInWindow() {
        var boundsInWindow = IntRect.Zero
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                with(LocalDensity.current) {
                    Box(
                        Modifier
                            .size(10.toDp())
                            .offset(1.toDp(), 2.toDp())
                            .align(AbsoluteAlignment.TopLeft)
                            .onGloballyPositioned {
                                val rect = it.boundsInWindow()
                                boundsInWindow = IntRect(
                                    rect.left.roundToInt(),
                                    rect.top.roundToInt(),
                                    rect.right.roundToInt(),
                                    rect.bottom.roundToInt()
                                )
                            }
                    )
                }
            }
        }
        rule.waitForIdle()

        var offsetFromRoot = IntOffset.Zero
        rule.activityRule.scenario.onActivity {
            val content = it.findViewById<View>(android.R.id.content)
            val position = IntArray(2)
            content.getLocationInWindow(position)
            offsetFromRoot = IntOffset(position[0], position[1])
        }

        assertEquals(
            IntRect(
                offsetFromRoot.x + 1,
                offsetFromRoot.y + 2,
                offsetFromRoot.x + 11,
                offsetFromRoot.y + 12
            ),
            boundsInWindow
        )
    }

    @Test
    fun boundsInWindowPartiallyObstructed() {
        var boundsInWindow = IntRect.Zero

        // Offset to the left
        var offset by mutableStateOf(IntOffset(-3, 0))
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                with(LocalDensity.current) {
                    Box(
                        Modifier
                            .size(10.toDp())
                            .offset(offset.x.toDp(), offset.y.toDp())
                            .align(AbsoluteAlignment.TopLeft)
                            .onGloballyPositioned {
                                val rect = it.boundsInWindow()
                                boundsInWindow = IntRect(
                                    rect.left.roundToInt(),
                                    rect.top.roundToInt(),
                                    rect.right.roundToInt(),
                                    rect.bottom.roundToInt()
                                )
                            }
                    )
                }
            }
        }
        rule.waitForIdle()

        var offsetFromRoot = IntOffset.Zero
        var rootSize = IntSize.Zero
        rule.activityRule.scenario.onActivity {
            val content = it.findViewById<View>(android.R.id.content)
            val position = IntArray(2)
            content.getLocationInWindow(position)
            offsetFromRoot = IntOffset(position[0], position[1])
            rootSize = IntSize(content.width, content.height)
        }

        assertEquals(
            IntRect(
                offsetFromRoot.x,
                offsetFromRoot.y,
                offsetFromRoot.x + 7,
                offsetFromRoot.y + 10
            ),
            boundsInWindow
        )

        // Offset to the top
        offset = IntOffset(0, -4)
        rule.waitForIdle()
        assertEquals(
            IntRect(
                offsetFromRoot.x,
                offsetFromRoot.y,
                offsetFromRoot.x + 10,
                offsetFromRoot.y + 6
            ),
            boundsInWindow
        )

        // Offset to the right
        offset = IntOffset(rootSize.width - 5, 0)
        rule.waitForIdle()
        assertEquals(
            IntRect(
                offsetFromRoot.x + rootSize.width - 5,
                offsetFromRoot.y,
                offsetFromRoot.x + rootSize.width,
                offsetFromRoot.y + 10
            ),
            boundsInWindow
        )

        // Offset to the bottom
        offset = IntOffset(0, rootSize.height - 6)
        rule.waitForIdle()
        assertEquals(
            IntRect(
                offsetFromRoot.x,
                offsetFromRoot.y + rootSize.height - 6,
                offsetFromRoot.x + 10,
                offsetFromRoot.y + rootSize.height
            ),
            boundsInWindow
        )
    }

    @Test
    fun boundsInWindowFullyObstructed() {
        var boundsInWindow = IntRect.Zero

        // Offset to the left
        var offset by mutableStateOf(IntOffset(-10, 0))
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                with(LocalDensity.current) {
                    Box(
                        Modifier
                            .size(10.toDp())
                            .offset(offset.x.toDp(), offset.y.toDp())
                            .align(AbsoluteAlignment.TopLeft)
                            .onGloballyPositioned {
                                val rect = it.boundsInWindow()
                                boundsInWindow = IntRect(
                                    rect.left.roundToInt(),
                                    rect.top.roundToInt(),
                                    rect.right.roundToInt(),
                                    rect.bottom.roundToInt()
                                )
                            }
                    )
                }
            }
        }
        rule.waitForIdle()

        var rootSize = IntSize.Zero
        rule.activityRule.scenario.onActivity {
            val content = it.findViewById<View>(android.R.id.content)
            val position = IntArray(2)
            content.getLocationInWindow(position)
            rootSize = IntSize(content.width, content.height)
        }

        assertEquals(IntRect.Zero, boundsInWindow)

        // Offset to the top
        offset = IntOffset(0, -10)
        rule.waitForIdle()
        assertEquals(IntRect.Zero, boundsInWindow)

        // Offset to the right
        offset = IntOffset(rootSize.width, 0)
        rule.waitForIdle()
        assertEquals(IntRect.Zero, boundsInWindow)

        // Offset to the bottom
        offset = IntOffset(0, rootSize.height)
        rule.waitForIdle()
        assertEquals(IntRect.Zero, boundsInWindow)
    }

    @Test
    fun viewSizeIsChildSizePlusPaddings() {
        var size = IntSize.Zero
        val latch = CountDownLatch(1)
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity)
            composeView.setPadding(10, 20, 30, 40)
            activity.setContentView(composeView, ViewGroup.LayoutParams(100, 100))
            composeView.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        composeView.viewTreeObserver.removeOnPreDrawListener(this)
                        size = IntSize(composeView.measuredWidth, composeView.measuredHeight)
                        latch.countDown()
                        return true
                    }
                }
            )
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(100, 100), size)
    }

    @Test
    @SmallTest
    fun throwsOnAddView() {
        rule.activityRule.scenario.onActivity { activity ->
            with(TestComposeView(activity)) {
                assertUnsupported("addView(View)") {
                    addView(View(activity))
                }

                assertUnsupported("addView(View, int)") {
                    addView(View(activity), 0)
                }

                assertUnsupported("addView(View, int, int)") {
                    addView(View(activity), 0, 0)
                }

                assertUnsupported("addView(View, LayoutParams)") {
                    addView(View(activity), ViewGroup.LayoutParams(0, 0))
                }

                assertUnsupported("addView(View, int, LayoutParams)") {
                    addView(View(activity), 0, ViewGroup.LayoutParams(0, 0))
                }

                assertUnsupported("addViewInLayout(View, int, LayoutParams)") {
                    addViewInLayout(View(activity), 0, ViewGroup.LayoutParams(0, 0))
                }

                assertUnsupported("addViewInLayout(View, int, LayoutParams, boolean)") {
                    addViewInLayout(View(activity), 0, ViewGroup.LayoutParams(0, 0), false)
                }
            }
        }
    }

    /**
     * Regression test for https://issuetracker.google.com/issues/181463117
     * Ensures that [ComposeView] can be constructed and attached a window even if View calls
     * [View.onRtlPropertiesChanged] in its constructor before subclass constructors run.
     * (AndroidComposeView is sensitive to this.)
     */
    @Test
    @SmallTest
    fun onRtlPropertiesChangedCalledByViewConstructor() {
        var result: Result<Unit>? = null
        rule.activityRule.scenario.onActivity { activity ->
            result = runCatching {
                activity.setContentView(
                    ComposeView(
                        ContextThemeWrapper(activity, R.style.Theme_WithScrollbarAttrSet)
                    ).apply {
                        setContent {}
                    }
                )
            }
        }
        assertNotNull("test did not run", result?.getOrThrow())
    }

    @Ignore // b/260006789
    @Test
    fun canScrollVerticallyDown_returnsTrue_onlyAfterDownEventInScrollable() {
        lateinit var composeView: View
        rule.setContent {
            composeView = LocalView.current
            ScrollableAndNonScrollable(vertical = true)
        }

        rule.onNodeWithTag(SCROLLABLE_FIRST_TAG)
            .performScrollTo()

        // No down event yet, should not be scrollable in any direction
        rule.runOnIdle {
            composeView.assertCanScroll()
        }

        // Send a down event.
        rule.onNodeWithTag(SCROLLABLE_TAG)
            .performTouchInput { down(center) }

        rule.runOnIdle {
            composeView.assertCanScroll(down = true)
        }
    }

    @Ignore // b/260006789
    @Test
    fun canScrollVerticallyUp_returnsTrue_onlyAfterDownEventInScrollable() {
        lateinit var composeView: View
        rule.setContent {
            composeView = LocalView.current
            ScrollableAndNonScrollable(vertical = true)
        }

        rule.onNodeWithTag(SCROLLABLE_LAST_TAG)
            .performScrollTo()

        // No down event yet, should not be scrollable in any direction
        rule.runOnIdle {
            composeView.assertCanScroll()
        }

        // Send a down event.
        rule.onNodeWithTag(SCROLLABLE_TAG)
            .performTouchInput {
                down(center)
            }

        rule.runOnIdle {
            composeView.assertCanScroll(up = true)
        }
    }

    @Ignore // b/260006789
    @Test
    fun canScrollVertically_returnsFalse_afterDownEventOutsideScrollable() {
        lateinit var composeView: View
        rule.setContent {
            composeView = LocalView.current
            ScrollableAndNonScrollable(vertical = true)
        }

        // No down event yet, should not be scrollable in any direction
        rule.runOnIdle {
            composeView.assertCanScroll()
        }

        // Send a down event.
        rule.onNodeWithTag(NON_SCROLLABLE_TAG)
            .performTouchInput { down(center) }

        // No down event yet, should not be scrollable in any direction
        rule.runOnIdle {
            composeView.assertCanScroll()
        }
    }

    @Ignore // b/260006789
    @Test
    fun canScrollHorizontallyRight_returnsTrue_onlyAfterDownEventInScrollable() {
        lateinit var composeView: View
        rule.setContent {
            composeView = LocalView.current
            ScrollableAndNonScrollable(vertical = false)
        }

        rule.onNodeWithTag(SCROLLABLE_FIRST_TAG)
            .performScrollTo()

        // No down event yet, should not be scrollable in any direction
        rule.runOnIdle {
            composeView.assertCanScroll()
        }

        // Send a down event.
        rule.onNodeWithTag(SCROLLABLE_TAG)
            .performTouchInput { down(center) }

        rule.runOnIdle {
            composeView.assertCanScroll(right = true)
        }
    }

    @Test
    fun canScrollHorizontallyLeft_returnsTrue_onlyAfterDownEventInScrollable() {
        lateinit var composeView: View
        rule.setContent {
            composeView = LocalView.current
            ScrollableAndNonScrollable(vertical = false)
        }

        rule.onNodeWithTag(SCROLLABLE_LAST_TAG)
            .performScrollTo()

        // No down event yet, should not be scrollable in any direction
        rule.runOnIdle {
            composeView.assertCanScroll()
        }

        // Send a down event.
        rule.onNodeWithTag(SCROLLABLE_TAG)
            .performTouchInput { down(center) }

        rule.runOnIdle {
            composeView.assertCanScroll(left = true)
        }
    }

    @Test
    fun canScrollHorizontally_returnsFalse_afterDownEventOutsideScrollable() {
        lateinit var composeView: View
        rule.setContent {
            composeView = LocalView.current
            ScrollableAndNonScrollable(vertical = false)
        }

        // No down event yet, should not be scrollable in any direction
        rule.runOnIdle {
            composeView.assertCanScroll()
        }

        // Send a down event.
        rule.onNodeWithTag(NON_SCROLLABLE_TAG)
            .performTouchInput { down(center) }

        // No down event yet, should not be scrollable in any direction
        rule.runOnIdle {
            composeView.assertCanScroll()
        }
    }

    /**
     * Puts a scrollable area of size 1 px square inside a series of nested paddings, both compose-
     * and view-based, to ensure that the pointer calculations account for all the offsets those
     * paddings introduce.
     *
     * ```
     *  ┌───────────────61────────────────┐
     *  │AndroidComposeView (root)        │
     *  │                                 │
     *  │  ┌─────────────41─────────────┐ │
     *  │  │AndroidView/FrameLayout     │ │
     *  │  │                            │ │
     *  │  │  ┌──────────21───────────┐ │ │
     *  6  │  │ComposeView            │ │ │
     *  1  4  │                       │ │ │
     *  │  1  2  ┌─────────1────────┐ │ │ │
     *  │  │  1  │Box (scrollable)  │ │ │ │
     *  │  │  │  1                  │ │ │ │
     *  │  │  │  └──────────────────┘ │ │ │
     *  │  │  └───────────────────────┘ │ │
     *  │  └────────────────────────────┘ │
     *  └─────────────────────────────────┘
     * ```
     */
    @Test
    fun canScroll_accountsForViewAndNodeOffsets() {
        lateinit var composeView: View
        rule.setContent {
            with(LocalDensity.current) {
                AndroidView(
                    modifier = Modifier
                        .requiredSize(61.toDp())
                        .padding(10.toDp()),
                    factory = { context ->
                        FrameLayout(context).apply {
                            setPadding(10)
                            addView(ComposeView(context).apply {
                                setContent {
                                    // Query the inner android view, not the outer one.
                                    composeView = LocalView.current
                                    Box(
                                        Modifier
                                            .padding(10.toDp())
                                            .testTag(SCROLLABLE_TAG)
                                            .horizontalScroll(rememberScrollState())
                                            // Give it something to scroll.
                                            .requiredSize(100.dp)
                                    )
                                }
                            })
                        }
                    }
                )
            }
        }

        val scrollable = rule.onNodeWithTag(SCROLLABLE_TAG)
            .fetchSemanticsNode()
        assertEquals(IntSize(1, 1), scrollable.size)

        rule.runOnIdle {
            composeView.assertCanScroll()
        }

        rule.onNodeWithTag(SCROLLABLE_TAG)
            .performTouchInput {
                down(center)
            }

        rule.runOnIdle {
            composeView.assertCanScroll(right = true)
        }
    }

    @Test
    fun composeView_densityChange() {
        lateinit var composeView: AndroidComposeView
        lateinit var currentDensity: Density
        rule.setContent {
            composeView = LocalView.current as AndroidComposeView
            currentDensity = LocalDensity.current
            Box(Modifier.size(100.dp))
        }

        val density = rule.density
        val newDensity = density.density * 2f

        rule.runOnUiThread {
            assertEquals(
                composeView.width,
                with(density) { 100.dp.roundToPx() }
            )

            rule.activity.resources.displayMetrics.density = newDensity
            val newConfig = Configuration().apply {
                setTo(rule.activity.resources.configuration)
            }
            composeView.dispatchConfigurationChanged(newConfig)
        }

        rule.runOnIdle {
            assertEquals(currentDensity.density, newDensity)
            assertEquals(
                composeView.width,
                with(Density(newDensity)) { 100.dp.roundToPx() }
            )
        }

        rule.runOnUiThread {
            // reset density to initial value to prevent it leaking to other tests
            rule.activity.resources.displayMetrics.density = density.density
        }
    }
}

private const val SCROLLABLE_TAG = "scrollable"
private const val NON_SCROLLABLE_TAG = "non-scrollable"
private const val SCROLLABLE_FIRST_TAG = "first-scrollable-child"
private const val SCROLLABLE_LAST_TAG = "last-scrollable-child"

private fun View.assertCanScroll(
    left: Boolean = false,
    up: Boolean = false,
    right: Boolean = false,
    down: Boolean = false
) {
    assertEquals(left, canScrollHorizontally(-1))
    assertEquals(right, canScrollHorizontally(1))
    assertEquals(up, canScrollVertically(-1))
    assertEquals(down, canScrollVertically(1))
}

private inline fun ViewGroup.assertUnsupported(
    testName: String,
    test: ViewGroup.() -> Unit
) {
    var exception: Throwable? = null
    try {
        test()
    } catch (t: Throwable) {
        exception = t
    }
    assertTrue(
        "$testName throws UnsupportedOperationException",
        exception is UnsupportedOperationException
    )
}

@Composable
private fun ScrollableAndNonScrollable(vertical: Boolean) {
    @Composable
    fun layout(size: Dp, content: @Composable (Modifier) -> Unit) {
        if (vertical) {
            Column(Modifier.requiredSize(size)) {
                content(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth())
            }
        } else {
            Row(Modifier.requiredSize(100.dp)) {
                content(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight())
            }
        }
    }

    val scrollState = rememberScrollState(0)
    val scrollModifier = if (vertical) {
        Modifier.verticalScroll(scrollState)
    } else {
        Modifier.horizontalScroll(scrollState)
    }

    layout(100.dp) { modifier ->
        Box(
            modifier
                .testTag(SCROLLABLE_TAG)
                .then(scrollModifier)
        ) {
            layout(10000.dp) {
                Box(Modifier.testTag(SCROLLABLE_FIRST_TAG))
                // Give the scrollable some content that actually requires scrolling.
                Box(it)
                Box(Modifier.testTag(SCROLLABLE_LAST_TAG))
            }
        }
        Box(modifier.testTag(NON_SCROLLABLE_TAG))
    }
}

private class TestComposeView(
    context: Context
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        // No content
    }

    public override fun addViewInLayout(child: View?, index: Int, params: LayoutParams?): Boolean {
        return super.addViewInLayout(child, index, params)
    }

    public override fun addViewInLayout(
        child: View?,
        index: Int,
        params: LayoutParams?,
        preventRequestLayout: Boolean
    ): Boolean {
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }
}
