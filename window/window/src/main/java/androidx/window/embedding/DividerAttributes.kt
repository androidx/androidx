/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.embedding

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.window.RequiresWindowSdkExtension

/**
 * The attributes of the divider layout and behavior.
 *
 * @property widthDp the width of the divider.
 * @property color the color of the divider.
 * @see SplitAttributes.Builder.setDividerAttributes
 * @see FixedDividerAttributes
 * @see DraggableDividerAttributes
 * @see NO_DIVIDER
 */
abstract class DividerAttributes
private constructor(
    @IntRange(from = WIDTH_SYSTEM_DEFAULT.toLong()) val widthDp: Int = WIDTH_SYSTEM_DEFAULT,
    @ColorInt val color: Int = Color.BLACK,
) {
    override fun toString(): String =
        DividerAttributes::class.java.simpleName + "{" + "width=$widthDp, " + "color=$color" + "}"

    /**
     * The attributes of a fixed divider. A fixed divider is a divider type that draws a static line
     * between the primary and secondary containers.
     *
     * @property widthDp the width of the divider.
     * @property color the color of the divider.
     * @see SplitAttributes.Builder.setDividerAttributes
     */
    class FixedDividerAttributes
    @RequiresWindowSdkExtension(6)
    private constructor(
        @IntRange(from = WIDTH_SYSTEM_DEFAULT.toLong()) widthDp: Int = WIDTH_SYSTEM_DEFAULT,
        @ColorInt color: Int = Color.BLACK
    ) : DividerAttributes(widthDp, color) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FixedDividerAttributes) return false
            return widthDp == other.widthDp && color == other.color
        }

        override fun hashCode(): Int = widthDp * 31 + color

        /**
         * The [FixedDividerAttributes] builder.
         *
         * @constructor creates a new [FixedDividerAttributes.Builder]
         */
        @RequiresWindowSdkExtension(6)
        class Builder() {
            @IntRange(from = WIDTH_SYSTEM_DEFAULT.toLong())
            private var widthDp = WIDTH_SYSTEM_DEFAULT

            @ColorInt private var color = Color.BLACK

            /**
             * The [FixedDividerAttributes] builder constructor initialized by an existing
             * [FixedDividerAttributes].
             *
             * @param original the original [FixedDividerAttributes] to initialize the [Builder].
             */
            @RequiresWindowSdkExtension(6)
            constructor(original: FixedDividerAttributes) : this() {
                widthDp = original.widthDp
                color = original.color
            }

            /**
             * Sets the divider width. It defaults to [WIDTH_SYSTEM_DEFAULT], which means the system
             * will choose a default value based on the display size and form factor.
             *
             * @throws IllegalArgumentException if the provided value is invalid.
             */
            @RequiresWindowSdkExtension(6)
            fun setWidthDp(@IntRange(from = WIDTH_SYSTEM_DEFAULT.toLong()) widthDp: Int): Builder =
                apply {
                    validateWidth(widthDp)
                    this.widthDp = widthDp
                }

            /**
             * Sets the color of the divider. If not set, the default color [Color.BLACK] is used.
             *
             * @throws IllegalArgumentException if the provided value is invalid.
             */
            @RequiresWindowSdkExtension(6)
            fun setColor(@ColorInt color: Int): Builder = apply {
                validateColor(color)
                this.color = color
            }

            /** Builds a [FixedDividerAttributes] instance. */
            @RequiresWindowSdkExtension(6)
            fun build(): FixedDividerAttributes {
                return FixedDividerAttributes(widthDp = widthDp, color = color)
            }
        }
    }

    /**
     * The attributes of a draggable divider. A draggable divider draws a line between the primary
     * and secondary containers with a drag handle that the user can drag and resize the containers.
     *
     * While dragging, the content of the activity is temporarily covered by a solid color veil,
     * where the color is determined by the window background color of the activity. Apps may use
     * [android.app.Activity.getWindow] and [android.view.Window.setBackgroundDrawable] to configure
     * the veil colors.
     *
     * @property widthDp the width of the divider.
     * @property color the color of the divider.
     * @property dragRange the range that a divider is allowed to be dragged. When the user drags
     *   the divider beyond this range, the system will choose to either fully expand the container
     *   or move the divider back into the range.
     * @property isDraggingToFullscreenAllowed if {@code true}, the user is allowed to drag beyond
     *   the specified range temporarily, and when dragging is finished, the system will choose to
     *   either fully expand the larger container or move the divider back to the range limit.
     *   Default to {@code false}.
     * @see SplitAttributes.Builder.setDividerAttributes
     */
    class DraggableDividerAttributes
    @RequiresWindowSdkExtension(6)
    private constructor(
        @IntRange(from = WIDTH_SYSTEM_DEFAULT.toLong()) widthDp: Int = WIDTH_SYSTEM_DEFAULT,
        @ColorInt color: Int = Color.BLACK,
        val dragRange: DragRange = DragRange.DRAG_RANGE_SYSTEM_DEFAULT,
        val isDraggingToFullscreenAllowed: Boolean = false,
    ) : DividerAttributes(widthDp, color) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DraggableDividerAttributes) return false
            return widthDp == other.widthDp &&
                color == other.color &&
                dragRange == other.dragRange &&
                isDraggingToFullscreenAllowed == other.isDraggingToFullscreenAllowed
        }

        override fun hashCode(): Int =
            (((widthDp * 31 + color) * 31 + dragRange.hashCode()) * 31 +
                isDraggingToFullscreenAllowed.hashCode())

        override fun toString(): String =
            DraggableDividerAttributes::class.java.simpleName +
                "{" +
                "width=$widthDp, " +
                "color=$color, " +
                "primaryContainerDragRange=$dragRange, " +
                "isDraggingToFullscreenAllowed=$isDraggingToFullscreenAllowed" +
                "}"

        /**
         * The [DraggableDividerAttributes] builder.
         *
         * @constructor creates a new [DraggableDividerAttributes.Builder]
         */
        @RequiresWindowSdkExtension(6)
        class Builder() {
            @IntRange(from = WIDTH_SYSTEM_DEFAULT.toLong())
            private var widthDp = WIDTH_SYSTEM_DEFAULT

            @ColorInt private var color = Color.BLACK

            private var dragRange: DragRange = DragRange.DRAG_RANGE_SYSTEM_DEFAULT

            private var isDraggingToFullscreenAllowed: Boolean = false

            /**
             * The [DraggableDividerAttributes] builder constructor initialized by an existing
             * [DraggableDividerAttributes].
             *
             * @param original the original [DraggableDividerAttributes] to initialize the [Builder]
             */
            @RequiresWindowSdkExtension(6)
            constructor(original: DraggableDividerAttributes) : this() {
                widthDp = original.widthDp
                dragRange = original.dragRange
                color = original.color
                isDraggingToFullscreenAllowed = original.isDraggingToFullscreenAllowed
            }

            /**
             * Sets the divider width. It defaults to [WIDTH_SYSTEM_DEFAULT], which means the system
             * will choose a default value based on the display size and form factor.
             *
             * @throws IllegalArgumentException if the provided value is invalid.
             */
            @RequiresWindowSdkExtension(6)
            fun setWidthDp(@IntRange(from = WIDTH_SYSTEM_DEFAULT.toLong()) widthDp: Int): Builder =
                apply {
                    validateWidth(widthDp)
                    this.widthDp = widthDp
                }

            /**
             * Sets the color of the divider. If not set, the default color [Color.BLACK] is used.
             *
             * @throws IllegalArgumentException if the provided value is invalid.
             */
            @RequiresWindowSdkExtension(6)
            fun setColor(@ColorInt color: Int): Builder = apply {
                validateColor(color)
                this.color = color
            }

            /**
             * Sets the drag range of the divider in terms of the split ratio of the primary
             * container. It defaults to [DragRange.DRAG_RANGE_SYSTEM_DEFAULT], which means the
             * system will choose a default value based on the display size and form factor.
             *
             * When the user drags the divider beyond this range, the system will choose to either
             * fully expand the container or move the divider back into the range.
             *
             * @param dragRange the [DragRange] for the draggable divider.
             */
            @RequiresWindowSdkExtension(6)
            fun setDragRange(dragRange: DragRange): Builder = apply { this.dragRange = dragRange }

            /**
             * Sets whether dragging to full screen is allowed.
             *
             * If `true`, the user is allowed to drag beyond the specified range temporarily. When
             * dragging is finished, if the dragging position is below the
             * [DragRange.SplitRatioDragRange.minRatio] or the default min ratio in
             * [DragRange.DRAG_RANGE_SYSTEM_DEFAULT], the system will choose to either fully expand
             * the secondary container or move the divider back to the range limit; if the dragging
             * position is above the [DragRange.SplitRatioDragRange.maxRatio] or the default max
             * ratio in [DragRange.DRAG_RANGE_SYSTEM_DEFAULT], the system will choose to either
             * fully expand the primary container or move the divider back to the range limit.
             *
             * When the primary container is fully expanded, the secondary container is dismissed.
             * When the secondary container is fully expanded, the primary container is hidden
             * behind the secondary container, and the drag handle is displayed on the edge to allow
             * the user to drag and bring back the primary container.
             *
             * Default to `false`.
             *
             * This is only supported on devices with Window SDK extensions version 7 and above. For
             * devices with Window SDK extensions below version 7, dragging to fullscreen is always
             * disabled.
             */
            @RequiresWindowSdkExtension(7)
            fun setDraggingToFullscreenAllowed(allowed: Boolean): Builder = apply {
                this.isDraggingToFullscreenAllowed = allowed
            }

            /** Builds a [DividerAttributes] instance. */
            @RequiresWindowSdkExtension(6)
            fun build(): DraggableDividerAttributes =
                DraggableDividerAttributes(
                    widthDp = widthDp,
                    color = color,
                    dragRange = dragRange,
                    isDraggingToFullscreenAllowed = isDraggingToFullscreenAllowed,
                )
        }
    }

    /**
     * Describes the range that the user is allowed to drag the draggable divider.
     *
     * @see SplitRatioDragRange
     * @see DRAG_RANGE_SYSTEM_DEFAULT
     */
    abstract class DragRange private constructor() {
        /**
         * A drag range represented as an interval of the primary container's split ratios.
         *
         * @constructor constructs a new [SplitRatioDragRange]
         * @property minRatio the minimum split ratio of the primary container that the user is
         *   allowed to drag to. When the divider is dragged beyond this ratio, the system will
         *   choose to either fully expand the secondary container, or move the divider back to this
         *   ratio.
         * @property maxRatio the maximum split ratio of the primary container that the user is
         *   allowed to drag to. When the divider is dragged beyond this ratio, the system will
         *   choose to either fully expand the primary container, or move the divider back to this
         *   ratio.
         * @throws IllegalArgumentException if the provided values are invalid.
         */
        class SplitRatioDragRange(
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
            val minRatio: Float,
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
            val maxRatio: Float,
        ) : DragRange() {
            init {
                if (minRatio <= 0.0 || minRatio >= 1.0) {
                    throw IllegalArgumentException("minRatio must be in the interval (0.0, 1.0)")
                }
                if (maxRatio <= 0.0 || maxRatio >= 1.0) {
                    throw IllegalArgumentException("maxRatio must be in the interval (0.0, 1.0)")
                }
                if (minRatio > maxRatio) {
                    throw IllegalArgumentException(
                        "minRatio must be less than or equal to maxRatio"
                    )
                }
            }

            override fun toString(): String = "SplitRatioDragRange[$minRatio, $maxRatio]"

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is SplitRatioDragRange) return false
                return minRatio == other.minRatio && maxRatio == other.maxRatio
            }

            override fun hashCode(): Int = minRatio.hashCode() * 31 + maxRatio.hashCode()
        }

        companion object {
            /**
             * A special value to indicate that the system will choose default values based on the
             * display size and form factor.
             *
             * @see DraggableDividerAttributes.dragRange
             */
            @JvmField
            val DRAG_RANGE_SYSTEM_DEFAULT =
                object : DragRange() {
                    override fun toString(): String = "DRAG_RANGE_SYSTEM_DEFAULT"
                }
        }
    }

    companion object {
        /**
         * A special value to indicate that the system will choose a default value based on the
         * display size and form factor.
         *
         * @see DividerAttributes.widthDp
         */
        const val WIDTH_SYSTEM_DEFAULT: Int = -1

        /** Indicates that no divider is requested. */
        @JvmField
        val NO_DIVIDER =
            object : DividerAttributes() {
                override fun toString(): String = "NO_DIVIDER"
            }

        private fun validateWidth(widthDp: Int) = run {
            require(widthDp == WIDTH_SYSTEM_DEFAULT || widthDp >= 0) {
                "widthDp must be greater than or equal to 0 or WIDTH_SYSTEM_DEFAULT. Got: $widthDp"
            }
        }

        private fun validateColor(@ColorInt color: Int) = run {
            require(color.alpha() == 255) {
                "Divider color must be opaque. Got: ${Integer.toHexString(color)}"
            }
        }

        /**
         * Returns the alpha value of the color. This is the same as [Color.alpha] and is used to
         * avoid test-time dependency.
         */
        private fun Int.alpha() = this ushr 24
    }
}
