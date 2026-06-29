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

package androidx.a2ui.engine.model

import androidx.a2ui.engine.internal.SynchronizedObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The central manager for all active surfaces on the client.
 *
 * It exposes an observable list of active [A2uiCoreSurfaceModel]s and acts as a registry to resolve
 * active surfaces by their unique ID.
 *
 * Concurrency design: This class is thread-safe for concurrent operations across different surface
 * IDs (e.g., adding or deleting different surfaces in parallel). It relies on external
 * sequentialization (such as sequential actors or single-threaded queues) to ensure that operations
 * targeting the same surface ID are executed sequentially.
 */
public class A2uiCoreSurfaceGroupModel internal constructor() {
    // TODO(annabelo): reconsider concurrency and disposal handling (including A2uiSurfaceModel)
    private val lock = SynchronizedObject()
    private val _activeSurfaces = MutableStateFlow<List<A2uiCoreSurfaceModel>>(emptyList())

    /** Exposes the currently active surfaces to the host UI framework. */
    public val activeSurfaces: StateFlow<List<A2uiCoreSurfaceModel>> = _activeSurfaces.asStateFlow()

    // Guarded by lock
    private var isDisposed = false

    /**
     * Resolves a specific surface model by its ID.
     *
     * @param id The unique identifier of the surface.
     * @return The active [A2uiCoreSurfaceModel], or `null` if not found.
     */
    internal fun getSurface(id: String): A2uiCoreSurfaceModel? {
        return _activeSurfaces.value.find { it.id == id }
    }

    /**
     * Adds a surface model. If a surface with the same ID already exists, it will be replaced and
     * disposed.
     *
     * @param surface The [A2uiCoreSurfaceModel] to add.
     * @return `true` if the surface was successfully added, `false` otherwise (e.g., if the group
     *   is already disposed).
     */
    internal fun add(surface: A2uiCoreSurfaceModel): Boolean {
        synchronized(lock) {
            if (isDisposed) return false
            val current = _activeSurfaces.value
            val existingSurface = current.find { it.id == surface.id }
            _activeSurfaces.value = current.filter { it.id != surface.id } + surface
            existingSurface?.dispose()
            return true
        }
    }

    /**
     * Deletes and disposes of a surface model by its ID.
     *
     * @param id The unique identifier of the surface to delete.
     */
    internal fun delete(id: String) {
        synchronized(lock) {
            if (isDisposed) return
            val current = _activeSurfaces.value
            val removedSurface = current.find { it.id == id }
            if (removedSurface != null) {
                _activeSurfaces.value = current.filter { it.id != id }
                removedSurface.dispose()
            }
        }
    }

    /** Cleans up all managed surfaces. */
    internal fun dispose() {
        synchronized(lock) {
            if (!isDisposed) {
                isDisposed = true
                val currentSurfaces = _activeSurfaces.value
                _activeSurfaces.value = emptyList()
                currentSurfaces.forEach { it.dispose() }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is A2uiCoreSurfaceGroupModel) return false
        return _activeSurfaces.value == other._activeSurfaces.value
    }

    override fun hashCode(): Int {
        return _activeSurfaces.value.hashCode()
    }

    override fun toString(): String {
        return "A2uiSurfaceGroupModel(activeSurfaces=${_activeSurfaces.value})"
    }
}
