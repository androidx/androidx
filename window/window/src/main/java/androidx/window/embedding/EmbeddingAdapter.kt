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
import android.view.WindowMetrics
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.PredicateAdapter
import androidx.window.extensions.embedding.ActivityRule as OEMActivityRule
import androidx.window.extensions.embedding.ActivityRule.Builder as ActivityRuleBuilder
import androidx.window.extensions.embedding.EmbeddingRule as OEMEmbeddingRule
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import androidx.window.extensions.embedding.SplitPairRule as OEMSplitPairRule
import androidx.window.extensions.embedding.SplitPairRule.Builder as SplitPairRuleBuilder
import androidx.window.extensions.embedding.SplitPlaceholderRule as OEMSplitPlaceholderRule
import androidx.window.extensions.embedding.SplitPlaceholderRule.Builder as SplitPlaceholderRuleBuilder

/**
 * Adapter class that translates data classes between Extension and Jetpack interfaces.
 */
@ExperimentalWindowApi
internal class EmbeddingAdapter(
    private val predicateAdapter: PredicateAdapter
) {

    fun translate(splitInfoList: List<OEMSplitInfo>): List<SplitInfo> {
        return splitInfoList.map(::translate)
    }

    private fun translate(splitInfo: OEMSplitInfo): SplitInfo {
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
    private fun translateActivityPairPredicates(splitPairFilters: Set<SplitPairFilter>): Any {
        return predicateAdapter.buildPairPredicate(
            Activity::class,
            Activity::class
        ) { first: Activity, second: Activity ->
            splitPairFilters.any { filter -> filter.matchesActivityPair(first, second) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    private fun translateActivityIntentPredicates(splitPairFilters: Set<SplitPairFilter>): Any {
        return predicateAdapter.buildPairPredicate(
            Activity::class,
            Intent::class
        ) { first, second ->
            splitPairFilters.any { filter -> filter.matchesActivityIntentPair(first, second) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    private fun translateParentMetricsPredicate(splitRule: SplitRule): Any {
        return predicateAdapter.buildPredicate(WindowMetrics::class) { windowMetrics ->
            splitRule.checkParentMetrics(windowMetrics)
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    private fun translateActivityPredicates(activityFilters: Set<ActivityFilter>): Any {
        return predicateAdapter.buildPredicate(Activity::class) { activity ->
            activityFilters.any { filter -> filter.matchesActivity(activity) }
        }
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    private fun translateIntentPredicates(activityFilters: Set<ActivityFilter>): Any {
        return predicateAdapter.buildPredicate(Intent::class) { intent ->
            activityFilters.any { filter -> filter.matchesIntent(intent) }
        }
    }

    @SuppressLint("WrongConstant") // Converting from Jetpack to Extensions constants
    private fun translateSplitPairRule(
        rule: SplitPairRule,
        predicateClass: Class<*>
    ): OEMSplitPairRule {
        val builder = SplitPairRuleBuilder::class.java.getConstructor(
            predicateClass,
            predicateClass,
            predicateClass
        ).newInstance(
            translateActivityPairPredicates(rule.filters),
            translateActivityIntentPredicates(rule.filters),
            translateParentMetricsPredicate(rule)
        )
            .setSplitRatio(rule.splitRatio)
            .setLayoutDirection(rule.layoutDirection)
            .setShouldClearTop(rule.clearTop)
            .setFinishPrimaryWithSecondary(rule.finishPrimaryWithSecondary)
            .setFinishSecondaryWithPrimary(rule.finishSecondaryWithPrimary)
        return builder.build()
    }

    @SuppressLint("WrongConstant") // Converting from Jetpack to Extensions constants
    private fun translateSplitPlaceholderRule(
        rule: SplitPlaceholderRule,
        predicateClass: Class<*>
    ): OEMSplitPlaceholderRule {
        val builder = SplitPlaceholderRuleBuilder::class.java.getConstructor(
            Intent::class.java,
            predicateClass,
            predicateClass,
            predicateClass
        ).newInstance(
            rule.placeholderIntent,
            translateActivityPredicates(rule.filters),
            translateIntentPredicates(rule.filters),
            translateParentMetricsPredicate(rule)
        )
            .setSplitRatio(rule.splitRatio)
            .setLayoutDirection(rule.layoutDirection)
            .setSticky(rule.isSticky)
            .setFinishPrimaryWithSecondary(rule.finishPrimaryWithSecondary)
        return builder.build()
    }

    private fun translateActivityRule(
        rule: ActivityRule,
        predicateClass: Class<*>
    ): OEMActivityRule {
        return ActivityRuleBuilder::class.java.getConstructor(
            predicateClass,
            predicateClass
        ).newInstance(
            translateActivityPredicates(rule.filters),
            translateIntentPredicates(rule.filters)
        )
            .setShouldAlwaysExpand(rule.alwaysExpand)
            .build()
    }

    fun translate(rules: Set<EmbeddingRule>): Set<OEMEmbeddingRule> {
        val predicateClass = predicateAdapter.predicateClassOrNull() ?: return emptySet()
        return rules.map { rule ->
            when (rule) {
                is SplitPairRule -> translateSplitPairRule(rule, predicateClass)
                is SplitPlaceholderRule -> translateSplitPlaceholderRule(rule, predicateClass)
                is ActivityRule -> translateActivityRule(rule, predicateClass)
                else -> throw IllegalArgumentException("Unsupported rule type")
            }
        }.toSet()
    }
}
