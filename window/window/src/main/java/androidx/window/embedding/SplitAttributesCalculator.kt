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
import androidx.annotation.NonNull
import androidx.window.core.ExperimentalWindowApi
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics

// TODO(b/240912390): refer to the real API in later CLs.
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
 * [SplitRule.minWidth], `SplitRule.minHeight` and [SplitRule.minSmallestWidth].
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
 * [computeSplitAttributesForParams] as the sample[1] below shows
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
 * [1]:
 *  ```
 *  fun computeSplitAttributesForState(
 *      params: SplitAttributesCalculatorParams
 *  ): SplitAttributes {
 *      val tag = params.splitRuleTag
 *      val parentWindowMetrics = params.parentWindowMetrics
 *      val parentConfig = params.parentConfiguration
 *      val foldingState = params.parentWindowLayoutInfo.displayFeatures.find { displayFeature ->
 *          displayFeature is FoldingFeature
 *      } as FoldingFeature?
 *
 *      // Tag can be used to filter the SplitRule to apply the SplitAttributes
 *      if (!TAG_MAIN_SPLIT_RULE.equals(tag) && params.isDefaultMinSizeSatisfied) {
 *          return params.defaultSplitAttributes
 *      }
 *
 *      // This sample will make the app show a layout to
 *      // - split the task bounds vertically if the device is in landscape
 *      // - fill the task bounds if the device is in portrait and its folding state does not
 *      //   split the screen
 *      // - split the task bounds horizontally in tabletop mode
 *      val bounds = parentWindowMetrics.bounds
 *      if (foldingState?.isSeparating ?: false) {
 *          // Split the parent container that followed by the hinge if the hinge separates the
 *          // parent window.
 *          return SplitAttributes.Builder()
 *          .setSplitType(SplitAttributes.SplitType.splitByHinge())
 *          .setLayoutDirection(
 *              if (foldingState.orientation == FoldingFeature.Orientation.HORIZONTAL) {
 *                  SplitAttributes.LayoutDirection.TOP_TO_BOTTOM
 *              } else {
 *                  SplitAttributes.LayoutDirection.LOCALE
 *              }
 *          ).build()
 *      }
 *      return if (parentConfig.screenWidthDp >= 600 && bounds.width() >= bounds.height()) {
 *          // Split the parent container equally and vertically if the device is in landscape.
 *          SplitAttributes.Builder()
 *              .setSplitType(SplitAttributes.SplitType.splitEqually())
 *              .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
 *              .build()
 *      } else {
 *          // Expand containers if the device is in portrait or the width is less than 600 dp.
 *          SplitAttributes.Builder()
 *              .setSplitType(SplitAttributes.SplitType.expandContainers())
 *              .build()
 *      }
 *  }
 * ```
 *
 * @see SplitRule.defaultSplitAttributes
 * @see SplitController.setSplitAttributesCalculator
 * @see SplitController.clearSplitAttributesCalculator
 * @see SplitController.isSplitAttributesCalculatorSupported
 */
@ExperimentalWindowApi
interface SplitAttributesCalculator {
    /**
     * Computes the [SplitAttributes] with the current device and window states.
     * @param params See [SplitAttributesCalculatorParams]
     */
    fun computeSplitAttributesForParams(
        @NonNull params: SplitAttributesCalculatorParams
    ): SplitAttributes

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
        // TODO(b/240912390): refer to the real API in later CLs.
        /**
         * Whether the [parentWindowMetrics] are larger than [SplitRule]'s minimum size criteria,
         * which are [SplitRule.minWidth], `SplitRule.minHeight` and [SplitRule.minSmallestWidth]
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
    )
}