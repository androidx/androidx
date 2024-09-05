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
    @ColorInt val color: Int = COLOR_SYSTEM_DEFAULT,
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
        @ColorInt color: Int = COLOR_SYSTEM_DEFAULT
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

            @ColorInt private var color = COLOR_SYSTEM_DEFAULT

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
     * @see SplitAttributes.Builder.setDividerAttributes
     */
    class DraggableDividerAttributes
    @RequiresWindowSdkExtension(6)
    private constructor(
        @IntRange(from = WIDTH_SYSTEM_DEFAULT.toLong()) widthDp: Int = WIDTH_SYSTEM_DEFAULT,
        @ColorInt color: Int = COLOR_SYSTEM_DEFAULT,
        val dragRange: DragRange = DragRange.DRAG_RANGE_SYSTEM_DEFAULT,
    ) : DividerAttributes(widthDp, color) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DraggableDividerAttributes) return false
            return widthDp == other.widthDp && color == other.color && dragRange == other.dragRange
        }

        override fun hashCode(): Int = (widthDp * 31 + color) * 31 + dragRange.hashCode()

        override fun toString(): String =
            DividerAttributes::class.java.simpleName +
                "{" +
                "width=$widthDp, " +
                "color=$color, " +
                "primaryContainerDragRange=$dragRange" +
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

            @ColorInt private var color = COLOR_SYSTEM_DEFAULT

            private var dragRange: DragRange = DragRange.DRAG_RANGE_SYSTEM_DEFAULT

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

            /** Builds a [DividerAttributes] instance. */
            @RequiresWindowSdkExtension(6)
            fun build(): DraggableDividerAttributes =
                DraggableDividerAttributes(
                    widthDp = widthDp,
                    color = color,
                    dragRange = dragRange,
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

        /** Specifies a fixed divider. Used by the XML rule parser and must match attrs.xml. */
        internal const val TYPE_VALUE_FIXED: Int = 0

        /** Specifies a draggable divider. Used by the XML rule parser and must match attrs.xml. */
        internal const val TYPE_VALUE_DRAGGABLE: Int = 1

        /** Indicates that the drag range value is unspecified. Used by the XML rule parser. */
        internal const val DRAG_RANGE_VALUE_UNSPECIFIED: Float = -1.0f

        /** The default color of a divider. */
        internal const val COLOR_SYSTEM_DEFAULT: Int = Color.BLACK

        /** Creates a [DividerAttributes] from values. Used by the XML rule parser. */
        internal fun createDividerAttributes(
            type: Int,
            widthDp: Int,
            color: Int,
            dragRangeMinRatio: Float,
            dragRangeMaxRatio: Float,
        ): DividerAttributes {
            return when (type) {
                TYPE_VALUE_FIXED ->
                    FixedDividerAttributes.Builder().setWidthDp(widthDp).setColor(color).build()
                TYPE_VALUE_DRAGGABLE -> {
                    val builder =
                        DraggableDividerAttributes.Builder().setWidthDp(widthDp).setColor(color)
                    if (
                        dragRangeMinRatio == DRAG_RANGE_VALUE_UNSPECIFIED ||
                            dragRangeMaxRatio == DRAG_RANGE_VALUE_UNSPECIFIED
                    ) {
                        builder.setDragRange(DragRange.DRAG_RANGE_SYSTEM_DEFAULT)
                    } else {
                        // Validation happens in SplitRatioDragRange constructor
                        builder.setDragRange(
                            DragRange.SplitRatioDragRange(dragRangeMinRatio, dragRangeMaxRatio)
                        )
                    }
                    builder.build()
                }
                else -> throw IllegalArgumentException("Got unknown divider type $type!")
            }
        }

        /** Validates divider XML attributes. */
        internal fun validateXmlDividerAttributes(
            type: Int,
            hasDragRangeMinRatio: Boolean,
            hasDragRangeMaxRatio: Boolean,
        ) {
            if (type == TYPE_VALUE_DRAGGABLE) {
                return
            }
            if (hasDragRangeMinRatio) {
                throw IllegalArgumentException(
                    "Fixed divider does not allow attribute dragRangeMinRatio!"
                )
            }
            if (hasDragRangeMaxRatio) {
                throw IllegalArgumentException(
                    "Fixed divider does not allow attribute dragRangeMaxRatio!"
                )
            }
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
