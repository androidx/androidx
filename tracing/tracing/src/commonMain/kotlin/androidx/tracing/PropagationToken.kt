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

package androidx.tracing

import kotlin.coroutines.CoroutineContext

/**
 * A token representing state carried forward for context propagation.
 *
 * This interface is only useful for libraries that want to bring their own implementation of
 * context propagation.
 */
public interface PropagationToken {
    /**
     * @return a [CoroutineContext.Element] that needs to be installed in the
     *   [kotlinx.coroutines.currentCoroutineContext] prior to dispatching the suspending block of
     *   code being traced; if the token is being used in a suspending context.
     */
    @DelicateTracingApi public fun contextElementOrNull(): CoroutineContext.Element?
}
