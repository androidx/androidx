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

package androidx.window.samples.embedding

import android.app.Application
import android.graphics.Color
import androidx.annotation.Sampled
import androidx.window.embedding.EmbeddingAnimationBackground
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EQUAL
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EXPAND
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_HINGE
import androidx.window.embedding.SplitController
import androidx.window.layout.FoldingFeature

@Sampled
fun splitAttributesCalculatorSample() {
    SplitController.getInstance(context).setSplitAttributesCalculator { params ->
        val tag = params.splitRuleTag
        val parentWindowMetrics = params.parentWindowMetrics
        val parentConfig = params.parentConfiguration
        val foldingFeatures =
            params.parentWindowLayoutInfo.displayFeatures.filterIsInstance<FoldingFeature>()
        val foldingState = if (foldingFeatures.size == 1) foldingFeatures[0] else null
        // Tag can be used to filter the SplitRule to apply the SplitAttributes
        if (TAG_SPLIT_RULE_MAIN != tag && params.areDefaultConstraintsSatisfied) {
            return@setSplitAttributesCalculator params.defaultSplitAttributes
        }

        // This sample will make the app show a layout to
        // - split the task bounds vertically if the device is in landscape
        // - fill the task bounds if the device is in portrait and its folding state does not
        //   split the screen
        // - split the task bounds horizontally in tabletop mode
        val bounds = parentWindowMetrics.bounds
        if (foldingState?.isSeparating == true) {
            // Split the parent container that followed by the hinge if the hinge separates the
            // parent window.
            return@setSplitAttributesCalculator SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_HINGE)
                .setLayoutDirection(
                    if (foldingState.orientation == FoldingFeature.Orientation.HORIZONTAL) {
                        SplitAttributes.LayoutDirection.TOP_TO_BOTTOM
                    } else {
                        SplitAttributes.LayoutDirection.LOCALE
                    }
                )
                // Optionally set the animation background to use when switching between
                // vertical and horizontal
                .setAnimationBackground(
                    EmbeddingAnimationBackground.createColorBackground(Color.GRAY)
                )
                .build()
        }
        return@setSplitAttributesCalculator if (
            parentConfig.screenWidthDp >= 600 && bounds.width() >= bounds.height()
        ) {
            // Split the parent container equally and vertically if the device is in landscape.
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_EQUAL)
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                // Optionally set the animation background to use when switching between
                // vertical and horizontal
                .setAnimationBackground(
                    EmbeddingAnimationBackground.createColorBackground(Color.GRAY)
                )
                .build()
        } else {
            // Expand containers if the device is in portrait or the width is less than 600 dp.
            SplitAttributes.Builder().setSplitType(SPLIT_TYPE_EXPAND).build()
        }
    }
}

@Sampled
fun splitWithOrientations() {
    SplitController.getInstance(context).setSplitAttributesCalculator { params ->
        // A sample to split with the dimension that larger than 600 DP. If there's no dimension
        // larger than 600 DP, show the presentation to fill the task bounds.
        val parentConfiguration = params.parentConfiguration
        val builder = SplitAttributes.Builder()
        return@setSplitAttributesCalculator if (parentConfiguration.screenWidthDp >= 600) {
            builder
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                // Optionally set the animation background to use when switching between
                // vertical and horizontal
                .setAnimationBackground(
                    EmbeddingAnimationBackground.createColorBackground(Color.GRAY)
                )
                .build()
        } else if (parentConfiguration.screenHeightDp >= 600) {
            builder
                .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
                // Optionally set the animation background to use when switching between
                // vertical and horizontal
                .setAnimationBackground(
                    EmbeddingAnimationBackground.createColorBackground(Color.GRAY)
                )
                .build()
        } else {
            // Fallback to expand the secondary container
            builder.setSplitType(SPLIT_TYPE_EXPAND).build()
        }
    }
}

@Sampled
fun expandContainersInPortrait() {
    SplitController.getInstance(context).setSplitAttributesCalculator { params ->
        // A sample to always fill task bounds when the device is in portrait.
        val tag = params.splitRuleTag
        val bounds = params.parentWindowMetrics.bounds
        val defaultSplitAttributes = params.defaultSplitAttributes
        val areDefaultConstraintsSatisfied = params.areDefaultConstraintsSatisfied

        val expandContainersAttrs =
            SplitAttributes.Builder().setSplitType(SPLIT_TYPE_EXPAND).build()
        if (!areDefaultConstraintsSatisfied) {
            return@setSplitAttributesCalculator expandContainersAttrs
        }
        // Always expand containers for the splitRule tagged as
        // TAG_SPLIT_RULE_EXPAND_IN_PORTRAIT if the device is in portrait
        // even if [areDefaultConstraintsSatisfied] reports true.
        if (bounds.height() > bounds.width() && TAG_SPLIT_RULE_EXPAND_IN_PORTRAIT == tag) {
            return@setSplitAttributesCalculator expandContainersAttrs
        }
        // Otherwise, use the default splitAttributes.
        return@setSplitAttributesCalculator defaultSplitAttributes
    }
}

@Sampled
fun fallbackToExpandContainersForSplitTypeHinge() {
    SplitController.getInstance(context).setSplitAttributesCalculator { params ->
        SplitAttributes.Builder()
            .setSplitType(
                if (
                    params.parentWindowLayoutInfo.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .isNotEmpty()
                ) {
                    SPLIT_TYPE_HINGE
                } else {
                    SPLIT_TYPE_EXPAND
                }
            )
            .build()
    }
}

/** Assume it's a valid [Application]... */
val context = Application()
const val TAG_SPLIT_RULE_MAIN = "main"
const val TAG_SPLIT_RULE_EXPAND_IN_PORTRAIT = "expand_in_portrait"
