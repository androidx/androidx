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
import androidx.window.embedding.SplitInfo
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * A class to verify logic within [StubEmbeddingBackend].
 */
internal class StubEmbeddingBackendTest {

    private val backend = StubEmbeddingBackend()

    @Test
    fun removingSplitInfoListenerClearsListeners() {
        val mockActivity = mock<Activity>()
        val mockCallback = mock<Consumer<List<SplitInfo>>>()

        backend.addSplitListenerForActivity(
            mockActivity,
            MoreExecutors.directExecutor(),
            mockCallback
        )
        backend.removeSplitListenerForActivity(mockCallback)

        assertFalse(backend.hasSplitInfoListeners(mockActivity))
    }

    @Test
    fun hasListenersIsInitiallyFalse() {
        assertFalse(backend.hasSplitInfoListeners(mock()))
    }
}
