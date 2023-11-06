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

package androidx.datastore.core

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred

/** The actions for the actor. */
internal sealed class Message<T> {
    abstract val lastState: State<T>?

    /**
     * Represents a read operation. If the data is already cached, this is a no-op. If data
     * has not been cached, it triggers a new read to the specified dataChannel.
     */
    class Read<T>(
        override val lastState: State<T>?
    ) : Message<T>()

    /** Represents an update operation. */
    class Update<T>(
        val transform: suspend (t: T) -> T,
        /**
         * Used to signal (un)successful completion of the update to the caller.
         */
        val ack: CompletableDeferred<T>,
        override val lastState: State<T>?,
        val callerContext: CoroutineContext
    ) : Message<T>()
}
