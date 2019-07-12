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

    init {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        screenWithSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
        screenHeightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
    }

    lateinit var view: ViewGroup

    abstract fun runSetup()

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

abstract class ComposeTestCase(
    activity: Activity
) : TestCase(activity) {

    lateinit var compositionContext: CompositionContext
}

fun TestCase.assertMeasureSizeIsPositive() {
    Truth.assertThat(view.measuredWidth).isAtLeast(1)
    Truth.assertThat(view.measuredHeight).isAtLeast(1)
}

fun TestCase.invalidateViews() {
    invalidateViews(view)
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
    val hadChanges = compositionContext.recomposeSync()
    Assert.assertTrue(
        "Expected pending changes on recomposition but there were none. Did " +
                "you forget to call FrameManager.next()?", hadChanges
    )
}

/**
 * Performs recomposition and asserts that there were no pending changes.
 */
fun ComposeTestCase.recomposeSyncAssertNoChanges() {
    val hadChanges = compositionContext.recomposeSync()
    Assert.assertFalse(
        "Expected no pending changes on recomposition but there were some.",
        hadChanges
    )
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