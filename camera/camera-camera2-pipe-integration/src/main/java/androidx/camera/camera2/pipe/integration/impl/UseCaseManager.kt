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

package androidx.camera.camera2.pipe.integration.impl

import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraComponent
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import javax.inject.Inject

/**
 * This class keeps track of the currently attached and active [UseCase]'s for a specific camera.
 */
@CameraScope
class UseCaseManager @Inject constructor(
    private val cameraConfig: CameraConfig,
    private val builder: UseCaseCameraComponent.Builder,
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // Java version required for Dagger
    private val controls: java.util.Set<UseCaseCameraControl>,
    cameraProperties: CameraProperties,
) {
    private val attachedUseCases = mutableListOf<UseCase>()
    private val enabledUseCases = mutableSetOf<UseCase>()
    private val meteringRepeating by lazy { MeteringRepeating.Builder(cameraProperties).build() }

    @Volatile
    private var _activeComponent: UseCaseCameraComponent? = null
    val camera: UseCaseCamera?
        get() = _activeComponent?.getUseCaseCamera()

    fun attach(useCases: List<UseCase>) {
        if (useCases.isEmpty()) {
            Log.warn { "Attach [] from $this (Ignored)" }
            return
        }
        Log.debug { "Attaching $useCases from $this" }

        var modified = false
        for (useCase in useCases) {
            if (!attachedUseCases.contains(useCase)) {
                attachedUseCases.add(useCase)
                modified = true
            }
        }

        if (modified) {
            start(attachedUseCases)
        }
    }

    fun detach(useCases: List<UseCase>) {
        if (useCases.isEmpty()) {
            Log.warn { "Detaching [] from $this (Ignored)" }
            return
        }
        Log.debug { "Detaching $useCases from $this" }

        var modified = false
        for (useCase in useCases) {
            enabledUseCases.remove(useCase)
            modified = attachedUseCases.remove(useCase) || modified
        }

        // TODO: We might only want to tear down when the number of attached use cases goes to
        //  zero. If a single UseCase is removed, we could deactivate it?
        if (modified) {
            start(attachedUseCases)
        }
    }

    fun enable(useCase: UseCase) {
        if (enabledUseCases.add(useCase)) {
            invalidate()
        }
    }

    fun disable(useCase: UseCase) {
        if (enabledUseCases.remove(useCase)) {
            invalidate()
        }
    }

    fun update(useCase: UseCase) {
        if (attachedUseCases.contains(useCase)) {
            invalidate()
        }
    }

    fun reset(useCase: UseCase) {
        if (attachedUseCases.contains(useCase)) {
            start(attachedUseCases)
        }
    }

    override fun toString(): String = "UseCaseManager<${cameraConfig.cameraId}>"

    private fun invalidate() {
        when {
            shouldAddRepeatingUseCase() -> addRepeatingUseCase()
            shouldRemoveRepeatingUseCase() -> removeRepeatingUseCase()
            else -> camera?.activeUseCases = enabledUseCases.toSet()
        }
    }

    private fun start(newUseCases: List<UseCase>) {
        val useCases = newUseCases.toList()

        // Close prior camera graph
        camera.let {
            _activeComponent = null
            it?.close()
        }

        // Update list of active useCases
        if (useCases.isEmpty()) {
            for (control in controls) {
                control.useCaseCamera = null
            }
            return
        }

        // Create and configure the new camera component.
        _activeComponent = builder.config(UseCaseCameraConfig(useCases)).build()
        for (control in controls) {
            control.useCaseCamera = camera
        }

        invalidate()
    }

    private fun shouldAddRepeatingUseCase(): Boolean {
        return enabledUseCases.only { it is ImageCapture }
    }

    private fun addRepeatingUseCase() {
        meteringRepeating.setupSession()
        attach(listOf(meteringRepeating))
        enable(meteringRepeating)
    }

    private fun shouldRemoveRepeatingUseCase(): Boolean {
        val onlyMeteringRepeatingEnabled = enabledUseCases.only { it is MeteringRepeating }
        val meteringRepeatingAndNonImageCaptureEnabled =
            enabledUseCases.any { it is MeteringRepeating } &&
                enabledUseCases.any { it !is MeteringRepeating && it !is ImageCapture }
        return onlyMeteringRepeatingEnabled || meteringRepeatingAndNonImageCaptureEnabled
    }

    private fun removeRepeatingUseCase() {
        disable(meteringRepeating)
        detach(listOf(meteringRepeating))
        meteringRepeating.onDetached()
    }

    /**
     * Returns true when the collection only has elements (1 or more) that verify the predicate,
     * false otherwise.
     */
    private fun <T> Collection<T>.only(predicate: (T) -> Boolean): Boolean {
        return isNotEmpty() && all(predicate)
    }
}