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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Dp
import kotlin.jvm.JvmInline

/**
 * A receiver scope that provides access to the properties of the current media environment. This
 * scope is used by [mediaQuery] to evaluate conditions based on the device and window state.
 */
@ExperimentalMediaQueryApi
interface UiMediaScope {
    /**
     * The current posture of the application window.
     *
     * This reflects how the window is laid out on the screen, which may be affected by the device's
     * physical state. See [Posture] for possible values.
     */
    val windowPosture: Posture

    /** The current width of the application window. */
    @get:FrequentlyChangingValue val windowWidth: Dp

    /** The current height of the application window. */
    @get:FrequentlyChangingValue val windowHeight: Dp

    /**
     * The highest-precision pointing device currently available.
     *
     * This property resolves the pointer type based on all connected input devices, prioritizing
     * higher precision (e.g., [PointerPrecision.Fine] takes precedence over
     * [PointerPrecision.Coarse] if both are present). See [PointerPrecision] for all possible
     * values.
     */
    val pointerPrecision: PointerPrecision

    /**
     * The type of keyboard currently available or connected.
     *
     * This property prioritizes a physical keyboard connection ([AnyKeyboard.Physical]) over an
     * on-screen soft keyboard ([AnyKeyboard.Virtual]). If neither is detected, it returns
     * [AnyKeyboard.None].
     */
    val keyboardKind: KeyboardKind

    /** Whether the microphone is supported on the current device. */
    @get:Suppress("GetterSetterNames") val hasMicrophone: Boolean

    /** Whether the camera is supported on the current device. */
    @get:Suppress("GetterSetterNames") val hasCamera: Boolean

    /**
     * The typical distance between the user and the device screen.
     *
     * This property provides an estimate of the viewing distance, which can be useful for adjusting
     * UI scale, content density, or hit target sizes for better ergonomics and readability.
     *
     * Note that this is a broad categorization and does not represent a precise physical
     * measurement. It is based on the device type and its typical usage context.
     */
    val viewingDistance: ViewingDistance

    /**
     * Describes the posture of the window, typically on a foldable device.
     *
     * This represents the arrangement of the window's display area in relation to physical features
     * like hinges or folds.
     *
     * Note that this describes the window's state, which may differ from the physical device
     * posture. For example, if the device is in a half-folded state but the app is in split-screen
     * mode on a single panel, the window posture will be [Posture.Flat].
     */
    @JvmInline
    @ExperimentalMediaQueryApi
    value class Posture private constructor(private val description: String) {
        override fun toString(): String = description

        companion object {
            /**
             * Represents a flat posture, where the foldable device's screen is fully open and flat,
             * similar to a traditional phone or tablet. It's the default posture for non-foldable
             * devices.
             */
            val Flat = Posture("Flat")

            /**
             * Represents a device in a semi-open state, similar to a laptop. The flexible display
             * area is split into two logical parts, with a fold in between.
             */
            val Tabletop = Posture("Tabletop")

            /**
             * The device is folded similarly to an open book, with the flexible screen area
             * oriented vertically.
             */
            val Book = Posture("Book")
        }
    }

    /** Describes the precision of the available pointing devices. */
    @JvmInline
    @ExperimentalMediaQueryApi
    value class PointerPrecision private constructor(private val description: String) {
        override fun toString(): String = description

        companion object {
            /**
             * Represents a pointing device with high precision, such as a mouse, trackpad, or
             * stylus.
             */
            val Fine = PointerPrecision("Fine")

            /** Represents a pointing device with limited precision, such as a touchscreen. */
            val Coarse = PointerPrecision("Coarse")

            /** Represents a pointing device with low precision, such as a joystick. */
            val Blunt = PointerPrecision("Blunt")

            /** Indicates that no pointing device is available. */
            val None = PointerPrecision("None")
        }
    }

    /** Describes the kind of keyboard available. */
    @JvmInline
    @ExperimentalMediaQueryApi
    value class KeyboardKind private constructor(private val description: String) {
        override fun toString(): String = description

        companion object {
            /** Represents a physical hardware keyboard. */
            val Physical = KeyboardKind("Physical")

            /** Represents an on-screen virtual keyboard (IME). */
            val Virtual = KeyboardKind("Virtual")

            /**
             * Indicates that no keyboard is currently available for input.
             *
             * This state occurs when no physical keyboard is connected to the device, and the
             * on-screen software keyboard (IME) is currently hidden or closed.
             */
            val None = KeyboardKind("None")
        }
    }

