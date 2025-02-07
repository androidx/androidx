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

package androidx.wear.protolayout.material3

import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.percentageHeightToDp
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.percentageWidthToDp
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.DEFAULT_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MAX_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MID_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MIN_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMarginsImpl.Companion.DEFAULT
import androidx.wear.protolayout.material3.PrimaryLayoutMarginsImpl.Companion.MAX
import androidx.wear.protolayout.material3.PrimaryLayoutMarginsImpl.Companion.MID
import androidx.wear.protolayout.material3.PrimaryLayoutMarginsImpl.Companion.MIN
import kotlin.Float.Companion.NaN

/**
 * The set of margins for the [primaryLayout]'s customization, represented as fraction of the screen
 * size.
 *
 * It is highly recommended to use these predefined values that are optimized for different screen
 * sizes, content's corners and slots presences. Those are:
 * * [MIN_PRIMARY_LAYOUT_MARGIN]
 * * [MID_PRIMARY_LAYOUT_MARGIN]
 * * [DEFAULT_PRIMARY_LAYOUT_MARGIN]
 * * [MAX_PRIMARY_LAYOUT_MARGIN].
 */
public abstract class PrimaryLayoutMargins internal constructor() {
    public companion object {
        /**
         * Default side margins for the main slot of [primaryLayout] that works for the majority of
         * [Shapes] inside the main content components, usually across round toward medium round
         * corners.
         *
         * The actual returned values depend on presence of slots in [primaryLayout] which will be
         * applied automatically.
         */
        @JvmField
        public val DEFAULT_PRIMARY_LAYOUT_MARGIN: PrimaryLayoutMargins =
            PrimaryLayoutMarginsImpl(DEFAULT)

        /**
         * Min side margins for the main slot of [primaryLayout] that should be used only when the
         * main slot contains content components with fully rounded corners, such as [Shapes.full]
         * to avoid clipping.
         *
         * The actual returned values depend on presence of slots in [primaryLayout] which will be
         * applied automatically.
         */
        @JvmField
        public val MIN_PRIMARY_LAYOUT_MARGIN: PrimaryLayoutMargins = PrimaryLayoutMarginsImpl(MIN)

        /**
         * Mid side margins for the main slot of [primaryLayout] that should be used when the main
         * slot contains content components with fully rounded to medium round corners, for example,
         * larger than [Shapes.medium] to avoid clipping.
         *
         * The actual returned values depend on presence of slots in [primaryLayout] which will be
         * applied automatically.
         */
        @JvmField
        public val MID_PRIMARY_LAYOUT_MARGIN: PrimaryLayoutMargins = PrimaryLayoutMarginsImpl(MID)

        /**
         * Max side margins for the main slot of [primaryLayout] that should be used when the main
         * slot contains content components with square corners, for example, smaller than
         * [Shapes.medium] to avoid clipping.
         *
         * The actual returned values depend on presence of slots in [primaryLayout] which will be
         * applied automatically.
         */
        @JvmField
        public val MAX_PRIMARY_LAYOUT_MARGIN: PrimaryLayoutMargins = PrimaryLayoutMarginsImpl(MAX)

        /**
         * Creates new set of margins to be used for [primaryLayout] customization. The passed in
         * values represent fraction of the screen size.
         *
         * Top margin is not included as it can't be customized in [primaryLayout] due to reserved
         * space at the top for the system placed icon.
         *
         * It is highly recommended to use predefined values instead of creating this custom one,
         * because they are optimized for different screen sizes, content's corners and slots
         * presences. Those predefined ones are:
         * * [MIN_PRIMARY_LAYOUT_MARGIN]
         * * [MID_PRIMARY_LAYOUT_MARGIN]
         * * [DEFAULT_PRIMARY_LAYOUT_MARGIN]
         * * [MAX_PRIMARY_LAYOUT_MARGIN].
         *
         * @param start Fraction of the screen width that should be applied as margin on the start
         *   side
         * @param end Fraction of the screen width that should be applied as margin on the end side
         */
        public fun customizedPrimaryLayoutMargin(
            @FloatRange(from = 0.0, to = 1.0) start: Float,
            @FloatRange(from = 0.0, to = 1.0) end: Float
        ): PrimaryLayoutMargins = CustomPrimaryLayoutMargins(start = start, end = end)

        /**
         * Creates new set of margins to be used for [primaryLayout] customization. The passed in
         * values represent fraction of the screen size.
         *
         * Top margin is not included as it can't be customized in [primaryLayout] due to reserved
         * space at the top for the system placed icon.
         *
         * It is highly recommended to use predefined values instead of creating this custom one,
         * because they are optimized for different screen sizes, content's corners and slots
         * presences. Those predefined ones are:
         * * [MIN_PRIMARY_LAYOUT_MARGIN]
         * * [MID_PRIMARY_LAYOUT_MARGIN]
         * * [DEFAULT_PRIMARY_LAYOUT_MARGIN]
         * * [MAX_PRIMARY_LAYOUT_MARGIN].
         *
         * @param start Fraction of the screen width that should be applied as margin on the start
         *   side
         * @param end Fraction of the screen width that should be applied as margin on the end side
         * @param bottom Fraction of the screen height that should be applied as margin on the
         *   bottom
         */
        public fun customizedPrimaryLayoutMargin(
            @FloatRange(from = 0.0, to = 1.0) start: Float,
            @FloatRange(from = 0.0, to = 1.0) end: Float,
            @FloatRange(from = 0.0, to = 1.0) bottom: Float
        ): PrimaryLayoutMargins =
            CustomPrimaryLayoutMargins(start = start, end = end, bottom = bottom)
    }
}

