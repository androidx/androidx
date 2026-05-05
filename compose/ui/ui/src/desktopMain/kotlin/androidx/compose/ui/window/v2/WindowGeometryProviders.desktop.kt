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

import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.MeasurableRootContent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpInsets
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.minus
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.requireReal
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.topLeft
import androidx.compose.ui.unit.width
import androidx.compose.ui.window.WindowLocationTracker
import androidx.compose.ui.window.density
import androidx.compose.ui.window.roundToDimension
import androidx.compose.ui.window.toDpInsets
import androidx.compose.ui.window.toDpOffset
import androidx.compose.ui.window.toDpRect
import java.awt.GraphicsDevice
import kotlin.math.roundToInt


/**
 * The scope in which [WindowScreenProvider] is evaluated.
 *
 * Note: this class may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
class WindowScreenProviderScope internal constructor(
    devices: List<GraphicsDevice>,
    defaultDevice: GraphicsDevice,
) {
    /**
     * The list of screens on which the window can be placed.
     */
    val screens: List<Screen> = devices.map { Screen(it) }

    /**
     * The default screen, on which the window should typically be placed.
     */
    val defaultScreen: Screen = Screen(defaultDevice)

    /**
     * Evaluates the given [WindowScreenProvider] in this scope.
     */
    internal fun WindowScreenProvider.getScreen(): Screen = with(this) {
        this@WindowScreenProviderScope.getScreen()
    }
}

/**
 * Provides the screen on which the window will be placed.
 *
 * Note: this interface may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
fun interface WindowScreenProvider {
    /**
     * Returns the screen on which the window will be placed.
     *
     * When implementing this function, use the given [WindowGeometryProviderScope] to examine the
     * available screens and determine the appropriate one for the window.
     */
    fun WindowScreenProviderScope.getScreen(): Screen

    @ExperimentalComposeUiApi
    companion object {
        /**
         * Returns the default screen for a new window.
         */
        val Default = WindowScreenProvider { defaultScreen }
    }
}

/**
 * The various properties of a window that are useful in a [WindowGeometryProviderScope].
 *
 * Note: this class may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
class WindowMetrics internal constructor(
    private val window: java.awt.Window
) {
    /**
     * The screen on which the window is placed.
     */
    val screen: Screen by lazy { Screen(window.graphicsConfiguration.device) }

    /**
     * The bounds of the entire window (including insets) on the screen.
     */
    val bounds: DpRect
        get() = window.bounds.toDpRect()

    /**
     * The window's insets (the sizes of the areas where the content isn't placed, such as the title
     * bar).
     */
    val insets: DpInsets
        get() = window.insets.toDpInsets()
}

/**
 * The scope in which window geometry providers (e.g. [WindowBoundsProvider]) are evaluated.
 *
 * Note: this class may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
class WindowGeometryProviderScope internal constructor(
    parentWindow: java.awt.Window?,
    private val window: java.awt.Window,
    private val measurableContentProvider: () -> MeasurableRootContent,
): Density {
    init {
        require(window.isDisplayable) {
            "Window must be displayable before it can be used in WindowGeometryProviderScope"
        }
    }

    /**
     * The density of the window.
     */
    private val windowDensity: Density
        get() = window.density

    override val density: Float
        get() = windowDensity.density

    override val fontScale: Float
        get() = windowDensity.fontScale

    /**
     * The metrics of the parent window, if any.
     */
    val parentWindowMetrics: WindowMetrics? = parentWindow?.let { WindowMetrics(it) }

    /**
     * The window's metrics.
     */
    val windowMetrics: WindowMetrics = WindowMetrics(window)

    /**
     * Returns the size a window should have, given the size of its content.
     *
     * The content size is expanded by the window's insets and then constrained to
     * [Screen.availableBounds].
     */
    fun contentToWindowSize(contentSize: DpSize): DpSize =
        with(windowMetrics) {
            (contentSize + insets).coerceAtMost(screen.availableBounds.size)
        }

    /**
     * Represents the composable content of the window, which can be queried for its preferred size
     * properties.
     */
    val windowContent: MeasurableRootContent
        get() = measurableContentProvider()

    /**
     * Evaluates the given [WindowSizeProvider] in this scope.
     */
    internal fun WindowSizeProvider.getSize(): DpSize = with(this) {
        this@WindowGeometryProviderScope.getSize()
    }

    /**
     * Evaluates the given [WindowPositionProvider] in this scope.
     */
    internal fun WindowPositionProvider.getPosition(size: DpSize): DpOffset = with(this) {
        this@WindowGeometryProviderScope.getPosition(size)
    }

    /**
     * Evaluates the given [WindowBoundsProvider] in this scope.
     */
    internal fun WindowBoundsProvider.getBounds(): DpRect = with(this) {
        this@WindowGeometryProviderScope.getBounds()
    }
}

