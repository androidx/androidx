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

@file:Suppress("DEPRECATION")
@file:OptIn(ExperimentalWasmJsInterop::class)

package androidx.compose.foundation.gestures

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.InternalFoundationApi
import androidx.compose.ui.dom.domEventOrNull
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlin.js.toDouble
import kotlin.math.abs
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.WheelEvent

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig = JsConfig

private object JsConfig : ScrollConfig {
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        return when (val deltaMode = (event.domEventOrNull as? WheelEvent)?.deltaMode) {
            WheelEvent.DOM_DELTA_LINE -> event.totalScrollDelta * -defaultLineScrollHeight.dp.toPx()

            WheelEvent.DOM_DELTA_PAGE ->
                Offset(
                    x = event.totalScrollDelta.x * bounds.width,
                    y = event.totalScrollDelta.y * bounds.height,
                ) * -1f

            WheelEvent.DOM_DELTA_PIXEL -> event.totalScrollDelta * -1.dp.toPx()

            else -> {
                println("Unknown delta mode: $deltaMode")
                event.totalScrollDelta * -1.dp.toPx()
            }
        }
    }

    private class LastWheelEvent(val deltaX: Double, val deltaY: Double, val timeStamp: Double)

    // Information about the previously processed wheel event, used to disambiguate
    // trackpad gestures from mouse wheel ticks (see [isTrackpadEvent]).
    private var lastWheelEvent: LastWheelEvent? = null
    private var lastWheelEventWasTrackpad = false

    override fun isPreciseWheelScroll(event: PointerEvent): Boolean {
        val wheelEvent = event.domEventOrNull as? WheelEvent
        if (wheelEvent == null) {
            lastWheelEvent = null
            lastWheelEventWasTrackpad = false
            return false
        }
        val isTrackpad = isTrackpadEvent(wheelEvent)
        val isPrecise = wheelEvent.deltaMode != WheelEvent.DOM_DELTA_PIXEL || isTrackpad
        lastWheelEvent = LastWheelEvent(
            deltaX = wheelEvent.deltaX,
            deltaY = wheelEvent.deltaY,
            timeStamp = wheelEvent.timeStamp.toDouble(),
        )
        lastWheelEventWasTrackpad = isTrackpad
        return isPrecise
    }

    /**
     * Heuristically detects whether a wheel event comes from a high-resolution input device
     * (a trackpad or a freely rotating, notch-less wheel) rather than a regular stepping
     * mouse wheel. High-resolution input should be applied immediately, while a stepping
     * wheel animates between ticks.
     */
    private fun isTrackpadEvent(event: WheelEvent): Boolean {
        // The disambiguation below reasons about pixel deltas. Line- and page-mode deltas are
        // already discrete, device-independent units (a line, a viewport), so there is no
        // trackpad/stepping-wheel ambiguity to resolve.
        if (event.deltaMode != WheelEvent.DOM_DELTA_PIXEL) {
            return false
        }
        // Firefox restricts the legacy wheelDelta properties, so they don't provide enough
        // information to reliably disambiguate trackpad events from mouse wheel events.
        // wheelDelta* are non-standard/deprecated (never adopted into the spec; present only
        // in Blink/WebKit/EdgeHTML) and Firefox derives them from deltaY rather than the raw
        // device value, so they carry no independent device information here. See:
        // https://developer.mozilla.org/en-US/docs/Web/API/WheelEvent
        // https://developer.mozilla.org/en-US/docs/Web/API/Element/mousewheel_event
        // https://github.com/w3c/uievents/issues/138
        if (isFirefox) {
            return false
        }
        val wheelDeltaX = legacyWheelDeltaX(event).takeUnless { it.isNaN() }
        val wheelDeltaY = legacyWheelDeltaY(event).takeUnless { it.isNaN() }
        if (
            isAcceleratedMouseWheelDelta(event.deltaX, wheelDeltaX) ||
            isAcceleratedMouseWheelDelta(event.deltaY, wheelDeltaY)
        ) {
            return false
        }
        // While not in any formal web standard, Blink and WebKit browsers use a delta of 120
        // to represent one mouse wheel turn. If both axes of the delta (or of wheelDelta) are
        // divisible by 120, this event is probably from a mouse. The 120-per-notch convention
        // (Windows WHEEL_DELTA, mirrored on Linux as 120/-120) was chosen for its divisibility
        // so higher-resolution wheels can report clean fractions of a notch. See:
        // https://devblogs.microsoft.com/oldnewthing/20130123-00/?p=5473
        val looksLikeMouseTick =
            (event.deltaX % 120.0 == 0.0 && event.deltaY % 120.0 == 0.0) ||
                ((wheelDeltaX ?: 1.0) % 120.0 == 0.0 && (wheelDeltaY ?: 1.0) % 120.0 == 0.0)
        if (looksLikeMouseTick) {
            val last = lastWheelEvent ?: return false
            val deltaXChange = abs(event.deltaX - last.deltaX)
            val deltaYChange = abs(event.deltaY - last.deltaY)
            // A trackpad event might by chance have a delta of exactly 120, so make sure this
            // event doesn't have a similar delta to the previous one before treating it as a
            // mouse wheel.
            // Note: the 50ms window and the 20.0 delta-change threshold below are empirical
            // anti-flapping values with no normative source;
            // If a large-delta event was preceded within 50ms by a trackpad event, it is
            // likely an unlucky 120-delta trackpad event during rapid movement.
            return lastWheelEventWasTrackpad &&
            event.timeStamp.toDouble() - last.timeStamp < 50.0 &&
            ((deltaXChange == 0.0 && deltaYChange == 0.0) || !(deltaXChange < 20.0 && deltaYChange < 20.0))
        }
        return true
    }

    private fun isAcceleratedMouseWheelDelta(delta: Double, wheelDelta: Double?): Boolean {
        // On macOS, scrolling with a mouse wheel applies an acceleration curve, so delta
        // values ramp up and are not fixed multiples of 120, but the wheelDelta property
        // keeps its original value: by convention three times the delta with the opposite
        // sign. Allow +-1px error to account for integer truncation.
        // The factor of 3 is the WebKit/Blink ratio of one notch's wheelDelta (120) to its
        // pixel delta (3 lines x pixelsPerLineStep == 40px). macOS applies acceleration to
        // delta but not to wheelDelta for non-continuous wheels, which is what we detect.
        if (wheelDelta == null) return false
        // Real wheel events always report wheelDelta with the sign opposite to delta. A
        // same-signed (or zero) pair only appears for programmatically synthesized events
        // that copy delta into wheelDelta verbatim; that can't be a hardware acceleration
        // artifact, so don't treat it as a mouse wheel here.
        if (delta * wheelDelta >= 0.0) return false
        return abs(wheelDelta - (-3.0 * delta)) > 1.0
    }

    fun resetWheelTracking() {
        lastWheelEvent = null
        lastWheelEventWasTrackpad = false
    }
}