/**
 * The predefined set of margin style sizes to be used by [primaryLayout] to define default values,
 * as fraction of the screen size.
 */
internal class PrimaryLayoutMarginsImpl internal constructor(internal val size: Int) :
    PrimaryLayoutMargins() {
    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(DEFAULT, MIN, MID, MAX)
        annotation class PrimaryLayoutStyleSizes

        internal const val DEFAULT = 0
        internal const val MIN = 1
        internal const val MID = 2
        internal const val MAX = 3
    }
}

/**
 * The custom set of margins represented as fraction of the screen size for the [primaryLayout]'s
 * customization.
 *
 * It is highly recommended to use predefined values instead of creating this custom once, because
 * they are optimized for different screen sizes, content's corners and slots presences. Those
 * predefined once are:
 * * [MIN_PRIMARY_LAYOUT_MARGIN]
 * * [MID_PRIMARY_LAYOUT_MARGIN]
 * * [DEFAULT_PRIMARY_LAYOUT_MARGIN]
 * * [MAX_PRIMARY_LAYOUT_MARGIN].
 */
internal class CustomPrimaryLayoutMargins
/**
 * Creates new set of margins to be used for [primaryLayout] customization. The passed in values
 * represent percentages based on the screen width.
 *
 * @param start Percentage of the screen width that should be applied as margin on the start side
 * @param end Percentage of the screen width that should be applied as margin on the end side
 */
(
    @FloatRange(from = 0.0, to = 1.0) internal val start: Float,
    @FloatRange(from = 0.0, to = 1.0) internal val end: Float
) : PrimaryLayoutMargins() {
    internal var bottom: Float = NaN

    /**
     * Creates new set of margins to be used for [primaryLayout] customization. The passed in values
     * represent percentages based on the screen width and screen height.
     *
     * @param start Percentage of the screen width that should be applied as margin on the start
     *   side
     * @param end Percentage of the screen width that should be applied as margin on the end side
     * @param bottom Percentage of the screen height that should be applied as margin on the bottom
     */
    constructor(
        @FloatRange(from = 0.0, to = 1.0) start: Float,
        @FloatRange(from = 0.0, to = 1.0) end: Float,
        @FloatRange(from = 0.0, to = 1.0) bottom: Float
    ) : this(start = start, end = end) {
        this.bottom = bottom
    }

    /** Returns the given margins as [Padding] object. */
    internal fun toPadding(scope: MaterialScope): Padding =
        if (bottom.isNaN()) {
            scope.percentagePadding(start = start, end = end)
        } else {
            scope.percentagePadding(start = start, end = end, bottom = bottom)
        }
}

/**
 * Default values for margins used in [primaryLayout] based on slots presence from [materialScope].
 *
 * These are represented as fractions of the screen size.
 */
