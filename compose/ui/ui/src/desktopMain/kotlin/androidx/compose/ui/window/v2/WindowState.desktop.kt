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

package androidx.compose.ui.window.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.toAwtRectangleRounded
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.requireReal
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.topLeft
import androidx.compose.ui.window.WindowPlacement
import java.awt.Rectangle
import kotlinx.coroutines.channels.Channel


/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param initialPosition The initial position of the window; default if `null`. All the
 * coordinates must be [Dp.isSpecified] and [Dp.isFinite], and the [DpOffset] object itself must be
 * [DpOffset.isSpecified].
 * @param initialSize The initial size of the window; default if `null`. All the
 * coordinates must be [Dp.isSpecified] and [Dp.isFinite], and the [DpSize] object itself must be
 * [DpOffset.isSpecified].
 * @param initiallyMinimized Whether the window is initially minimized.
 */
@ExperimentalComposeUiApi
@Composable
fun rememberWindowStateWithBounds(
    initialPosition: DpOffset? = null,
    initialSize: DpSize? = null,
    initiallyMinimized: Boolean = false,
): WindowState = rememberSaveable(saver = WindowState.Saver) {
    WindowStateWithBounds(
        initialPosition = initialPosition,
        initialSize = initialSize,
        initiallyMinimized = initiallyMinimized
    )
}

/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param initialScreenProvider Provides the initial screen on which the window will be placed.
 * @param initialPlacement The initial placement of the window.
 * @param initialBoundsProvider Provides the initial bounds of the window.
 * @param initiallyMinimized Whether the window is initially minimized.
 */
@ExperimentalComposeUiApi
@Composable
fun rememberWindowState(
    initialScreenProvider: WindowScreenProvider = WindowScreenProvider.Default,
    initialPlacement: WindowPlacement = WindowPlacement.Floating,
    initialBoundsProvider: WindowBoundsProvider = WindowBoundsProvider.Default,
    initiallyMinimized: Boolean = false,
): WindowState = rememberSaveable(saver = WindowState.Saver) {
    WindowState(
        initialScreenProvider = initialScreenProvider,
        initialPlacement = initialPlacement,
        initialBoundsProvider = initialBoundsProvider,
        initiallyMinimized = initiallyMinimized
    )
}


/**
 * Creates a [WindowState] with the specified initial values.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param initialPosition The initial position of the window; default if `null`. All the
 * coordinates must be [Dp.isSpecified] and [Dp.isFinite], and the [DpOffset] object itself must be
 * [DpOffset.isSpecified].
 * @param initialSize The initial size of the window; default if `null`. All the
 * coordinates must be [Dp.isSpecified] and [Dp.isFinite], and the [DpSize] object itself must be
 * [DpOffset.isSpecified].
 * @param initiallyMinimized Whether the window is initially minimized.
 */
@ExperimentalComposeUiApi
fun WindowStateWithBounds(
    initialPosition: DpOffset? = null,
    initialSize: DpSize? = null,
    initiallyMinimized: Boolean = false,
): WindowState {
    val sizeProvider =
        initialSize?.let { WindowSizeProvider.Fixed(it) } ?: WindowSizeProvider.Default
    val positionProvider =
        initialPosition?.let { WindowPositionProvider.Absolute(it) } ?: WindowPositionProvider.Default
    return WindowState(
        initialBoundsProvider = WindowBoundsProvider(sizeProvider, positionProvider),
        initiallyMinimized = initiallyMinimized
    )
}

/**
 * Creates a [WindowState] with the specified initial values.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param initialScreenProvider Provides the initial screen on which the window will be placed.
 * @param initialPlacement The initial placement of the window.
 * @param initialBoundsProvider Provides the initial bounds of the window.
 * @param initiallyMinimized Whether the window is initially minimized.
 */
@ExperimentalComposeUiApi
fun WindowState(
    initialScreenProvider: WindowScreenProvider = WindowScreenProvider.Default,
    initialPlacement: WindowPlacement = WindowPlacement.Floating,
    initialBoundsProvider: WindowBoundsProvider = WindowBoundsProvider.Default,
    initiallyMinimized: Boolean = false,
): WindowState = WindowState.createUninitialized().apply {
    requestScreen(initialScreenProvider)
    requestPlacement(initialPlacement)
    requestBounds(initialBoundsProvider)
    requestMinimized(initiallyMinimized)
}

/**
 * A state object that can be hoisted to control and observe window attributes
 * (size, position, etc.).
 *
 * Note: this class may be moved to `androidx.compose.ui.window` before stabilization.
 */
