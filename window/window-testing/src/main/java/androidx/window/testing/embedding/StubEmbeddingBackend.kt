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
import android.app.ActivityOptions
import android.os.IBinder
import androidx.core.util.Consumer
import androidx.window.embedding.ActivityStack
import androidx.window.embedding.EmbeddingBackend
import androidx.window.embedding.EmbeddingRule
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributesCalculatorParams
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitController.SplitSupportStatus.Companion.SPLIT_UNAVAILABLE
import androidx.window.embedding.SplitInfo
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * A stub implementation of [EmbeddingBackend] that's intended to be used by Robolectric.
 */
internal class StubEmbeddingBackend : EmbeddingBackend {

    private val embeddedActivities = HashSet<Activity>()
    private val embeddingRules = HashSet<EmbeddingRule>()
    private val splitInfoFlow = mutableMapOf<Activity, MutableSharedFlow<List<SplitInfo>>>()
    private val splitInfoJobs = mutableMapOf<Consumer<*>, Job>()

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

    /**
     * Adds a callback to the list of listeners associated to the [Activity]. If the listener
     * has been added before it is ignored. If a value has been set for the [Activity] then it is
     * emitted immediately to the listener
     *
     * @param activity the associated [Activity] for the listener
     * @param executor the executor that will deliver values to the callback
     * @param callback a listener that wants to receive updates about the current [SplitInfo]
     */
    override fun addSplitListenerForActivity(
        activity: Activity,
        executor: Executor,
        callback: Consumer<List<SplitInfo>>
    ) {
        if (splitInfoJobs.containsKey(callback)) {
            return
        }
        val job = CoroutineScope(executor.asCoroutineDispatcher()).launch {
            splitInfoFlow.getOrPut(activity) { MutableStateFlow(emptyList()) }
                .collect { value -> callback.accept(value) }
        }
        splitInfoJobs[callback] = job
    }

    /**
     * Removes the [Consumer] if it is present for any [Activity]. If the [Consumer] was not
     * registered then no effect happens. If a [Consumer] is registered for multiple [Activity]s
     * then it will be removed for each one.
     *
     * @param consumer a consumer that no longer wishes to receive updates.
     */
    override fun removeSplitListenerForActivity(consumer: Consumer<List<SplitInfo>>) {
        splitInfoJobs[consumer]?.cancel()
        splitInfoJobs.remove(consumer)
    }

    /**
     * Overrides the list of [SplitInfo]s for a specific [Activity]. If there was an existing
     * listener for the [SplitInfo]s then it will be updated with the new value.
     *
     * @param activity an [Activity] that is associated with the [List<SplitInfo>]
     * @param value the new value to emit.
     */
    fun overrideSplitInfo(activity: Activity, value: List<SplitInfo>) {
        splitInfoFlow.getOrPut(activity) { MutableStateFlow(emptyList()) }.tryEmit(value)
    }

    fun hasSplitInfoListeners(activity: Activity): Boolean {
        return splitInfoFlow[activity]?.let { splitInfoFlow ->
            splitInfoFlow.subscriptionCount.value != 0
        } ?: false
    }

    override var splitSupportStatus: SplitController.SplitSupportStatus = SPLIT_UNAVAILABLE

    override fun isActivityEmbedded(activity: Activity): Boolean =
        embeddedActivities.contains(activity)

    override fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    ) {
        TODO("Not yet implemented")
    }

    override fun clearSplitAttributesCalculator() {
        TODO("Not yet implemented")
    }

    override fun getActivityStack(activity: Activity): ActivityStack? {
        TODO("Not yet implemented")
    }

    override fun setLaunchingActivityStack(
        options: ActivityOptions,
        token: IBinder
    ): ActivityOptions {
        TODO("Not yet implemented")
    }

    override fun finishActivityStacks(activityStacks: Set<ActivityStack>) {
        TODO("Not yet implemented")
    }

    override fun invalidateTopVisibleSplitAttributes() {
        TODO("Not yet implemented")
    }

    override fun updateSplitAttributes(splitInfo: SplitInfo, splitAttributes: SplitAttributes) {
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
