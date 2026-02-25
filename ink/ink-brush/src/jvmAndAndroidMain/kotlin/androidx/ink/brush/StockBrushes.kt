/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import kotlin.jvm.JvmStatic

/**
 * Provides a fixed set of stock [BrushFamily] objects that any app can use.
 *
 * All stock brushes are versioned, so apps can store input points and brush specs instead of the
 * pixel result, but be able to regenerate strokes from stored input points that look generally like
 * the strokes originally drawn by the user. Stock brushes are intended to evolve over time.
 *
 * Each successive stock brush version will keep to the spirit of the brush, but the details can
 * change between versions. For example, a new version of the highlighter may introduce a variation
 * on how round the tip is, or what sort of curve maps color to pressure.
 *
 * We generally recommend that applications use the latest brush version available, which is what
 * the factory functions in this class do by default. But for some artistic use-cases, it may be
 * useful to specify a specific stock brush version to minimize visual changes when the Ink
 * dependency is upgraded. For example, the following will always return the initial version of the
 * marker stock brush.
 *
 * ```
 * val markerBrush = StockBrushes.marker(StockBrushes.MarkerVersion.V1)
 * ```
 *
 * Specific stock brushes may see minor tweaks and bug-fixes when the library is upgraded, but will
 * avoid major changes in behavior.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
public object StockBrushes {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @JvmStatic
    public val predictionFadeOutBehavior: BrushBehavior by lazy {
        BrushBehavior.wrapNative(StockBrushesNative.predictionFadeOutBehavior())
    }

    /** Version option for the [marker] stock brush factory function. */
    public class MarkerVersion private constructor(internal val value: Int) {

        override fun toString(): String = "MarkerVersion.V$value"

        public companion object {
            /** Initial version of a simple, circular fixed-width brush. */
            @JvmField public val V1: MarkerVersion = MarkerVersion(1)

            /** Whichever version of marker is currently the latest. */
            @JvmField public val LATEST: MarkerVersion = V1
        }
    }

    private val markerV1 by lazy {
        BrushFamily.wrapNative(StockBrushesNative.marker(MarkerVersion.V1.value))
    }

    /**
     * Factory function for constructing a simple marker brush.
     *
     * @param version The version of the marker brush to use. By default, uses the latest version.
     */
    @JvmStatic
    @JvmOverloads
    public fun marker(version: MarkerVersion = MarkerVersion.LATEST): BrushFamily =
        when (version) {
            MarkerVersion.V1 -> markerV1
            else -> throw IllegalArgumentException("Unsupported marker version: $version")
        }

    /** Version option for the [pressurePen] stock brush factory function. */
    public class PressurePenVersion private constructor(internal val value: Int) {

        override fun toString(): String = "PressurePenVersion.V$value"

        public companion object {
            /**
             * Initial version of a pressure- and speed-sensitive brush that is optimized for
             * handwriting with a stylus.
             */
            @JvmField public val V1: PressurePenVersion = PressurePenVersion(1)

            /**
             * The latest version of a pressure- and speed-sensitive brush that is optimized for
             * handwriting with a stylus.
             */
            @JvmField public val LATEST: PressurePenVersion = V1
        }
    }

    private val pressurePenV1 by lazy {
        BrushFamily.wrapNative(StockBrushesNative.pressurePen(PressurePenVersion.V1.value))
    }

    /**
     * Factory function for constructing a pressure- and speed-sensitive brush that is optimized for
     * handwriting with a stylus.
     *
     * @param version The version of the pressure pen brush to use. By default, uses the latest
     *   version.
     */
    @JvmStatic
    @JvmOverloads
    public fun pressurePen(version: PressurePenVersion = PressurePenVersion.LATEST): BrushFamily =
        when (version) {
            PressurePenVersion.V1 -> pressurePenV1
            else -> throw IllegalArgumentException("Unsupported pressure pen version: $version")
        }

    /** Version option for the [highlighter] stock brush factory function. */
    public class HighlighterVersion private constructor(internal val value: Int) {

        override fun toString(): String = "HighlighterVersion.V$value"

        public companion object {
            /**
             * Initial of a chisel-tip brush that is intended for highlighting text in a document
             * (when used with a translucent brush color).
             */
            @JvmField public val V1: HighlighterVersion = HighlighterVersion(1)

            /**
             * The latest version of a chisel-tip brush that is intended for highlighting text in a
             * document (when used with a translucent brush color).
             */
            @JvmField public val LATEST: HighlighterVersion = V1
        }
    }

    private val selfOverlapToHighlighterV1 =
        listOf(SelfOverlap.ANY, SelfOverlap.ACCUMULATE, SelfOverlap.DISCARD).associateWith {
            lazy {
                BrushFamily.wrapNative(
                    StockBrushesNative.highlighter(it.value, HighlighterVersion.V1.value)
                )
            }
        }

    /**
     * Factory function for constructing a chisel-tip brush that is intended for highlighting text
     * in a document (when used with a translucent brush color).
     *
     * @param selfOverlap Guidance to renderers on how to treat self-overlapping areas of strokes
     *   created with this brush. See [SelfOverlap] for more detail. Consider using
     *   [SelfOverlap.DISCARD] if the visual representation of the stroke must look exactly the same
     *   across all Android versions, or if the visual representation must match that of an exported
     *   PDF path or SVG object based on strokes authored using this brush.
     * @param version The version of the highlighter brush to use. By default, uses the latest
     *   version.
     */
    @JvmStatic
    @JvmOverloads
    public fun highlighter(
        selfOverlap: SelfOverlap = SelfOverlap.ANY,
        version: HighlighterVersion = HighlighterVersion.LATEST,
    ): BrushFamily =
        when (version) {
            HighlighterVersion.V1 -> {
                checkNotNull(selfOverlapToHighlighterV1[selfOverlap]) {
                        "Unrecognized SelfOverlap value: $selfOverlap"
                    }
                    .value
            }
            else -> throw IllegalArgumentException("Unsupported highlighter version: $version")
        }

    /** Version option for the [dashedLine] stock brush factory function. */
    public class DashedLineVersion private constructor(internal val value: Int) {

        override fun toString(): String = "DashedLineVersion.V$value"

        public companion object {
            /**
             * Initial version of a brush that appears as rounded rectangles with gaps in between
             * them. This may be decorative, or can be used to signify a user interaction like
             * free-form (lasso) selection.
             */
            @JvmField public val V1: DashedLineVersion = DashedLineVersion(1)

            /** The latest version of a dashed-line brush. */
            @JvmField public val LATEST: DashedLineVersion = V1
        }
    }

    private val dashedLineV1 by lazy {
        BrushFamily.wrapNative(StockBrushesNative.dashedLine(DashedLineVersion.V1.value))
    }

    /**
     * Factory function for constructing a brush that appears as rounded rectangles with gaps in
     * between them. This may be decorative, or can be used to signify a user interaction like
     * free-form (lasso) selection.
     *
     * @param version The version of the dashed line brush to use. By default, uses the latest
     *   version.
     */
    @JvmStatic
    @JvmOverloads
    public fun dashedLine(version: DashedLineVersion = DashedLineVersion.LATEST): BrushFamily =
        when (version) {
            DashedLineVersion.V1 -> dashedLineV1
            else -> throw IllegalArgumentException("Unsupported dashed line version: $version")
        }

    /** Version option for the [emojiHighlighter] stock brush factory function. */
    public class EmojiHighlighterVersion private constructor(internal val value: Int) {

        override fun toString(): String = "EmojiHighlighterVersion.V$value"

        public companion object {
            /**
             * Initial version of emoji highlighter, which has a colored streak drawing behind a
             * moving emoji sticker, possibly with a trail of miniature versions of the chosen emoji
             * sparkling behind.
             */
            @JvmField public val V1: EmojiHighlighterVersion = EmojiHighlighterVersion(1)

            /** Whichever version of emoji highlighter is currently the latest. */
            @JvmField public val LATEST: EmojiHighlighterVersion = V1
        }
    }

    /**
     * Factory function for constructing an emoji highlighter brush.
     *
     * In order to use this brush, the [TextureBitmapStore] provided to your renderer must map the
     * [clientTextureId] to a bitmap; otherwise, no texture will be visible. The emoji bitmap should
     * be a square, though the image can have a transparent background for emoji shapes that aren't
     * square.
     *
     * @param clientTextureId The client texture ID of the emoji to appear at the end of the stroke.
     *   This ID should map to a square bitmap with a transparent background in the implementation
     *   of [androidx.ink.brush.TextureBitmapStore] passed to
     *   [androidx.ink.rendering.android.canvas.CanvasStrokeRenderer.create].
     * @param showMiniEmojiTrail Whether to show a trail of miniature emojis disappearing from the
     *   stroke as it is drawn. Note that this will only render properly starting with Android U,
     *   and before Android U it is recommended to set this to false.
     * @param selfOverlap Guidance to renderers on how to treat self-overlapping areas of strokes
     *   created with this brush. See [SelfOverlap] for more detail. Consider using
     *   [SelfOverlap.DISCARD] if the visual representation of the stroke must look exactly the same
     *   across all Android versions, or if the visual representation must match that of an exported
     *   PDF path or SVG object based on strokes authored using this brush.
     * @param version The version of the emoji highlighter to use. By default, uses the latest
     *   version of the emoji highlighter brush tip and behavior.
     */
    @JvmStatic
    @JvmOverloads
    public fun emojiHighlighter(
        clientTextureId: String,
        showMiniEmojiTrail: Boolean = false,
        selfOverlap: SelfOverlap = SelfOverlap.ANY,
        version: EmojiHighlighterVersion = EmojiHighlighterVersion.LATEST,
    ): BrushFamily {
        if (!(version in listOf(EmojiHighlighterVersion.V1))) {
            throw IllegalArgumentException("Unsupported emoji highlighter version: ${version}")
        }
        return BrushFamily.wrapNative(
            StockBrushesNative.emojiHighlighter(
                clientTextureId,
                showMiniEmojiTrail,
                selfOverlap.value,
                version.value,
            )
        )
    }
}

/** Singleton wrapper around native JNI calls. */
@UsedByNative
private object StockBrushesNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun marker(version: Int): Long

    @UsedByNative external fun pressurePen(version: Int): Long

    @UsedByNative external fun highlighter(selfOverlap: Int, version: Int): Long

    @UsedByNative external fun dashedLine(version: Int): Long

    @UsedByNative
    external fun emojiHighlighter(
        clientTextureId: String,
        showMiniEmojiTrail: Boolean,
        selfOverlap: Int,
        version: Int,
    ): Long

    @UsedByNative external fun predictionFadeOutBehavior(): Long
}
