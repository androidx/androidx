/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.extensions

import androidx.annotation.RestrictTo
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraProvider
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.ViewPort

/**
 * A [SessionConfig] for extension sessions.
 *
 * This class encapsulates the necessary configurations for a extension session. Once configured,
 * this config can be bound to a camera and lifecycle using
 * `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` or
 * `androidx.camera.lifecycle.LifecycleCameraProvider.bindToLifecycle`.
 *
 * It consists of a collection of [UseCase], session parameters to be applied on the camera session,
 * and common properties like the field-of-view defined by [ViewPort]. Note that [ImageAnalysis] is
 * not supported in extension sessions.
 *
 * **Constraints:**
 * - When used for binding to a camera via
 *   `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` or
 *   `androidx.camera.lifecycle.LifecycleCameraProvider.bindToLifecycle`, the list of [UseCase]
 *   provided to the constructor or added via [Builder.addUseCase] cannot be empty.
 *
 * Apps can use [CameraProvider.getCameraInfo] with an [ExtensionSessionConfig] to obtain the
 * [CameraInfo] of the camera which can support the given [ExtensionSessionConfig].
 *
 * **Usage Example:**
 *
 * ```
 * // In a coroutine scope
 * try {
 *     val extensionsManager = ExtensionsManager.getInstanceAsync(context, cameraProvider).await()
 *
 *     val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
 *     if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT)) {
 *         // This is the correct time to create an ExtensionSessionConfig
 *         val imageCapture = ImageCapture.Builder().build()
 *         val preview = Preview.Builder().build()
 *
 *         val config = ExtensionSessionConfig.Builder(
 *             ExtensionMode.NIGHT,
 *             extensionsManager
 *         )
 *         .addUseCase(preview)
 *         .addUseCase(imageCapture)
 *         .build()
 *
 *         // Now it's safe to bind the configuration
 *         cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, config)
 *     } else {
 *         // Handle the case where the extension is not available.
 *     }
 * } catch (e: Exception) {
 *     // Handle failure
 * }
 * ```
 *
 * @param mode The extension mode. See [ExtensionMode] for the list of available modes.
 * @param extensionsManager The [ExtensionsManager] instance.
 * @param useCases The list of [UseCase] to be attached to the camera and receive camera data.
 * @param viewPort The [ViewPort] to be applied on the camera session. If not set, the default is no
 *   viewport.
 * @param effects The list of [CameraEffect] to be applied on the camera session. If not set, the
 *   default is no effects.
 * @throws IllegalArgumentException if the given mode is not a valid extension mode.
 * @see androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle
 * @see ExtensionsManager.getInstanceAsync
 */
public class ExtensionSessionConfig
@JvmOverloads
constructor(
    @param:ExtensionMode.Mode @get:ExtensionMode.Mode public val mode: Int,
    public val extensionsManager: ExtensionsManager,
    useCases: List<UseCase> = emptyList(),
    viewPort: ViewPort? = null,
    effects: List<CameraEffect> = emptyList(),
) : SessionConfig(useCases, viewPort, effects) {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val requireNonEmptyUseCases: Boolean
        get() = false

    private val _cameraFilter: CameraFilter? =
        extensionsManager.getExtensionCameraFilterAndInjectCameraConfig(mode)

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val cameraFilter: CameraFilter?
        get() = _cameraFilter

    /**
     * Creates an [ExtensionSessionConfig] with a variable number of [UseCase] instances.
     *
     * @param mode The extension mode. See [ExtensionMode] for the list of available modes.
     * @param extensionsManager The [ExtensionsManager] instance.
     * @param useCases The [UseCase] instances to be attached to the camera and receive camera data.
     * @throws IllegalArgumentException if the given mode is not a valid extension mode.
     */
    public constructor(
        @ExtensionMode.Mode mode: Int,
        extensionsManager: ExtensionsManager,
        vararg useCases: UseCase,
    ) : this(mode, extensionsManager, useCases.toList())

    /**
     * Builder for [ExtensionSessionConfig].
     *
     * @param mode The extension mode. See [ExtensionMode] for the list of available modes.
     * @param extensionsManager The [ExtensionsManager] instance.
     * @see ExtensionsManager.getInstanceAsync
     */
    public class Builder(
        @param:ExtensionMode.Mode private val mode: Int,
        private val extensionsManager: ExtensionsManager,
    ) {
        private val useCases: MutableList<UseCase> = mutableListOf()
        private var viewPort: ViewPort? = null
        private var effects: MutableList<CameraEffect> = mutableListOf()

        /** Adds a [UseCase] to the session. */
        public fun addUseCase(useCase: UseCase): Builder {
            useCases.add(useCase)
            return this
        }

        /** Sets the [ViewPort] for the session. */
        public fun setViewPort(viewPort: ViewPort): Builder {
            this.viewPort = viewPort
            return this
        }

        /** Adds a [CameraEffect] for the session. */
        public fun addEffect(effect: CameraEffect): Builder {
            effects.add(effect)
            return this
        }

        /**
         * Builds an [ExtensionSessionConfig] from the current configuration.
         *
         * @throws IllegalArgumentException if the given mode is not a valid extension mode.
         */
        public fun build(): ExtensionSessionConfig {
            return ExtensionSessionConfig(
                mode = mode,
                extensionsManager = extensionsManager,
                useCases = useCases.toList(),
                viewPort = viewPort,
                effects = effects.toList(),
            )
        }
    }
}
