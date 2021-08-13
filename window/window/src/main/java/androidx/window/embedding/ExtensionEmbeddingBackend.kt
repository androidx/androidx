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
import androidx.core.util.Consumer
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
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
                    impl = EmbeddingCompat()
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

    private val splitRules: CopyOnWriteArraySet<EmbeddingRule> =
        CopyOnWriteArraySet<EmbeddingRule>()

    override fun getSplitRules(): Set<EmbeddingRule> {
        return splitRules
    }

    override fun setSplitRules(rules: Set<EmbeddingRule>) {
        splitRules.clear()
        splitRules.addAll(rules)
        embeddingExtension?.setSplitRules(splitRules)
    }

    override fun registerRule(rule: EmbeddingRule) {
        if (!splitRules.contains(rule)) {
            splitRules.add(rule)
            embeddingExtension?.setSplitRules(splitRules)
        }
    }

    override fun unregisterRule(rule: EmbeddingRule) {
        if (splitRules.contains(rule)) {
            splitRules.remove(rule)
            embeddingExtension?.setSplitRules(splitRules)
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
}