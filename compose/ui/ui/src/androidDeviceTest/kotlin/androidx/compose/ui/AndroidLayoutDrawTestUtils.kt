/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeViewContext
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

fun Bitmap.assertRect(
    color: Color,
    holeSize: Int = 0,
    size: Int = width,
    centerX: Int = width / 2,
    centerY: Int = height / 2,
) {
    assertTrue(centerX + size / 2 <= width)
    assertTrue(centerX - size / 2 >= 0)
    assertTrue(centerY + size / 2 <= height)
    assertTrue(centerY - size / 2 >= 0)
    val halfHoleSize = holeSize / 2
    for (x in centerX - size / 2 until centerX + size / 2) {
        for (y in centerY - size / 2 until centerY + size / 2) {
            if (abs(x - centerX) > halfHoleSize && abs(y - centerY) > halfHoleSize) {
                val currentColor = Color(getPixel(x, y))
                assertColorsEqual(color, currentColor)
            }
        }
    }
}

fun assertColorsEqual(
    expected: Color,
    color: Color,
    error: () -> String = { "$expected and $color are not similar!" },
) {
    val errorString = error()
    assertEquals(errorString, expected.red, color.red, 0.05f)
    assertEquals(errorString, expected.green, color.green, 0.05f)
    assertEquals(errorString, expected.blue, color.blue, 0.05f)
    assertEquals(errorString, expected.alpha, color.alpha, 0.05f)
}

@Composable
fun AtLeastSize(size: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Layout(
        measurePolicy = { measurables, constraints ->
            val newConstraints =
                Constraints(
                    minWidth = max(size, constraints.minWidth),
                    maxWidth =
                        if (constraints.hasBoundedWidth) {
                            max(size, constraints.maxWidth)
                        } else {
                            Constraints.Infinity
                        },
                    minHeight = max(size, constraints.minHeight),
                    maxHeight =
                        if (constraints.hasBoundedHeight) {
                            max(size, constraints.maxHeight)
                        } else {
                            Constraints.Infinity
                        },
                )
            val placeables = measurables.map { m -> m.measure(newConstraints) }
            var maxWidth = size
            var maxHeight = size
            placeables.forEach { child ->
                maxHeight = max(child.height, maxHeight)
                maxWidth = max(child.width, maxWidth)
            }
            layout(maxWidth, maxHeight) { placeables.forEach { child -> child.place(0, 0) } }
        },
        modifier = modifier,
        content = content,
    )
}

@Composable
fun FixedSize(size: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Layout(content = content, modifier = modifier) { measurables, _ ->
        val newConstraints = Constraints.fixed(size, size)
        val placeables = measurables.map { m -> m.measure(newConstraints) }
        layout(size, size) { placeables.forEach { child -> child.placeRelative(0, 0) } }
    }
}

@Composable
fun Align(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val newConstraints =
                Constraints(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight,
                )
            val placeables = measurables.map { m -> m.measure(newConstraints) }
            var maxWidth = constraints.minWidth
            var maxHeight = constraints.minHeight
            placeables.forEach { child ->
                maxHeight = max(child.height, maxHeight)
                maxWidth = max(child.width, maxWidth)
            }
            layout(maxWidth, maxHeight) {
                placeables.forEach { child -> child.placeRelative(0, 0) }
            }
        },
        content = content,
    )
}

@Composable
internal fun Padding(size: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val totalDiff = size * 2
            val targetMinWidth = constraints.minWidth - totalDiff
            val targetMaxWidth =
                if (constraints.hasBoundedWidth) {
                    constraints.maxWidth - totalDiff
                } else {
                    Constraints.Infinity
                }
            val targetMinHeight = constraints.minHeight - totalDiff
            val targetMaxHeight =
                if (constraints.hasBoundedHeight) {
                    constraints.maxHeight - totalDiff
                } else {
                    Constraints.Infinity
                }
            val newConstraints =
                Constraints(
                    minWidth = targetMinWidth.coerceAtLeast(0),
                    maxWidth = targetMaxWidth.coerceAtLeast(0),
                    minHeight = targetMinHeight.coerceAtLeast(0),
                    maxHeight = targetMaxHeight.coerceAtLeast(0),
                )
            val placeables = measurables.map { m -> m.measure(newConstraints) }
            var maxWidth = size
            var maxHeight = size
            placeables.forEach { child ->
                maxHeight = max(child.height + totalDiff, maxHeight)
                maxWidth = max(child.width + totalDiff, maxWidth)
            }
            layout(maxWidth, maxHeight) {
                placeables.forEach { child -> child.placeRelative(size, size) }
            }
        },
        content = content,
    )
}

