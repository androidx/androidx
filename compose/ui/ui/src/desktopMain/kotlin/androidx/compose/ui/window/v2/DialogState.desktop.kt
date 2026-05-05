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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.requireReal
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.topLeft
import java.awt.Rectangle
import kotlinx.coroutines.channels.Channel


/**
 * Creates a [DialogState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param initialPosition The initial position of the dialog; default if `null`. All the
 * coordinates must be [Dp.isSpecified] and [Dp.isFinite], and the [DpOffset] object itself must be
 * [DpOffset.isSpecified].
 * @param initialSize The initial size of the dialog; default if `null`. All the
 * coordinates must be [Dp.isSpecified] and [Dp.isFinite], and the [DpSize] object itself must be
 * [DpOffset.isSpecified].
 */
@ExperimentalComposeUiApi
@Composable
fun rememberDialogStateWithBounds(
    initialPosition: DpOffset? = null,
    initialSize: DpSize? = null,
): DialogState = rememberSaveable(saver = DialogState.Saver) {
    DialogStateWithBounds(
        initialPosition = initialPosition,
        initialSize = initialSize,
    )
}

/**
 * Creates a [DialogState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param initialScreenProvider Provides the initial screen on which the dialog will be placed.
 * @param initialBoundsProvider Provides the initial bounds of the dialog.
 */
@ExperimentalComposeUiApi
@Composable
fun rememberDialogState(
    initialScreenProvider: WindowScreenProvider = WindowScreenProvider.Default,
    initialBoundsProvider: WindowBoundsProvider = WindowBoundsProvider.Default,
): DialogState = rememberSaveable(saver = DialogState.Saver) {
    DialogState(
        initialScreenProvider = initialScreenProvider,
        initialBoundsProvider = initialBoundsProvider,
    )
}


/**
 * Creates a [DialogState] with the specified initial values.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param initialSize The initial size of the dialog; default if `null`. All the
 * coordinates must be [Dp.isSpecified] and [Dp.isFinite], and the [DpSize] object itself must be
 * [DpOffset.isSpecified].
 * @param initialPosition The initial position of the dialog; default if `null`. All the
 * coordinates must be [Dp.isSpecified] and [Dp.isFinite], and the [DpOffset] object itself must be
 * [DpOffset.isSpecified].
 */
@ExperimentalComposeUiApi
fun DialogStateWithBounds(
    initialSize: DpSize? = null,
    initialPosition: DpOffset? = null,
): DialogState {
    val sizeProvider =
        initialSize?.let { WindowSizeProvider.Fixed(it) } ?: WindowSizeProvider.Default
    val positionProvider =
        initialPosition?.let { WindowPositionProvider.Absolute(it) } ?: WindowPositionProvider.Default
    return DialogState(
        initialBoundsProvider = WindowBoundsProvider(sizeProvider, positionProvider),
    )
}

/**
 * Creates a [DialogState] with the specified initial bounds provider.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 *
 * @param initialScreenProvider Provides the initial screen on which the dialog will be placed.
 * @param initialBoundsProvider Provides the initial bounds of the dialog.
 */
@ExperimentalComposeUiApi
fun DialogState(
    initialScreenProvider: WindowScreenProvider = WindowScreenProvider.Default,
    initialBoundsProvider: WindowBoundsProvider = WindowBoundsProvider.Default,
): DialogState = DialogState.createUninitialized().apply {
    requestScreen(initialScreenProvider)
    requestBounds(initialBoundsProvider)
}

/**
 * A state object that can be hoisted to control and observe dialog attributes (size, position).
 *
 * Note: this class may be moved to `androidx.compose.ui.window` before stabilization.
 */
