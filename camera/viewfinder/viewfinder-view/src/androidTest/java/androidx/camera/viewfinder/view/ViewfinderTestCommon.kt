/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.viewfinder.view

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Surface
import android.view.ViewGroup
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.test.core.app.ActivityScenario
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

const val ANY_WIDTH = 640
const val ANY_HEIGHT = 480

val ANY_TRANSFORMATION_INFO =
    TransformationInfo(
        sourceRotation = 0,
        cropRectLeft = 0f,
        cropRectTop = 0f,
        cropRectRight = ANY_WIDTH.toFloat(),
        cropRectBottom = ANY_HEIGHT.toFloat(),
        isSourceMirroredHorizontally = false,
        isSourceMirroredVertically = false,
    )

val REQUEST_TIMEOUT = 2.seconds

/** Size of the surface used with [drawPreRotatedQuadrants]. */
const val QUADRANTS_SURFACE_WIDTH = 200
const val QUADRANTS_SURFACE_HEIGHT = 400

/** Colors of the top-left, top-right, bottom-left and bottom-right quadrants when upright. */
val UPRIGHT_QUADRANT_COLORS = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)

/**
 * Draws [UPRIGHT_QUADRANT_COLORS] to this surface, pre-rotated counter-clockwise by
 * [sourceRotation] so that rotating the buffer clockwise by [sourceRotation] restores the upright
 * orientation.
 */
fun Surface.drawPreRotatedQuadrants(width: Int, height: Int, sourceRotation: Int) {
    val (topLeft, topRight, bottomLeft, bottomRight) =
        when (sourceRotation) {
            0 -> listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
            90 -> listOf(Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE)
            180 -> listOf(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED)
            270 -> listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN)
            else -> throw IllegalArgumentException("Unexpected rotation: $sourceRotation")
        }
    val paint = Paint().apply { style = Paint.Style.FILL }
    val halfWidth = width / 2f
    val halfHeight = height / 2f
    val canvas = lockHardwareCanvas()
    try {
        for ((color, rect) in
            listOf(
                topLeft to RectF(0f, 0f, halfWidth, halfHeight),
                topRight to RectF(halfWidth, 0f, width.toFloat(), halfHeight),
                bottomLeft to RectF(0f, halfHeight, halfWidth, height.toFloat()),
                bottomRight to RectF(halfWidth, halfHeight, width.toFloat(), height.toFloat()),
            )) {
            paint.color = color
            canvas.drawRect(rect, paint)
        }
    } finally {
        unlockCanvasAndPost(canvas)
    }
}

/** Returns whether each channel of this color is within [tolerance] of [expected]. */
fun Int.isCloseTo(expected: Int, tolerance: Int = 25): Boolean =
    abs(Color.red(this) - Color.red(expected)) <= tolerance &&
        abs(Color.green(this) - Color.green(expected)) <= tolerance &&
        abs(Color.blue(this) - Color.blue(expected)) <= tolerance

inline fun runViewfinderTest(
    viewfinderInitiallyAttached: Boolean = true,
    crossinline block: suspend ViewfinderTestScope.() -> Unit,
) {
    runBlocking {
        ActivityScenario.launch(FakeActivity::class.java).use { scenario ->
            var viewfinderTestScope: ViewfinderTestScope? = null
            scenario.onActivity { activity ->
                val viewfinderView = ViewfinderView(activity)
                if (viewfinderInitiallyAttached) {
                    activity.setContentView(viewfinderView)
                }
                viewfinderTestScope =
                    ViewfinderTestScope(
                        viewfinder = viewfinderView,
                        activityScenario = scenario,
                        coroutineContext = coroutineContext,
                        viewfinderInitiallyAttached,
                    )
            }
            val testScope =
                viewfinderTestScope ?: throw AssertionError("Unable to create ViewfinderTestScope")

            with(testScope) { block() }
        }
    }
}

data class ViewfinderTestScope(
    val viewfinder: ViewfinderView,
    private val activityScenario: ActivityScenario<FakeActivity>,
    override val coroutineContext: CoroutineContext,
    private var attached: Boolean = false,
) : CoroutineScope {
    fun detachViewfinder() {
        if (attached) {
            activityScenario.onActivity {
                (viewfinder.parent as? ViewGroup)?.removeView(viewfinder)
            }
            attached = false
        }
    }
}