/**
 * Provides the bounds of the window.
 *
 * Note: this interface may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
interface WindowBoundsProvider {
    /**
     * Returns the bounds of the window.
     *
     * When implementing this function, use the given [WindowGeometryProviderScope] to examine the
     * geometry of the screen and determine the appropriate bounds for the window.
     *
     * All coordinates in the returned [DpRect] must be [Dp.isSpecified] and [Dp.isFinite].
     */
    fun WindowGeometryProviderScope.getBounds(): DpRect

    @ExperimentalComposeUiApi
    companion object {
        /**
         * Returns the default position and size for a new window.
         */
        val Default = WindowBoundsProvider(
            sizeProvider = WindowSizeProvider.Default,
            positionProvider = WindowPositionProvider.Default
        )

        /**
         * Positions the window at the given [bounds].
         *
         * @param bounds The bounds of the window.
         *
         */
        fun Absolute(bounds: DpRect): WindowBoundsProvider {
            bounds.requireReal()
            return WindowBoundsProvider { bounds }
        }
    }
}

/**
 * Creates a [WindowBoundsProvider] from the given [bounds] function.
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
fun WindowBoundsProvider(
    bounds: WindowGeometryProviderScope.() -> DpRect,
) = object : WindowBoundsProvider {
    override fun WindowGeometryProviderScope.getBounds() = bounds()
}

/**
 * Combines a [WindowSizeProvider] and [WindowPositionProvider] into a [WindowBoundsProvider].
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
fun WindowBoundsProvider(
    sizeProvider: WindowSizeProvider = WindowSizeProvider.Current,
    positionProvider: WindowPositionProvider = WindowPositionProvider.Current,
): WindowBoundsProvider = WindowBoundsProvider {
    val size = sizeProvider.getSize().requireReal()
    val position = positionProvider.getPosition(size)
    DpRect(position, size)
}

/**
 * Provides the position of the window.
 *
 * Use this in conjunction with a [WindowSizeProvider] to construct a [WindowBoundsProvider].
 *
 * Note: this interface may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
fun interface WindowPositionProvider {
    /**
     * Returns the position of the window.
     *
     * When implementing this function, use the given [WindowGeometryProviderScope] to examine the
     * geometry of the screen and determine the appropriate position for the window.
     *
     * All coordinates in the returned [DpOffset] must be [Dp.isSpecified] and [Dp.isFinite].
     * The [DpOffset] itself must also be [DpOffset.isSpecified].
     */
    fun WindowGeometryProviderScope.getPosition(size: DpSize): DpOffset

    @ExperimentalComposeUiApi
    companion object {
        /**
         * Returns the default position for a new window.
         */
        val Default = WindowPositionProvider { size ->
            WindowLocationTracker.getCascadeLocationFor(
                graphicsDevice = windowMetrics.screen.device,
                windowSize = size.roundToDimension()
            ).toDpOffset()
        }

        /**
         * Returns the current position of the window.
         */
        val Current = WindowPositionProvider { windowMetrics.bounds.topLeft }

        /**
         * Positions the window at the given [position].
         *
         * @param position The position of the window.
         */
        fun Absolute(position: DpOffset): WindowPositionProvider {
            position.requireReal()
            return WindowPositionProvider { position }
        }

        /**
         * Positions the window at the given coordinates.
         *
         * @param x The x position of the window.
         * @param y The y position of the window.
         */
        fun Absolute(x: Dp, y: Dp): WindowPositionProvider = Absolute(DpOffset(x, y))

        /**
         * Aligns the window within the screen according to [alignment] and [offset].
         *
         * @param alignment The alignment of the window relative to the screen.
         * @param offset An additional absolute offset added after aligning.
         */
        fun AlignedToScreen(
            alignment: Alignment,
            offset: DpOffset = DpOffset.Zero,
        ): WindowPositionProvider = WindowPositionProvider { size ->
            val availableBounds = windowMetrics.screen.availableBounds

            val position = alignment.align(
                size = size.roundToIntSize(),
                space = availableBounds.size.roundToIntSize(),
                layoutDirection = LayoutDirection.Ltr
            )
            DpOffset(
                x = availableBounds.left + position.x.dp + offset.x,
                y = availableBounds.top + position.y.dp + offset.y
            )
        }

        /**
         * Aligns the window relative to its parent window, according to [anchor], [alignment] and
         * [offset].
         *
         * [anchor] specifies the point in the parent bounds relative to which [alignment] is
         * applied. For example, [Alignment.BottomEnd] specifies the bottom-right corner.
         * [alignment] specifies the alignment inside an area centered at [anchor] and is twice the
         * width and height of the window. For example, [Alignment.TopStart] will position it such
         * that its bottom-right corner is at [anchor].
         *
         * @param anchor The anchor relative to which [alignment] is applied.
         * @param alignment The alignment of the window relative to the [anchor].
         * @param offset An additional absolute offset added after aligning.
         * @param excludeParentInsets Whether to position relative to the content of the parent
         * window, excluding the insets.
         */
        fun AlignedToParentWindow(
            anchor: Alignment,
            alignment: Alignment = Alignment.Center,
            offset: DpOffset = DpOffset.Zero,
            excludeParentInsets: Boolean = false,
        ): WindowPositionProvider = WindowPositionProvider { size ->
            val parentMetrics = parentWindowMetrics ?: error("No parent window metrics specified")
            val parentBounds = if (excludeParentInsets) {
                parentMetrics.bounds - parentMetrics.insets
            } else {
                parentMetrics.bounds
            }

            val anchorPointInParent = anchor.align(
                size = IntSize.Zero,
                space = parentBounds.size.roundToIntSize(),
                layoutDirection = LayoutDirection.Ltr
            )
            val anchorPoint = IntOffset(
                anchorPointInParent.x + parentBounds.left.value.roundToInt(),
                anchorPointInParent.y + parentBounds.top.value.roundToInt(),
            )

            val intSize = IntSize(
                width = size.width.value.roundToInt(),
                height = size.height.value.roundToInt()
            )
            val targetArea = IntRect(
                left = anchorPoint.x - intSize.width,
                top = anchorPoint.y - intSize.height,
                right = anchorPoint.x + intSize.width,
                bottom = anchorPoint.y + intSize.height
            )
            val positionInTargetArea =
                alignment.align(intSize, targetArea.size, LayoutDirection.Ltr)

            DpOffset(
                x = (targetArea.left + positionInTargetArea.x).dp,
                y = (targetArea.top + positionInTargetArea.y).dp
            ) + offset
        }
    }
}

