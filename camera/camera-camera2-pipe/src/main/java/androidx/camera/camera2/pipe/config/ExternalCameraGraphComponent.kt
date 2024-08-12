/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.camera.camera2.pipe.config

import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraStatusMonitor
import androidx.camera.camera2.pipe.RequestProcessor
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.compat.ExternalCameraController
import androidx.camera.camera2.pipe.graph.GraphListener
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

@CameraGraphScope
@Subcomponent(modules = [SharedCameraGraphModules::class, ExternalCameraGraphConfigModule::class])
internal interface ExternalCameraGraphComponent {
    fun cameraGraph(): CameraGraph

    @Subcomponent.Builder
    interface Builder {
        fun externalCameraGraphConfigModule(config: ExternalCameraGraphConfigModule): Builder

        fun build(): ExternalCameraGraphComponent
    }
}

@Module
internal class ExternalCameraGraphConfigModule(
    private val graphConfig: CameraGraph.Config,
    private val cameraMetadata: CameraMetadata,
    private val requestProcessor: RequestProcessor
) {
    private val externalCameraBackend =
        object : CameraBackend {
            override val id: CameraBackendId
                get() = CameraBackendId("External")

            override val cameraStatus: Flow<CameraStatusMonitor.CameraStatus>
                get() = MutableSharedFlow()

            override suspend fun getCameraIds(): List<CameraId>? {
                throwUnsupportedOperationException()
            }

            override fun awaitCameraIds(): List<CameraId>? {
                throwUnsupportedOperationException()
            }

            override fun awaitConcurrentCameraIds(): Set<Set<CameraId>>? {
                throwUnsupportedOperationException()
            }

            override fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata? {
                throwUnsupportedOperationException()
            }

            override fun disconnectAllAsync(): Deferred<Unit> {
                throwUnsupportedOperationException()
            }

            override fun shutdownAsync(): Deferred<Unit> {
                throwUnsupportedOperationException()
            }

            override fun createCameraController(
                cameraContext: CameraContext,
                graphId: CameraGraphId,
                graphConfig: CameraGraph.Config,
                graphListener: GraphListener,
                streamGraph: StreamGraph
            ): CameraController {
                throwUnsupportedOperationException()
            }

            override fun prewarm(cameraId: CameraId) {
                throwUnsupportedOperationException()
            }

            override fun disconnect(cameraId: CameraId) {
                throwUnsupportedOperationException()
            }

            override fun disconnectAsync(cameraId: CameraId): Deferred<Unit> {
                throwUnsupportedOperationException()
            }

            override fun disconnectAll() {
                throwUnsupportedOperationException()
            }

            private fun throwUnsupportedOperationException(): Nothing =
                throw UnsupportedOperationException("External CameraPipe should not use backends")
        }

    @Provides fun provideCameraGraphConfig(): CameraGraph.Config = graphConfig

    @Provides fun provideCameraMetadata(): CameraMetadata = cameraMetadata

    @CameraGraphScope
    @Provides
    fun provideGraphController(
        graphId: CameraGraphId,
        graphListener: GraphListener
    ): CameraController =
        ExternalCameraController(graphId, graphConfig, graphListener, requestProcessor)

    @CameraGraphScope @Provides fun provideCameraBackend(): CameraBackend = externalCameraBackend
}