@Composable
fun Wrap(
    modifier: Modifier = Modifier,
    minWidth: Int = 0,
    minHeight: Int = 0,
    content: @Composable () -> Unit = {},
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = max(placeables.maxByOrNull { it.width }?.width ?: 0, minWidth)
        val height = max(placeables.maxByOrNull { it.height }?.height ?: 0, minHeight)
        layout(width, height) { placeables.forEach { it.placeRelative(0, 0) } }
    }
}

@Composable
fun SimpleRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        var width = 0
        var height = 0
        val placeables =
            measurables.map { measurable ->
                measurable.measure(constraints.copy(maxWidth = constraints.maxWidth - width)).also {
                    width += it.width
                    height = max(height, it.height)
                }
            }
        layout(width, height) {
            var currentWidth = 0
            placeables.forEach {
                it.placeRelative(currentWidth, 0)
                currentWidth += it.width
            }
        }
    }
}

private class DrawCounterListener(private val view: View) : ViewTreeObserver.OnPreDrawListener {
    val latch = CountDownLatch(5)

    override fun onPreDraw(): Boolean {
        latch.countDown()
        if (latch.count > 0) {
            view.postInvalidate()
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(this)
        }
        return true
    }
}

fun Modifier.padding(padding: Int) = this.then(PaddingModifier(padding, padding, padding, padding))

private data class PaddingModifier(val left: Int, val top: Int, val right: Int, val bottom: Int) :
    LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable =
            measurable.measure(
                constraints.offset(horizontal = -left - right, vertical = -top - bottom)
            )
        return layout(
            constraints.constrainWidth(left + placeable.width + right),
            constraints.constrainHeight(top + placeable.height + bottom),
        ) {
            placeable.placeRelative(left, top)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int =
        measurable.minIntrinsicWidth((height - (top + bottom)).coerceAtLeast(0)) + (left + right)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int =
        measurable.maxIntrinsicWidth((height - (top + bottom)).coerceAtLeast(0)) + (left + right)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int =
        measurable.minIntrinsicHeight((width - (left + right)).coerceAtLeast(0)) + (top + bottom)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int =
        measurable.maxIntrinsicHeight((width - (left + right)).coerceAtLeast(0)) + (top + bottom)
}

internal val AlignTopLeft =
    object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints.copyMaxDimensions())
            return layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeRelative(0, 0)
            }
        }
    }

@Stable
class SquareModel(
    size: Int = 10,
    outerColor: Color = Color(0xFF000080),
    innerColor: Color = Color(0xFFFFFFFF),
) {
    var size: Int by mutableStateOf(size)
    var outerColor: Color by mutableStateOf(outerColor)
    var innerColor: Color by mutableStateOf(innerColor)
}

@Suppress("DEPRECATION")
// We only need this because IR compiler doesn't like converting lambdas to Runnables
internal fun AndroidComposeTestRule<*, TestActivity>.createAndroidComposeView(
    coroutineContext: CoroutineContext
): AndroidComposeView {
    val lifecycleOwner =
        object : LifecycleOwner {
            override val lifecycle: Lifecycle
                get() =
                    object : Lifecycle() {
                        override val currentState: Lifecycle.State
                            get() = Lifecycle.State.RESUMED

                        override fun addObserver(observer: LifecycleObserver) {}

                        override fun removeObserver(observer: LifecycleObserver) {}
                    }
        }
    val savedStateRegistryOwner =
        object : SavedStateRegistryOwner {
            val lifecycleRegistry = LifecycleRegistry.createUnsafe(this)
            private val controller =
                SavedStateRegistryController.create(this).apply { performRestore(Bundle()) }

            init {
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            }

            override val savedStateRegistry: SavedStateRegistry
                get() = controller.savedStateRegistry

            override val lifecycle: LifecycleRegistry
                get() = lifecycleRegistry
        }

    return AndroidComposeView(
        activity,
        ComposeViewContext(
            compositionContext = Recomposer(coroutineContext),
            lifecycleOwner = lifecycleOwner,
            savedStateRegistryOwner = savedStateRegistryOwner,
            viewModelStoreOwner = null,
            view = activity.window.decorView,
        ),
    )
}

