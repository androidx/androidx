/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.core.SpecificationComputer.Companion.startSpecification
import androidx.window.core.VerificationMode
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LOCALE
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EQUAL

/**
 * Attributes that describe how the parent window (typically the activity task window) is split
 * between the primary and secondary activity containers, including:
 * - Split type &mdash; Categorizes the split and specifies the sizes of the primary and secondary
 *   activity containers relative to the parent bounds
 * - Layout direction &mdash; Specifies whether the parent window is split vertically or
 *   horizontally and in which direction the primary and secondary containers are respectively
 *   positioned (left to right, right to left, top to bottom, and so forth)
 * - Animation background color &mdash; The color of the background during animation of the split
 *   involving this `SplitAttributes` object if the animation requires a background
 * - Divider attributes &mdash; Specifies whether a divider is needed between the split containers
 *   and the properties of the divider, including the color, the width, whether the divider is
 *   draggable, etc.
 *
 * Attributes can be configured by:
 * - Setting the default `SplitAttributes` using [SplitPairRule.Builder.setDefaultSplitAttributes]
 *   or [SplitPlaceholderRule.Builder.setDefaultSplitAttributes].
 * - Setting `splitRatio`, `splitLayoutDirection`, and `animationBackgroundColor` attributes in
 *   `<SplitPairRule>` or `<SplitPlaceholderRule>` tags in an XML configuration file. The attributes
 *   are parsed as [SplitType], [LayoutDirection], and [EmbeddingAnimationBackground], respectively.
 *   Note that [SplitType.HingeSplitType] is not supported XML format.
 * - Using [SplitAttributesCalculator.computeSplitAttributesForParams] to customize the
 *   `SplitAttributes` for a given device and window state.
 *
 * @property splitType The split type attribute. Defaults to an equal split of the parent window for
 *   the primary and secondary containers.
 * @property layoutDirection The layout direction of the parent window split. The default is based
 *   on locale value.
 * @property animationBackground The animation background to use during the animation of the split
 *   involving this `SplitAttributes` object if the animation requires a background. The default is
 *   to use the current theme window background color.
 * @property dividerAttributes The [DividerAttributes] for this split. Defaults to
 *   [DividerAttributes.NO_DIVIDER], which means no divider is requested.
 * @see SplitAttributes.SplitType
 * @see SplitAttributes.LayoutDirection
 * @see EmbeddingAnimationBackground
 * @see EmbeddingAnimationBackground.createColorBackground
 * @see EmbeddingAnimationBackground.DEFAULT
 */
