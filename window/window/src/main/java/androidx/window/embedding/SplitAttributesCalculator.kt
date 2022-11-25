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

import android.content.res.Configuration
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics

/**
 * A developer-defined [SplitAttributes] calculator to compute the current [SplitAttributes] with
 * the current device and window state if it is registered via
 * [SplitController.setSplitAttributesCalculator]. Then [computeSplitAttributesForParams] will be
 * called when there's
 * - An activity is started and matches a registered [SplitRule].
 * - There's a parent configuration update and there's an existing split pair.
 *
 * By default, [SplitRule.defaultSplitAttributes] are applied if the parent container's
 * [WindowMetrics] satisfies the [SplitRule]'s minimum dimensions requirements, which are
 * [SplitRule.minWidth], [SplitRule.minHeight] and [SplitRule.minSmallestWidth].
 * The [SplitRule.defaultSplitAttributes] can be set by
 * - [SplitRule] Builder APIs, which are
 *   [SplitPairRule.Builder.setDefaultSplitAttributes] and
 *   [SplitPlaceholderRule.Builder.setDefaultSplitAttributes].
 * - Specifying with `splitRatio` and `splitLayoutDirection` attributes in `<SplitPairRule>` or
 * `<SplitPlaceHolderRule>` tags in XML files.
 *
 * However, developers may want to apply different [SplitAttributes] for different device or window
 * states. For example, on foldable devices, developers may want to split the screen vertically if
 * the device is in landscape, fill the screen if the device is in portrait and split the screen
 * horizontally if the device is in
 * [tabletop posture](https://developer.android.com/guide/topics/ui/foldables#postures).
 * In this case, the [SplitAttributes] can be customized by this callback, which takes effects after
 * calling [SplitController.setSplitAttributesCalculator]. Developers can also clear the callback
 * via [SplitController.clearSplitAttributesCalculator]. Then, developers could implement
 * [computeSplitAttributesForParams] as the sample linked below shows.
 *
 * **Note** that [SplitController.setSplitAttributesCalculator] and
 * [SplitController.clearSplitAttributesCalculator] are only supported if
 * [SplitController.isSplitAttributesCalculatorSupported] reports `true`. It's callers'
 * responsibility to check if [SplitAttributesCalculator] is supported by
 * [SplitController.isSplitAttributesCalculatorSupported] before using the
 * [SplitAttributesCalculator] feature. It is suggested to always set meaningful
 * [SplitRule.defaultSplitAttributes] in case [SplitAttributesCalculator] is not supported on
 * some devices.
 *
 * @sample androidx.window.samples.embedding.splitAttributesCalculatorSample
 *
 * @see SplitRule.defaultSplitAttributes
 * @see SplitController.setSplitAttributesCalculator
 * @see SplitController.clearSplitAttributesCalculator
 * @see SplitController.isSplitAttributesCalculatorSupported
 */
interface SplitAttributesCalculator {
    /**
     * Computes the [SplitAttributes] with the current device and window states.
     * @param params See [SplitAttributesCalculatorParams]
     */
    fun computeSplitAttributesForParams(params: SplitAttributesCalculatorParams): SplitAttributes

    /** The container of [SplitAttributesCalculator] parameters */
    class SplitAttributesCalculatorParams internal constructor(
        /** The parent container's [WindowMetrics] */
        val parentWindowMetrics: WindowMetrics,
        /** The parent container's [Configuration] */
        val parentConfiguration: Configuration,
        /**
         * The [SplitRule.defaultSplitAttributes]. It could be from [SplitRule] Builder APIs
         * ([SplitPairRule.Builder.setDefaultSplitAttributes] or
         * [SplitPlaceholderRule.Builder.setDefaultSplitAttributes]) or from the `splitRatio` and
         * `splitLayoutDirection` attributes from static rule definitions.
         */
        val defaultSplitAttributes: SplitAttributes,
        /**
         * Whether the [parentWindowMetrics] are larger than [SplitRule]'s minimum size criteria,
         * which are [SplitRule.minWidth], [SplitRule.minHeight] and [SplitRule.minSmallestWidth]
         */
        @get: JvmName("isDefaultMinSizeSatisfied")
        val isDefaultMinSizeSatisfied: Boolean,
        /** The parent container's [WindowLayoutInfo] */
        val parentWindowLayoutInfo: WindowLayoutInfo,
        /**
         * The [tag of `SplitRule`][SplitRule.tag] to apply this [SplitAttributes], which is `null`
         * if the tag is not set.
         *
         * @see SplitPairRule.Builder.setTag
         * @see SplitPlaceholderRule.Builder.setTag
         */
        val splitRuleTag: String?,
    ) {
        override fun toString(): String =
            "${SplitAttributesCalculatorParams::class.java.simpleName}:{" +
                "windowMetrics=$parentWindowMetrics" +
                ", configuration=$parentConfiguration" +
                ", windowLayoutInfo=$parentWindowLayoutInfo" +
                ", defaultSplitAttributes=$defaultSplitAttributes" +
                ", isDefaultMinSizeSatisfied=$isDefaultMinSizeSatisfied" +
                ", tag=$splitRuleTag}"
    }
}