/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.tracing.benchmark.driver

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

class TestThreadContextElement : ThreadContextElement<Unit> {

    override val key = KEY

    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) {
        // Intentionally does nothing
    }

    override fun updateThreadContext(context: CoroutineContext) {
        // Intentionally does nothing.
    }

    companion object {
        private val KEY: CoroutineContext.Key<TestThreadContextElement> =
            object : CoroutineContext.Key<TestThreadContextElement> {}
    }
}
