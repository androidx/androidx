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
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.SplitInfo
import java.util.function.Consumer
import androidx.window.extensions.embedding.EmbeddingRule as ExtensionsEmbeddingRule

/**
 * Adapter implementation for different historical versions of activity embedding OEM interface in
 * [ActivityEmbeddingComponent]. Only supports the single current version in this implementation.
 */
@ExperimentalWindowApi
internal class EmbeddingCompat constructor(
    private val embeddingExtension: ActivityEmbeddingComponent,
    private val adapter: EmbeddingAdapter
) : EmbeddingInterfaceCompat {
    constructor() : this(
        embeddingComponent(),
        EmbeddingAdapter()
    )

    override fun setSplitRules(rules: Set<EmbeddingRule>) {
        embeddingExtension.setEmbeddingRules(adapter.translate(rules))
    }

    override fun setEmbeddingCallback(embeddingCallback: EmbeddingCallbackInterface) {
        val translatingCallback = EmbeddingTranslatingCallback(embeddingCallback, adapter)
        embeddingExtension.setSplitInfoCallback(translatingCallback)
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
                    ?: EmptyEmbeddingComponent()
            } else {
                EmptyEmbeddingComponent()
            }
        }
    }
}

// Empty implementation of the embedding component to use when the device doesn't provide one and
// avoid null checks.
private class EmptyEmbeddingComponent : ActivityEmbeddingComponent {
    override fun setEmbeddingRules(splitRules: MutableSet<ExtensionsEmbeddingRule>) {
        // empty
    }

    override fun setSplitInfoCallback(consumer: Consumer<MutableList<SplitInfo>>) {
        // empty
    }
}