/**
 * Provides the size of the window.
 *
 * Use this in conjunction with a [WindowPositionProvider] to construct a [WindowBoundsProvider].
 *
 * Note: this function may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
fun interface WindowSizeProvider {
    /**
     * Returns the size of the window.
     *
     * When implementing this function, use the given [WindowGeometryProviderScope] to examine the
     * geometry of the screen and the size properties of the window's content to determine the
     * appropriate size for the window.
     *
     * All coordinates in the returned [DpSize] must be [Dp.isSpecified] and [Dp.isFinite].
     * The [DpSize] itself must also be [DpSize.isSpecified].
     */
    fun WindowGeometryProviderScope.getSize(): DpSize

    @ExperimentalComposeUiApi
    companion object {
        /**
         * Sets the size of the window to the default one.
         */
        val Default = Fixed(DpSize(800.dp, 600.dp))

        /**
         * Returns the current size of the window.
         */
        val Current = WindowSizeProvider { windowMetrics.bounds.size }

        /**
         * Sets the size of the window to the given [size].
         *
         * @param size The size of the window.
         */
        fun Fixed(size: DpSize): WindowSizeProvider {
            size.requireReal()
            return WindowSizeProvider { size }
        }

        /**
         * Sets the size of the window to the given [width] and [height].
         *
         * @param width The width of the window.
         * @param height The height of the window.
         */
        fun Fixed(width: Dp, height: Dp): WindowSizeProvider = Fixed(DpSize(width, height))

        /**
         * Sets the size of the window to its preferred size, constrained only by the size of the
         * screen.
         *
         * The preferred size is computed by measuring the content with infinite
         * [Constraints], and adding the window's insets to that.
         */
        val Unconstrained = WindowSizeProvider {
            windowContent.measuringIn(Constraints()) {
                contentToWindowSize(
                    DpSize(
                        width = it.measuredWidth.toDp(),
                        height = it.measuredHeight.toDp()
                    )
                )
            }
        }

        /**
         * Sets one dimension of the window to its intrinsic size at the given [otherDimensionSize]
         * on the other dimension.
         */
        private fun IntrinsicDimension(
            isWidth: Boolean,
            intrinsicSize: WindowIntrinsicSize,
            otherDimensionSize: Dp,
            otherDimensionName: String,
        ): WindowSizeProvider {
            otherDimensionSize.requireReal(otherDimensionName)
            return WindowSizeProvider {
                val otherDimensionPx = otherDimensionSize.roundToPx()
                val width: Dp
                val height: Dp
                if (isWidth) {
                    width = intrinsicSize.widthOf(windowContent, otherDimensionPx).toDp()
                    height = otherDimensionSize
                } else {
                    width = otherDimensionSize
                    height = intrinsicSize.heightOf(windowContent, otherDimensionPx).toDp()
                }
                contentToWindowSize(
                    DpSize(
                        width = width,
                        height = height
                    )
                )
            }
        }

        /**
         * Sets the width of the window to its minimum intrinsic width at the given [height].
         *
         * The height of the window is set to [height].
         *
         * @param height The height of the window.
         *
         * @see [IntrinsicMeasurable.minIntrinsicWidth]
         */
        fun MinIntrinsicWidth(height: Dp) = IntrinsicDimension(
            isWidth = true,
            intrinsicSize = WindowIntrinsicSize.Min,
            otherDimensionSize = height,
            otherDimensionName = "height"
        )

        /**
         * Sets the width of the window to its maximum intrinsic width at the given [height].
         *
         * The height of the window is set to [height].
         *
         * @param height The height of the window.
         *
         * @see [IntrinsicMeasurable.maxIntrinsicWidth]
         */
        fun MaxIntrinsicWidth(height: Dp) = IntrinsicDimension(
            isWidth = true,
            intrinsicSize = WindowIntrinsicSize.Max,
            otherDimensionSize = height,
            otherDimensionName = "height"
        )

        /**
         * Sets the height of the window to its minimum intrinsic height at the given [width].
         *
         * The width of the window is set to [width].
         *
         * @param width The width of the window.
         *
         * @see [IntrinsicMeasurable.minIntrinsicHeight]
         */
        fun MinIntrinsicHeight(width: Dp) = IntrinsicDimension(
            isWidth = false,
            intrinsicSize = WindowIntrinsicSize.Min,
            otherDimensionSize = width,
            otherDimensionName = "width"
        )

        /**
         * Sets the height of the window to its maximum intrinsic height at the given [width].
         *
         * The width of the window is set to [width].
         *
         * @param width The width of the window.
         *
         * @see [IntrinsicMeasurable.maxIntrinsicHeight]
         */
        fun MaxIntrinsicHeight(width: Dp) = IntrinsicDimension(
            isWidth = false,
            intrinsicSize = WindowIntrinsicSize.Max,
            otherDimensionSize = width,
            otherDimensionName = "width"
        )

        /**
         * Sets the primary dimension of the window to its intrinsic size, unconstrained at the
         * secondary dimension, and the secondary dimension to its intrinsic size at the size of
         * the primary dimension.
         *
         * This is useful for cases where the window is fixed on one dimension, but the one is
         * flexible.
         *
         * @param isWidth Whether the primary dimension is width.
         * @param intrinsicPrimary The intrinsic width to measure.
         * @param intrinsicSecondary The intrinsic height to measure.
         */
        private fun IntrinsicDimensionWithMatchingOtherDimension(
            isWidth: Boolean,
            intrinsicPrimary: WindowIntrinsicSize,
            intrinsicSecondary: WindowIntrinsicSize,
        ) = WindowSizeProvider {
            val availableScreenBounds = windowMetrics.screen.availableBounds
            val width: Int
            val height: Int
            if (isWidth) {
                width = intrinsicPrimary.widthOf(windowContent, availableScreenBounds.height.roundToPx())
                height = intrinsicSecondary.heightOf(windowContent, width)
            } else {
                height = intrinsicPrimary.heightOf(windowContent, availableScreenBounds.width.roundToPx())
                width = intrinsicSecondary.widthOf(windowContent, height)
            }
            contentToWindowSize(
                DpSize(
                    width = width.toDp(),
                    height = height.toDp()
                )
            )
        }

        /**
         * Sets the width of the window to its intrinsic width at unconstrained height, and
         * the height of the window to its intrinsic height at that width.
         *
         * This is useful for cases where the window has a fixed width, but the height is flexible.
         *
         * @param intrinsicWidth The intrinsic width to measure.
         * @param intrinsicHeight The intrinsic height to measure.
         */
        fun IntrinsicWidthWithMatchingIntrinsicHeight(
            intrinsicWidth: WindowIntrinsicSize,
            intrinsicHeight: WindowIntrinsicSize,
        ): WindowSizeProvider = IntrinsicDimensionWithMatchingOtherDimension(
            isWidth = true,
            intrinsicPrimary = intrinsicWidth,
            intrinsicSecondary = intrinsicHeight,
        )

        /**
         * Sets the height of the window to its intrinsic height at unconstrained width, and
         * the width of the window to its intrinsic width at that height.
         *
         * This is useful for cases where the window has a fixed height, but the width is flexible.
         *
         * @param intrinsicWidth The intrinsic width to measure.
         * @param intrinsicHeight The intrinsic height to measure.
         */
        fun IntrinsicHeightWithMatchingIntrinsicWidth(
            intrinsicHeight: WindowIntrinsicSize,
            intrinsicWidth: WindowIntrinsicSize,
        ) = IntrinsicDimensionWithMatchingOtherDimension(
            isWidth = false,
            intrinsicPrimary = intrinsicHeight,
            intrinsicSecondary = intrinsicWidth,
        )
    }
}


