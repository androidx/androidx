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
import java.lang.IllegalArgumentException
import java.util.function.Predicate
import androidx.window.extensions.embedding.ActivityRule.Builder as ActivityRuleBuilder
import androidx.window.extensions.embedding.SplitPairRule.Builder as SplitPairRuleBuilder
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
        val primaryFragment = ActivityStack(
            splitInfo.primaryActivityStack.activities
        )
        val secondaryFragment = ActivityStack(
            splitInfo.secondaryActivityStack.activities
        )
        return SplitInfo(primaryFragment, secondaryFragment, splitInfo.splitRatio)
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateActivityPairPredicates(
        splitPairFilters: Set<SplitPairFilter>
    ): Predicate<Pair<Activity, Activity>> {
        return Predicate<Pair<Activity, Activity>> {
            (first, second) ->
            splitPairFilters.any { filter -> filter.matchesActivityPair(first, second) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateActivityIntentPredicates(
        splitPairFilters: Set<SplitPairFilter>
    ): Predicate<Pair<Activity, Intent>> {
        return Predicate<Pair<Activity, Intent>> {
            (first, second) ->
            splitPairFilters.any { filter -> filter.matchesActivityIntentPair(first, second) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateParentMetricsPredicate(
        splitRule: SplitRule
    ): Predicate<WindowMetrics> {
        return Predicate<WindowMetrics> {
            windowMetrics ->
            splitRule.checkParentMetrics(windowMetrics)
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateActivityPredicates(
        activityFilters: Set<ActivityFilter>
    ): Predicate<Activity> {
        return Predicate<Activity> {
            activity ->
            activityFilters.any { filter -> filter.matchesActivity(activity) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    fun translateIntentPredicates(
        activityFilters: Set<ActivityFilter>
    ): Predicate<Intent> {
        return Predicate<Intent> {
            intent ->
            activityFilters.any { filter -> filter.matchesIntent(intent) }
        }
    }

    fun translate(
        rules: Set<EmbeddingRule>
    ): Set<androidx.window.extensions.embedding.EmbeddingRule> {
        return rules.map {
            rule ->
            when (rule) {
                is SplitPairRule ->
                    SplitPairRuleBuilder(
                        translateActivityPairPredicates(rule.filters),
                        translateActivityIntentPredicates(rule.filters),
                        translateParentMetricsPredicate(rule)
                    )
                        .setSplitRatio(rule.splitRatio)
                        .setLayoutDirection(rule.layoutDirection)
                        .setShouldFinishPrimaryWithSecondary(rule.finishPrimaryWithSecondary)
                        .setShouldFinishSecondaryWithPrimary(rule.finishSecondaryWithPrimary)
                        .setShouldClearTop(rule.clearTop)
                        .build()
                is SplitPlaceholderRule ->
                    SplitPlaceholderRuleBuilder(
                        rule.placeholderIntent,
                        translateActivityPredicates(rule.filters),
                        translateIntentPredicates(rule.filters),
                        translateParentMetricsPredicate(rule)
                    )
                        .setSplitRatio(rule.splitRatio)
                        .setLayoutDirection(rule.layoutDirection)
                        .build()
                is ActivityRule ->
                    ActivityRuleBuilder(
                        translateActivityPredicates(rule.filters),
                        translateIntentPredicates(rule.filters)
                    )
                        .setShouldAlwaysExpand(rule.alwaysExpand)
                        .build()
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