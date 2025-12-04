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
package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline

@InternalComposeUiApi
interface PlatformWindowInsets {
    /**
     * Returns a list of [Rect] objects representing the bounds of display cutouts (such as notches,
     * camera holes, or other areas that intrude into the screen).
     *
     * This property provides the actual geometric bounds of the cutouts themselves, while
     * [displayCutout] provides the safe insets that content should observe to avoid these cutouts.
     *
     * Different platforms may represent cutouts differently:
     * - On platforms with a notch (like iOS), this typically includes a rectangle at the top of the screen
     * - On platforms with camera holes, this includes circles or rounded rectangles where cameras or sensors are located
     * - On platforms with curved edges or "waterfall" displays, this may include areas along the edges
     *
     * The coordinates of these rectangles are relative to the containing window or scene.
     * An empty list is returned when there are no display cutouts.
     */
    val displayCutouts: List<Rect> get() = emptyList()
    /**
     * A [Path] representing the union of all display cutouts.
     * Returns null if there are no cutouts or if the platform does not provide cutout geometry.
     */
    val cutoutPath: Path? get() = null
    val captionBar: PlatformInsets get() = PlatformInsets.Zero
    /**
     * Represents the safe inset areas that content should observe to avoid all display cutouts.
     *
     * Unlike [displayCutouts] which provides the actual geometric bounds of cutouts, this property
     * provides aggregated inset values for all sides of the screen to avoid any cutouts.
     */
    val displayCutout: PlatformInsets get() = PlatformInsets.Zero
    val ime: PlatformInsets get() = PlatformInsets.Zero
    val mandatorySystemGestures: PlatformInsets get() = PlatformInsets.Zero
    val navigationBars: PlatformInsets get() = PlatformInsets.Zero
    val statusBars: PlatformInsets get() = PlatformInsets.Zero
    val systemBars: PlatformInsets get() = PlatformInsets.Zero
    val systemGestures: PlatformInsets get() = PlatformInsets.Zero
    val tappableElement: PlatformInsets get() = PlatformInsets.Zero
    val waterfall: PlatformInsets get() = PlatformInsets.Zero
    fun excluding(
        safeInsets: Boolean = true,
        ime: Boolean = true
    ): PlatformWindowInsets = this
}

// TODO: Remove as part of https://youtrack.jetbrains.com/issue/CMP-9379
@Composable
internal fun PlatformWindowInsets.exclude(
    safeInsets: Boolean,
    ime: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPlatformWindowInsets provides excluding(safeInsets, ime),
        content = content
    )
}

internal object EmptyPlatformWindowInsets : PlatformWindowInsets

@InternalComposeUiApi
val PlatformWindowInsets.safeDrawing: PlatformInsets get() = object : PlatformInsets {
    override val left: Int get() = maxOf(displayCutout.left, ime.left, systemBars.left)
    override val top: Int get() = maxOf(displayCutout.top, ime.top, systemBars.top)
    override val right: Int get() = maxOf(displayCutout.right, ime.right, systemBars.right)
    override val bottom: Int get() = maxOf(displayCutout.bottom, ime.bottom, systemBars.bottom)
}

@InternalComposeUiApi
val PlatformWindowInsets.safeGestures: PlatformInsets get() = object : PlatformInsets {
    override val left: Int get() = maxOf(mandatorySystemGestures.left, systemGestures.left, tappableElement.left, waterfall.left)
    override val top: Int get() = maxOf(mandatorySystemGestures.top, systemGestures.top, tappableElement.top, waterfall.top)
    override val right: Int get() = maxOf(mandatorySystemGestures.right, systemGestures.right, tappableElement.right, waterfall.right)
    override val bottom: Int get() = maxOf(mandatorySystemGestures.bottom, systemGestures.bottom, tappableElement.bottom, waterfall.bottom)
}

