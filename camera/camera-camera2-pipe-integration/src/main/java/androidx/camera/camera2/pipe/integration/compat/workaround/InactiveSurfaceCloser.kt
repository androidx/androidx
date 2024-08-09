/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ConfigureSurfaceToSecondarySessionFailQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.PreviewOrientationIncorrectQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.TextureViewIsClosedQuirk
import androidx.camera.core.impl.DeferrableSurface
import dagger.Module
import dagger.Provides

/**
 * A workaround to close the DeferrableSurface if it has been configured to the camera.
 *
 * This workaround will be enabled when one of the [ConfigureSurfaceToSecondarySessionFailQuirk],
 * [PreviewOrientationIncorrectQuirk], [TextureViewIsClosedQuirk] is loaded.
 */
public interface InactiveSurfaceCloser {

    public fun configure(
        streamId: StreamId,
        deferrableSurface: DeferrableSurface,
        graph: CameraGraph
    )

    public fun onSurfaceInactive(deferrableSurface: DeferrableSurface)

    public fun closeAll()

    @Module
    public abstract class Bindings {
        public companion object {
            @Provides
            public fun provideInactiveSurfaceCloser(
                cameraQuirks: CameraQuirks
            ): InactiveSurfaceCloser {
                val enabled =
                    cameraQuirks.quirks.run {
                        contains(ConfigureSurfaceToSecondarySessionFailQuirk::class.java) ||
                            contains(PreviewOrientationIncorrectQuirk::class.java) ||
                            contains(TextureViewIsClosedQuirk::class.java)
                    }

                return if (enabled) InactiveSurfaceCloserImpl() else NoOpInactiveSurfaceCloser
            }
        }
    }
}

public class InactiveSurfaceCloserImpl : InactiveSurfaceCloser {
    private val lock = Any()
    private val configuredOutputs = mutableListOf<ConfiguredOutput>()

    override fun configure(
        streamId: StreamId,
        deferrableSurface: DeferrableSurface,
        graph: CameraGraph
    ) {
        synchronized(lock) {
            configuredOutputs.add(ConfiguredOutput(streamId, deferrableSurface, graph))
        }
    }

    override fun onSurfaceInactive(deferrableSurface: DeferrableSurface) {
        synchronized(lock) { configuredOutputs.closeIfConfigured(deferrableSurface) }
    }

    override fun closeAll() {
        synchronized(lock) {
            configuredOutputs.forEach { it.close() }
            configuredOutputs.clear()
        }
    }

    public data class ConfiguredOutput(
        val streamId: StreamId,
        val deferrableSurface: DeferrableSurface,
        val graph: CameraGraph
    ) {
        public fun close() {
            graph.setSurface(streamId, null)
            deferrableSurface.close()
        }

        public fun contains(deferrableSurface: DeferrableSurface): Boolean {
            return this.deferrableSurface == deferrableSurface
        }
    }

    private fun List<ConfiguredOutput>.closeIfConfigured(deferrableSurface: DeferrableSurface) =
        forEach {
            if (it.contains(deferrableSurface)) {
                deferrableSurface.close()
            }
        }
}

public object NoOpInactiveSurfaceCloser : InactiveSurfaceCloser {
    override fun configure(
        streamId: StreamId,
        deferrableSurface: DeferrableSurface,
        graph: CameraGraph
    ) {
        // Nothing to do.
    }

    override fun onSurfaceInactive(deferrableSurface: DeferrableSurface) {
        // Nothing to do.
    }

    override fun closeAll() {
        // Nothing to do.
    }
}
