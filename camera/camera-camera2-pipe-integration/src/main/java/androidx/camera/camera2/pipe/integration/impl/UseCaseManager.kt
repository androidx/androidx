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

import androidx.annotation.RequiresApi
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
 * A [UseCase] during its lifetime, can be:
 *
 *   - Attached: This happens when a use case is bound to a CameraX Lifecycle, and signals that the
 *       camera should be opened, and a camera capture session should be created to include the
 *       stream corresponding to the use case. In the integration layer here, we'll recreate a
 *       CameraGraph when a use case is attached.
 *   - Detached: This happens when a use case is unbound from a CameraX Lifecycle, and signals that
 *       we no longer need this specific use case and therefore its corresponding stream in our
 *       current capture session. In the integration layer, we'll also recreate a CameraGraph when
 *       a use case is detached, though it might not be strictly necessary.
 *   - Active: This happens when the use case is considered "ready", meaning that the use case is
 *       ready to have frames delivered to it. In the case of the integration layer, this means we
 *       can start submitting the capture requests corresponding to the use case. An important note
 *       here is that a use case can actually become "active" before it is "attached", and thus we
 *       should only take action when a use case is both "attached" and "active".
 *   - Inactive: This happens when use case no longer needs frames delivered to it. This is can be
 *       seen as an optimization signal, as we technically are allowed to continue submitting
 *       capture requests, but we no longer need to. An example of this is when you clear the
 *       analyzer during ImageAnalysis.
 *
 *  In this class, we also define a new term - "Running". A use case is considered running when it's
 *  both "attached" and "active". This means we should have a camera opened, a capture session with
 *  the streams created and have capture requests submitting.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class UseCaseManager @Inject constructor(
    private val cameraConfig: CameraConfig,
    private val builder: UseCaseCameraComponent.Builder,
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // Java version required for Dagger
    private val controls: java.util.Set<UseCaseCameraControl>,
    cameraProperties: CameraProperties,
) {
    private val attachedUseCases = mutableSetOf<UseCase>()
    private val activeUseCases = mutableSetOf<UseCase>()

    private val meteringRepeating by lazy { MeteringRepeating.Builder(cameraProperties).build() }

    @Volatile
    private var _activeComponent: UseCaseCameraComponent? = null
    val camera: UseCaseCamera?
        get() = _activeComponent?.getUseCaseCamera()

    /**
     * This attaches the specified [useCases] to the current set of attached use cases. When any
     * changes are identified (i.e., a new use case is added), the subsequent actions would trigger
     * a recreation of the current CameraGraph if there is one.
     */
    fun attach(useCases: List<UseCase>) {
        if (useCases.isEmpty()) {
            Log.warn { "Attach [] from $this (Ignored)" }
            return
        }
        Log.debug { "Attaching $useCases from $this" }

        // Notify state attached to use cases
        for (useCase in useCases) {
            if (!attachedUseCases.contains(useCase)) {
                useCase.onStateAttached()
            }
        }

        if (attachedUseCases.addAll(useCases)) {
            refreshAttachedUseCases(attachedUseCases)
        }
    }

    /**
     * This detaches the specified [useCases] from the current set of attached use cases. When any
     * changes are identified (i.e., an existing use case is removed), the subsequent actions would
     * trigger a recreation of the current CameraGraph.
     */
    fun detach(useCases: List<UseCase>) {
        if (useCases.isEmpty()) {
            Log.warn { "Detaching [] from $this (Ignored)" }
            return
        }
        Log.debug { "Detaching $useCases from $this" }

        // When use cases are detached, they should be considered inactive as well. Also note that
        // we remove the use cases from our set directly because the subsequent cleanup actions from
        // detaching the use cases should suffice here.
        activeUseCases.removeAll(useCases)

        // Notify state detached to use cases
        for (useCase in useCases) {
            if (attachedUseCases.contains(useCase)) {
                useCase.onStateDetached()
            }
        }

        // TODO: We might only want to tear down when the number of attached use cases goes to
        //  zero. If a single UseCase is removed, we could deactivate it?
        if (attachedUseCases.removeAll(useCases)) {
            refreshAttachedUseCases(attachedUseCases)
        }
    }

    /**
     * This marks the specified [useCase] as active ("activate"). This refreshes the current set of
     * active use cases, and if any changes are identified, we update [UseCaseCamera] with the
     * latest set of "running" (attached and active) use cases, which will in turn trigger actions
     * for SessionConfig updates.
     */
    fun activate(useCase: UseCase) {
        if (activeUseCases.add(useCase)) {
            refreshRunningUseCases()
        }
    }

    /**
     * This marks the specified [useCase] as inactive ("deactivate"). This refreshes the current set
     * of active use cases, and if any changes are identified, we update [UseCaseCamera] with the
     * latest set of "running" (attached and active) use cases, which will in turn trigger actions
     * for SessionConfig updates.
     */
    fun deactivate(useCase: UseCase) {
        if (activeUseCases.remove(useCase)) {
            refreshRunningUseCases()
        }
    }

    fun update(useCase: UseCase) {
        if (attachedUseCases.contains(useCase)) {
            refreshRunningUseCases()
        }
    }

    fun reset(useCase: UseCase) {
        if (attachedUseCases.contains(useCase)) {
            refreshAttachedUseCases(attachedUseCases)
        }
    }

    override fun toString(): String = "UseCaseManager<${cameraConfig.cameraId}>"

    private fun refreshRunningUseCases() {
        val runningUseCases = getRunningUseCases()
        when {
            shouldAddRepeatingUseCase(runningUseCases) -> addRepeatingUseCase()
            shouldRemoveRepeatingUseCase(runningUseCases) -> removeRepeatingUseCase()
            else -> camera?.runningUseCases = runningUseCases
        }
    }

    private fun refreshAttachedUseCases(newUseCases: Set<UseCase>) {
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
                control.reset()
            }
            return
        }

        // Create and configure the new camera component.
        _activeComponent = builder.config(UseCaseCameraConfig(useCases)).build()
        for (control in controls) {
            control.useCaseCamera = camera
        }

        refreshRunningUseCases()
    }

    private fun getRunningUseCases(): Set<UseCase> {
        return attachedUseCases.intersect(activeUseCases)
    }

    private fun shouldAddRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        return runningUseCases.only { it is ImageCapture }
    }

    private fun addRepeatingUseCase() {
        meteringRepeating.setupSession()
        attach(listOf(meteringRepeating))
        activate(meteringRepeating)
    }

    private fun shouldRemoveRepeatingUseCase(runningUseCases: Set<UseCase>): Boolean {
        val onlyMeteringRepeatingEnabled = runningUseCases.only { it is MeteringRepeating }
        val meteringRepeatingAndNonImageCaptureEnabled =
            runningUseCases.any { it is MeteringRepeating } &&
                runningUseCases.any { it !is MeteringRepeating && it !is ImageCapture }
        return onlyMeteringRepeatingEnabled || meteringRepeatingAndNonImageCaptureEnabled
    }

    private fun removeRepeatingUseCase() {
        deactivate(meteringRepeating)
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