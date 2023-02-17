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

package androidx.window.testing.embedding

import android.app.Activity
import androidx.core.util.Consumer
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.EmbeddingBackend
import androidx.window.embedding.EmbeddingRule
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributesCalculatorParams
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitInfo
import java.util.concurrent.Executor

// TODO(b/269360912): Support SplitController.
/**
 * A stub implementation of [EmbeddingBackend] that's intended to be used by Robolectric.
 */
internal class StubEmbeddingBackend : EmbeddingBackend {

    private val embeddedActivities = HashSet<Activity>()
    private val embeddingRules = HashSet<EmbeddingRule>()

    fun reset() {
        embeddedActivities.clear()
        embeddingRules.clear()
    }

    fun overrideIsActivityEmbedded(activity: Activity, isActivityEmbedded: Boolean) {
        if (isActivityEmbedded) {
            embeddedActivities.add(activity)
        } else {
            embeddedActivities.remove(activity)
        }
    }

    override fun setRules(rules: Set<EmbeddingRule>) {
        validateRules(rules)
        embeddingRules.clear()
        embeddingRules.addAll(rules)
    }

    override fun getRules(): Set<EmbeddingRule> {
        return embeddingRules
    }

    override fun addRule(rule: EmbeddingRule) {
        if (rule in embeddingRules) {
            return
        }
        val tag = rule.tag
        if (tag != null) {
            // Check if there is duplicate tag
            for (existingRule in embeddingRules) {
                if (tag == existingRule.tag) {
                    // Remove the duplicate to update with the new rule.
                    embeddingRules.remove(existingRule)
                    break
                }
            }
        }
        embeddingRules.add(rule)
    }

    override fun removeRule(rule: EmbeddingRule) {
        embeddingRules.remove(rule)
    }

    override fun addSplitListenerForActivity(
        activity: Activity,
        executor: Executor,
        callback: Consumer<List<SplitInfo>>
    ) {
        TODO("Not yet implemented")
    }

    override fun removeSplitListenerForActivity(consumer: Consumer<List<SplitInfo>>) {
        TODO("Not yet implemented")
    }

    override val splitSupportStatus: SplitController.SplitSupportStatus
        get() = TODO("Not yet implemented")

    override fun isActivityEmbedded(activity: Activity): Boolean =
        embeddedActivities.contains(activity)

    @ExperimentalWindowApi
    override fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    ) {
        TODO("Not yet implemented")
    }

    override fun clearSplitAttributesCalculator() {
        TODO("Not yet implemented")
    }

    override fun isSplitAttributesCalculatorSupported(): Boolean {
        TODO("Not yet implemented")
    }

    private fun validateRules(rules: Set<EmbeddingRule>) {
        val tags = HashSet<String>()
        rules.forEach { rule ->
            val tag = rule.tag
            if (tag != null && !tags.add(tag)) {
                // Duplicated tag is not allowed.
                throw IllegalArgumentException(
                    "Duplicated tag: $tag. Tag must be unique among all registered rules"
                )
            }
        }
    }
}