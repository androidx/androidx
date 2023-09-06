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
import android.app.ActivityOptions
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.collection.ArraySet
import androidx.core.util.Consumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowProperties
import androidx.window.core.BuildConfig
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.core.PredicateAdapter
import androidx.window.core.VerificationMode
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import androidx.window.embedding.ExtensionEmbeddingBackend.Api31Impl.isSplitPropertyEnabled
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ExtensionEmbeddingBackend @VisibleForTesting constructor(
    private val applicationContext: Context,
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

        fun getInstance(context: Context): EmbeddingBackend {
            if (globalInstance == null) {
                globalLock.withLock {
                    if (globalInstance == null) {
                        val applicationContext = context.applicationContext
                        val embeddingExtension = initAndVerifyEmbeddingExtension(applicationContext)
                        globalInstance = ExtensionEmbeddingBackend(
                            applicationContext,
                            embeddingExtension
                        )
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
        private fun initAndVerifyEmbeddingExtension(
            applicationContext: Context
        ): EmbeddingInterfaceCompat? {
            var impl: EmbeddingInterfaceCompat? = null
            try {
                if (isExtensionVersionSupported(ExtensionsUtil.safeVendorApiLevel) &&
                    EmbeddingCompat.isEmbeddingAvailable()
                ) {
                    impl = EmbeddingBackend::class.java.classLoader?.let { loader ->
                        EmbeddingCompat(
                            EmbeddingCompat.embeddingComponent(),
                            EmbeddingAdapter(PredicateAdapter(loader)),
                            ConsumerAdapter(loader),
                            applicationContext
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
    override fun getRules(): Set<EmbeddingRule> {
        globalLock.withLock { return ruleTracker.splitRules.toSet() }
    }

    @GuardedBy("globalLock")
    override fun setRules(rules: Set<EmbeddingRule>) {
        globalLock.withLock {
            ruleTracker.setRules(rules)
            embeddingExtension?.setRules(getRules())
        }
    }

    @GuardedBy("globalLock")
    override fun addRule(rule: EmbeddingRule) {
        globalLock.withLock {
            if (rule !in ruleTracker) {
                ruleTracker.addOrUpdateRule(rule)
                embeddingExtension?.setRules(getRules())
            }
        }
    }

    @GuardedBy("globalLock")
    override fun removeRule(rule: EmbeddingRule) {
        globalLock.withLock {
            if (rule in ruleTracker) {
                ruleTracker.removeRule(rule)
                embeddingExtension?.setRules(getRules())
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
                    throw IllegalArgumentException(
                        "Duplicated tag: $tag. Tag must be unique " +
                            "among all registered rules"
                    )
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

    override fun addSplitListenerForActivity(
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

    override fun removeSplitListenerForActivity(
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

    private fun areExtensionsAvailable(): Boolean {
        return embeddingExtension != null
    }

    override val splitSupportStatus: SplitController.SplitSupportStatus by lazy {
        when {
            !areExtensionsAvailable() -> {
                SplitController.SplitSupportStatus.SPLIT_UNAVAILABLE
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                isSplitPropertyEnabled(applicationContext)
            }
            else -> {
                // The PackageManager#getProperty API is not supported before S, assuming
                // the property is enabled to keep the same behavior on earlier platforms.
                SplitController.SplitSupportStatus.SPLIT_AVAILABLE
            }
        }
    }

    override fun isActivityEmbedded(activity: Activity): Boolean {
        return embeddingExtension?.isActivityEmbedded(activity) ?: false
    }

    @RequiresWindowSdkExtension(2)
    override fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    ) {
        globalLock.withLock {
            embeddingExtension?.setSplitAttributesCalculator(calculator)
        }
    }

    @RequiresWindowSdkExtension(2)
    override fun clearSplitAttributesCalculator() {
        globalLock.withLock {
            embeddingExtension?.clearSplitAttributesCalculator()
        }
    }

    override fun getActivityStack(activity: Activity): ActivityStack? {
        globalLock.withLock {
            val lastInfo: List<SplitInfo> = splitInfoEmbeddingCallback.lastInfo ?: return null
            for (info in lastInfo) {
                if (activity !in info) {
                    continue
                }
                if (activity in info.primaryActivityStack) {
                    return info.primaryActivityStack
                }
                if (activity in info.secondaryActivityStack) {
                    return info.secondaryActivityStack
                }
            }
            return null
        }
    }

    @RequiresWindowSdkExtension(3)
    override fun setLaunchingActivityStack(
        options: ActivityOptions,
        token: IBinder
    ): ActivityOptions = embeddingExtension?.setLaunchingActivityStack(options, token) ?: options

    @RequiresWindowSdkExtension(3)
    override fun finishActivityStacks(activityStacks: Set<ActivityStack>) {
        embeddingExtension?.finishActivityStacks(activityStacks)
    }

    @RequiresWindowSdkExtension(3)
    override fun invalidateTopVisibleSplitAttributes() {
        embeddingExtension?.invalidateTopVisibleSplitAttributes()
    }

    @RequiresWindowSdkExtension(3)
    override fun updateSplitAttributes(
        splitInfo: SplitInfo,
        splitAttributes: SplitAttributes
    ) {
        embeddingExtension?.updateSplitAttributes(splitInfo, splitAttributes)
    }

    @RequiresApi(31)
    private object Api31Impl {
        @DoNotInline
        fun isSplitPropertyEnabled(context: Context): SplitController.SplitSupportStatus {
            val property = try {
                context.packageManager.getProperty(
                    WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED,
                    context.packageName
                )
            } catch (e: PackageManager.NameNotFoundException) {
                if (BuildConfig.verificationMode == VerificationMode.LOG) {
                    Log.w(TAG, WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED +
                            " must be set and enabled in AndroidManifest.xml to use splits APIs."
                    )
                }
                return SplitController.SplitSupportStatus.SPLIT_ERROR_PROPERTY_NOT_DECLARED
            } catch (e: Exception) {
                if (BuildConfig.verificationMode == VerificationMode.LOG) {
                    // This can happen when it is a test environment that doesn't support
                    // getProperty.
                    Log.e(TAG, "PackageManager.getProperty is not supported", e)
                }
                return SplitController.SplitSupportStatus.SPLIT_ERROR_PROPERTY_NOT_DECLARED
            }
            if (!property.isBoolean) {
                if (BuildConfig.verificationMode == VerificationMode.LOG) {
                    Log.w(TAG, WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED +
                            " must have a boolean value"
                    )
                }
                return SplitController.SplitSupportStatus.SPLIT_ERROR_PROPERTY_NOT_DECLARED
            }
            return if (property.boolean) {
                SplitController.SplitSupportStatus.SPLIT_AVAILABLE
            } else {
                SplitController.SplitSupportStatus.SPLIT_UNAVAILABLE
            }
        }
    }
}
