/*
 * Copyright 2023 The Android Open Source Project
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
@file:JvmName("TestSplitAttributesCalculatorParams")

package androidx.window.testing.embedding

import android.content.res.Configuration
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributesCalculatorParams
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitPlaceholderRule
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import java.util.Collections

/**
 * Returns an instance of [SplitAttributesCalculatorParams] for testing. It is used to verify the
 * developer implemented callback set by [SplitController.setSplitAttributesCalculator] by setting
 * the relevant values in [SplitAttributesCalculatorParams] with this method.
 *
 * @param parentWindowMetrics The [WindowMetrics] of the host task. See
 *   [SplitAttributesCalculatorParams.parentWindowMetrics].
 * @param parentConfiguration The [Configuration] of the host task with empty [Configuration] as
 *   the default value. See [SplitAttributesCalculatorParams.parentConfiguration]
 * @param parentWindowLayoutInfo Used for reporting the
 *   [androidx.window.layout.FoldingFeature] with empty [WindowLayoutInfo] as the default value.
 *   See [androidx.window.testing.layout.TestWindowLayoutInfo] and
 *   [androidx.window.testing.layout.FoldingFeature] for how to create test
 *   [WindowLayoutInfo] with [androidx.window.layout.FoldingFeature].
 * @param defaultSplitAttributes The [SplitPairRule.defaultSplitAttributes] or
 *   the [SplitPlaceholderRule.defaultSplitAttributes] that the callback is applied a vertical
 *   equal [SplitAttributes] as the default value.
 *   See [SplitAttributesCalculatorParams.defaultSplitAttributes]
 * @param areDefaultConstraintsSatisfied `true` to indicate that the [parentWindowMetrics] satisfies
 *   the constraints of [SplitPairRule] or [SplitPlaceholderRule] which defaults to `true`.
 *   See [SplitAttributesCalculatorParams.areDefaultConstraintsSatisfied]
 * @param splitRuleTag The [SplitPairRule.tag] or the [SplitPlaceholderRule.tag] that the callback
 *   is applied with `null` as the default value.
 *   See [SplitAttributesCalculatorParams.splitRuleTag].
 *
 * @see SplitAttributesCalculatorParams
 */
@OptIn(ExperimentalWindowApi::class)
@Suppress("FunctionName")
@JvmName("createTestSplitAttributesCalculatorParams")
@JvmOverloads
fun TestSplitAttributesCalculatorParams(
    parentWindowMetrics: WindowMetrics,
    parentConfiguration: Configuration = Configuration(),
    parentWindowLayoutInfo: WindowLayoutInfo = WindowLayoutInfo(Collections.emptyList()),
    defaultSplitAttributes: SplitAttributes = SplitAttributes.Builder().build(),
    areDefaultConstraintsSatisfied: Boolean = true,
    splitRuleTag: String? = null,
): SplitAttributesCalculatorParams =
    SplitAttributesCalculatorParams(
        parentWindowMetrics,
        parentConfiguration,
        parentWindowLayoutInfo,
        defaultSplitAttributes,
        areDefaultConstraintsSatisfied,
        splitRuleTag
    )
