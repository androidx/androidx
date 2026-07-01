/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.runtime.a2ui

import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An implementation of [A2uiCoreComponentRegistry] for the Jetpack Compose A2UI renderer.
 *
 * This registry manages A2UI components backed by Compose [MutableState]s. It enforces
 * single-snapshot atomic batch updates, and utilizes structural equality checks on the caller's
 * thread prior to applying updates to enable strict referential equality for component properties
 * on the UI thread.
 */
@Stable
internal class A2uiComponentRegistry : A2uiCoreComponentRegistry {

    /** The map of component IDs to the corresponding component record snapshot states. */
    private val registry = MutableScatterMap<String, MutableState<A2uiComponentRecord?>>()

    /**
     * Synchronizes concurrent updates to ensure thread-safety and prevent snapshot conflicts.
     *
     * The lock is configured as fair to ensure updates are applied in the order they are received.
     */
    private val updateLock = ReentrantLock(true)

    /**
     * Synchronizes concurrent [registry]-level operations, e.g., record insertion.
     *
     * The lock is configured as fair to ensure updates are applied in the order they are received.
     */
    private val registryLock = ReentrantLock(true)

    override fun update(components: List<A2uiComponentPayload>) {
        if (components.isEmpty()) return

        updateLock.withLock {
            val statesToApply = ArrayList<MutableState<A2uiComponentRecord?>>(components.size)
            val recordsToApply = ArrayList<A2uiComponentRecord.Valid>(components.size)
            val seenIds = MutableScatterSet<String>(components.size)

            // Iterate backwards so that if a batch contains duplicate IDs, the last payload
            // provided naturally wins.
            for (i in components.indices.reversed()) {
                val payload = components[i]
                if (!seenIds.add(payload.id)) {
                    continue // Skip earlier payloads for an ID that was already processed
                }

                val state =
                    registryLock.withLock { registry.getOrPut(payload.id) { mutableStateOf(null) } }
                val existing = state.value as? A2uiComponentRecord.Valid

                if (
                    existing == null ||
                        existing.type != payload.type ||
                        existing.properties.raw != payload.properties
                ) {
                    statesToApply.add(state)
                    recordsToApply.add(
                        A2uiComponentRecord.Valid(
                            type = payload.type,
                            properties = A2uiComponentProperties(payload.properties),
                        )
                    )
                }
            }

            if (statesToApply.isNotEmpty()) {
                Snapshot.withMutableSnapshot {
                    for (i in statesToApply.indices) {
                        statesToApply[i].value = recordsToApply[i]
                    }
                }
            }
        }

        Snapshot.sendApplyNotifications()
    }

    override fun reportError(id: String, exception: A2uiException) {
        updateLock.withLock {
            val state = registryLock.withLock { registry.getOrPut(id) { mutableStateOf(null) } }
            Snapshot.withMutableSnapshot { state.value = A2uiComponentRecord.Error(exception) }
        }

        Snapshot.sendApplyNotifications()
    }

    override fun dispose() {
        updateLock.withLock {
            registryLock.withLock {
                Snapshot.withMutableSnapshot { registry.forEachValue { it.value = null } }
                registry.clear()
            }
        }

        Snapshot.sendApplyNotifications()
    }

    internal fun get(id: String): A2uiComponentRecord? {
        val state = registryLock.withLock { registry.getOrPut(id) { mutableStateOf(null) } }
        return state.value
    }
}
