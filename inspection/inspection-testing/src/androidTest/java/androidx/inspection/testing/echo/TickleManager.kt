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

package androidx.inspection.testing.echo

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow

/**
 * Fake component for test purposes. It represents a library component that is used by "app"
 * and it is observed by our inspector
 */
object TickleManager {
    private val channel = Channel<Unit>(Channel.UNLIMITED)

    val tickles = flow {
        for (tickle in channel) {
            emit(tickle)
        }
    }

    fun tickle() {
        channel.trySend(Unit)
    }
}