@Suppress("DEPRECATION")
fun AndroidComposeTestRule<*, *>.findAndroidComposeView(): ViewGroup {
    val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
    return findAndroidComposeView(contentViewGroup)!!
}

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
fun AndroidComposeTestRule<*, *>.waitAndScreenShot(
    view: View,
    forceInvalidate: Boolean = true,
): Bitmap {
    val flushListener = DrawCounterListener(view)
    val offset = intArrayOf(0, 0)
    var handler: Handler? = null
    runOnUiThread {
        view.getLocationInWindow(offset)
        if (forceInvalidate) {
            view.viewTreeObserver.addOnPreDrawListener(flushListener)
            view.invalidate()
        }
        handler = Handler(Looper.getMainLooper())
    }

    if (forceInvalidate) {
        assertTrue("Drawing latch timed out", flushListener.latch.await(1, TimeUnit.SECONDS))
    }
    val width = view.width
    val height = view.height

    val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val srcRect = android.graphics.Rect(0, 0, width, height)
    srcRect.offset(offset[0], offset[1])
    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished =
        PixelCopy.OnPixelCopyFinishedListener { result ->
            copyResult = result
            latch.countDown()
        }
    PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler!!)
    assertTrue("Pixel copy latch timed out", latch.await(1, TimeUnit.SECONDS))
    assertEquals(PixelCopy.SUCCESS, copyResult)
    return dest
}

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<*>.runOnUiThreadIR(block: () -> Unit) {
    val runnable = Runnable { block() }
    runOnUiThread(runnable)
}

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<*>.findAndroidComposeView(): ViewGroup {
    val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
    return findAndroidComposeView(contentViewGroup)!!
}

fun findAndroidComposeView(parent: ViewGroup): ViewGroup? {
    for (index in 0 until parent.childCount) {
        val child = parent.getChildAt(index)
        if (child is ViewGroup) {
            if (child is Owner) return child
            else {
                val composeView = findAndroidComposeView(child)
                if (composeView != null) {
                    return composeView
                }
            }
        }
    }
    return null
}

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
fun androidx.test.rule.ActivityTestRule<*>.waitAndScreenShot(
    forceInvalidate: Boolean = true
): Bitmap = waitAndScreenShot(findAndroidComposeView(), forceInvalidate)

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.O)
fun androidx.test.rule.ActivityTestRule<*>.waitAndScreenShot(
    view: View,
    forceInvalidate: Boolean = true,
): Bitmap {
    val flushListener = DrawCounterListener(view)
    val offset = intArrayOf(0, 0)
    var handler: Handler? = null
    runOnUiThread {
        view.getLocationInWindow(offset)
        if (forceInvalidate) {
            view.viewTreeObserver.addOnPreDrawListener(flushListener)
            view.invalidate()
        }
        handler = Handler(Looper.getMainLooper())
    }

    if (forceInvalidate) {
        assertTrue("Drawing latch timed out", flushListener.latch.await(1, TimeUnit.SECONDS))
    }
    val width = view.width
    val height = view.height

    val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val srcRect = android.graphics.Rect(0, 0, width, height)
    srcRect.offset(offset[0], offset[1])
    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished =
        PixelCopy.OnPixelCopyFinishedListener { result ->
            copyResult = result
            latch.countDown()
        }
    PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler!!)
    assertTrue("Pixel copy latch timed out", latch.await(1, TimeUnit.SECONDS))
    assertEquals(PixelCopy.SUCCESS, copyResult)
    return dest
}

fun Modifier.background(model: SquareModel, isInner: Boolean) = drawBehind {
    drawRect(if (isInner) model.innerColor else model.outerColor)
}

@RequiresApi(Build.VERSION_CODES.O)
fun AndroidComposeTestRule<*, *>.validateSquareColors(
    outerColor: Color,
    innerColor: Color,
    size: Int,
    offset: Int = 0,
    totalSize: Int = size * 3,
) {
    waitForIdle()
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    assertEquals(totalSize, bitmap.width)
    assertEquals(totalSize, bitmap.height)
    val squareStart = (totalSize - size) / 2 + offset
    val squareEnd = totalSize - ((totalSize - size) / 2) + offset
    for (x in 0 until totalSize) {
        for (y in 0 until totalSize) {
            val pixel = Color(bitmap.getPixel(x, y))
            val expected =
                if (!(x !in squareStart..<squareEnd || y < squareStart || y >= squareEnd)) {
                    innerColor
                } else {
                    outerColor
                }
            assertColorsEqual(expected, pixel) {
                "Pixel within drawn rect[$x, $y] is $expected, but was $pixel"
            }
        }
    }
}
