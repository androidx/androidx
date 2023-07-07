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
import androidx.annotation.RestrictTo
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics

/**
 * The parameter container used to report the current device and window state in
 * [SplitController.setSplitAttributesCalculator] and references the corresponding [SplitRule] by
 * [splitRuleTag] if [SplitPairRule.tag] is specified.
 */
class SplitAttributesCalculatorParams @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    /** The parent container's [WindowMetrics] */
    val parentWindowMetrics: WindowMetrics,
    /** The parent container's [Configuration] */
    val parentConfiguration: Configuration,
    /** The parent container's [WindowLayoutInfo] */
    val parentWindowLayoutInfo: WindowLayoutInfo,
    /**
     * The [SplitRule.defaultSplitAttributes]. It could be from [SplitRule] Builder APIs
     * ([SplitPairRule.Builder.setDefaultSplitAttributes] or
     * [SplitPlaceholderRule.Builder.setDefaultSplitAttributes]) or from the `splitRatio` and
     * `splitLayoutDirection` attributes from static rule definitions.
     */
    val defaultSplitAttributes: SplitAttributes,
    /**
     * Whether the [parentWindowMetrics] satisfies the dimensions and aspect
     * ratios requirements specified in the [SplitRule], which are:
     *  - [SplitRule.minWidthDp]
     *  - [SplitRule.minHeightDp]
     *  - [SplitRule.minSmallestWidthDp]
     *  - [SplitRule.maxAspectRatioInPortrait]
     *  - [SplitRule.maxAspectRatioInLandscape]
     */
    @get: JvmName("areDefaultConstraintsSatisfied")
    val areDefaultConstraintsSatisfied: Boolean,
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
            ", areDefaultConstraintsSatisfied=$areDefaultConstraintsSatisfied" +
            ", tag=$splitRuleTag}"
}
