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

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.SubcompositionReusableContentHost
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnCreate
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnRelease
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnReset
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnUpdate
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnViewAttach
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnViewDetach
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.ViewLifecycleEvent
import androidx.core.view.SoftwareKeyboardControllerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat
import androidx.core.view.WindowInsetsAnimationCompat.Callback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.test.assertIs
import kotlin.test.assertNull
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidViewTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private val setDurationScale =
        ValueAnimator::class.java.getDeclaredMethod("setDurationScale", Float::class.java).apply {
            isAccessible = true
        }

    private val getDurationScale =
        ValueAnimator::class.java.getDeclaredMethod("getDurationScale").apply {
            isAccessible = true
        }

    private var oldDurationScale = 1f

    @Before
    fun edgeToEdge() {
        rule.runOnUiThread { rule.activity.enableEdgeToEdge() }
    }

    @Before
    fun setDurationScale() {
        rule.runOnUiThread {
            oldDurationScale = getDurationScale.invoke(null) as Float
            setDurationScale.invoke(null, 1f)
        }
    }

    @After
    fun resetDurationScale() {
        rule.runOnUiThread { setDurationScale.invoke(null, oldDurationScale) }
    }

    @Test
    fun androidViewWithConstructor() {
        rule.setContent { AndroidView({ TextView(it).apply { text = "Test" } }) }
        Espresso.onView(instanceOf(TextView::class.java)).check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithResourceTest() {
        rule.setContent {
            AndroidView({ LayoutInflater.from(it).inflate(R.layout.test_layout, null) })
        }
        Espresso.onView(instanceOf(RelativeLayout::class.java)).check(matches(isDisplayed()))
    }

    @Test
    fun androidViewInvalidatingDuringDrawTest() {
        var drawCount = 0
        val timesToInvalidate = 10
        var customView: InvalidatedTextView? = null
        rule.setContent {
            AndroidView(
                factory = {
                    val view: View =
                        LayoutInflater.from(it)
                            .inflate(R.layout.test_multiple_invalidation_layout, null)
                    customView = view.findViewById<InvalidatedTextView>(R.id.custom_draw_view)
                    customView!!.timesToInvalidate = timesToInvalidate
                    customView!!.onDraw = { ++drawCount }
                    view
                }
            )
        }
        // the first drawn was not caused by invalidation, thus add it to expected draw count.
        var expectedDraws = timesToInvalidate + 1
        repeat(expectedDraws) { rule.mainClock.advanceTimeByFrame() }

        // Ensure we wait until the time advancement actually happened as sometimes we can race if
        // we use runOnIdle directly making the test fail, so providing a big enough timeout to
        // give plenty of time for the frame advancement to happen.
        rule.waitUntil(3000) { drawCount == expectedDraws }

        rule.runOnIdle {
            // Verify that we only drew once per invalidation
            assertThat(drawCount).isEqualTo(expectedDraws)
            assertThat(drawCount).isEqualTo(customView!!.timesDrawn)
        }
    }

    @Test
    fun androidViewWithViewTest() {
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout =
                FrameLayout(activity).apply { layoutParams = ViewGroup.LayoutParams(300, 300) }
        }
        rule.setContent { AndroidView({ frameLayout }) }
        Espresso.onView(equalTo(frameLayout)).check(matches(isDisplayed()))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun androidViewAccessibilityDelegate() {
        rule.setContent {
            AndroidView({
                TextView(it).apply {
                    text = "Test"
                    setScreenReaderFocusable(true)
                }
            })
        }
        Espresso.onView(instanceOf(TextView::class.java)).check(matches(isDisplayed())).check {
            view,
            exception ->
            val viewParent = view.getParent()
            if (viewParent !is View) {
                throw exception
            }
            val delegate = viewParent.getAccessibilityDelegate()
            if (viewParent.getAccessibilityDelegate() == null) {
                throw exception
            }
            val info: AccessibilityNodeInfo = AccessibilityNodeInfo()
            delegate.onInitializeAccessibilityNodeInfo(view, info)
            if (!info.isVisibleToUser()) {
                throw exception
            }
            if (!info.isScreenReaderFocusable()) {
                throw exception
            }
        }
    }

    @Test
    fun androidViewWithResourceTest_preservesLayoutParams() {
        rule.setContent {
            AndroidView({
                LayoutInflater.from(it).inflate(R.layout.test_layout, FrameLayout(it), false)
            })
        }
        Espresso.onView(withClassName(endsWith("RelativeLayout")))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                if (view.layoutParams.width != 300.dp.toPx(view.context.resources.displayMetrics)) {
                    throw exception
                }
                if (view.layoutParams.height != WRAP_CONTENT) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewProperlyDetached() {
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout =
                FrameLayout(activity).apply { layoutParams = ViewGroup.LayoutParams(300, 300) }
        }
        var emit by mutableStateOf(true)
        rule.setContent {
            if (emit) {
                AndroidView({ frameLayout })
            }
        }

        // Assert view initially attached
        rule.runOnUiThread {
            assertThat(frameLayout.parent).isNotNull()
            emit = false
        }

        // Assert view detached when removed from composition hierarchy
        rule.runOnIdle {
            assertThat(frameLayout.parent).isNull()
            emit = true
        }

        // Assert view reattached when added back to the composition hierarchy
        rule.runOnIdle { assertThat(frameLayout.parent).isNotNull() }
    }

    @Test
    @LargeTest
    fun androidView_attachedAfterDetached_addsViewBack() {
        lateinit var root: FrameLayout
        lateinit var composeView: ComposeView
        lateinit var viewInsideCompose: View
        rule.activityRule.scenario.onActivity { activity ->
            root = FrameLayout(activity)
            composeView = ComposeView(activity)
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity)
            )
            viewInsideCompose = View(activity)

            activity.setContentView(root)
            root.addView(composeView)
            composeView.setContent { AndroidView({ viewInsideCompose }) }
        }

        var viewInsideComposeHolder: ViewGroup? = null
        rule.runOnUiThread {
            assertThat(viewInsideCompose.parent).isNotNull()
            viewInsideComposeHolder = viewInsideCompose.parent as ViewGroup
            root.removeView(composeView)
        }

        rule.runOnIdle {
            // Views don't detach from the parent when the parent is detached
            assertThat(viewInsideCompose.parent).isNotNull()
            assertThat(viewInsideComposeHolder?.childCount).isEqualTo(1)
            root.addView(composeView)
        }

        rule.runOnIdle {
            assertThat(viewInsideCompose.parent).isEqualTo(viewInsideComposeHolder)
            assertThat(viewInsideComposeHolder?.childCount).isEqualTo(1)
        }
    }

    @Test
    fun androidViewWithResource_modifierIsApplied() {
        val size = 20.dp
        rule.setContent {
            AndroidView(
                { LayoutInflater.from(it).inflate(R.layout.test_layout, null) },
                Modifier.requiredSize(size),
            )
        }
        Espresso.onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewWithView_modifierIsApplied() {
        val size = 20.dp
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity -> frameLayout = FrameLayout(activity) }
        rule.setContent { AndroidView({ frameLayout }, Modifier.requiredSize(size)) }

        Espresso.onView(equalTo(frameLayout)).check(matches(isDisplayed())).check { view, exception
            ->
            val expectedSize = size.toPx(view.context.resources.displayMetrics)
            if (view.width != expectedSize || view.height != expectedSize) {
                throw exception
            }
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidViewWithView_drawModifierIsApplied() {
        val size = 300
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout =
                FrameLayout(activity).apply { layoutParams = ViewGroup.LayoutParams(size, size) }
        }
        rule.setContent {
            AndroidView({ frameLayout }, Modifier.testTag("view").background(color = Color.Blue))
        }

        rule.onNodeWithTag("view").captureToImage().assertPixels(IntSize(size, size)) { Color.Blue }
    }

    @Test
    fun androidViewWithResource_modifierIsCorrectlyChanged() {
        val size = mutableStateOf(20.dp)
        rule.setContent {
            AndroidView(
                { LayoutInflater.from(it).inflate(R.layout.test_layout, null) },
                Modifier.requiredSize(size.value),
            )
        }
        Espresso.onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.value.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
        rule.runOnIdle { size.value = 30.dp }
        Espresso.onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.value.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    fun androidView_notDetachedFromWindowTwice() {
        // Should not crash.
        rule.setContent { Box { AndroidView(::ComposeView) { it.setContent { Box(Modifier) } } } }
    }

    @Test
    fun androidView_updateIsRanInitially() {
        rule.setContent { Box { AndroidView(::UpdateTestView) { view -> view.counter = 1 } } }

        onView(instanceOf(UpdateTestView::class.java)).check { view, _ ->
            assertIs<UpdateTestView>(view)
            assertThat(view.counter).isEqualTo(1)
        }
    }

    @Test
    fun androidView_updateObservesMultipleStateChanges() {
        var counter by mutableStateOf(1)

        rule.setContent { Box { AndroidView(::UpdateTestView) { view -> view.counter = counter } } }

        counter = 2
        onView(instanceOf(UpdateTestView::class.java)).check { view, _ ->
            assertIs<UpdateTestView>(view)
            assertThat(view.counter).isEqualTo(counter)
        }

        counter = 3
        onView(instanceOf(UpdateTestView::class.java)).check { view, _ ->
            assertIs<UpdateTestView>(view)
            assertThat(view.counter).isEqualTo(counter)
        }

        counter = 4
        onView(instanceOf(UpdateTestView::class.java)).check { view, _ ->
            assertIs<UpdateTestView>(view)
            assertThat(view.counter).isEqualTo(counter)
        }
    }

    @Test
    fun androidView_updateObservesStateChanges_fromDisposableEffect() {
        var counter by mutableStateOf(1)

        rule.setContent {
            DisposableEffect(Unit) {
                counter = 2
                onDispose {}
            }

            Box { AndroidView(::UpdateTestView) { view -> view.counter = counter } }
        }

        onView(instanceOf(UpdateTestView::class.java)).check { view, _ ->
            assertIs<UpdateTestView>(view)
            assertThat(view.counter).isEqualTo(2)
        }
    }

    @Test
    fun androidView_updateObservesStateChanges_fromLaunchedEffect() {
        var counter by mutableStateOf(1)

        rule.setContent {
            LaunchedEffect(Unit) { counter = 2 }

            Box { AndroidView(::UpdateTestView) { view -> view.counter = counter } }
        }

        onView(instanceOf(UpdateTestView::class.java)).check { view, _ ->
            assertIs<UpdateTestView>(view)
            assertThat(view.counter).isEqualTo(2)
        }
    }

    @Test
    fun androidView_updateObservesMultipleStateChanges_fromEffect() {
        var counter by mutableStateOf(1)

        rule.setContent {
            LaunchedEffect(Unit) {
                counter = 2
                withFrameNanos { counter = 3 }
            }

            Box { AndroidView(::UpdateTestView) { view -> view.counter = counter } }
        }

        onView(instanceOf(UpdateTestView::class.java)).check { view, _ ->
            assertIs<UpdateTestView>(view)
            assertThat(view.counter).isEqualTo(3)
        }
    }

    @Test
    fun androidView_updateObservesLayoutStateChanges() {
        var size by mutableStateOf(20)
        var obtainedSize: IntSize = IntSize.Zero
        rule.setContent {
            Box {
                AndroidView(::View, Modifier.onGloballyPositioned { obtainedSize = it.size }) { view
                    ->
                    view.layoutParams = ViewGroup.LayoutParams(size, size)
                }
            }
        }
        rule.runOnIdle {
            assertThat(obtainedSize).isEqualTo(IntSize(size, size))
            size = 40
        }
        rule.runOnIdle { assertThat(obtainedSize).isEqualTo(IntSize(size, size)) }
    }

    @Test
    fun androidView_propagatesDensity() {
        rule.setContent {
            val size = 50.dp
            val density = Density(3f)
            val sizeIpx = with(density) { size.roundToPx() }
            CompositionLocalProvider(LocalDensity provides density) {
                AndroidView(
                    { FrameLayout(it) },
                    Modifier.requiredSize(size).onGloballyPositioned {
                        assertThat(it.size).isEqualTo(IntSize(sizeIpx, sizeIpx))
                    },
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun androidView_propagatesViewTreeCompositionContext() {
        lateinit var parentComposeView: ComposeView
        lateinit var compositionChildView: View
        rule.activityRule.scenario.onActivity { activity ->
            parentComposeView =
                ComposeView(activity).apply {
                    setContent { AndroidView(::View) { compositionChildView = it } }
                    activity.setContentView(this)
                }
        }
        rule.runOnIdle {
            assertThat(compositionChildView.findViewTreeCompositionContext())
                .isNotEqualTo(parentComposeView.findViewTreeCompositionContext())
        }
    }

    @Test
    fun androidView_propagatesLocalsToComposeViewChildren() {
        val ambient = compositionLocalOf { "unset" }
        var childComposedAmbientValue = "uncomposed"
        rule.setContent {
            CompositionLocalProvider(ambient provides "setByParent") {
                AndroidView(
                    factory = {
                        ComposeView(it).apply {
                            setContent { childComposedAmbientValue = ambient.current }
                        }
                    }
                )
            }
        }
        rule.runOnIdle { assertThat(childComposedAmbientValue).isEqualTo("setByParent") }
    }

    @Test
    fun androidView_propagatesLayoutDirectionToComposeViewChildren() {
        var childViewLayoutDirection: Int = Int.MIN_VALUE
        var childCompositionLayoutDirection: LayoutDirection? = null
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AndroidView(
                    factory = {
                        FrameLayout(it).apply {
                            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                                childViewLayoutDirection = layoutDirection
                            }
                            addView(
                                ComposeView(it).apply {
                                    // The view hierarchy's layout direction should always override
                                    // the ambient layout direction from the parent composition.
                                    layoutDirection = android.util.LayoutDirection.LTR
                                    setContent {
                                        childCompositionLayoutDirection =
                                            LocalLayoutDirection.current
                                    }
                                },
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                ),
                            )
                        }
                    }
                )
            }
        }
        rule.runOnIdle {
            assertThat(childViewLayoutDirection).isEqualTo(android.util.LayoutDirection.RTL)
            assertThat(childCompositionLayoutDirection).isEqualTo(LayoutDirection.Ltr)
        }
    }

    @Test
    fun androidView_propagatesLocalLifecycleOwnerAsViewTreeOwner() {
        lateinit var parentLifecycleOwner: LifecycleOwner
        val compositionLifecycleOwner = TestLifecycleOwner()
        var childViewTreeLifecycleOwner: LifecycleOwner? = null

        rule.setContent {
            LocalLifecycleOwner.current.also { SideEffect { parentLifecycleOwner = it } }

            CompositionLocalProvider(LocalLifecycleOwner provides compositionLifecycleOwner) {
                AndroidView(
                    factory = {
                        object : FrameLayout(it) {
                            override fun onAttachedToWindow() {
                                super.onAttachedToWindow()
                                childViewTreeLifecycleOwner = findViewTreeLifecycleOwner()
                            }
                        }
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(childViewTreeLifecycleOwner).isSameInstanceAs(compositionLifecycleOwner)
            assertThat(childViewTreeLifecycleOwner).isNotSameInstanceAs(parentLifecycleOwner)
        }
    }

    @Test
    fun androidView_propagatesLocalSavedStateRegistryOwnerAsViewTreeOwner() {
        lateinit var parentSavedStateRegistryOwner: SavedStateRegistryOwner
        val compositionSavedStateRegistryOwner =
            object : SavedStateRegistryOwner, LifecycleOwner by TestLifecycleOwner() {
                // We don't actually need to ever get actual instance.
                override val savedStateRegistry: SavedStateRegistry
                    get() = throw UnsupportedOperationException()
            }
        var childViewTreeSavedStateRegistryOwner: SavedStateRegistryOwner? = null

        rule.setContent {
            LocalSavedStateRegistryOwner.current.also {
                SideEffect { parentSavedStateRegistryOwner = it }
            }

            CompositionLocalProvider(
                LocalSavedStateRegistryOwner provides compositionSavedStateRegistryOwner
            ) {
                AndroidView(
                    factory = {
                        object : FrameLayout(it) {
                            override fun onAttachedToWindow() {
                                super.onAttachedToWindow()
                                childViewTreeSavedStateRegistryOwner =
                                    findViewTreeSavedStateRegistryOwner()
                            }
                        }
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(childViewTreeSavedStateRegistryOwner)
                .isSameInstanceAs(compositionSavedStateRegistryOwner)
            assertThat(childViewTreeSavedStateRegistryOwner)
                .isNotSameInstanceAs(parentSavedStateRegistryOwner)
        }
    }

    @Test
    fun androidView_runsFactoryExactlyOnce_afterFirstComposition() {
        var factoryRunCount = 0
        rule.setContent {
            val view = remember { View(rule.activity) }
            AndroidView({
                ++factoryRunCount
                view
            })
        }
        rule.runOnIdle { assertThat(factoryRunCount).isEqualTo(1) }
    }

    @Test
    fun androidView_runsFactoryExactlyOnce_evenWhenFactoryIsChanged() {
        var factoryRunCount = 0
        var first by mutableStateOf(true)
        rule.setContent {
            val view = remember { View(rule.activity) }
            AndroidView(
                if (first) {
                    {
                        ++factoryRunCount
                        view
                    }
                } else {
                    {
                        ++factoryRunCount
                        view
                    }
                }
            )
        }
        rule.runOnIdle {
            assertThat(factoryRunCount).isEqualTo(1)
            first = false
        }
        rule.runOnIdle { assertThat(factoryRunCount).isEqualTo(1) }
    }

    @Ignore
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidView_clipsToBounds() {
        val size = 20
        val sizeDp = with(rule.density) { size.toDp() }
        rule.setContent {
            Column {
                Box(Modifier.size(sizeDp).background(Color.Blue).testTag("box"))
                AndroidView(factory = { SurfaceView(it) })
            }
        }

        rule.onNodeWithTag("box").captureToImage().assertPixels(IntSize(size, size)) { Color.Blue }
    }

    @Test
    fun androidView_callsOnRelease() {
        var releaseCount = 0
        var showContent by mutableStateOf(true)
        rule.setContent {
            if (showContent) {
                AndroidView(
                    factory = { TextView(it) },
                    update = { it.text = "onRelease test" },
                    onRelease = { releaseCount++ },
                )
            }
        }

        onView(instanceOf(TextView::class.java)).check(matches(isDisplayed()))

        assertEquals("onRelease() was called unexpectedly", 0, releaseCount)

        showContent = false

        onView(instanceOf(TextView::class.java)).check(doesNotExist())

        assertEquals(
            "onRelease() should be called exactly once after " +
                "removing the view from the composition hierarchy",
            1,
            releaseCount,
        )
    }

    @Test
    fun androidView_restoresState() {
        var result = ""

        @Composable
        fun <T : Any> Navigation(
            currentScreen: T,
            modifier: Modifier = Modifier,
            content: @Composable (T) -> Unit,
        ) {
            val saveableStateHolder = rememberSaveableStateHolder()
            Box(modifier) {
                saveableStateHolder.SaveableStateProvider(currentScreen) { content(currentScreen) }
            }
        }

        var screen by mutableStateOf("screen1")
        rule.setContent {
            Navigation(screen) { currentScreen ->
                if (currentScreen == "screen1") {
                    AndroidView({
                        StateSavingView(
                            context = it,
                            value = "testValue",
                            onRestoredValue = { restoredValue -> result = restoredValue },
                        )
                    })
                } else {
                    Box(Modifier)
                }
            }
        }

        rule.runOnIdle { screen = "screen2" }
        rule.runOnIdle { screen = "screen1" }
        rule.runOnIdle { assertThat(result).isEqualTo("testValue") }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidView_noClip() {
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                with(LocalDensity.current) {
                    Box(Modifier.requiredSize(150.toDp()).testTag("box")) {
                        Box(
                            Modifier.size(100.toDp(), 100.toDp()).align(AbsoluteAlignment.TopLeft)
                        ) {
                            AndroidView(
                                factory = { context ->
                                    object : View(context) {
                                        init {
                                            clipToOutline = false
                                        }

                                        override fun onDraw(canvas: Canvas) {
                                            val paint = Paint()
                                            paint.color = Color.Blue.toArgb()
                                            paint.style = Paint.Style.FILL
                                            canvas.drawRect(0f, 0f, 150f, 150f, paint)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("box").captureToImage().assertPixels(IntSize(150, 150)) { Color.Blue }
    }

    @Test
    fun testInitialComposition_causesViewToBecomeActive() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        rule.setContent {
            ReusableContent("never-changes") {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add,
                )
            }
        }

        onView(instanceOf(TextView::class.java)).check(matches(isDisplayed()))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewRecomposition_onlyInvokesUpdate() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var state by mutableStateOf(0)
        rule.setContent {
            ReusableContent("never-changes") {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it) },
                    update = { it.text = "Text $state" },
                    onLifecycleEvent = lifecycleEvents::add,
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Text 0")))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        state++

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Text 1")))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when recomposed",
            listOf(OnUpdate),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewDeactivation_causesViewResetAndDetach() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var attached by mutableStateOf(true)
        rule.setContent {
            ReusableContentHost(attached) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add,
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        attached = false

        onView(instanceOf(TextView::class.java)).check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy and retained by Compose",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewReattachment_causesViewToBecomeReusedAndReactivated() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var attached by mutableStateOf(true)
        rule.setContent {
            ReusableContentHost(attached) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add,
                )
            }
        }

        onView(instanceOf(TextView::class.java)).check(matches(isDisplayed()))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        attached = false

        onView(instanceOf(TextView::class.java)).check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy and retained by Compose",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        attached = true

        onView(instanceOf(TextView::class.java)).check(matches(isDisplayed()))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "reattached to the composition hierarchy",
            listOf(OnViewAttach, OnUpdate),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewDisposalWhenDetached_causesViewToBeReleased() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var active by mutableStateOf(true)
        var emit by mutableStateOf(true)
        rule.setContent {
            if (emit) {
                ReusableContentHost(active) {
                    ReusableAndroidViewWithLifecycleTracking(
                        factory = { TextView(it).apply { text = "Test" } },
                        onLifecycleEvent = lifecycleEvents::add,
                    )
                }
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        active = false

        onView(instanceOf(TextView::class.java)).check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy and retained by Compose",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        emit = false

        onView(instanceOf(TextView::class.java)).check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy while deactivated",
            listOf(OnRelease),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewRemovedFromComposition_causesViewToBeReleased() {
        var includeViewInComposition by mutableStateOf(true)
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        rule.setContent {
            if (includeViewInComposition) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add,
                )
            }
        }

        onView(instanceOf(TextView::class.java)).check(matches(isDisplayed()))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        includeViewInComposition = false

        onView(instanceOf(TextView::class.java)).check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from composition while visible",
            listOf(OnViewDetach, OnRelease),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewReusedInComposition_invokesReuseCallbackSequence() {
        var key by mutableStateOf(0)
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        rule.setContent {
            ReusableContent(key) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it) },
                    update = { it.text = "Test" },
                    onLifecycleEvent = lifecycleEvents::add,
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Test")))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        key++

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Test")))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " + "reused in composition",
            listOf(OnReset, OnUpdate),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewInComposition_experiencesHostLifecycle_andDoesNotRecreateView() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        rule.setContent {
            ReusableContentHost(active = true) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add,
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()

        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.runOnIdle { /* Ensure lifecycle callbacks propagate */ }

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "its host transitioned from RESUMED to CREATED while the view was attached",
            listOf(ViewLifecycleEvent(ON_PAUSE), ViewLifecycleEvent(ON_STOP)),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.runOnIdle { /* Ensure lifecycle callbacks propagate */ }

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "its host transitioned from CREATED to RESUMED while the view was attached",
            listOf(ViewLifecycleEvent(ON_START), ViewLifecycleEvent(ON_RESUME)),
            lifecycleEvents,
        )
    }

    @Test
    fun testReactivationWithChangingKey_onlyResetsOnce() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var attach by mutableStateOf(true)
        var key by mutableStateOf(1)
        rule.setContent {
            ReusableContentHost(active = attach) {
                ReusableContent(key = key) {
                    ReusableAndroidViewWithLifecycleTracking(
                        factory = { TextView(it).apply { text = "Test" } },
                        onLifecycleEvent = lifecycleEvents::add,
                    )
                }
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        attach = false

        onView(instanceOf(TextView::class.java)).check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "detached from the composition hierarchy",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        rule.runOnUiThread {
            // Make sure both changes are applied in the same composition.
            attach = true
            key++
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "simultaneously reactivating and changing reuse keys",
            listOf(OnViewAttach, OnUpdate),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewDetachedFromComposition_stillExperiencesHostLifecycle() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var attached by mutableStateOf(true)
        rule.setContent {
            ReusableContentHost(attached) {
                val content =
                    @Composable {
                        ReusableAndroidViewWithLifecycleTracking(
                            factory = { TextView(it).apply { text = "Test" } },
                            onLifecycleEvent = lifecycleEvents::add,
                        )
                    }

                // Placing items when they are in reused state is not supported for now.
                // Reusing only happens in SubcomposeLayout atm which never places reused nodes
                Layout(content) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    val firstPlaceable = placeables.first()
                    layout(firstPlaceable.width, firstPlaceable.height) {
                        if (attached) {
                            placeables.forEach { it.place(IntOffset.Zero) }
                        }
                    }
                }
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        attached = false

        onView(instanceOf(TextView::class.java)).check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy and retained by Compose",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents,
        )
        lifecycleEvents.clear()

        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.runOnIdle { /* Ensure lifecycle callbacks propagate */ }

        assertEquals(
            "AndroidView did not receive callbacks when its host transitioned from " +
                "RESUMED to CREATED while the view was detached",
            listOf(ViewLifecycleEvent(ON_PAUSE), ViewLifecycleEvent(ON_STOP)),
            lifecycleEvents,
        )

        lifecycleEvents.clear()
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.runOnIdle { /* Wait for UI to settle */ }

        assertEquals(
            "AndroidView did not receive callbacks when its host transitioned from " +
                "CREATED to RESUMED while the view was detached",
            listOf(ViewLifecycleEvent(ON_START), ViewLifecycleEvent(ON_RESUME)),
            lifecycleEvents,
        )
    }

    @Test
    fun testViewIsReused_whenMoved() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var slotWithContent by mutableStateOf(0)

        rule.setContent {
            val movableContext = remember {
                movableContentOf {
                    ReusableAndroidViewWithLifecycleTracking(
                        factory = { context -> StateSavingView(context, "") },
                        onLifecycleEvent = lifecycleEvents::add,
                    )
                }
            }

            Column {
                repeat(10) { slot ->
                    key(slot) {
                        if (slot == slotWithContent) {
                            ReusableContent(Unit) { movableContext() }
                        } else {
                            Text("Slot $slot")
                        }
                    }
                }
            }
        }

        rule.activityRule.withActivity {
            val view = findViewById<StateSavingView>(StateSavingView.ID)
            assertEquals("View didn't have the expected initial value", "", view.value)
            view.value = "Value 1"
        }

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME),
            ),
            lifecycleEvents,
        )
        lifecycleEvents.clear()
        slotWithContent++

        rule.waitForIdle()

        assertEquals(
            "AndroidView experienced unexpected lifecycle events when " +
                "moved in the composition",
            listOf(OnViewDetach, OnViewAttach),
            lifecycleEvents,
        )

        // Check that the state of the view is retained
        rule.activityRule.withActivity {
            val view = findViewById<StateSavingView>(StateSavingView.ID)
            assertEquals("View didn't retain its state across reuse", "Value 1", view.value)
        }
    }

    @Test
    fun testViewRestoresState_whenRemovedAndRecreatedWithNoReuse() {
        var screen by mutableStateOf("screen1")
        rule.setContent {
            with(rememberSaveableStateHolder()) {
                if (screen == "screen1") {
                    SaveableStateProvider("screen1") {
                        AndroidView(
                            factory = { context ->
                                StateSavingView(context, "screen1 first value")
                            },
                            update = {},
                            onReset = {},
                            onRelease = {},
                        )
                    }
                }
            }
        }

        rule.activityRule.withActivity {
            val view = findViewById<StateSavingView>(StateSavingView.ID)
            assertEquals(
                "View didn't have the expected initial value",
                "screen1 first value",
                view.value,
            )
            view.value = "screen1 new value"
        }

        rule.runOnIdle { screen = "screen2" }
        rule.waitForIdle()

        rule.activityRule.withActivity {
            assertNull(
                findViewById<StateSavingView>(StateSavingView.ID),
                "StateSavingView should be removed from the hierarchy",
            )
        }

        rule.runOnIdle { screen = "screen1" }
        rule.waitForIdle()

        rule.activityRule.withActivity {
            val view = findViewById<StateSavingView>(StateSavingView.ID)
            assertEquals(
                "View did not restore with the correct state",
                "screen1 new value",
                view.value,
            )
        }
    }

    @Test
    fun androidView_withParentDataModifier() {
        val columnHeight = 100
        val columnHeightDp = with(rule.density) { columnHeight.toDp() }
        var viewSize = IntSize.Zero
        rule.setContent {
            Column(Modifier.height(columnHeightDp).fillMaxWidth()) {
                AndroidView(
                    factory = { View(it) },
                    modifier = Modifier.weight(1f).onGloballyPositioned { viewSize = it.size },
                )

                Box(Modifier.height(columnHeightDp / 4))
            }
        }

        rule.runOnIdle { assertEquals(columnHeight * 3 / 4, viewSize.height) }
    }

    @Test
    fun androidView_visibilityGone() {
        var view: View? = null
        var drawCount = 0
        val viewSizeDp = 50.dp
        val viewSize = with(rule.density) { viewSizeDp.roundToPx() }
        rule.setContent {
            AndroidView(
                modifier = Modifier.testTag("wrapper").heightIn(max = viewSizeDp),
                factory = {
                    object : View(it) {
                        override fun dispatchDraw(canvas: Canvas) {
                            drawCount++
                            super.dispatchDraw(canvas)
                        }
                    }
                },
                update = {
                    view = it
                    it.layoutParams = ViewGroup.LayoutParams(viewSize, WRAP_CONTENT)
                },
            )
        }

        rule.onNodeWithTag("wrapper").assertHeightIsEqualTo(viewSizeDp)

        rule.runOnUiThread {
            drawCount = 0
            view?.visibility = View.GONE
        }

        rule.onNodeWithTag("wrapper").assertHeightIsEqualTo(0.dp)
        assertEquals(0, drawCount)
    }

    @Test
    fun androidView_visibilityGone_column() {
        var view: View? = null
        val viewSizeDp = 50.dp
        val viewSize = with(rule.density) { viewSizeDp.roundToPx() }
        rule.setContent {
            Column {
                AndroidView(
                    modifier = Modifier.testTag("wrapper").heightIn(max = viewSizeDp),
                    factory = { View(it) },
                    update = {
                        view = it
                        it.layoutParams = ViewGroup.LayoutParams(viewSize, WRAP_CONTENT)
                    },
                )

                Box(Modifier.size(viewSizeDp).testTag("box"))
            }
        }

        rule
            .onNodeWithTag("box")
            .assertTopPositionInRootIsEqualTo(viewSizeDp)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        rule.runOnUiThread { view?.visibility = View.GONE }

        rule
            .onNodeWithTag("box")
            .assertTopPositionInRootIsEqualTo(0.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun updateIsNotCalledOnDeactivatedNode() {
        var active by mutableStateOf(true)
        var counter by mutableStateOf(0)
        val updateCalls = mutableListOf<Int>()
        rule.setContent {
            SubcompositionReusableContentHost(active = active) {
                AndroidView(
                    modifier = Modifier.size(10.dp),
                    factory = { View(it) },
                    update = { updateCalls.add(counter) },
                    onReset = {
                        counter++
                        Snapshot.sendApplyNotifications()
                    },
                )
            }
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEqualTo(listOf(0))
            updateCalls.clear()

            active = false
        }

        rule.runOnIdle {
            assertThat(updateCalls).isEmpty()

            active = true
        }

        rule.runOnIdle {
            // make sure the update is called after reactivation.
            assertThat(updateCalls).isEqualTo(listOf(1))
            updateCalls.clear()

            counter++
        }

        rule.runOnIdle {
            // make sure the state observation is active after reactivation.
            assertThat(updateCalls).isEqualTo(listOf(2))
        }
    }

    @Test
    @LargeTest
    fun androidView_attachingDoesNotCauseRelayout() {
        lateinit var root: RequestLayoutTrackingFrameLayout
        lateinit var composeView: ComposeView
        lateinit var viewInsideCompose: View
        var showAndroidView by mutableStateOf(false)

        rule.activityRule.scenario.onActivity { activity ->
            root = RequestLayoutTrackingFrameLayout(activity)
            composeView = ComposeView(activity)
            viewInsideCompose = View(activity)

            activity.setContentView(root)
            root.addView(composeView)
            composeView.setContent {
                Box(Modifier.fillMaxSize()) {
                    // this view will create AndroidViewsHandler (causes relayout)
                    AndroidView({ View(it) })
                    if (showAndroidView) {
                        // attaching this view should not cause relayout
                        AndroidView({ viewInsideCompose })
                    }
                }
            }
        }

        rule.runOnUiThread {
            assertThat(viewInsideCompose.parent).isNull()
            assertThat(root.requestLayoutCalled).isTrue()
            root.requestLayoutCalled = false
            showAndroidView = true
        }

        rule.runOnIdle {
            assertThat(viewInsideCompose.parent).isNotNull()
            assertThat(root.requestLayoutCalled).isFalse()
        }
    }

    // regression test for b/339527377
    @Test
    fun androidView_layoutChangesInvokeGlobalLayoutListener() {
        lateinit var textView1: TextView
        var callbackInvocations = 0

        @Composable
        fun GlobalLayoutAwareTextView(init: (TextView) -> Unit, modifier: Modifier = Modifier) {
            AndroidView(
                factory = {
                    TextView(it).apply {
                        layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                        init(this)
                    }
                },
                modifier = modifier,
            )
        }

        rule.activityRule.withActivity {
            window.decorView.viewTreeObserver.addOnGlobalLayoutListener { callbackInvocations++ }
        }

        rule.setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                GlobalLayoutAwareTextView(
                    init = { textView1 = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                )
            }
        }

        rule.waitForIdle()
        assertWithMessage(
                "The initial layout did not invoke the viewTreeObserver's OnGlobalLayoutListener"
            )
            .that(callbackInvocations)
            .isAtLeast(1)
        callbackInvocations = 0

        rule.runOnUiThread { textView1.text = "Foo".repeat(20) }
        rule.waitForIdle()

        assertWithMessage(
                "Expected an invocation of the viewTreeObserver's OnGlobalLayoutListener " +
                    "after re-laying out the contained AndroidView."
            )
            .that(callbackInvocations)
            .isAtLeast(1)
    }

    // secondary regression test for b/339527377
    @Test
    @FlakyTest(
        detail =
            "This test flakes in CI because the platform may invoke the global layout " +
                "callback in a way that this test can't account for. This test asserts an upper " +
                "bound on the number of invocations to the global layout listener that we will " +
                "dispatch, which affects performance instead of correctness. This test should always " +
                "pass locally, but it is acceptable to flake and be ignored by CI since the test " +
                "`androidView_layoutChangesInvokeGlobalLayoutListener` asserts the lower bound " +
                "of the required behavior."
    )
    fun androidView_layoutChangesInvokeGlobalLayoutListenerExactlyOnce() {
        lateinit var textView1: TextView
        lateinit var textView2: TextView
        var callbackInvocations = 0

        @Composable
        fun GlobalLayoutAwareTextView(init: (TextView) -> Unit, modifier: Modifier = Modifier) {
            AndroidView(
                factory = {
                    TextView(it).apply {
                        layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                        init(this)
                    }
                },
                modifier = modifier,
            )
        }

        rule.activityRule.withActivity {
            window.decorView.viewTreeObserver.addOnGlobalLayoutListener { callbackInvocations++ }
        }

        rule.setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                GlobalLayoutAwareTextView(
                    init = { textView1 = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                )

                GlobalLayoutAwareTextView(
                    init = { textView2 = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                )
            }
        }

        rule.waitForIdle()
        assertWithMessage(
                "The initial layout did not invoke the viewTreeObserver's OnGlobalLayoutListener"
            )
            .that(callbackInvocations)
            .isAtLeast(1)
        callbackInvocations = 0

        rule.runOnUiThread {
            textView1.text = "Foo".repeat(20)
            textView2.text = "Bar".repeat(20)
        }
        rule.waitForIdle()

        assertWithMessage(
                "Expected exactly one invocation of the viewTreeObserver's OnGlobalLayoutListener " +
                    "after re-laying out multiple AndroidViews."
            )
            .that(callbackInvocations)
            .isEqualTo(1)
    }

    @Test
    fun insetsMoveWithChild() {
        rule.runOnIdle {
            WindowInsetsControllerCompat(rule.activity.window, rule.activity.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }

        var topPadding by mutableIntStateOf(0)
        var topInset = 0
        var outerTopInset = 0
        var latch = CountDownLatch(1)
        var isAnimating = false
        lateinit var composeView: ComposeView

        rule.setContent {
            composeView = LocalView.current.parent as ComposeView
            composeView.consumeWindowInsets = false // call this before accessing insets
            val insets = WindowInsets.systemBars
            Box(
                Modifier.layout { m, c ->
                        outerTopInset = insets.getTop(this)
                        val p = m.measure(c.offset(vertical = -topPadding))
                        layout(p.width, p.height) { p.place(0, topPadding) }
                    }
                    .background(Color.Blue)
                    .fillMaxSize()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        ComposeView(context).apply {
                            setContent {
                                val systemBars = WindowInsets.systemBars
                                val density = LocalDensity.current
                                Box(
                                    Modifier.fillMaxSize().onPlaced {
                                        topInset = systemBars.getTop(density)
                                        latch.countDown()
                                    }
                                )
                                Box(Modifier.fillMaxSize().systemBarsPadding())
                            }
                        }
                    },
                )
                Box(Modifier.fillMaxSize().background(Color.White).safeContentPadding())
            }
        }

        rule.runOnIdle {
            ViewCompat.setWindowInsetsAnimationCallback(
                composeView.parent as View,
                object : Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                    override fun onProgress(
                        insets: WindowInsetsCompat,
                        runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                    ): WindowInsetsCompat = insets

                    override fun onStart(
                        animation: WindowInsetsAnimationCompat,
                        bounds: BoundsCompat,
                    ): BoundsCompat {
                        isAnimating = true
                        return super.onStart(animation, bounds)
                    }

                    override fun onEnd(animation: WindowInsetsAnimationCompat) {
                        isAnimating = false
                        super.onEnd(animation)
                    }
                },
            )
        }

        rule.waitForIdle()

        assumeTrue(outerTopInset > 0) // This device must have a status bar inset

        rule.runOnIdle {
            assertThat(topInset).isEqualTo(outerTopInset)
            latch = CountDownLatch(1)
            topPadding = 5
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        // For some reason, the status bar insets animate to the target
        // value on older SDKs
        rule.waitForIdle()
        rule.waitUntil { !isAnimating }

        rule.runOnIdle { assertThat(topInset).isEqualTo(outerTopInset - 5) }
    }

    @Test
    fun insetsMoveWithChildSize() {
        rule.runOnIdle {
            WindowInsetsControllerCompat(rule.activity.window, rule.activity.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }

        var topInset = 0
        var bottomInset = 0
        var outerTopInset = 0
        var outerBottomInset = 0
        var latch = CountDownLatch(1)
        lateinit var composeView: ComposeView
        var childUsesMaxSize by mutableStateOf(false)

        rule.setContent {
            composeView = LocalView.current.parent as ComposeView
            composeView.consumeWindowInsets = false // call this before accessing insets
            val insets = WindowInsets.systemBars
            Box(
                Modifier.layout { m, c ->
                        outerTopInset = insets.getTop(this)
                        outerBottomInset = insets.getBottom(this)
                        val p = m.measure(c)
                        layout(p.width, p.height) { p.place(0, 0) }
                    }
                    .background(Color.Blue)
                    .fillMaxSize()
            ) {
                AndroidView(
                    modifier = Modifier.align(AbsoluteAlignment.TopLeft),
                    factory = { context ->
                        ComposeView(context).apply {
                            setContent {
                                val systemBars = WindowInsets.systemBars
                                val density = LocalDensity.current
                                val sizeModifier =
                                    if (childUsesMaxSize) {
                                        Modifier.fillMaxSize()
                                    } else {
                                        Modifier.size(100.dp)
                                    }
                                Box(
                                    sizeModifier
                                        .onPlaced {
                                            topInset = systemBars.getTop(density)
                                            bottomInset = systemBars.getBottom(density)
                                            latch.countDown()
                                        }
                                        .background(Color.White)
                                )
                            }
                        }
                    },
                )
                Box(Modifier.fillMaxSize().background(Color.White).safeContentPadding())
            }
        }

        // The device must have system bars
        assumeTrue(outerTopInset != 0 || outerBottomInset != 0)

        rule.runOnIdle {
            assertThat(topInset).isEqualTo(outerTopInset)
            assertThat(bottomInset).isEqualTo(0)
            latch = CountDownLatch(1)
            childUsesMaxSize = true
        }

        rule.waitForIdle()

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        // On older devices, the insets animate over a few frames. Wait for that animation to
        // finish.
        var framesAtSameValue = 0
        var lastBottom = 0
        rule.waitUntil {
            if (lastBottom == bottomInset) {
                framesAtSameValue++
            } else {
                framesAtSameValue = 0
            }
            lastBottom = bottomInset
            rule.runOnUiThread { composeView.invalidate() }
            framesAtSameValue > 3
        }
        rule.runOnIdle {
            assertThat(topInset).isEqualTo(outerTopInset)
            assertThat(bottomInset).isEqualTo(outerBottomInset)
        }
    }

    // Test for b/391862082
    // Below API 30 ViewCompat.setOnApplyWindowInsetsListener needs to request insets to be applied
    // when the callback is invoked, so there will be some extra root inset dispatches because of
    // that.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun insetsAreNotDispatchedToRootWhenAndroidViewMoves() {
        rule.runOnIdle {
            WindowInsetsControllerCompat(rule.activity.window, rule.activity.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }

        var topPadding by mutableIntStateOf(0)
        var topInset = 0
        var outerTopInset = 0
        var latch = CountDownLatch(1)
        lateinit var root: InsetsTrackingFrameLayout
        lateinit var composeView: ComposeView

        rule.activityRule.scenario.onActivity { activity ->
            root = InsetsTrackingFrameLayout(activity)
            composeView = ComposeView(activity)
            activity.setContentView(root)
            root.addView(composeView)
            composeView.setContent {
                composeView.consumeWindowInsets = false // call this before accessing insets
                val insets = WindowInsets.systemBars
                Box {
                    Box(
                        Modifier.layout { m, c ->
                                outerTopInset = insets.getTop(this)
                                val p = m.measure(c.offset(vertical = -topPadding))
                                layout(p.width, p.height) { p.place(0, topPadding) }
                            }
                            .background(Color.Blue)
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                ComposeView(context).apply {
                                    setContent {
                                        val systemBars = WindowInsets.systemBars
                                        val density = LocalDensity.current
                                        Box(
                                            Modifier.fillMaxSize().onPlaced {
                                                topInset = systemBars.getTop(density)
                                                latch.countDown()
                                            }
                                        )
                                        Box(Modifier.fillMaxSize().systemBarsPadding())
                                    }
                                }
                            },
                        )
                        Box(Modifier.fillMaxSize().background(Color.White).safeContentPadding())
                    }
                }
            }
        }

        rule.waitForIdle()

        assumeTrue(outerTopInset > 0) // This device must have a status bar inset

        rule.runOnIdle {
            assertThat(root.insetsDispatchCount).isEqualTo(1)
            assertThat(topInset).isEqualTo(outerTopInset)
            latch = CountDownLatch(1)
            topPadding = 5
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        rule.runOnIdle {
            // Even though we dispatched new insets to the AndroidView, we shouldn't have caused
            // insets to be dispatched to the root again
            assertThat(root.insetsDispatchCount).isEqualTo(1)
            assertThat(topInset).isEqualTo(outerTopInset - 5)
        }
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun insetsAnimateForChildren() {
        val hardKeyboardHidden = rule.activity.resources.configuration.hardKeyboardHidden
        // can't test with a hardware keyboard active because we can't bring up the IME
        assumeTrue(hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO)

        lateinit var composeView: ComposeView
        lateinit var outerBounds: BoundsCompat
        lateinit var innerBounds: BoundsCompat
        val outerProgressInsets = mutableListOf<WindowInsetsCompat>()
        val innerProgressInsets = mutableListOf<WindowInsetsCompat>()
        var isAnimating = false
        var isImeVisible = false
        var wasAnimated = false

        rule.setContent {
            composeView = LocalView.current.parent as ComposeView
            composeView.consumeWindowInsets = false // call this before accessing insets
            Box(Modifier.background(Color.White).fillMaxSize().systemBarsPadding()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        View(context).apply {
                            ViewCompat.setWindowInsetsAnimationCallback(
                                this,
                                object : Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                                    override fun onProgress(
                                        insets: WindowInsetsCompat,
                                        runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                                    ): WindowInsetsCompat {
                                        innerProgressInsets += insets
                                        return insets
                                    }

                                    override fun onStart(
                                        animation: WindowInsetsAnimationCompat,
                                        bounds: BoundsCompat,
                                    ): BoundsCompat {
                                        innerBounds = bounds
                                        return bounds
                                    }
                                },
                            )
                        }
                    },
                )
            }
        }

        rule.runOnIdle {
            val view = composeView.parent as View
            ViewCompat.setWindowInsetsAnimationCallback(
                view,
                object : Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                    override fun onProgress(
                        insets: WindowInsetsCompat,
                        runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                    ): WindowInsetsCompat {
                        outerProgressInsets += insets
                        return insets
                    }

                    override fun onStart(
                        animation: WindowInsetsAnimationCompat,
                        bounds: BoundsCompat,
                    ): BoundsCompat {
                        outerBounds = bounds
                        isAnimating = true
                        wasAnimated = true
                        return bounds
                    }

                    override fun onEnd(animation: WindowInsetsAnimationCompat) {
                        isAnimating = false
                    }
                },
            )
            ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                insets
            }
            WindowInsetsControllerCompat(rule.activity.window, composeView)
                .show(WindowInsetsCompat.Type.systemBars())
        }

        // For some reason, the status bar insets animate to the target
        // value on older SDKs
        rule.waitForIdle()
        rule.waitUntil { !isAnimating }

        rule.runOnIdle {
            assertThat(isImeVisible).isFalse()
            outerProgressInsets.clear()
            innerProgressInsets.clear()
            wasAnimated = false
            SoftwareKeyboardControllerCompat(composeView).show()
        }

        rule.waitForIdle()

        rule.waitUntil { !isAnimating && isImeVisible }

        // the IME wasn't animated, so we can't test
        assumeTrue(wasAnimated)

        rule.runOnIdle {
            // With the system bars being part of the padding, the bounds should be different by
            // the size of the system bars padding
            assertThat(innerBounds.lowerBound.bottom).isEqualTo(0)
            assertThat(innerBounds.upperBound.bottom).isLessThan(outerBounds.upperBound.bottom)

            innerProgressInsets.forEach { insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                assertThat(systemBars.left).isEqualTo(0)
                assertThat(systemBars.top).isEqualTo(0)
                assertThat(systemBars.right).isEqualTo(0)
                assertThat(systemBars.bottom).isEqualTo(0)
            }
            outerProgressInsets.forEach { insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                assertThat(
                        maxOf(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    )
                    .isGreaterThan(0)
            }
        }
    }

    @Test
    fun asNestedMovableContentChild() {
        var useRowContainer by mutableStateOf(false)

        @Composable
        fun MovableContentContainer(content: @Composable () -> Unit) {
            val movableContent = remember(content) { movableContentOf(content) }
            if (useRowContainer) {
                Row { movableContent() }
            } else {
                Column { movableContent() }
            }
        }

        rule.setContent {
            MovableContentContainer {
                MovableContentContainer {
                    AndroidView(
                        factory = { context ->
                            View(context).apply { layoutParams = ViewGroup.LayoutParams(100, 100) }
                        }
                    )
                }
            }
        }

        rule.waitForIdle()
        useRowContainer = true
        rule.waitForIdle()
    }

    @Test
    fun asNestedMovableContentChildWithReuse() {
        var useRowContainer by mutableStateOf(false)

        @Composable
        fun MovableContentContainer(content: @Composable () -> Unit) {
            val movableContent = remember(content) { movableContentOf(content) }
            if (useRowContainer) {
                Row { movableContent() }
            } else {
                Column { movableContent() }
            }
        }

        rule.setContent {
            MovableContentContainer {
                MovableContentContainer {
                    AndroidView(
                        factory = { context ->
                            View(context).apply { layoutParams = ViewGroup.LayoutParams(100, 100) }
                        },
                        onReset = { _ -> },
                    )
                }
            }
        }

        rule.waitForIdle()
        useRowContainer = true
        rule.waitForIdle()
    }

    @Composable
    private inline fun <T : View> ReusableAndroidViewWithLifecycleTracking(
        crossinline factory: (Context) -> T,
        noinline onLifecycleEvent: @DisallowComposableCalls (AndroidViewLifecycleEvent) -> Unit,
        modifier: Modifier = Modifier,
        crossinline update: (T) -> Unit = {},
        crossinline reuse: (T) -> Unit = {},
        crossinline release: (T) -> Unit = {},
    ) {
        AndroidView(
            factory = {
                onLifecycleEvent(OnCreate)
                factory(it).apply {
                    addOnAttachStateChangeListener(
                        object : OnAttachStateChangeListener, LifecycleEventObserver {
                            override fun onViewAttachedToWindow(v: View) {
                                onLifecycleEvent(OnViewAttach)
                                findViewTreeLifecycleOwner()!!.lifecycle.addObserver(this)
                            }

                            override fun onViewDetachedFromWindow(v: View) {
                                onLifecycleEvent(OnViewDetach)
                            }

                            override fun onStateChanged(
                                source: LifecycleOwner,
                                event: Lifecycle.Event,
                            ) {
                                onLifecycleEvent(ViewLifecycleEvent(event))
                            }
                        }
                    )
                }
            },
            modifier = modifier,
            update = {
                onLifecycleEvent(OnUpdate)
                update(it)
            },
            onReset = {
                onLifecycleEvent(OnReset)
                reuse(it)
            },
            onRelease = {
                onLifecycleEvent(OnRelease)
                release(it)
            },
        )
    }

    private sealed class AndroidViewLifecycleEvent {
        override fun toString(): String {
            return javaClass.simpleName
        }

        // Sent when the factory lambda is invoked
        object OnCreate : AndroidViewLifecycleEvent()

        object OnUpdate : AndroidViewLifecycleEvent()

        object OnReset : AndroidViewLifecycleEvent()

        object OnRelease : AndroidViewLifecycleEvent()

        object OnViewAttach : AndroidViewLifecycleEvent()

        object OnViewDetach : AndroidViewLifecycleEvent()

        data class ViewLifecycleEvent(val event: Lifecycle.Event) : AndroidViewLifecycleEvent() {
            override fun toString() = "ViewLifecycleEvent($event)"
        }
    }

    private class StateSavingView(
        context: Context,
        var value: String = "",
        private val onRestoredValue: (String) -> Unit = {},
    ) : View(context) {
        init {
            id = ID
        }

        override fun onSaveInstanceState(): Parcelable {
            val superState = super.onSaveInstanceState()
            val bundle = Bundle()
            bundle.putParcelable("superState", superState)
            bundle.putString(KEY, value)
            return bundle
        }

        @Suppress("DEPRECATION")
        override fun onRestoreInstanceState(state: Parcelable?) {
            super.onRestoreInstanceState((state as Bundle).getParcelable("superState"))
            val value = state.getString(KEY)!!
            this.value = value
            onRestoredValue(value)
        }

        companion object {
            const val ID = 73
            private const val KEY: String = "StateSavingView.Key"
        }
    }

    private class UpdateTestView(context: Context) : View(context) {
        var counter = 0
    }

    private fun Dp.toPx(displayMetrics: DisplayMetrics) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).roundToInt()

    private class RequestLayoutTrackingFrameLayout(context: Context) : FrameLayout(context) {
        var requestLayoutCalled = false

        override fun requestLayout() {
            super.requestLayout()
            requestLayoutCalled = true
        }
    }

    private class InsetsTrackingFrameLayout(context: Context) : FrameLayout(context) {
        var insetsDispatchCount = 0

        override fun onApplyWindowInsets(
            insets: android.view.WindowInsets?
        ): android.view.WindowInsets? {
            insetsDispatchCount++
            return super.onApplyWindowInsets(insets)
        }
    }
}