class SplitAttributes
@JvmOverloads
constructor(
    val splitType: SplitType = SPLIT_TYPE_EQUAL,
    val layoutDirection: LayoutDirection = LOCALE,
    val animationBackground: EmbeddingAnimationBackground = EmbeddingAnimationBackground.DEFAULT,
    val dividerAttributes: DividerAttributes = DividerAttributes.NO_DIVIDER,
) {

    /**
     * The type of parent window split, which defines the proportion of the parent window occupied
     * by the primary and secondary activity containers.
     */
    class SplitType
    internal constructor(

        /** The description of this `SplitType`. */
        internal val description: String,

        /**
         * An identifier for the split type.
         *
         * Used in the evaluation in the `equals()` method.
         */
        internal val value: Float,
    ) {

        /**
         * A string representation of this split type.
         *
         * @return The string representation of the object.
         */
        override fun toString(): String = description

        /**
         * Determines whether this object is the same type of split as the compared object.
         *
         * @param other The object to compare to this object.
         * @return True if the objects are the same split type, false otherwise.
         */
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is SplitType) return false
            return value == other.value && description == other.description
        }

        /**
         * Returns a hash code for this split type.
         *
         * @return The hash code for this object.
         */
        override fun hashCode(): Int = description.hashCode() + 31 * value.hashCode()

        /** Methods that create various split types. */
        companion object {
            /**
             * Creates a split type based on the proportion of the parent window occupied by the
             * primary container of the split.
             *
             * Values in the non-inclusive range (0.0, 1.0) define the size of the primary container
             * relative to the size of the parent window:
             * - 0.5 &mdash; Primary container occupies half of the parent window; secondary
             *   container, the other half
             * - &gt; 0.5 &mdash; Primary container occupies a larger proportion of the parent
             *   window than the secondary container
             * - &lt; 0.5 &mdash; Primary container occupies a smaller proportion of the parent
             *   window than the secondary container
             *
             * @param ratio The proportion of the parent window occupied by the primary container of
             *   the split.
             * @return An instance of `SplitType` with the specified ratio.
             */
            @JvmStatic
            fun ratio(
                @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
                ratio: Float
            ): SplitType {
                val checkedRatio =
                    ratio
                        .startSpecification(TAG, VerificationMode.STRICT)
                        .require(
                            "Ratio must be in range (0.0, 1.0). " +
                                "Use SplitType.expandContainers() instead of 0 or 1."
                        ) {
                            ratio in 0.0..1.0 && ratio !in arrayOf(0.0f, 1.0f)
                        }
                        .compute()!!
                return SplitType("ratio:$checkedRatio", checkedRatio)
            }

            /**
             * A split type in which the primary and secondary activity containers each expand to
             * fill the parent window; the secondary container overlays the primary container.
             *
             * It is useful to use this `SplitType` with the function set in
             * [SplitController.setSplitAttributesCalculator] to expand the activity containers in
             * some device or window states. The following sample shows how to always fill the
             * parent bounds if the device is in portrait orientation:
             *
             * @sample androidx.window.samples.embedding.expandContainersInPortrait
             */
            @JvmField val SPLIT_TYPE_EXPAND = SplitType("expandContainers", 0.0f)

            /**
             * A split type in which the primary and secondary containers occupy equal portions of
             * the parent window.
             *
             * Serves as the default [SplitType].
             */
            @JvmField val SPLIT_TYPE_EQUAL = ratio(0.5f)

            /**
             * A split type in which the split ratio conforms to the position of a hinge or
             * separating fold in the device display.
             *
             * The split type works only if:
             * <ul>
             * <li>The host task is not in multi-window mode (e.g., split-screen mode or
             *   picture-in-picture mode)</li>
             * <li>The device has a hinge or separating fold reported by
             *   [androidx.window.layout.FoldingFeature.isSeparating]</li>
             * <li>The hinge or separating fold orientation matches how the parent bounds are split:
             *   <ul style="list-style-type: circle;">
             * <li>The hinge or fold orientation is vertical, and the parent bounds are also split
             *   vertically (containers are side by side)</li>
             * <li>The hinge or fold orientation is horizontal, and the parent bounds are also split
             *   horizontally (containers are top and bottom)</li>
             * </ul>
             *
             * </li> </ul>
             *
             * Otherwise, this `SplitType` fallback to show the split with [SPLIT_TYPE_EQUAL].
             *
             * If the app wants to have another fallback `SplitType` if [SPLIT_TYPE_HINGE] cannot be
             * applied. It is suggested to use [SplitController.setSplitAttributesCalculator] to
             * customize the fallback `SplitType`.
             *
             * The following sample shows how to fallback to [SPLIT_TYPE_EXPAND] if there's no hinge
             * area in the parent window container bounds.
             *
             * @sample androidx.window.samples.embedding.fallbackToExpandContainersForSplitTypeHinge
             */
            @JvmField val SPLIT_TYPE_HINGE = SplitType("hinge", -1.0f)

            // TODO(b/241044092): add XML support to SPLIT_TYPE_HINGE
            /** Returns a `SplitType` with the given `value`. */
            @SuppressLint("Range") // value = 0.0 is covered.
            internal fun buildSplitTypeFromValue(
                @FloatRange(from = 0.0, to = 1.0, toInclusive = false) value: Float
            ) =
                if (value == SPLIT_TYPE_EXPAND.value) {
                    SPLIT_TYPE_EXPAND
                } else {
                    ratio(value)
                }
        }
    }

    /** The layout direction of the primary and secondary activity containers. */
    class LayoutDirection
    private constructor(

        /** The description of this `LayoutDirection`. */
        private val description: String,

        /** The enum value defined in `splitLayoutDirection` attributes in `attrs.xml`. */
        internal val value: Int,
    ) {

        /**
         * A string representation of this `LayoutDirection`.
         *
         * @return The string representation of the object.
         */
        override fun toString(): String = description

        /** Non-public properties and methods. */
        companion object {
            /**
             * Specifies that the parent bounds are split vertically (side to side).
             *
             * The direction of the primary and secondary containers is deduced from the locale as
             * either `LEFT_TO_RIGHT` or `RIGHT_TO_LEFT`.
             *
             * See also [layoutDirection].
             */
            @JvmField val LOCALE = LayoutDirection("LOCALE", 0)
            /**
             * Specifies that the parent bounds are split vertically (side to side).
             *
             * Places the primary container in the left portion of the parent window, and the
             * secondary container in the right portion.
             *
             * <img width="70%" height="70%"
             * src="/images/guide/topics/large-screens/activity-embedding/reference-docs/a_to_a_b_ltr.png"
             * alt="Activity A starts activity B to the right."/>
             *
             * See also [layoutDirection].
             */
            @JvmField val LEFT_TO_RIGHT = LayoutDirection("LEFT_TO_RIGHT", 1)
            /**
             * Specifies that the parent bounds are split vertically (side to side).
             *
             * Places the primary container in the right portion of the parent window, and the
             * secondary container in the left portion.
             *
             * <img width="70%" height="70%"
             * src="/images/guide/topics/large-screens/activity-embedding/reference-docs/a_to_a_b_rtl.png"
             * alt="Activity A starts activity B to the left."/>
             *
             * See also [layoutDirection].
             */
            @JvmField val RIGHT_TO_LEFT = LayoutDirection("RIGHT_TO_LEFT", 2)
            /**
             * Specifies that the parent bounds are split horizontally (top and bottom).
             *
             * Places the primary container in the top portion of the parent window, and the
             * secondary container in the bottom portion.
             *
             * <img width="70%" height="70%"
             * src="/images/guide/topics/large-screens/activity-embedding/reference-docs/a_to_a_b_ttb.png"
             * alt="Activity A starts activity B to the bottom."/>
             *
             * If the horizontal layout direction is not supported on the device that
             * [WindowSdkExtensions.extensionVersion] is less than 2, layout direction falls back to
             * `LOCALE`.
             *
             * See also [layoutDirection].
             */
            @JvmField val TOP_TO_BOTTOM = LayoutDirection("TOP_TO_BOTTOM", 3)
            /**
             * Specifies that the parent bounds are split horizontally (top and bottom).
             *
             * Places the primary container in the bottom portion of the parent window, and the
             * secondary container in the top portion.
             *
             * <img width="70%" height="70%"
             * src="/images/guide/topics/large-screens/activity-embedding/reference-docs/a_to_a_b_btt.png"
             * alt="Activity A starts activity B to the top."/>
             *
             * If the horizontal layout direction is not supported on the device that
             * [WindowSdkExtensions.extensionVersion] is less than 2, layout direction falls back to
             * `LOCALE`.
             *
             * See also [layoutDirection].
             */
            @JvmField val BOTTOM_TO_TOP = LayoutDirection("BOTTOM_TO_TOP", 4)

            /** Returns `LayoutDirection` with the given `value`. */
            @JvmStatic
            internal fun getLayoutDirectionFromValue(@IntRange(from = 0, to = 4) value: Int) =
                when (value) {
                    LEFT_TO_RIGHT.value -> LEFT_TO_RIGHT
                    RIGHT_TO_LEFT.value -> RIGHT_TO_LEFT
                    LOCALE.value -> LOCALE
                    TOP_TO_BOTTOM.value -> TOP_TO_BOTTOM
                    BOTTOM_TO_TOP.value -> BOTTOM_TO_TOP
                    else -> throw IllegalArgumentException("Undefined value:$value")
                }
        }
    }

    /** Non-public properties and methods. */
    companion object {
        private val TAG = SplitAttributes::class.java.simpleName
    }

    /**
     * Returns a hash code for this `SplitAttributes` object.
     *
     * @return The hash code for this object.
     */
    override fun hashCode(): Int {
        var result = splitType.hashCode()
        result = result * 31 + layoutDirection.hashCode()
        result = result * 31 + animationBackground.hashCode()
        result = result * 31 + dividerAttributes.hashCode()
        return result
    }

    /**
     * Determines whether this object has the same split attributes as the compared object.
     *
     * @param other The object to compare to this object.
     * @return True if the objects have the same split attributes, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitAttributes) return false
        return splitType == other.splitType &&
            layoutDirection == other.layoutDirection &&
            animationBackground == other.animationBackground &&
            dividerAttributes == other.dividerAttributes
    }

    /**
     * A string representation of this `SplitAttributes` object.
     *
     * @return The string representation of the object.
     */
    override fun toString(): String =
        "${SplitAttributes::class.java.simpleName}:" +
            "{splitType=$splitType, layoutDir=$layoutDirection, " +
            "animationBackground=$animationBackground, " +
            "dividerAttributes=$dividerAttributes }"

    /**
     * Builder for creating an instance of [SplitAttributes].
     * - The default split type is an equal split between primary and secondary containers.
     * - The default layout direction is based on locale.
     * - The default animation background color is to use the current theme window background color.
     * - The default divider attributes is not to use divider.
     */
    class Builder() {
        private var splitType = SPLIT_TYPE_EQUAL
        private var layoutDirection = LOCALE
        private var animationBackground = EmbeddingAnimationBackground.DEFAULT
        private var dividerAttributes: DividerAttributes = DividerAttributes.NO_DIVIDER

        /** Creates a Builder with values initialized from the original [SplitAttributes] */
        internal constructor(original: SplitAttributes) : this() {
            this.setSplitType(original.splitType)
                .setLayoutDirection(original.layoutDirection)
                .setAnimationBackground(animationBackground)
                .setDividerAttributes(original.dividerAttributes)
        }

        /**
         * Sets the split type attribute.
         *
         * The default is an equal split between primary and secondary containers.
         *
         * @param type The split type attribute.
         * @return This `Builder`.
         * @see SplitAttributes.SplitType
         */
        fun setSplitType(type: SplitType): Builder = apply { splitType = type }

        /**
         * Sets the split layout direction attribute.
         *
         * The default is based on locale.
         *
         * @param layoutDirection The layout direction attribute.
         * @return This `Builder`.
         * @see SplitAttributes.LayoutDirection
         */
        fun setLayoutDirection(layoutDirection: LayoutDirection): Builder = apply {
            this.layoutDirection = layoutDirection
        }

        /**
         * Sets the animation background to use during animation of the split involving this
         * `SplitAttributes` object if the animation requires a background.
         *
         * The default is [EmbeddingAnimationBackground.DEFAULT], which means to use the current
         * theme window background color.
         *
         * The [EmbeddingAnimationBackground] can be supported only if the vendor API level of the
         * target device is equals or higher than required API level. Otherwise, it would be no-op
         * when setting the [EmbeddingAnimationBackground] on a target device that has lower API
         * level.
         *
         * @param background The animation background.
         * @return This `Builder`.
         * @see EmbeddingAnimationBackground.createColorBackground
         * @see EmbeddingAnimationBackground.DEFAULT
         */
        @RequiresWindowSdkExtension(5)
        fun setAnimationBackground(background: EmbeddingAnimationBackground): Builder = apply {
            animationBackground = background
        }

        /** Sets the [DividerAttributes]. */
        @RequiresWindowSdkExtension(6)
        fun setDividerAttributes(dividerAttributes: DividerAttributes): Builder = apply {
            this.dividerAttributes = dividerAttributes
        }

        /**
         * Builds a `SplitAttributes` instance with the attributes specified by [setSplitType],
         * [setLayoutDirection], and [setAnimationBackground].
         *
         * @return The new `SplitAttributes` instance.
         */
        fun build(): SplitAttributes =
            SplitAttributes(splitType, layoutDirection, animationBackground, dividerAttributes)
    }
}
