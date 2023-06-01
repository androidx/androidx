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
import android.os.IBinder
import android.util.Log
import androidx.window.core.BuildConfig
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.core.VerificationMode
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import androidx.window.embedding.SplitController.SplitSupportStatus.Companion.SPLIT_AVAILABLE
import androidx.window.extensions.WindowExtensions.VENDOR_API_LEVEL_2
import androidx.window.extensions.WindowExtensions.VENDOR_API_LEVEL_3
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import java.lang.reflect.Proxy

/**
 * Adapter implementation for different historical versions of activity embedding OEM interface in
 * [ActivityEmbeddingComponent]. Only supports the single current version in this implementation.
 */
internal class EmbeddingCompat constructor(
    private val embeddingExtension: ActivityEmbeddingComponent,
    private val adapter: EmbeddingAdapter,
    private val consumerAdapter: ConsumerAdapter,
    private val applicationContext: Context
) : EmbeddingInterfaceCompat {

    override fun setRules(rules: Set<EmbeddingRule>) {
        var hasSplitRule = false
        for (rule in rules) {
            if (rule is SplitRule) {
                hasSplitRule = true
                break
            }
        }
        if (hasSplitRule &&
            SplitController.getInstance(applicationContext).splitSupportStatus != SPLIT_AVAILABLE
        ) {
            if (BuildConfig.verificationMode == VerificationMode.LOG) {
                Log.w(
                    TAG, "Cannot set SplitRule because ActivityEmbedding Split is not " +
                        "supported or PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED is not set."
                )
            }
            return
        }

        val r = adapter.translate(applicationContext, rules)
        embeddingExtension.setEmbeddingRules(r)
    }

    override fun setEmbeddingCallback(embeddingCallback: EmbeddingCallbackInterface) {
        if (ExtensionsUtil.safeVendorApiLevel < VENDOR_API_LEVEL_2) {
            consumerAdapter.addConsumer(
                embeddingExtension,
                List::class,
                "setSplitInfoCallback"
            ) { values ->
                val splitInfoList = values.filterIsInstance<OEMSplitInfo>()
                embeddingCallback.onSplitInfoChanged(adapter.translate(splitInfoList))
            }
        } else {
            val callback = Consumer<List<OEMSplitInfo>> { splitInfoList ->
                embeddingCallback.onSplitInfoChanged(adapter.translate(splitInfoList))
            }
            embeddingExtension.setSplitInfoCallback(callback)
        }
    }

    override fun isActivityEmbedded(activity: Activity): Boolean {
        return embeddingExtension.isActivityEmbedded(activity)
    }

    override fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    ) {
        if (!isSplitAttributesCalculatorSupported()) {
            throw UnsupportedOperationException("#setSplitAttributesCalculator is not supported " +
                "on the device.")
        }
        embeddingExtension.setSplitAttributesCalculator(
            adapter.translateSplitAttributesCalculator(calculator)
        )
    }

    override fun clearSplitAttributesCalculator() {
        if (!isSplitAttributesCalculatorSupported()) {
            throw UnsupportedOperationException("#clearSplitAttributesCalculator is not " +
                "supported on the device.")
        }
        embeddingExtension.clearSplitAttributesCalculator()
    }

    override fun isSplitAttributesCalculatorSupported(): Boolean =
        ExtensionsUtil.safeVendorApiLevel >= VENDOR_API_LEVEL_2

    override fun finishActivityStacks(activityStacks: Set<ActivityStack>) {
        if (!isFinishActivityStacksSupported()) {
            throw UnsupportedOperationException("#finishActivityStacks is not " +
                "supported on the device.")
        }
        val stackTokens = activityStacks.mapTo(mutableSetOf()) { it.token }
        embeddingExtension.finishActivityStacks(stackTokens)
    }

    override fun isFinishActivityStacksSupported(): Boolean =
        ExtensionsUtil.safeVendorApiLevel >= VENDOR_API_LEVEL_3

    override fun invalidateTopVisibleSplitAttributes() {
        if (!areSplitAttributesUpdatesSupported()) {
            throw UnsupportedOperationException("#invalidateTopVisibleSplitAttributes is not " +
                "supported on the device.")
        }
        embeddingExtension.invalidateTopVisibleSplitAttributes()
    }

    override fun updateSplitAttributes(
        splitInfo: SplitInfo,
        splitAttributes: SplitAttributes
    ) {
        if (!areSplitAttributesUpdatesSupported()) {
            throw UnsupportedOperationException("#updateSplitAttributes is not supported on the " +
                "device.")
        }
        embeddingExtension.updateSplitAttributes(
            splitInfo.token,
            adapter.translateSplitAttributes(splitAttributes)
        )
    }

    override fun areSplitAttributesUpdatesSupported(): Boolean =
        ExtensionsUtil.safeVendorApiLevel >= VENDOR_API_LEVEL_3

    override fun setLaunchingActivityStack(
        options: ActivityOptions,
        token: IBinder
    ): ActivityOptions {
        return embeddingExtension.setLaunchingActivityStack(options, token)
    }

    companion object {
        const val DEBUG = true
        private const val TAG = "EmbeddingCompat"

        fun isEmbeddingAvailable(): Boolean {
            return try {
                EmbeddingCompat::class.java.classLoader?.let { loader ->
                    SafeActivityEmbeddingComponentProvider(
                        loader,
                        ConsumerAdapter(loader),
                        WindowExtensionsProvider.getWindowExtensions(),
                    ).activityEmbeddingComponent != null
                } ?: false
            } catch (e: NoClassDefFoundError) {
                if (DEBUG) {
                    Log.d(TAG, "Embedding extension version not found")
                }
                false
            } catch (e: UnsupportedOperationException) {
                if (DEBUG) {
                    Log.d(TAG, "Stub Extension")
                }
                false
            }
        }

        fun embeddingComponent(): ActivityEmbeddingComponent {
            return if (isEmbeddingAvailable()) {
                EmbeddingCompat::class.java.classLoader?.let { loader ->
                    SafeActivityEmbeddingComponentProvider(
                        loader,
                        ConsumerAdapter(loader),
                        WindowExtensionsProvider.getWindowExtensions(),
                    ).activityEmbeddingComponent
                } ?: emptyActivityEmbeddingProxy()
            } else {
                emptyActivityEmbeddingProxy()
            }
        }

        private fun emptyActivityEmbeddingProxy(): ActivityEmbeddingComponent {
            return Proxy.newProxyInstance(
                EmbeddingCompat::class.java.classLoader,
                arrayOf(ActivityEmbeddingComponent::class.java)
            ) { _, _, _ -> } as ActivityEmbeddingComponent
        }
    }
}