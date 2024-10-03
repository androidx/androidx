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

package androidx.camera.camera2.pipe.graph

import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.media.ImageSource

/**
 * A SurfaceGraph tracks the current stream-to-surface mapping state for a [CameraGraph] instance.
 *
 * It's primary responsibility is aggregating the current stream-to-surface mapping and passing the
 * most up to date version to the [CameraController] instance.
 */
internal class SurfaceGraph(
    private val streamGraphImpl: StreamGraphImpl,
    private val cameraController: CameraController,
    private val surfaceManager: CameraSurfaceManager,
    private val imageSources: Map<StreamId, ImageSource>,
    private val cameraGraphFlags: CameraGraph.Flags,
) {
    private val lock = Any()

    @GuardedBy("lock")
    private val surfaceMap = imageSources.mapValuesTo(mutableMapOf()) { it.value.surface }

    @GuardedBy("lock")
    private val surfaceUsageMap: MutableMap<Surface, AutoCloseable> = mutableMapOf()

    @GuardedBy("lock") private var closed: Boolean = false

    operator fun set(streamId: StreamId, surface: Surface?) {
        check(!imageSources.keys.contains(streamId)) {
            "Cannot configure surface for $streamId, it is permanently assigned to " +
                "${imageSources[streamId]}"
        }
        val closeable =
            synchronized(lock) {
                if (closed) {
                    Log.warn { "Attempted to set $streamId to $surface after close!" }
                    return
                }

                Log.info {
                    if (surface != null) {
                        "Configured $streamId to use $surface"
                    } else {
                        "Removed surface for $streamId"
                    }
                }

                if (cameraGraphFlags.disableGraphLevelSurfaceTracking) {
                    if (surface == null) {
                        surfaceMap.remove(streamId)
                    } else {
                        surfaceMap[streamId] = surface
                    }
                    return@synchronized null
                }

                var oldSurfaceToken: AutoCloseable? = null
                if (surface == null) {
                    // TODO: Tell the graph processor that it should resubmit the repeating request
                    // or reconfigure the camera2 captureSession
                    val oldSurface = surfaceMap.remove(streamId)
                    if (oldSurface != null) {
                        oldSurfaceToken = surfaceUsageMap.remove(oldSurface)
                    }
                } else {
                    val oldSurface = surfaceMap[streamId]
                    surfaceMap[streamId] = surface

                    if (oldSurface != surface) {
                        check(!surfaceUsageMap.containsKey(surface)) {
                            "Surface ($surface) is already in use!"
                        }
                        oldSurfaceToken = surfaceUsageMap.remove(oldSurface)
                        val newToken = surfaceManager.registerSurface(surface)
                        surfaceUsageMap[surface] = newToken
                    }
                }

                return@synchronized oldSurfaceToken
            }
        maybeUpdateSurfaces()
        closeable?.close()
    }

    fun close() {
        val closeables =
            synchronized(lock) {
                if (closed) {
                    return
                }
                closed = true
                surfaceMap.clear()
                val tokensToClose = surfaceUsageMap.values.toList()
                surfaceUsageMap.clear()
                tokensToClose
            }

        for (closeable in closeables) {
            closeable.close()
        }
    }

    private fun maybeUpdateSurfaces() {
        // Rules:
        // 1. There must be at least one non-null surface.
        // 2. All non-deferrable streams must have a non-null surface.

        val surfaces = buildSurfaceMap()
        if (surfaces.isEmpty()) {
            return
        }
        cameraController.updateSurfaceMap(surfaces)
    }

    private fun buildSurfaceMap(): Map<StreamId, Surface> =
        synchronized(lock) {
            val surfaces = mutableMapOf<StreamId, Surface>()
            for (outputConfig in streamGraphImpl.outputConfigs) {
                for (stream in outputConfig.streamBuilder) {
                    val surface = surfaceMap[stream.id]
                    if (surface == null) {
                        if (!outputConfig.deferrable) {
                            // If output is non-deferrable, a surface must be available or the
                            // config is not yet valid. Exit now with an empty map.
                            return emptyMap()
                        }
                    } else {
                        surfaces[stream.id] = surface
                    }
                }
            }
            return surfaces
        }
}