/**
 * The kinds of intrinsic sizes that can be used with [WindowSizeProvider].
 *
 * Note: this class may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
abstract class WindowIntrinsicSize internal constructor() {

    /**
     * Returns the intrinsic width (min or max) of the given [measurable] at the given [height].
     */
    abstract fun widthOf(measurable: IntrinsicMeasurable, height: Int): Int

    /**
     * Returns the intrinsic height (min or max) of the given [measurable] at the given [width].
     */
    abstract fun heightOf(measurable: IntrinsicMeasurable, width: Int): Int

    /**
     * Measures minimum intrinsic size.
     */
    @ExperimentalComposeUiApi
    data object Min: WindowIntrinsicSize() {
        override fun widthOf(measurable: IntrinsicMeasurable, height: Int): Int {
            return measurable.minIntrinsicWidth(height)
        }

        override fun heightOf(measurable: IntrinsicMeasurable, width: Int): Int {
            return measurable.minIntrinsicHeight(width)
        }
    }

    /**
     * Measures maximum intrinsic size.
     */
    @ExperimentalComposeUiApi
    data object Max: WindowIntrinsicSize() {
        override fun widthOf(measurable: IntrinsicMeasurable, height: Int): Int {
            return measurable.maxIntrinsicWidth(height)
        }

        override fun heightOf(measurable: IntrinsicMeasurable, width: Int): Int {
            return measurable.maxIntrinsicHeight(width)
        }
    }
}