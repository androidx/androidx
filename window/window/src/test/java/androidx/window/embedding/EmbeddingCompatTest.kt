/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.core.PredicateAdapter
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import java.util.function.Consumer as JavaConsumer
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EmbeddingCompatTest {

    private val component = mock<ActivityEmbeddingComponent>()
    private val vendorApiLevel = ExtensionsUtil.safeVendorApiLevel
    private val embeddingCompat = EmbeddingCompat(
        component,
        EMBEDDING_ADAPTER,
        CONSUMER_ADAPTER,
        mock()
    )

    @Suppress("Deprecation")
    @Test
    fun setSplitInfoCallback_callsActualMethod() {
        val callback = object : EmbeddingInterfaceCompat.EmbeddingCallbackInterface {
            override fun onSplitInfoChanged(splitInfo: List<SplitInfo>) {
            }
        }
        embeddingCompat.setEmbeddingCallback(callback)

        if (vendorApiLevel < 2) {
            verify(component).setSplitInfoCallback(any<JavaConsumer<List<OEMSplitInfo>>>())
        } else {
            verify(component).setSplitInfoCallback(any<Consumer<List<OEMSplitInfo>>>())
        }
    }

    @Test
    fun setSplitRules_delegatesToActivityEmbeddingComponent() {
        embeddingCompat.setRules(emptySet())

        verify(component).setEmbeddingRules(any())
    }

    @Test
    fun isActivityEmbedded_delegatesToComponent() {
        val activity = mock<Activity>()

        embeddingCompat.isActivityEmbedded(activity)

        verify(component).isActivityEmbedded(activity)
    }

    companion object {
        private val LOADER = EmbeddingCompatTest::class.java.classLoader!!
        private val PREDICATE_ADAPTER = PredicateAdapter(LOADER)
        private val CONSUMER_ADAPTER = ConsumerAdapter(LOADER)
        private val EMBEDDING_ADAPTER = EmbeddingAdapter(PREDICATE_ADAPTER)
    }
}