    /** Describes the typical distance between the user and the screen. */
    @JvmInline
    @ExperimentalMediaQueryApi
    value class ViewingDistance private constructor(private val description: String) {
        override fun toString(): String = description

        companion object {
            /**
             * Represents a device used within close range, such as a handheld phone, tablet,
             * laptop, or desktop monitor. This is the default for most personal devices.
             */
            val Near = ViewingDistance("Near")

            /**
             * Represents a device positioned slightly further away, such as an automotive device,
             * or a tablet in a dock mode.
             */
            val Medium = ViewingDistance("Medium")

            /** Represents a device viewed from a significant distance, such as a television. */
            val Far = ViewingDistance("Far")
        }
    }
}

/**
 * A [UiMediaScope] provides information about the window environment and input devices, enabling
 * adaptive layouts and behavior. This `CompositionLocal` is the primary mechanism to access the
 * scope within the composable tree.
 *
 * It is typically provided at the root of an application. Accessing it without a provider will
 * result in a runtime error.
 */
@ExperimentalMediaQueryApi
val LocalUiMediaScope =
    staticCompositionLocalOf<UiMediaScope> {
        error("CompositionLocal LocalUiMediaScope not present")
    }

/**
 * Evaluates a boolean query against the current [UiMediaScope].
 *
 * Avoid reading properties marked with `@FrequentlyChangingValue` (such as
 * [UiMediaScope.windowWidth] or [UiMediaScope.windowHeight]) inside the `query` block. Reading
 * these properties will cause the composable to recompose on every frame during events such as
 * window resizing, which can affect the performance.
 *
 * For queries involving such frequently changing values, use [derivedMediaQuery] instead.
 *
 * @sample androidx.compose.ui.samples.FoldableAwareSample
 * @sample androidx.compose.ui.samples.AdaptiveButtonSample
 * @param query The condition to evaluate against the [UiMediaScope].
 * @return The boolean result of the query.
 */
@ExperimentalMediaQueryApi
@Composable
@ReadOnlyComposable
fun mediaQuery(query: UiMediaScope.() -> Boolean): Boolean = LocalUiMediaScope.current.query()

/**
 * Evaluates a boolean query against the current [UiMediaScope], wrapped in a [derivedStateOf].
 *
 * Use this function for queries that involve frequently changing values, such as
 * [UiMediaScope.windowWidth] or [UiMediaScope.windowHeight]. It ensures that compositions only
 * recompose when the boolean result of the [query] changes, not on every small change to the
 * underlying values (like a 1px size change).
 *
 * For queries on stable properties, you can use the simpler [mediaQuery] function.
 *
 * @sample androidx.compose.ui.samples.MediaQuerySample
 * @param query The condition to evaluate against the [UiMediaScope].
 * @return A [State] holding the boolean result of the query. The state will only update when the
 *   evaluated result of the query changes.
 */
@ExperimentalMediaQueryApi
@Composable
fun derivedMediaQuery(query: UiMediaScope.() -> Boolean): State<Boolean> {
    val mediaScope = LocalUiMediaScope.current
    val currentQuery by rememberUpdatedState(query)

    return remember(mediaScope) { derivedStateOf { mediaScope.currentQuery() } }
}

/**
 * Evaluates a boolean query against the current [UiMediaScope] from a
 * [CompositionLocalAccessorScope].
 *
 * If called within a snapshot-aware context, the specific property reads within the query will be
 * tracked, and the scope will be invalidated when any of those properties change.
 *
 * @sample androidx.compose.ui.samples.AdaptiveStylesSample
 * @param query A lambda expression with [UiMediaScope] as its receiver, representing the condition
 *   to check.
 * @return The immediate boolean result of the query.
 */
@ExperimentalMediaQueryApi
inline fun CompositionLocalAccessorScope.mediaQuery(query: UiMediaScope.() -> Boolean): Boolean =
    LocalUiMediaScope.currentValue.query()

/**
 * Evaluates a boolean query against the current [UiMediaScope] from a [Modifier.Node].
 *
 * This function is designed to be used within a [Modifier.Node] that implements
 * [CompositionLocalConsumerModifierNode].
 *
 * If called within a snapshot-aware context like [LayoutModifierNode.measure] or
 * [DrawModifierNode.draw] callbacks, the reads within the query will be tracked, and the scope will
 * be invalidated when the properties change.
 *
 * @sample androidx.compose.ui.samples.MediaQueryModifierNodeSample
 * @param query A lambda expression with [UiMediaScope] as its receiver, representing the condition
 *   to check.
 * @return The immediate boolean result of the query.
 */
@ExperimentalMediaQueryApi
inline fun CompositionLocalConsumerModifierNode.mediaQuery(
    query: UiMediaScope.() -> Boolean
): Boolean = currentValueOf(LocalUiMediaScope).query()
