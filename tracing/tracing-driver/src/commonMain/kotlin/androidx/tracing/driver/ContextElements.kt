/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.tracing.driver

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** An [AbstractCoroutineContextElement] that can hold to the [List] of current `flowId`s.. */
internal class FlowContextElement(internal val flowIds: List<Long>) :
    AbstractCoroutineContextElement(KEY) {

    internal companion object {
        internal val KEY: CoroutineContext.Key<FlowContextElement> =
            object : CoroutineContext.Key<FlowContextElement> {}
    }
}

/** Useful in the context of structured concurrency to keep track of flows. */
internal suspend fun obtainFlowContext(): FlowContextElement? {
    return coroutineContext[FlowContextElement.KEY]
}
