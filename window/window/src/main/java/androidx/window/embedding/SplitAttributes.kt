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

import androidx.annotation.FloatRange
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.SpecificationComputer
import androidx.window.core.SpecificationComputer.Companion.startSpecification
import androidx.window.embedding.SplitAttributes.LayoutDirection
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LEFT_TO_RIGHT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LOCALE
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.RIGHT_TO_LEFT
import androidx.window.embedding.SplitAttributes.SplitType
import androidx.window.embedding.SplitAttributes.SplitType.Companion.splitEqually

// TODO(b/240912390): refer to the real API in later CLs.
/**
 * Attributes to describe how the task bounds are split, which includes information on how the task
 * bounds are split between the activity containers:
 * - Layout directions
 * - Whether the task bounds are split vertically horizontally
 * - The position of the primary and the secondary activity containers
 * Attributes can be configured in the following ways:
 * - Set the default `SplitAttributes` via `SplitPairRule.Builder.setDefaultSplitAttributes` and
 * `SplitPlaceholderRule.Builder.setDefaultSplitAttributes`
 * - Set `splitRatio` and `splitLayoutDirection` attributes in `<SplitPairRule>` or
 * `<SplitPlaceholderRule>` tag in XML file. They will be parsed as [SplitType] and
 * [layoutDirection]. Note that [SplitType.HingeSplitType] is not supported to be defined in XML
 * format.
 * - Used in `SplitAttributesCalculator.computeSplitAttributesForState` to customize the
 * `SplitAttributes` for a given device and window state.
 *
 * @see SplitAttributes.SplitType
 * @see SplitAttributes.LayoutDirection
 */