@Stable
@ExperimentalComposeUiApi
class DialogState private constructor(
    isInitialized: Boolean,
    screenId: String?,
    bounds: DpRect?,
) {
    /**
     * Creates a new [DialogState] that is initialized with the specified values.
     */
    internal constructor(
        screenId: String,
        bounds: DpRect,
    ): this(
        isInitialized = true,
        screenId = screenId,
        bounds = bounds,
    )

    init {
        bounds?.requireReal()
    }

    /**
     * Whether the dialog associated with this state has become visible at least once.
     */
    var isInitialized: Boolean by mutableStateOf(isInitialized)
        internal set

    /**
     * The id of the screen with which the dialog is currently associated; `null` if the dialog is
     * not yet [isInitialized].
     */
    @Suppress("PropertyName")
    internal var _screenId: String? by mutableStateOf(screenId)

    /**
     * The id of the screen with which the dialog is currently associated; throws
     * [IllegalStateException] if the dialog is not yet [isInitialized].
     */
    val screenId: String
        get() = _screenId ?: dialogNotInitializedError("screenId")

    internal val screenRequests = Channel<WindowScreenProvider>(Channel.CONFLATED)

    /**
     * Requests to position the dialog on the specified screen.
     *
     * Note that the actual positioning is done asynchronously.
     */
    fun requestScreen(screenProvider: WindowScreenProvider) {
        screenRequests.trySend(screenProvider)
    }

    /**
     * The current bounds of the dialog; `null` if the dialog is not yet [isInitialized].
     */
    @Suppress("PropertyName")
    internal var _bounds: DpRect? by mutableStateOf(bounds)

    /**
     * The current bounds of the dialog; throws [IllegalStateException] if the dialog is not yet
     * [isInitialized].
     */
    val bounds: DpRect
        get() = _bounds ?: dialogNotInitializedError("bounds")

    internal val boundsRequests = Channel<WindowBoundsProvider>(Channel.UNLIMITED)

    /**
     * Requests to set the bounds of the dialog via a [WindowBoundsProvider].
     *
     * Note that the actual bounds are set asynchronously and may be different from the requested
     * ones (e.g., if the window manager can't position as requested).
     *
     * @param boundsProvider Provides the bounds to apply to the window.
     */
    fun requestBounds(boundsProvider: WindowBoundsProvider) {
        boundsRequests.trySend(boundsProvider)
    }

    /**
     * Requests to set the bounds of the dialog via a function that returns a [DpRect].
     *
     * Note that the actual bounds are set asynchronously and may be different from the requested
     * ones (e.g., if the window manager can't position as requested).
     *
     * @param boundsProvider Returns the bounds to apply to the window.
     */
    fun requestBounds(boundsProvider: WindowGeometryProviderScope.() -> DpRect) {
        boundsRequests.trySend(WindowBoundsProvider(boundsProvider))
    }

    /**
     * Requests to set the bounds of the dialog.
     *
     * This is the same as using [WindowBoundsProvider.Absolute].
     *
     *
     * Note that the actual bounds are set asynchronously and may be different from the requested
     * ones (e.g., if the window manager can't position as requested).
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
     * The current position of the dialog; throws [IllegalStateException] if the dialog is not yet
     * [isInitialized].
     */
    val position: DpOffset
        get() = _bounds?.topLeft ?: dialogNotInitializedError("position")

    /**
     * Requests to set the position of the dialog via a [WindowPositionProvider].
     *
     * Note that the actual position is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't position as requested).
     *
     * @param positionProvider Provides the position to apply to the dialog.
     */
    fun requestPosition(positionProvider: WindowPositionProvider) {
        boundsRequests.trySend(
            WindowBoundsProvider(
                positionProvider = positionProvider,
            )
        )
    }

    /**
     * Requests to set the position of the dialog.
     *
     * Note that the actual position is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't position as requested).
     *
     * @param position The position to apply to the dialog. The value must be [DpOffset.isSpecified]
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
     * Requests to set the position of the dialog.
     *
     * Note that the actual position is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't position as requested).
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
     * The current size of the dialog; throws [IllegalStateException] if the dialog is not yet
     * [isInitialized].
     */
    val size: DpSize
        get() = _bounds?.size ?: dialogNotInitializedError("size")

    /**
     * Requests to set the size of the dialog via a [WindowSizeProvider].
     *
     * Note that the actual size is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't size as requested).
     *
     * @param sizeProvider Provides the size to apply to the dialog.
     */
    fun requestSize(sizeProvider: WindowSizeProvider) {
        boundsRequests.trySend(
            WindowBoundsProvider(
                sizeProvider = sizeProvider,
            )
        )
    }

    /**
     * Requests to set the size of the dialog.
     *
     * Note that the actual size is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't size as requested).
     *
     * @param size The position to apply to the dialog. The value must be [DpSize.isSpecified]
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
     * Requests to set the size of the dialog.
     *
     * Note that the actual size is set asynchronously and may be different from the requested
     * one (e.g., if the window manager can't size as requested).
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
         * Creates a new [DialogState] that is not yet initialized.
         */
        internal fun createUninitialized() =
            DialogState(
                isInitialized = false,
                screenId = null,
                bounds = null
            )

        /**
         * A [Saver] implementation for [DialogState].
         */
        val Saver: Saver<DialogState, Any> = listSaver(
            save = {
                if (!it.isInitialized) return@listSaver emptyList()
                val bounds = it.bounds
                arrayListOf(
                    it.screenId,
                    bounds.top.value,
                    bounds.left.value,
                    bounds.right.value,
                    bounds.bottom.value,
                )
            },
            restore = { state ->
                if (state.isEmpty()) return@listSaver null
                DialogState(
                    screenId = state[0] as String,
                    bounds = DpRect(
                        top = Dp(state[1] as Float),
                        left = Dp(state[2] as Float),
                        right = Dp(state[3] as Float),
                        bottom = Dp(state[4] as Float)
                    )
                )
            }
        )
    }
}

/**
 * Returns the bounds of the dialog, as an AWT [Rectangle]; throws [IllegalStateException] if the
 * window is not yet [isInitialized].
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
val DialogState.awtBounds: Rectangle
    get() = bounds.toAwtRectangleRounded()

private fun dialogNotInitializedError(propertyName: String): Nothing =
    throw IllegalStateException("Can't read $propertyName before the dialog has been made visible;" +
        " use isInitialized to check.")