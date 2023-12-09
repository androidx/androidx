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

package androidx.compose.ui.window

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropTransaction
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Canvas
import platform.CoreGraphics.CGPoint
import platform.Foundation.NSTimeInterval
import platform.UIKit.UIEvent
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UIView

internal enum class UITouchesEventPhase {
    BEGAN, MOVED, ENDED, CANCELLED
}

internal interface SkikoUIViewDelegate {
    fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean
    fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase)
    fun retrieveInteropTransaction(): UIKitInteropTransaction
    fun render(canvas: Canvas, targetTimestamp: NSTimeInterval)
    var metalOffset: Offset
}

internal class SkikoUIViewDelegateImpl(
    private val sceneProvider: () -> ComposeScene,
    private val interopContext: UIKitInteropContext,
    private val densityProvider: DensityProvider,
) : SkikoUIViewDelegate {
    val scene get() = sceneProvider()
    val density get() = densityProvider()
    override var metalOffset: Offset = Offset.Zero

    override fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean =
        point.useContents {
            val position = Offset(
                (x * density.density).toFloat(),
                (y * density.density).toFloat()
            )

            !scene.hitTestInteropView(position)
        }

    override fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase) {
        scene.sendPointerEvent(
            eventType = phase.toPointerEventType(),
            pointers = event.touchesForView(view)?.map {
                val touch = it as UITouch
                val id = touch.hashCode().toLong()
                val position = touch.offsetInView(view, density.density)
                ComposeScenePointer(
                    id = PointerId(id),
                    position = position,
                    pressed = touch.isPressed,
                    type = PointerType.Touch,
                    pressure = touch.force.toFloat(),
                    historical = event.historicalChangesForTouch(touch, view, density.density)
                )
            } ?: emptyList(),
            timeMillis = (event.timestamp * 1e3).toLong(),
            nativeEvent = event
        )
    }

    override fun retrieveInteropTransaction(): UIKitInteropTransaction =
        interopContext.retrieve()

    override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
        // The calculation is split in two instead of
        // `(targetTimestamp * 1e9).toLong()`
        // to avoid losing precision for fractional part
        val integral = floor(targetTimestamp)
        val fractional = targetTimestamp - integral
        val secondsToNanos = 1_000_000_000L
        val nanos = integral.roundToLong() * secondsToNanos + (fractional * 1e9).roundToLong()
        val composeCanvas = canvas.asComposeCanvas()
        val dx = metalOffset.x
        val dy = metalOffset.y
        composeCanvas.translate(dx, dy)
        scene.render(composeCanvas, nanos)
        composeCanvas.translate(-dx, -dy)
    }

}

private fun UITouchesEventPhase.toPointerEventType(): PointerEventType =
    when (this) {
        UITouchesEventPhase.BEGAN -> PointerEventType.Press
        UITouchesEventPhase.MOVED -> PointerEventType.Move
        UITouchesEventPhase.ENDED -> PointerEventType.Release
        UITouchesEventPhase.CANCELLED -> PointerEventType.Release
    }

private fun UIEvent.historicalChangesForTouch(
    touch: UITouch,
    view: UIView,
    density: Float
): List<HistoricalChange> {
    val touches = coalescedTouchesForTouch(touch) ?: return emptyList()

    return if (touches.size > 1) {
        // subList last index is exclusive, so the last touch in the list is not included
        // because it's the actual touch for which coalesced touches were requested
        touches.dropLast(1).map {
            val historicalTouch = it as UITouch
            HistoricalChange(
                uptimeMillis = (historicalTouch.timestamp * 1e3).toLong(),
                position = historicalTouch.offsetInView(view, density)
            )
        }
    } else {
        emptyList()
    }
}

private val UITouch.isPressed
    get() = when (phase) {
        UITouchPhase.UITouchPhaseEnded, UITouchPhase.UITouchPhaseCancelled -> false
        else -> true
    }

private fun UITouch.offsetInView(view: UIView, density: Float): Offset =
    locationInView(view).useContents {
        Offset(x.toFloat() * density, y.toFloat() * density)
    }
