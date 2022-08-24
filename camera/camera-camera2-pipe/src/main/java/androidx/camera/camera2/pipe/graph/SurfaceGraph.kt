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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.core.Log
import javax.inject.Inject

/**
 * A SurfaceGraph tracks the current stream-to-surface mapping state for a [CameraGraph] instance.
 *
 * It's primary responsibility is aggregating the current stream-to-surface mapping and passing the
 * most up to date version to the [CameraController] instance.
 */
@RequiresApi(21)
@CameraGraphScope
internal class SurfaceGraph @Inject constructor(
    private val streamGraph: StreamGraphImpl,
    private val cameraController: CameraController
) {
    private val lock = Any()

    @GuardedBy("lock")
    private val surfaceMap: MutableMap<StreamId, Surface> = mutableMapOf()

    operator fun set(stream: StreamId, surface: Surface?) {
        Log.info {
            if (surface != null) {
                "Configured $stream to use $surface"
            } else {
                "Removed surface for $stream"
            }
        }

        synchronized(lock) {
            if (surface == null) {
                // TODO: Tell the graph processor that it should resubmit the repeating request or
                //  reconfigure the camera2 captureSession
                surfaceMap.remove(stream)
            } else {
                surfaceMap[stream] = surface
            }
        }

        maybeUpdateSurfaces()
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

    private fun buildSurfaceMap(): Map<StreamId, Surface> = synchronized(lock) {
        val surfaces = mutableMapOf<StreamId, Surface>()
        for (outputConfig in streamGraph.outputConfigs) {
            for (stream in outputConfig.streamBuilder) {
                val surface = surfaceMap[stream.id]
                if (surface == null) {
                    if (!outputConfig.deferrable) {
                        // If output is non-deferrable, a surface must be available or the config
                        // is not yet valid. Exit now with an empty map.
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