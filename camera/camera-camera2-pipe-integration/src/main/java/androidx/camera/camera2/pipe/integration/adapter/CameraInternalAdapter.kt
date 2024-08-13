/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.UseCaseManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.SessionProcessor
import com.google.common.util.concurrent.ListenableFuture
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal val cameraAdapterIds = atomic(0)

/** Adapt the [CameraInternal] class to one or more [CameraPipe] based Camera instances. */
@CameraScope
public class CameraInternalAdapter
@Inject
constructor(
    config: CameraConfig,
    private val useCaseManager: UseCaseManager,
    private val cameraInfo: CameraInfoInternal,
    private val cameraController: CameraControlInternal,
    private val threads: UseCaseThreads,
    private val cameraStateAdapter: CameraStateAdapter
) : CameraInternal {
    private val cameraId = config.cameraId
    private var coreCameraConfig: androidx.camera.core.impl.CameraConfig =
        CameraConfigs.defaultConfig()
    private val debugId = cameraAdapterIds.incrementAndGet()
    private var sessionProcessor: SessionProcessor? = null

    init {
        debug { "Created $this for $cameraId" }
        // TODO: Consider preloading the list of camera ids and metadata.
    }

    internal fun setCameraGraphCreationMode(createImmediately: Boolean) {
        useCaseManager.setCameraGraphCreationMode(createImmediately)
    }

    internal fun getDeferredCameraGraphConfig(): CameraGraph.Config? =
        useCaseManager.getDeferredCameraGraphConfig()

    internal fun resumeDeferredCameraGraphCreation(cameraGraph: CameraGraph) {
        useCaseManager.resumeDeferredComponentCreation(cameraGraph)
    }

    // Load / unload methods
    override fun open() {
        debug { "$this#open" }
    }

    override fun close() {
        debug { "$this#close" }
    }

    override fun setPrimary(isPrimary: Boolean) {
        useCaseManager.setPrimary(isPrimary)
    }

    override fun setActiveResumingMode(enabled: Boolean) {
        useCaseManager.setActiveResumeMode(enabled)
    }

    override fun release(): ListenableFuture<Void> {
        return threads.scope
            .launch { useCaseManager.close() }
            .asListenableFuture()
            .apply { addListener({ threads.scope.cancel() }, Dispatchers.Default.asExecutor()) }
    }

    override fun getCameraInfoInternal(): CameraInfoInternal = cameraInfo

    override fun getCameraState(): Observable<CameraInternal.State> =
        cameraStateAdapter.cameraInternalState

    override fun getCameraControlInternal(): CameraControlInternal = cameraController

    // UseCase attach / detach behaviors.
    override fun attachUseCases(useCasesToAdd: MutableCollection<UseCase>) {
        useCaseManager.attach(useCasesToAdd.toList())
    }

    override fun detachUseCases(useCasesToRemove: MutableCollection<UseCase>) {
        useCaseManager.detach(useCasesToRemove.toList())
    }

    // UseCase state callbacks
    override fun onUseCaseActive(useCase: UseCase) {
        useCaseManager.activate(useCase)
    }

    override fun onUseCaseUpdated(useCase: UseCase) {
        useCaseManager.update(useCase)
    }

    override fun onUseCaseReset(useCase: UseCase) {
        useCaseManager.reset(useCase)
    }

    override fun onUseCaseInactive(useCase: UseCase) {
        useCaseManager.deactivate(useCase)
    }

    override fun getExtendedConfig(): androidx.camera.core.impl.CameraConfig {
        return coreCameraConfig
    }

    override fun setExtendedConfig(cameraConfig: androidx.camera.core.impl.CameraConfig?) {
        coreCameraConfig = cameraConfig ?: CameraConfigs.defaultConfig()
        sessionProcessor = cameraConfig?.getSessionProcessor(null)
        useCaseManager.sessionProcessor = sessionProcessor
    }

    override fun toString(): String = "CameraInternalAdapter<$cameraId($debugId)>"
}