@ExperimentalWindowApi
class SplitAttributes internal constructor(
    /**
     * Returns [SplitType] for the [SplitAttributes] with a default value of
     * [SplitType.splitEqually].
     *
     * @see SplitType.ratio
     * @see SplitType.splitByHinge
     * @see SplitType.splitEqually
     * @see SplitType.expandContainers
     */
    val splitType: SplitType = splitEqually(),

    /**
     * Returns [LayoutDirection] for the [SplitAttributes] with a default value
     * [LayoutDirection.LOCALE].
     */
    val layoutDirection: LayoutDirection = LOCALE,
) {
    /**
     * Defines how the Task should be split between the primary and the secondary containers.
     *
     * @see SplitType.ratio
     * @see SplitType.splitByHinge
     */
    open class SplitType internal constructor(
        internal val description: String,
        internal val value: Float,
    ) {
        override fun toString(): String = description

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is SplitType) return false
            return value == other.value &&
                description == other.description
        }

        override fun hashCode(): Int = description.hashCode() + 31 * value.hashCode()

        /** @see SplitAttributes.SplitType.ratio */
        class RatioSplitType internal constructor(
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
            val ratio: Float
        ) : SplitType("ratio:$ratio", ratio)

        /** @see SplitAttributes.SplitType.ExpandContainersSplitType */
        class ExpandContainersSplitType internal constructor() : SplitType("expandContainer", 0.0f)

        /** @see SplitAttributes.SplitType.splitByHinge */
        class HingeSplitType internal constructor(
            val fallbackSplitType: SplitType
        ) : SplitType("hinge, fallback=$fallbackSplitType", -1.0f)

        companion object {
            /**
             * Defines what activity container should be given to the primary part of the task
             * bounds. Values in range (0.0, 1.0) define the size of the primary container of the
             * split relative to the corresponding task dimension size.
             * - `0.5` means the primary and secondary container shares an equal split.
             * - ratio larger than `0.5` means the primary container takes more space.
             * - Otherwise, the secondary container takes more space.
             *
             * @see SplitType.expandContainers
             * @see SplitType.splitEqually
             */
            @JvmStatic
            fun ratio(
                @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
                ratio: Float
            ): RatioSplitType {
                val checkedRatio = ratio.startSpecification(
                    TAG,
                    SpecificationComputer.VerificationMode.STRICT
                ).require("Ratio must be in range (0.0, 1.0). " +
                    "Use SplitType.expandContainers() instead of 0 or 1.") {
                    ratio in 0.0..1.0 && ratio !in arrayOf(0.0f, 1.0f)
                }.compute()!!
                return RatioSplitType(checkedRatio)
            }

            /**
             * Indicate that both primary and secondary activity containers are expanded to fill the
             * task parent container, and the secondary container occludes the primary one. It is
             * useful to make the apps occupy the full task bounds in some device states.
             */
            @JvmStatic
            fun expandContainers(): ExpandContainersSplitType = ExpandContainersSplitType()

            /**
             * Indicate that the primary and secondary container share an equal split. It is also
             * the default [SplitType].
             */
            @JvmStatic
            fun splitEqually(): RatioSplitType = ratio(0.5f)

            /**
             * Indicates that the split ratio follows hinge area position. This value will only be
             * applied if:
             *   1. The host Task is not in multi-window mode (ex: split-screen,
             *      picture-in-picture).
             *   2. The device has hinge reported by [androidx.window.layout.FoldingFeature.bounds].
             *   3. The hinge area orientation matches how the host task bounds are split:
             *     - The hinge area orientation is vertical, and the task bounds are also split
             *       vertically.
             *     - The hinge area orientation is horizontal, and the task bounds are also split
             *       horizontally.
             *
             * Otherwise, it fallbacks to use [fallbackSplitType], which defaults to
             * [SplitType.splitEqually]. [fallbackSplitType] can only be either a [RatioSplitType]
             * or a [ExpandContainersSplitType].
             */
            @JvmStatic
            fun splitByHinge(
                fallbackSplitType: SplitType = splitEqually()
            ): HingeSplitType {
                val checkedType = fallbackSplitType.startSpecification(
                    TAG,
                    SpecificationComputer.VerificationMode.STRICT
                ).require(
                    "FallbackSplitType must be a RatioSplitType or ExpandContainerSplitType"
                ) {
                    fallbackSplitType is RatioSplitType ||
                        fallbackSplitType is ExpandContainersSplitType
                }.compute()!!
                return HingeSplitType(checkedType)
            }
        }
    }

    /** Defines split layout directions. */
    class LayoutDirection private constructor(private val description: String) {
        override fun toString(): String = description

        companion object {
            /**
             * A value of [layoutDirection]:
             * It splits the task bounds vertically and the direction is deduced from the
             * language script of locale. The direction can be either [LEFT_TO_RIGHT]
             * or [RIGHT_TO_LEFT].
             */
            @JvmField
            val LOCALE = LayoutDirection("LOCALE")
            // TODO(b/241043844): Add the illustration below in DAC.
            // -------------------------
            // |           |           |
            // |  Primary  | Secondary |
            // |           |           |
            // -------------------------
            /**
             * A value of [layoutDirection]:
             * It splits the task bounds vertically, puts the primary container on the left portion,
             * and the secondary container on the right portion.
             */
            @JvmField
            val LEFT_TO_RIGHT = LayoutDirection("LEFT_TO_RIGHT")
            // TODO(b/241043844): Add the illustration below in DAC.
            //            -------------------------
            //            |           |           |
            //            | Secondary |  Primary  |
            //            |           |           |
            //            -------------------------
            /**
             * A value of [layoutDirection]:
             * It splits the task bounds vertically, puts the primary container on the right
             * portion, and the secondary container on the left portion.
             */
            @JvmField
            val RIGHT_TO_LEFT = LayoutDirection("RIGHT_TO_LEFT")
            // TODO(b/241043844): Add the illustration below in DAC.
            //            -------------
            //            |           |
            //            |  Primary  |
            //            |           |
            //            -------------
            //            |           |
            //            | Secondary |
            //            |           |
            //            -------------
            /**
             * A value of [layoutDirection]:
             * It splits the task bounds horizontally, puts the primary container on the top
             * portion, and the secondary container on the bottom portion.
             *
             * Note that if the horizontal layout direction is not supported on the device, it will
             * fallback to [LOCALE].
             */
            @JvmField
            val TOP_TO_BOTTOM = LayoutDirection("TOP_TO_BOTTOM")
            // TODO(b/241043844): Add the illustration below in DAC.
            //            -------------
            //            |           |
            //            | Secondary |
            //            |           |
            //            -------------
            //            |           |
            //            |  Primary  |
            //            |           |
            //            -------------
            /**
             * A value of [layoutDirection]:
             * It splits the task bounds horizontally, puts the primary container on the bottom
             * portion, and the secondary container on the top portion.
             *
             * Note that if the horizontal layout direction is not supported on the device, it will
             * fallback to [LOCALE].
             */
            @JvmField
            val BOTTOM_TO_TOP = LayoutDirection("BOTTOM_TO_TOP")
        }
    }

    companion object {
        private val TAG = SplitAttributes::class.java.simpleName
    }

    override fun hashCode(): Int = splitType.hashCode() * 31 + layoutDirection.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitAttributes) return false
        return splitType == other.splitType &&
            layoutDirection == other.layoutDirection
    }

    override fun toString(): String =
        "${SplitAttributes::class.java.simpleName}:" +
            "{splitType=$splitType, layoutDir=$layoutDirection}"

    /** Builders for [SplitAttributes] */
    class Builder {
        private var splitType: SplitType = splitEqually()
        private var layoutDirection = LOCALE

        /** @see SplitAttributes.splitType */
        fun setSplitType(type: SplitType): Builder = apply { splitType = type }

        /** @see SplitAttributes.layoutDirection */
        fun setLayoutDirection(layoutDirection: LayoutDirection): Builder =
            apply { this.layoutDirection = layoutDirection }

        fun build(): SplitAttributes = SplitAttributes(splitType, layoutDirection)
    }
}