/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.testutils

import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext

/**
 * [CoroutineDispatcher] which keeps track of all its queued jobs.
 */
class TestDispatcher : CoroutineDispatcher() {
    val queue = ConcurrentLinkedQueue<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue.add(block)
    }

    fun executeAll() {
        do {
            val runnable = queue.poll()
            runnable?.run()
        } while (runnable != null)
    }
}