/**
 * Clears the wheel-event tracking state held by the [JsConfig] singleton. The state is global
 * (shared across all scrollables on the page), so tests that dispatch synthetic wheel events
 * must reset it between cases to avoid one test's last event leaking into the next.
 */
@VisibleForTesting
@InternalFoundationApi
public fun resetWheelEventTrackingForTests(): Unit = JsConfig.resetWheelTracking()

private val PointerEvent.totalScrollDelta
    get() = this.changes.fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta }

/** Whether the current browser is Firefox, detected once from the user agent. */
private val isFirefox: Boolean by lazy {
    window.navigator.userAgent.contains("firefox", ignoreCase = true)
}

// The legacy wheelDeltaX/wheelDeltaY properties are non-standard and may be absent (e.g. in
// Firefox), in which case these helpers return NaN to represent an unavailable value.
private fun legacyWheelDeltaX(event: WheelEvent): Double =
    js("(event.wheelDeltaX == null) ? NaN : event.wheelDeltaX")

private fun legacyWheelDeltaY(event: WheelEvent): Double =
    js("(event.wheelDeltaY == null) ? NaN : event.wheelDeltaY")

/**
 * The default line height (in dp) used to convert line-mode wheel deltas to pixels.
 */
private val defaultLineScrollHeight: Float by lazy { computeDefaultLineScrollHeight() }

private const val FallbackLineScrollHeight = 16f
private fun computeDefaultLineScrollHeight(): Float {
    val body = document.body ?: return FallbackLineScrollHeight
    val probe = document.createElement("div") as HTMLElement
    probe.style.fontSize = "initial"
    probe.style.display = "none"
    body.appendChild(probe)
    val fontSize = window.getComputedStyle(probe).fontSize
    body.removeChild(probe)
    return fontSize.removeSuffix("px").toFloatOrNull() ?: FallbackLineScrollHeight
}