internal object PredefinedPrimaryLayoutMargins {
    /**
     * Default side margins for the main slot of [primaryLayout] that works for the majority of
     * [Shapes] inside the main content components, usually across round toward medium round
     * corners.
     *
     * The actual returned values depend on presence of slots in [primaryLayout] which will be
     * applied automatically.
     */
    fun MaterialScope.defaultPrimaryLayoutMargins(): Padding =
        if (layoutSlotsPresence.isTitleSlotPresent) {
            if (layoutSlotsPresence.isBottomSlotPresent) {
                if (layoutSlotsPresence.isBottomSlotEdgeButton) {
                    defaultPrimaryLayoutMarginsWithTitleWithEdgeButton()
                } else {
                    defaultPrimaryLayoutMarginsWithBottomSlotAsOther()
                }
            } else {
                defaultPrimaryLayoutMarginsWithTitleWithoutBottomSlot()
            }
        } else {
            if (layoutSlotsPresence.isBottomSlotPresent) {
                if (layoutSlotsPresence.isBottomSlotEdgeButton) {
                    defaultPrimaryLayoutMarginsWithoutTitleWithEdgeButton()
                } else {
                    defaultPrimaryLayoutMarginsWithBottomSlotAsOther()
                }
            } else {
                defaultPrimaryLayoutMarginsWithoutTitleWithoutBottomSlot()
            }
        }

    /**
     * Min side margins for the main slot of [primaryLayout] that should be used only when the main
     * slot contains content components with fully rounded corners, such as [Shapes.full] to avoid
     * clipping.
     *
     * The actual returned values depend on presence of slots in [primaryLayout] which will be
     * applied automatically.
     */
    internal fun MaterialScope.minPrimaryLayoutMargins(): Padding =
        // Values are the same regardless of title slot presence.
        if (layoutSlotsPresence.isBottomSlotPresent) {
            if (layoutSlotsPresence.isBottomSlotEdgeButton) {
                minPrimaryLayoutMarginsWithEdgeButton()
            } else {
                minPrimaryLayoutMarginsWithBottomSlotAsOther()
            }
        } else {
            minPrimaryLayoutMarginsWithoutBottomSlot()
        }

    /**
     * Mid side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains content components with fully rounded to medium round corners, for example, larger
     * than [Shapes.medium] to avoid clipping.
     *
     * The actual returned values depend on presence of slots in [primaryLayout] which will be
     * applied automatically.
     */
    internal fun MaterialScope.midPrimaryLayoutMargins(): Padding =
        if (layoutSlotsPresence.isTitleSlotPresent) {
            if (layoutSlotsPresence.isBottomSlotPresent) {
                if (layoutSlotsPresence.isBottomSlotEdgeButton) {
                    midPrimaryLayoutMarginsWithTitleWithEdgeButton()
                } else {
                    midPrimaryLayoutMarginsWithTitleWithBottomSlotAsOther()
                }
            } else {
                midPrimaryLayoutMarginsWithTitleWithoutBottomSlot()
            }
        } else {
            if (layoutSlotsPresence.isBottomSlotPresent) {
                if (layoutSlotsPresence.isBottomSlotEdgeButton) {
                    midPrimaryLayoutMarginsWithoutTitleWithEdgeButton()
                } else {
                    midPrimaryLayoutMarginsWithoutTitleWithBottomSlotAsOther()
                }
            } else {
                midPrimaryLayoutMarginsWithoutTitleWithoutBottomSlot()
            }
        }

    /**
     * Max side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains content components with square corners, for example, smaller than [Shapes.medium] to
     * avoid clipping.
     *
     * The actual returned values depend on presence of slots in [primaryLayout] which will be
     * applied automatically.
     */
    internal fun MaterialScope.maxPrimaryLayoutMargins(): Padding =
        if (layoutSlotsPresence.isTitleSlotPresent) {
            if (layoutSlotsPresence.isBottomSlotPresent) {
                if (layoutSlotsPresence.isBottomSlotEdgeButton) {
                    maxPrimaryLayoutMarginsWithTitleWithEdgeButton()
                } else {
                    maxPrimaryLayoutMarginsWithTitleWithBottomSlotAsOther()
                }
            } else {
                maxPrimaryLayoutMarginsWithTitleWithoutBottomSlot()
            }
        } else {
            if (layoutSlotsPresence.isBottomSlotPresent) {
                if (layoutSlotsPresence.isBottomSlotEdgeButton) {
                    maxPrimaryLayoutMarginsWithoutTitleWithEdgeButton()
                } else {
                    maxPrimaryLayoutMarginsWithoutTitleWithBottomSlotAsOther()
                }
            } else {
                maxPrimaryLayoutMarginsWithoutTitleWithoutBottomSlot()
            }
        }

    // Separate all cases internally so it's easier to track from the spec.

    // Bottom slot as EdgeButton. Bottom margin are not allowed to be customized in these cases.