@Stable
@ExperimentalComposeUiApi
class WindowState private constructor(
    isInitialized: Boolean,
    screenId: String?,
    placement: WindowPlacement?,
    isMinimized: Boolean?,
    bounds: DpRect?,
) {
    /**
     * Creates a new [WindowState] that is initialized with the specified values.
     */
    internal constructor(
        screenId: String,
        placement: WindowPlacement,
        isMinimized: Boolean,
        bounds: DpRect,
    ): this(
        isInitialized = true,
        screenId = screenId,
        placement = placement,
        isMinimized = isMinimized,
        bounds = bounds,
    )

    init {
        bounds?.requireReal()
    }

    /**
     * Whether the window associated with this state has become visible at least once.
     */
    var isInitialized: Boolean by mutableStateOf(isInitialized)
        internal set

    /**
     * The id of the screen with which the window is currently associated; `null` if the window is
     * not yet [isInitialized].
     */
    @Suppress("PropertyName")
    internal var _screenId: String? by mutableStateOf(screenId)

    /**
     * The id of the screen with which the window is currently associated; throws
     * [IllegalStateException] if the window is not yet [isInitialized].
     */
    val screenId: String
        get() = _screenId ?: windowNotInitializedError("screenId")

    internal val screenRequests = Channel<WindowScreenProvider>(Channel.CONFLATED)

    /**
     * Requests to position the window on the specified screen.
     *
     * Note that the actual positioning is done asynchronously.
     */
    fun requestScreen(screenProvider: WindowScreenProvider) {
        screenRequests.trySend(screenProvider)
    }

    /**
     * The placement of the window on the screen; `null` if the window is not yet [isInitialized].
     */
    @Suppress("PropertyName")
    internal var _placement: WindowPlacement? by mutableStateOf(placement)

    /**
     * The placement of the window on the screen; throws [IllegalStateException] if the window is
     * not yet [isInitialized].
     */
    val placement: WindowPlacement
        get() = _placement ?: windowNotInitializedError("placement")

    internal val placementRequests = Channel<WindowPlacement>(Channel.CONFLATED)

    /**
     * Requests to set the placement of the window.
     *
     * Note that the actual placement is set asynchronously.
     */
    fun requestPlacement(placement: WindowPlacement) {
        placementRequests.trySend(placement)
    }

    /**
     * Whether the window is minimized; `null` if the window is not [isInitialized] yet.
     */
    @Suppress("PropertyName")
    internal var _isMinimized: Boolean? by mutableStateOf(isMinimized)

    /**
     * Whether the window is minimized; throws [IllegalStateException] if the window is not yet
     * [isInitialized].
     */
    val isMinimized: Boolean
        get() = _isMinimized ?: windowNotInitializedError("isMinimized")

    internal val isMinimizedRequests = Channel<Boolean>(Channel.CONFLATED)

    /**
     * Requests to set the minimized state of the window.
     *
     * Note that the actual minimized state is set asynchronously.
     */
    fun requestMinimized(value: Boolean) {
        isMinimizedRequests.trySend(value)
    }

    /**
     * The current bounds of the window; `null` if the window is not yet [isInitialized].
     */
    @Suppress("PropertyName")
    internal var _bounds: DpRect? by mutableStateOf(bounds)

    /**
     * The current bounds of the window; throws [IllegalStateException] if the window is not yet
     * [isInitialized].
     */
    val bounds: DpRect
        get() = _bounds ?: windowNotInitializedError("bounds")

    internal val boundsRequests = Channel<WindowBoundsProvider>(Channel.UNLIMITED)

    /**
     * Requests to set the bounds of the window via a [WindowBoundsProvider].
     *
     * Note that the actual bounds are set asynchronously and may be different from the requested
     * ones (e.g., if the window manager can't position as requested).
     *
     * Setting the bounds when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param boundsProvider Provides the bounds to apply to the window.
     */
    fun requestBounds(boundsProvider: WindowBoundsProvider) {
        boundsRequests.trySend(boundsProvider)
    }

    /**
     * Requests to set the bounds of the window via a function that returns a [DpRect].
     *
     * Note that the actual bounds are set asynchronously and may be different from the requested
     * ones (e.g., if the window manager can't position as requested).
     *
     * Setting the bounds when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param boundsProvider Returns the bounds to apply to the window.
     */
    fun requestBounds(boundsProvider: WindowGeometryProviderScope.() -> DpRect) {
        boundsRequests.trySend(WindowBoundsProvider(boundsProvider))
    }

    /**
     * Requests to set the bounds of the window.
     *
     * This is the same as using [WindowBoundsProvider.Absolute].
     *
     * Note that the actual bounds are set asynchronously and may be different from the requested
     * ones (e.g., if the window manager can't position as requested).
     *
     * Setting the bounds when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param bounds The bounds to apply to the window. All the coordinates must be [Dp.isSpecified]
     * and [Dp.isFinite].
     */
    fun requestBounds(bounds: DpRect) {
        boundsRequests.trySend(
            WindowBoundsProvider.Absolute(bounds)
        )
    }

    /**
     * The current position of the window; throws [IllegalStateException] if the window is not yet
     * [isInitialized].
     */
    val position: DpOffset
        get() = _bounds?.topLeft ?: windowNotInitializedError("position")

    /**
     * Requests to set the position of the window via a [WindowPositionProvider].
     *
     * Note that the actual position is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't position as requested).
     *
     * Setting the position when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param positionProvider Provides the position to apply to the window.
     */
    fun requestPosition(positionProvider: WindowPositionProvider) {
        boundsRequests.trySend(
            WindowBoundsProvider(
                positionProvider = positionProvider,
            )
        )
    }

    /**
     * Requests to set the position of the window.
     *
     * Note that the actual position is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't position as requested).
     *
     * Setting the position when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param position The position to apply to the window. The value must be [DpOffset.isSpecified]
     * and all the coordinates must be [Dp.isSpecified] and [Dp.isFinite].
     */
    fun requestPosition(position: DpOffset) {
        boundsRequests.trySend(
            WindowBoundsProvider(
                positionProvider = WindowPositionProvider.Absolute(position),
            )
        )
    }

    /**
     * Requests to set the position of the window.
     *
     * Note that the actual position is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't position as requested).
     *
     * Setting the position when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param x The x coordinate. The value must be [Dp.isSpecified] and [Dp.isFinite].
     * @param y The y coordinate. The value must be [Dp.isSpecified] and [Dp.isFinite].
     */
    fun requestPosition(x: Dp, y: Dp) {
        boundsRequests.trySend(
            WindowBoundsProvider(
                positionProvider = WindowPositionProvider.Absolute(x, y),
            )
        )
    }


    /**
     * The current size of the window; throws [IllegalStateException] if the window is not yet
     * [isInitialized].
     */
    val size: DpSize
        get() = _bounds?.size ?: windowNotInitializedError("size")

    /**
     * Requests to set the size of the window via a [WindowSizeProvider].
     *
     * Note that the actual size is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't size as requested).
     *
     * Setting the size when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param sizeProvider Provides the size to apply to the window.
     */
    fun requestSize(sizeProvider: WindowSizeProvider) {
        boundsRequests.trySend(
            WindowBoundsProvider(
                sizeProvider = sizeProvider,
            )
        )
    }

    /**
     * Requests to set the size of the window.
     *
     * Note that the actual size is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't size as requested).
     *
     * Setting the size when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param size The position to apply to the window. The value must be [DpSize.isSpecified]
     * and all the coordinates must be [Dp.isSpecified] and [Dp.isFinite].
     */
    fun requestSize(size: DpSize) {
        boundsRequests.trySend(
            WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(size),
            )
        )
    }

    /**
     * Requests to set the size of the window.
     *
     * Note that the actual size is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't size as requested).
     *
     * Setting the size when the window placement is not [WindowPlacement.Floating] will change
     * the placement to floating.
     *
     * @param width The width. The value must be [Dp.isSpecified] and [Dp.isFinite].
     * @param height The height. The value must be [Dp.isSpecified] and [Dp.isFinite].
     */
    fun requestSize(width: Dp, height: Dp) {
        boundsRequests.trySend(
            WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(width, height),
            )
        )
    }

    @ExperimentalComposeUiApi
    companion object {
        /**
         * Creates a new [WindowState] that is not yet initialized.
         */
        internal fun createUninitialized() =
            WindowState(
                isInitialized = false,
                screenId = null,
                placement = null,
                isMinimized = null,
                bounds = null
            )

        /**
         * A [Saver] implementation for [WindowState].
         */
        val Saver: Saver<WindowState, Any> = listSaver(
            save = {
                if (!it.isInitialized) return@listSaver emptyList()
                val bounds = it.bounds
                arrayListOf(
                    it.screenId,
                    it.placement.ordinal,
                    it.isMinimized,
                    bounds.top.value,
                    bounds.left.value,
                    bounds.right.value,
                    bounds.bottom.value,
                )
            },
            restore = { state ->
                if (state.isEmpty()) return@listSaver null
                WindowState(
                    screenId = state[0] as String,
                    placement = WindowPlacement.entries[(state[1] as Int)],
                    isMinimized = state[2] as Boolean,
                    bounds = DpRect(
                        top = Dp(state[3] as Float),
                        left = Dp(state[4] as Float),
                        right = Dp(state[5] as Float),
                        bottom = Dp(state[6] as Float)
                    )
                )
            }
        )
    }
}

/**
 * Returns the bounds of the window, as an AWT [Rectangle]; throws [IllegalStateException] if the
 * window is not yet [isInitialized].
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
val WindowState.awtBounds: Rectangle
    get() = bounds.toAwtRectangleRounded()

private fun windowNotInitializedError(propertyName: String): Nothing =
    throw IllegalStateException("Can't read $propertyName before the window has been made visible;" +
        " use isInitialized to check.")