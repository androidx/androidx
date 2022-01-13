/*
 * Copyright 2021 The Android Open Source Project
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
import android.app.Activity
import android.content.Intent
import android.util.Pair
import android.view.WindowMetrics
import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.embedding.EmbeddingRule as OEMEmbeddingRule
import androidx.window.extensions.embedding.ActivityRule as OEMActivityRule
import androidx.window.extensions.embedding.ActivityRule.Builder as ActivityRuleBuilder
import androidx.window.extensions.embedding.SplitPairRule as OEMSplitPairRule
import androidx.window.extensions.embedding.SplitPairRule.Builder as SplitPairRuleBuilder
import androidx.window.extensions.embedding.SplitPlaceholderRule as OEMSplitPlaceholderRule
import androidx.window.extensions.embedding.SplitPlaceholderRule.Builder as SplitPlaceholderRuleBuilder

/**
 * Adapter class that translates data classes between Extension and Jetpack interfaces.
 */
@ExperimentalWindowApi
internal class EmbeddingAdapter {
    fun translate(
        splitInfoList: List<androidx.window.extensions.embedding.SplitInfo>
    ): List<SplitInfo> {
        return splitInfoList.map(::translate)
    }

    private fun translate(splitInfo: androidx.window.extensions.embedding.SplitInfo): SplitInfo {
        val primaryActivityStack = splitInfo.primaryActivityStack
        val isPrimaryStackEmpty = try {
            primaryActivityStack.isEmpty
        } catch (e: NoSuchMethodError) {
            // Users may use older library which #isEmpty hasn't existed. Provide a fallback value
            // for this case to avoid crash.
            false
        }
        val primaryFragment = ActivityStack(primaryActivityStack.activities, isPrimaryStackEmpty)

        val secondaryActivityStack = splitInfo.secondaryActivityStack
        val isSecondaryStackEmpty = try {
            secondaryActivityStack.isEmpty
        } catch (e: NoSuchMethodError) {
            // Users may use older library which #isEmpty hasn't existed. Provide a fallback value
            // for this case to avoid crash.
            false
        }
        val secondaryFragment = ActivityStack(
            secondaryActivityStack.activities,
            isSecondaryStackEmpty
        )
        return SplitInfo(primaryFragment, secondaryFragment, splitInfo.splitRatio)
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateActivityPairPredicates(
        splitPairFilters: Set<SplitPairFilter>
    ): (Pair<Activity, Activity>) -> Boolean {
        return { (first, second) ->
            splitPairFilters.any { filter -> filter.matchesActivityPair(first, second) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateActivityIntentPredicates(
        splitPairFilters: Set<SplitPairFilter>
    ): (Pair<Activity, Intent>) -> Boolean {
        return { (first, second) ->
            splitPairFilters.any { filter -> filter.matchesActivityIntentPair(first, second) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateParentMetricsPredicate(
        splitRule: SplitRule
    ): (WindowMetrics) -> Boolean {
        return { windowMetrics ->
            splitRule.checkParentMetrics(windowMetrics)
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateActivityPredicates(
        activityFilters: Set<ActivityFilter>
    ): (Activity) -> Boolean {
        return { activity ->
            activityFilters.any { filter -> filter.matchesActivity(activity) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateIntentPredicates(
        activityFilters: Set<ActivityFilter>
    ): (Intent) -> Boolean {
        return { intent ->
            activityFilters.any { filter -> filter.matchesIntent(intent) }
        }
    }

    @SuppressLint("WrongConstant") // Converting from Jetpack to Extensions constants
    private fun translateSplitPairRule(
        rule: SplitPairRule
    ): OEMSplitPairRule {
        val builder = SplitPairRuleBuilder(
            translateActivityPairPredicates(rule.filters),
            translateActivityIntentPredicates(rule.filters),
            translateParentMetricsPredicate(rule)
        )
            .setSplitRatio(rule.splitRatio)
            .setLayoutDirection(rule.layoutDirection)
            .setShouldClearTop(rule.clearTop)

        try {
            builder.setFinishPrimaryWithSecondary(rule.finishPrimaryWithSecondary)
            builder.setFinishSecondaryWithPrimary(rule.finishSecondaryWithPrimary)
        } catch (error: NoSuchMethodError) {
            // TODO(b/205181250): Old extension interface, to be dropped with next developer preview
        }
        return builder.build()
    }

    @SuppressLint("WrongConstant") // Converting from Jetpack to Extensions constants
    private fun translateSplitPlaceholderRule(
        rule: SplitPlaceholderRule
    ): OEMSplitPlaceholderRule {
        val builder = SplitPlaceholderRuleBuilder(
            rule.placeholderIntent,
            translateActivityPredicates(rule.filters),
            translateIntentPredicates(rule.filters),
            translateParentMetricsPredicate(rule)
        )
            .setSplitRatio(rule.splitRatio)
            .setLayoutDirection(rule.layoutDirection)

        try {
            builder.setSticky(rule.isSticky)
            builder.setFinishPrimaryWithSecondary(rule.finishPrimaryWithSecondary)
        } catch (error: NoSuchMethodError) {
            // TODO(b/205181250): Old extension interface, to be dropped with next developer preview
        }
        return builder.build()
    }

    private fun translateActivityRule(rule: ActivityRule): OEMActivityRule {
        return ActivityRuleBuilder(
            translateActivityPredicates(rule.filters),
            translateIntentPredicates(rule.filters)
        )
            .setShouldAlwaysExpand(rule.alwaysExpand)
            .build()
    }

    fun translate(rules: Set<EmbeddingRule>): Set<OEMEmbeddingRule> {
        return rules.map { rule ->
            when (rule) {
                is SplitPairRule -> translateSplitPairRule(rule)
                is SplitPlaceholderRule -> translateSplitPlaceholderRule(rule)
                is ActivityRule -> translateActivityRule(rule)
                else -> throw IllegalArgumentException("Unsupported rule type")
            }
        }.toSet()
    }

    private operator fun <F, S> Pair<F, S>.component1(): F {
        return first
    }

    private operator fun <F, S> Pair<F, S>.component2(): S {
        return second
    }
}
