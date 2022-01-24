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

import android.util.Log
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import java.lang.reflect.Proxy
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo

/**
 * Adapter implementation for different historical versions of activity embedding OEM interface in
 * [ActivityEmbeddingComponent]. Only supports the single current version in this implementation.
 */
@ExperimentalWindowApi
internal class EmbeddingCompat constructor(
    private val embeddingExtension: ActivityEmbeddingComponent,
    private val adapter: EmbeddingAdapter,
    private val consumerAdapter: ConsumerAdapter
) : EmbeddingInterfaceCompat {

    override fun setSplitRules(rules: Set<EmbeddingRule>) {
        val r = adapter.translate(rules)
        embeddingExtension.setEmbeddingRules(r)
    }

    override fun setEmbeddingCallback(embeddingCallback: EmbeddingCallbackInterface) {
        consumerAdapter.addConsumer(
            embeddingExtension,
            List::class,
            "setSplitInfoCallback"
        ) { values ->
            val splitInfoList = values.filterIsInstance<OEMSplitInfo>()
            embeddingCallback.onSplitInfoChanged(adapter.translate(splitInfoList))
        }
    }

    companion object {
        const val DEBUG = true
        private const val TAG = "EmbeddingCompat"

        fun getExtensionApiLevel(): Int? {
            return try {
                WindowExtensionsProvider.getWindowExtensions().vendorApiLevel
            } catch (e: NoClassDefFoundError) {
                if (DEBUG) {
                    Log.d(TAG, "Embedding extension version not found")
                }
                null
            } catch (e: UnsupportedOperationException) {
                if (DEBUG) {
                    Log.d(TAG, "Stub Extension")
                }
                null
            }
        }

        fun isEmbeddingAvailable(): Boolean {
            return try {
                WindowExtensionsProvider.getWindowExtensions().activityEmbeddingComponent != null
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
                WindowExtensionsProvider.getWindowExtensions().getActivityEmbeddingComponent()
                    ?: Proxy.newProxyInstance(
                        EmbeddingCompat::class.java.classLoader,
                        arrayOf(ActivityEmbeddingComponent::class.java)
                    ) { _, _, _ -> } as ActivityEmbeddingComponent
            } else {
                Proxy.newProxyInstance(
                    EmbeddingCompat::class.java.classLoader,
                    arrayOf(ActivityEmbeddingComponent::class.java)
                ) { _, _, _ -> } as ActivityEmbeddingComponent
            }
        }
    }
}
