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

import android.app.Activity
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.collection.ArraySet
import androidx.core.util.Consumer
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.PredicateAdapter
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@ExperimentalWindowApi
internal class ExtensionEmbeddingBackend @VisibleForTesting constructor(
    @field:VisibleForTesting @field:GuardedBy(
        "globalLock"
    ) var embeddingExtension: EmbeddingInterfaceCompat?
) : EmbeddingBackend {

    @VisibleForTesting
    val splitChangeCallbacks: CopyOnWriteArrayList<SplitListenerWrapper>
    private val splitInfoEmbeddingCallback = EmbeddingCallbackImpl()

    init {
        splitChangeCallbacks = CopyOnWriteArrayList<SplitListenerWrapper>()
        embeddingExtension?.setEmbeddingCallback(splitInfoEmbeddingCallback)
    }

    companion object {
        @Volatile
        private var globalInstance: ExtensionEmbeddingBackend? = null
        private val globalLock = ReentrantLock()
        private const val TAG = "EmbeddingBackend"

        fun getInstance(): ExtensionEmbeddingBackend {
            if (globalInstance == null) {
                globalLock.withLock {
                    if (globalInstance == null) {
                        val embeddingExtension = initAndVerifyEmbeddingExtension()
                        globalInstance = ExtensionEmbeddingBackend(embeddingExtension)
                    }
                }
            }
            return globalInstance!!
        }

        /**
         * Loads an instance of [androidx.window.extensions.embedding.ActivityEmbeddingComponent]
         * implemented by OEM if available on this device. This also verifies if the loaded
         * implementation conforms to the declared API version.
         */
        private fun initAndVerifyEmbeddingExtension(): EmbeddingInterfaceCompat? {
            var impl: EmbeddingInterfaceCompat? = null
            try {
                if (isExtensionVersionSupported(EmbeddingCompat.getExtensionApiLevel()) &&
                    EmbeddingCompat.isEmbeddingAvailable()
                ) {
                    impl = EmbeddingBackend::class.java.classLoader?.let { loader ->
                        EmbeddingCompat(
                            EmbeddingCompat.embeddingComponent(),
                            EmbeddingAdapter(PredicateAdapter(loader)),
                            ConsumerAdapter(loader)
                        )
                    }
                    // TODO(b/190433400): Check API conformance
                }
            } catch (t: Throwable) {
                if (EmbeddingCompat.DEBUG) {
                    Log.d(TAG, "Failed to load embedding extension: $t")
                }
                impl = null
            }
            if (impl == null) {
                if (EmbeddingCompat.DEBUG) {
                    Log.d(TAG, "No supported embedding extension found")
                }
            }
            return impl
        }

        /**
         * Checks if the Extension version provided on this device is supported by the current
         * version of the library.
         */
        @VisibleForTesting
        fun isExtensionVersionSupported(extensionVersion: Int?): Boolean {
            if (extensionVersion == null) {
                return false
            }

            return extensionVersion >= 1
        }
    }

    @GuardedBy("globalLock")
    private val ruleTracker = RuleTracker()

    @GuardedBy("globalLock")
    override fun getSplitRules(): Set<EmbeddingRule> {
        globalLock.withLock { return ruleTracker.splitRules }
    }

    @GuardedBy("globalLock")
    override fun setSplitRules(rules: Set<EmbeddingRule>) {
        globalLock.withLock {
            ruleTracker.setRules(rules)
            embeddingExtension?.setSplitRules(getSplitRules())
        }
    }

    @GuardedBy("globalLock")
    override fun registerRule(rule: EmbeddingRule) {
        globalLock.withLock {
            if (rule !in ruleTracker) {
                ruleTracker.addOrUpdateRule(rule)
                embeddingExtension?.setSplitRules(getSplitRules())
            }
        }
    }

    @GuardedBy("globalLock")
    override fun unregisterRule(rule: EmbeddingRule) {
        globalLock.withLock {
            if (rule in ruleTracker) {
                ruleTracker.removeRule(rule)
                embeddingExtension?.setSplitRules(getSplitRules())
            }
        }
    }

    /**
     * A helper class to manage the registered [tags][EmbeddingRule.tag] and [rules][EmbeddingRule]
     * It supports:
     *   - Add a set of [rules][EmbeddingRule] and verify if there's duplicated [EmbeddingRule.tag]
     *     if needed.
     *   - Clears all registered [rules][EmbeddingRule]
     *   - Add a runtime [rule][EmbeddingRule] or update an existing [rule][EmbeddingRule] by
     *   [tag][EmbeddingRule.tag] if the tag has been registered.
     *   - Remove a runtime [rule][EmbeddingRule]
     */
    private class RuleTracker {
        val splitRules = ArraySet<EmbeddingRule>()
        private val tagRuleMap = HashMap<String, EmbeddingRule>()

        fun setRules(rules: Set<EmbeddingRule>) {
            clearRules()
            rules.forEach { rule -> addOrUpdateRule(rule, throwOnDuplicateTag = true) }
        }

        fun clearRules() {
            splitRules.clear()
            tagRuleMap.clear()
        }

        /**
         * Adds a rule to [RuleTracker] or update an existing rule if the [tag][EmbeddingRule.tag]
         * has been registered and `throwOnDuplicateTag` is `false`
         * @throws IllegalArgumentException if `throwOnDuplicateTag` is `true` and the
         * [tag][EmbeddingRule.tag] has been registered.
         */
        fun addOrUpdateRule(rule: EmbeddingRule, throwOnDuplicateTag: Boolean = false) {
            if (rule in splitRules) {
                return
            }
            val tag = rule.tag
            if (tag == null) {
                splitRules.add(rule)
            } else if (tagRuleMap.containsKey(tag)) {
                if (throwOnDuplicateTag) {
                    throw IllegalArgumentException("Duplicated tag: $tag. Tag must be unique " +
                        "among all registered rules")
                } else {
                    // Update the rule if throwOnDuplicateTag = false
                    val oldRule = tagRuleMap[tag]
                    splitRules.remove(oldRule)
                    tagRuleMap[tag] = rule
                    splitRules.add(rule)
                }
            } else {
                tagRuleMap[tag] = rule
                splitRules.add(rule)
            }
        }

        fun removeRule(rule: EmbeddingRule) {
            if (rule !in splitRules) {
                return
            }
            splitRules.remove(rule)
            val tag = rule.tag
            if (tag != null) {
                tagRuleMap.remove(rule.tag)
            }
        }

        operator fun contains(rule: EmbeddingRule): Boolean {
            return splitRules.contains(rule)
        }
    }

    /**
     * Wrapper around [Consumer<List<SplitInfo>>] that also includes the [Executor]
     * on which the callback should run and the [Activity].
     */
    internal class SplitListenerWrapper(
        private val activity: Activity,
        private val executor: Executor,
        val callback: Consumer<List<SplitInfo>>
    ) {
        private var lastValue: List<SplitInfo>? = null
        fun accept(splitInfoList: List<SplitInfo>) {
            val splitsWithActivity = splitInfoList.filter { splitState ->
                splitState.contains(activity)
            }
            if (splitsWithActivity == lastValue) {
                return
            }
            lastValue = splitsWithActivity
            executor.execute { callback.accept(splitsWithActivity) }
        }
    }

    override fun registerSplitListenerForActivity(
        activity: Activity,
        executor: Executor,
        callback: Consumer<List<SplitInfo>>
    ) {
        globalLock.withLock {
            if (embeddingExtension == null) {
                if (EmbeddingCompat.DEBUG) {
                    Log.v(TAG, "Extension not loaded, skipping callback registration.")
                }
                callback.accept(emptyList())
                return
            }

            val callbackWrapper = SplitListenerWrapper(activity, executor, callback)
            splitChangeCallbacks.add(callbackWrapper)
            if (splitInfoEmbeddingCallback.lastInfo != null) {
                callbackWrapper.accept(splitInfoEmbeddingCallback.lastInfo!!)
            } else {
                callbackWrapper.accept(emptyList())
            }
        }
    }

    override fun unregisterSplitListenerForActivity(
        consumer: Consumer<List<SplitInfo>>
    ) {
        globalLock.withLock {
            for (callbackWrapper in splitChangeCallbacks) {
                if (callbackWrapper.callback == consumer) {
                    splitChangeCallbacks.remove(callbackWrapper)
                    break
                }
            }
        }
    }

    /**
     * Extension callback implementation of the split information. Keeps track of last reported
     * values.
     */
    internal inner class EmbeddingCallbackImpl : EmbeddingCallbackInterface {
        var lastInfo: List<SplitInfo>? = null
        override fun onSplitInfoChanged(splitInfo: List<SplitInfo>) {
            lastInfo = splitInfo
            for (callbackWrapper in splitChangeCallbacks) {
                callbackWrapper.accept(splitInfo)
            }
        }
    }

    override fun isSplitSupported(): Boolean {
        return embeddingExtension != null
    }

    override fun isActivityEmbedded(activity: Activity): Boolean {
        return embeddingExtension?.isActivityEmbedded(activity) ?: false
    }

    override fun setSplitAttributesCalculator(calculator: SplitAttributesCalculator) {
        embeddingExtension?.setSplitAttributesCalculator(calculator)
    }

    override fun clearSplitAttributesCalculator() {
        embeddingExtension?.clearSplitAttributesCalculator()
    }

    override fun isSplitAttributesCalculatorSupported(): Boolean =
        embeddingExtension?.isSplitAttributesCalculatorSupported() ?: false
}