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

package androidx.glance.session

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * Mechanism for glance sessions to start a monitor of global snapshot state writes in order to
 * schedule periodic dispatch of apply notifications.
 * Sessions should call [ensureStarted] during setup to initialize periodic global snapshot
 * notifications (which are necessary in order for recompositions to be scheduled in response to
 * state changes). These will be sent on Dispatchers.Default.
 * This is based on [androidx.compose.ui.platform.GlobalSnapshotManager].
 */
@RestrictTo(Scope.LIBRARY_GROUP)
object GlobalSnapshotManager {
    private val started = AtomicBoolean(false)
    private val sent = AtomicBoolean(false)

    fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            val channel = Channel<Unit>(1)
            CoroutineScope(Dispatchers.Default).launch {
                channel.consumeEach {
                    sent.set(false)
                    Snapshot.sendApplyNotifications()
                }
            }
            Snapshot.registerGlobalWriteObserver {
                if (sent.compareAndSet(false, true)) {
                    channel.trySend(Unit)
                }
            }
        }
    }
}

/**
 * Monitors global snapshot state writes and sends apply notifications.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
suspend fun globalSnapshotMonitor() {
    val channel = Channel<Unit>(1)
    val sent = AtomicBoolean(false)
    val observerHandle = Snapshot.registerGlobalWriteObserver {
        if (sent.compareAndSet(false, true)) {
            channel.trySend(Unit)
        }
    }
    try {
        channel.consumeEach {
            sent.set(false)
            Snapshot.sendApplyNotifications()
        }
    } finally {
        observerHandle.dispose()
    }
}