    /**
     * Default side margins for the main slot of [primaryLayout] that should be used when there is a
     * title slot in the layout and bottom slot is set to be [edgeButton].
     *
     * These values work for the majority of [Shapes], usually across round toward medium round
     * corners.
     */
    internal fun MaterialScope.defaultPrimaryLayoutMarginsWithTitleWithEdgeButton(): Padding =
        percentagePadding(start = 7.1f / 100, end = 7.1f / 100)

    /**
     * Default side margins for the main slot of [primaryLayout] that should be used when the title
     * slot is not present in the layout and bottom slot is set to be [edgeButton].
     *
     * These values work for the majority of [Shapes], usually across round toward medium round
     * corners.
     */
    internal fun MaterialScope.defaultPrimaryLayoutMarginsWithoutTitleWithEdgeButton(): Padding =
        percentagePadding(start = 12f / 100, end = 12f / 100)

    /**
     * Min side margins for the main slot of [primaryLayout] that should be used only when the main
     * slot contains fully rounded corners, such as [Shapes.full] to avoid clipping, and when the
     * bottom slot is set to be [edgeButton].
     *
     * This can be used regardless of the title slot presence.
     */
    internal fun MaterialScope.minPrimaryLayoutMarginsWithEdgeButton(): Padding =
        percentagePadding(start = 3f / 100, end = 3f / 100)

    /**
     * Mid side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains fully rounded to medium round corners, for example, larger than [Shapes.medium] to
     * avoid clipping, and when there's title slot present and bottom slot is set to be
     * [edgeButton].
     */
    internal fun MaterialScope.midPrimaryLayoutMarginsWithTitleWithEdgeButton(): Padding =
        percentagePadding(start = 5.2f / 100, end = 5.2f / 100)

    /**
     * Mid side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains fully rounded to medium round corners, for example, larger than [Shapes.medium] to
     * avoid clipping, and when title slot is not present and bottom slot is set to be [edgeButton].
     */
    internal fun MaterialScope.midPrimaryLayoutMarginsWithoutTitleWithEdgeButton(): Padding =
        percentagePadding(start = 7.1f / 100, end = 7.1f / 100)

    /**
     * Max side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains square corners, for example, smaller than [Shapes.medium] to avoid clipping, and
     * when there's title slot present and bottom slot is set to be [edgeButton].
     */
    internal fun MaterialScope.maxPrimaryLayoutMarginsWithTitleWithEdgeButton(): Padding =
        percentagePadding(start = 10f / 100, end = 10f / 100)

    /**
     * Max side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains square corners, for example, smaller than [Shapes.medium] to avoid clipping, and
     * when title slot is not present and bottom slot is set to be [edgeButton].
     */
    internal fun MaterialScope.maxPrimaryLayoutMarginsWithoutTitleWithEdgeButton(): Padding =
        percentagePadding(start = 15.5f / 100, end = 15.5f / 100)

    // Bottom slot as other content. Bottom margin are not allowed to be customized in these cases.

    /**
     * Default side margins for the main slot of [primaryLayout] that should be used when the bottom
     * slot is set to some other content besides [edgeButton].
     *
     * These values work for the majority of [Shapes], usually across round toward medium round
     * corners. This can be used regardless of title slot presence.
     */
    internal fun MaterialScope.defaultPrimaryLayoutMarginsWithBottomSlotAsOther(): Padding =
        percentagePadding(start = 12f / 100, end = 12f / 100)

    /**
     * Min side margins for the main slot of [primaryLayout] that should be used only when the main
     * slot contains fully rounded corners, such as [Shapes.full] to avoid clipping, and when the
     * bottom slot is set to some other content besides [edgeButton].
     *
     * This can be used regardless of the title slot presence.
     */
    internal fun MaterialScope.minPrimaryLayoutMarginsWithBottomSlotAsOther(): Padding =
        percentagePadding(start = 3f / 100, end = 3f / 100)

    /**
     * Mid side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains fully rounded to medium round corners, for example, larger than [Shapes.medium] to
     * avoid clipping, and when there's title slot present and bottom slot is set to some other
     * content besides [edgeButton].
     */
    internal fun MaterialScope.midPrimaryLayoutMarginsWithTitleWithBottomSlotAsOther(): Padding =
        percentagePadding(start = 7.1f / 100, end = 7.1f / 100)

