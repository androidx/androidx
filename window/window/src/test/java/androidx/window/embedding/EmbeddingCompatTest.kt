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
import androidx.window.WindowSdkExtensions
import androidx.window.core.ConsumerAdapter
import androidx.window.core.PredicateAdapter
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.ActivityStack as OEMActivityStack
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import java.util.concurrent.Executor
import java.util.function.Consumer as JavaConsumer
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EmbeddingCompatTest {

    private val component = mock<ActivityEmbeddingComponent>()
    private val extensionVersion = WindowSdkExtensions.getInstance().extensionVersion
    private val embeddingCompat =
        EmbeddingCompat(
            component,
            EMBEDDING_ADAPTER,
            CONSUMER_ADAPTER,
            mock(),
            mock(),
            mock(),
        )

    @Suppress("Deprecation")
    @Test
    fun setSplitInfoCallback_callsActualMethod() {
        val callback =
            object : EmbeddingInterfaceCompat.EmbeddingCallbackInterface {
                override fun onSplitInfoChanged(splitInfo: List<SplitInfo>) {}

                override fun onActivityStackChanged(activityStacks: List<ActivityStack>) {}
            }
        embeddingCompat.setEmbeddingCallback(callback)

        when (extensionVersion) {
            1 -> verify(component).setSplitInfoCallback(any<JavaConsumer<List<OEMSplitInfo>>>())
            in 2..4 -> verify(component).setSplitInfoCallback(any<Consumer<List<OEMSplitInfo>>>())
            5 -> {
                verify(component).setSplitInfoCallback(any<Consumer<List<OEMSplitInfo>>>())
                verify(component)
                    .registerActivityStackCallback(
                        any<Executor>(),
                        any<Consumer<List<OEMActivityStack>>>()
                    )
            }
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
