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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import kotlinx.atomicfu.atomic

/**
 * A helper class to schedule an update for the interop component whenever the [State] used by
 * the [update] lambda is changed.
 *
 * @param component The interop component to be updated.
 * @param update The lambda to be called whenever the state used by this lambda is changed.
 * @param invokeLater The lambda to register [update] execution to defer it in order to sync it with
 * Compose rendering. The aim of this is to make visual changes to UIKit and Compose
 * simultaneously.
 */
internal class InteropViewUpdater<T : Any>(
    private val component: T,
    update: (T) -> Unit,
    private val invokeLater: (() -> Unit) -> Unit,
) {
    private var isDisposed = false
    private val isUpdateScheduled = atomic(false)
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    private val scheduleUpdate = { _: T ->
        if (!isUpdateScheduled.getAndSet(true)) {
            invokeLater {
                isUpdateScheduled.value = false
                if (!isDisposed) {
                    performUpdate()
                }
            }
        }
    }

    var update: (T) -> Unit = update
        set(value) {
            if (field != value) {
                field = value
                performUpdate()
            }
        }

    private fun performUpdate() {
        // don't replace scheduleUpdate by lambda reference,
        // scheduleUpdate should always be the same instance
        snapshotObserver.observeReads(component, scheduleUpdate) {
            update(component)
        }
    }

    init {
        snapshotObserver.start()
        performUpdate()
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
        isDisposed = true
    }
}