    /**
     * Mid side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains fully rounded to medium round corners, for example, larger than [Shapes.medium] to
     * avoid clipping, and when title slot is not present and bottom slot is set to some other
     * content besides [edgeButton].
     */
    internal fun MaterialScope.midPrimaryLayoutMarginsWithoutTitleWithBottomSlotAsOther(): Padding =
        percentagePadding(start = 8.3f / 100, end = 8.3f / 100)

    /**
     * Max side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains square corners, for example, smaller than [Shapes.medium] to avoid clipping, and
     * when there's title slot present and bottom slot is set to some other content besides
     * [edgeButton].
     */
    internal fun MaterialScope.maxPrimaryLayoutMarginsWithTitleWithBottomSlotAsOther(): Padding =
        percentagePadding(start = 14f / 100, end = 14f / 100)

    /**
     * Max side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains square corners, for example, smaller than [Shapes.medium] to avoid clipping, and
     * when title slot is not present and bottom slot is set to some other content besides
     * [edgeButton].
     */
    internal fun MaterialScope.maxPrimaryLayoutMarginsWithoutTitleWithBottomSlotAsOther(): Padding =
        percentagePadding(start = 15.5f / 100, end = 15.5f / 100)

    // No bottom slot. Bottom margin are allowed to be customized in these cases.

    /**
     * Default side margins for the main slot of [primaryLayout] that should be used when title slot
     * is present but the bottom slot is not.
     *
     * These values work for the majority of [Shapes], usually across round toward medium round
     * corners.
     */
    internal fun MaterialScope.defaultPrimaryLayoutMarginsWithTitleWithoutBottomSlot(): Padding =
        percentagePadding(start = 10f / 100, end = 10f / 100, bottom = 16.64f / 100)

    /**
     * Default side margins for the main slot of [primaryLayout] that should be used when neither
     * title slot or bottom slot are present.
     *
     * These values work for the majority of [Shapes], usually across round toward medium round
     * corners.
     */
    internal fun MaterialScope.defaultPrimaryLayoutMarginsWithoutTitleWithoutBottomSlot(): Padding =
        percentagePadding(start = 12f / 100, end = 12f / 100, bottom = 14f / 100)

    /**
     * Min side margins for the main slot of [primaryLayout] that should be used only when the main
     * slot contains fully rounded corners, such as [Shapes.full] to avoid clipping, and when the
     * bottom slot is not present.
     *
     * This can be used regardless of the title slot presence.
     */
    internal fun MaterialScope.minPrimaryLayoutMarginsWithoutBottomSlot(): Padding =
        percentagePadding(start = 3f / 100, end = 3f / 100, bottom = 10f / 100)

    /**
     * Mid side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains fully rounded to medium round corners, for example, larger than [Shapes.medium] to
     * avoid clipping, and when there's title slot present and bottom slot is not.
     */
    internal fun MaterialScope.midPrimaryLayoutMarginsWithTitleWithoutBottomSlot(): Padding =
        percentagePadding(start = 5.2f / 100, end = 5.2f / 100, bottom = 19.6f / 100)

    /**
     * Mid side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains fully rounded to medium round corners, for example, larger than [Shapes.medium] to
     * avoid clipping, and when neither title slot or bottom slot are present.
     */
    internal fun MaterialScope.midPrimaryLayoutMarginsWithoutTitleWithoutBottomSlot(): Padding =
        percentagePadding(start = 8.3f / 100, end = 8.3f / 100, bottom = 19.6f / 100)

    /**
     * Max side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains square corners, for example, smaller than [Shapes.medium] to avoid clipping, and
     * when there's title slot present and bottom slot is not.
     */
    internal fun MaterialScope.maxPrimaryLayoutMarginsWithTitleWithoutBottomSlot(): Padding =
        percentagePadding(start = 14f / 100, end = 14f / 100, bottom = 16.64f / 100)

    /**
     * Max side margins for the main slot of [primaryLayout] that should be used when the main slot
     * contains square corners, for example, smaller than [Shapes.medium] to avoid clipping, and
     * when neither title slot or bottom slot are present.
     */
    internal fun MaterialScope.maxPrimaryLayoutMarginsWithoutTitleWithoutBottomSlot(): Padding =
        percentagePadding(start = 16.64f / 100, end = 16.64f / 100, bottom = 14f / 100)

    internal fun MaterialScope.maxSideMargin() = percentageWidthToDp(16.64f / 100)

    internal fun MaterialScope.maxBottomMargin() = percentageHeightToDp(19.6f / 100)
}
