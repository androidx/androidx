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

package androidx.ui.test

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.RenderNode
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.CompositionContext
import androidx.compose.FrameManager
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.ComponentNode
import androidx.ui.core.DrawNode
import com.google.common.truth.Truth
import org.junit.Assert

abstract class TestCase(
    val activity: Activity
) {

    private val screenWithSpec: Int
    private val screenHeightSpec: Int
    private val renderNode = RenderNode("test")
    private var canvas: Canvas? = null

    lateinit var view: ViewGroup
        private set

    init {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        screenWithSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
        screenHeightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
    }

    fun setupContent(activity: Activity) {
        view = setupContentInternal(activity)
    }

    protected abstract fun setupContentInternal(activity: Activity): ViewGroup

    /**
     * Runs all the steps leading into drawing first pixels. Useful to get into the initial state
     * before you benchmark a change of the state.
     */
    fun runToFirstDraw() {
        setupContent(activity)
        measure()
        layout()
        drawSlow()
    }

    /**
     * To be run in benchmark.
     */
    fun measure() {
        view.measure(screenWithSpec, screenHeightSpec)
    }

    /**
     * To be run in benchmark.
     */
    fun measureWithSpec(widthSpec: Int, heightSpec: Int) {
        view.measure(widthSpec, heightSpec)
    }

    /**
     * Do not measure this in benchmark.
     */
    fun prepareDraw() {
        renderNode.setPosition(0, 0, view.width, view.height)
        canvas = renderNode.beginRecording()
    }

    /**
     * To be run in benchmark. Call [prepareDraw] before and [finishDraw] after.
     */
    fun draw() {
        view.draw(canvas)
    }

    /**
     * Do not measure this in benchmark.
     */
    fun finishDraw() {
        renderNode.endRecording()
    }

    /**
     * Do not measure this in benchmark.
     */
    fun drawSlow() {
        prepareDraw()
        draw()
        finishDraw()
    }

    /**
     * To be run in benchmark.
     */
    fun layout() {
        view.layout(view.left, view.top, view.right, view.bottom)
    }
}

abstract class AndroidTestCase(
    activity: Activity
) : TestCase(activity) {

    override fun setupContentInternal(activity: Activity) = createViewContent(activity)
        .also { activity.setContentView(it) }

    abstract fun createViewContent(activity: Activity): ViewGroup
}

abstract class ComposeTestCase(
    activity: Activity
) : TestCase(activity) {

    lateinit var compositionContext: CompositionContext
        private set

    override fun setupContentInternal(activity: Activity): ViewGroup {
        compositionContext = setComposeContent(activity)
        FrameManager.nextFrame()
        return findComposeView()!!
    }

    abstract fun setComposeContent(activity: Activity): CompositionContext
}

/**
 * Test case that can trigger a change of state.
 */
interface ToggleableTestCase {
    fun toggleState()
}

fun TestCase.assertMeasureSizeIsPositive() {
    Truth.assertThat(view.measuredWidth).isAtLeast(1)
    Truth.assertThat(view.measuredHeight).isAtLeast(1)
}

fun TestCase.invalidateViews() {
    invalidateViews(view)
}

/**
 * Invalidate the layout so that measure() and layout() do something
 */
fun TestCase.requestLayout() {
    view.requestLayout()
}

private fun invalidateViews(view: View) {
    view.invalidate()
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            invalidateViews(child)
        }
    }
    if (view is AndroidCraneView) {
        invalidateComponentNodes(view.root)
    }
}

private fun invalidateComponentNodes(node: ComponentNode) {
    if (node is DrawNode) {
        node.invalidate()
    }
    node.visitChildren { child ->
        invalidateComponentNodes(child)
    }
}

/**
 * Performs recomposition and asserts that there were some pending changes.
 */
fun ComposeTestCase.recomposeSyncAssertHadChanges() {
    recomposeSyncAssert(expectingChanges = true)
}

/**
 * Performs recomposition and asserts that there were no pending changes.
 */
fun ComposeTestCase.recomposeSyncAssertNoChanges() {
    recomposeSyncAssert(expectingChanges = false)
}

/**
 * Performs recomposition and asserts that there were or weren't pending changes based on
 * [expectingChanges].
 */
fun ComposeTestCase.recomposeSyncAssert(expectingChanges: Boolean) {
    val message = if (expectingChanges) {
        "Expected pending changes on recomposition but there were none. Did " +
                "you forget to call FrameManager.next()?"
    } else {
        "Expected no pending changes on recomposition but there were some."
    }
    val hadChanges = compositionContext.recomposeSync()
    Assert.assertEquals(message, expectingChanges, hadChanges)
}

/**
 * This is only for tests debugging purposes.
 *
 * Draws the given view into [Picture] and presents it in [ImageView] in the given Activity. This is
 * useful for one time verification that your benchmark is producing excepted results and doing
 * useful work.
 */
fun TestCase.capturePreviewPictureToActivity() {
    val picture = Picture()
    val canvas = picture.beginRecording(view.width, view.height)
    view.draw(canvas)
    picture.endRecording()
    val imageView = ImageView(activity)
    imageView.setImageBitmap(Bitmap.createBitmap(picture))
    activity.setContentView(imageView)
}

/**
 * Returns the first found [AndroidCraneView] in the content view hierarchy:
 *
 *     override fun setupContent(activity: Activity) {
 *         activity.setContent { ... }
 *         view = findComposeView()!!
 *         FrameManager.nextFrame()
 *     }
 */
fun ComposeTestCase.findComposeView(): AndroidCraneView? {
    return findComposeView(activity.findViewById(android.R.id.content))
}

private fun findComposeView(view: View): AndroidCraneView? {
    if (view is AndroidCraneView) {
        return view
    }

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val composeView = findComposeView(view.getChildAt(i))
            if (composeView != null) {
                return composeView
            }
        }
    }
    return null
}