@InternalComposeUiApi
val PlatformWindowInsets.safeContent: PlatformInsets get() = object : PlatformInsets {
    override val left: Int get() = maxOf(safeDrawing.left, safeGestures.left)
    override val top: Int get() = maxOf(safeDrawing.top, safeGestures.top)
    override val right: Int get() = maxOf(safeDrawing.right, safeGestures.right)
    override val bottom: Int get() = maxOf(safeDrawing.bottom, safeGestures.bottom)
}

/**
 * This class represents platform insets.
 */
@InternalComposeUiApi
interface PlatformInsets {
    /**
     * The left inset in pixels.
     */
    val left: Int

    /**
     * The top inset in pixels.
     */
    val top: Int

    /**
     * The right inset in pixels.
     */
    val right: Int

    /**
     * The bottom inset in pixels.
     */
    val bottom: Int

    companion object {
        val Zero: PlatformInsets = ValuePlatformInsets(0L)
    }
}

@InternalComposeUiApi
fun PlatformInsets(
    getLeft: () -> Int = { 0 },
    getTop: () -> Int = { 0 },
    getRight: () -> Int = { 0 },
    getBottom: () -> Int = { 0 }
): PlatformInsets = DynamicPlatformInsets(getLeft, getTop, getRight, getBottom)

internal fun PlatformInsets.exclude(insets: PlatformInsets) = PlatformInsets(
    getLeft = { (left - insets.left).coerceAtLeast(0) },
    getTop = { (top - insets.top).coerceAtLeast(0) },
    getRight = { (right - insets.right).coerceAtLeast(0) },
    getBottom = { (bottom - insets.bottom).coerceAtLeast(0) }
)

internal fun PlatformInsets.union(insets: PlatformInsets) = PlatformInsets(
    getLeft = { maxOf(left, insets.left) },
    getTop = { maxOf(top, insets.top) },
    getRight = { maxOf(right, insets.right) },
    getBottom = { maxOf(bottom, insets.bottom) }
)

private class DynamicPlatformInsets(
    private val getLeft: () -> Int = { 0 },
    private val getTop: () -> Int = { 0 },
    private val getRight: () -> Int = { 0 },
    private val getBottom: () -> Int = { 0 }
): PlatformInsets {
    override val left: Int get() = getLeft()
    override val top: Int get() = getTop()
    override val right: Int get() = getRight()
    override val bottom: Int get() = getBottom()
}

@InternalComposeUiApi
fun Density.PlatformInsets(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
): PlatformInsets = ValuePlatformInsets(
    left.roundToPx(),
    top.roundToPx(),
    right.roundToPx(),
    bottom.roundToPx()
)

@InternalComposeUiApi
fun PlatformInsets(
    left: Int = 0,
    top: Int = 0,
    right: Int = 0,
    bottom: Int = 0
): PlatformInsets = ValuePlatformInsets(left, top, right, bottom)

@JvmInline
private value class ValuePlatformInsets(
    val packedValue: Long
): PlatformInsets {

    constructor(
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0
    ): this(checkBoundsAndPackInsets(left, top, right, bottom))

    override val left: Int
        get() = ((packedValue ushr 48) and 0xFFFF).toInt()

    override val top: Int
        get() = ((packedValue ushr 32) and 0xFFFF).toInt()

    override val right: Int
        get() = ((packedValue ushr 16) and 0xFFFF).toInt()

    override val bottom: Int
        get() = (packedValue and 0xFFFF).toInt()

    override fun toString(): String {
        return "ValueInsets($left, $top, $right, $bottom)"
    }

    companion object {
        private fun checkBoundsAndPackInsets(left: Int, top: Int, right: Int, bottom: Int): Long {
            checkBounds(left, "left")
            checkBounds(top, "top")
            checkBounds(right, "right")
            checkBounds(bottom, "bottom")
            return (left.toLong() shl 48) or
                (top.toLong() shl 32) or
                (right.toLong() shl 16) or
                bottom.toLong()
        }

        private fun checkBounds(value: Int, name: String) {
            check(value in 0..0xFFFF) {
                "$name should be in 0..0xFFFF range, but was $value"
            }
        }
    }
}
