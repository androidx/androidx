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

@file:JvmName("ExtensionSessionConfigKt")

package androidx.camera.extensions

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraProvider
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
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
 * @param useCases The [UseCase] instances to be attached to the camera and receive camera data.
 * @param viewPort The [ViewPort] to be applied on the camera session. If not set, the default is no
 *   viewport.
 * @param effects The list of [CameraEffect] to be applied on the camera session. If not set, the
 *   default is no effects.
 * @throws IllegalArgumentException if the given mode is not a valid extension mode.
 * @see androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle
 * @see ExtensionsManager.getInstanceAsync
 */
public class ExtensionSessionConfig
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    @param:ExtensionMode.Mode @get:ExtensionMode.Mode public val mode: Int,
    public val extensionsManager: ExtensionsManager,
    useCases: List<UseCase> = emptyList(),
    viewPort: ViewPort? = null,
    effects: List<CameraEffect> = emptyList(),
    /**
     * Whether to use auto rotation. When enabled, CameraX will monitor the device motion sensor and
     * set the target rotation for [ImageCapture] and [androidx.camera.video.VideoCapture].
     *
     * @see ExtensionSessionConfig.Builder.setAutoRotationEnabled
     */
    isAutoRotationEnabled: Boolean = false,
) :
    SessionConfig(
        useCases,
        viewPort,
        effects,
        isAutoRotationEnabled = isAutoRotationEnabled,
        requireNonEmptyUseCases = false,
        cameraFilter = extensionsManager.getExtensionCameraFilterAndInjectCameraConfig(mode),
    ) {
    /**
     * Creates an [ExtensionSessionConfig] from the given parameters.
     *
     * @param mode The extension mode. See [ExtensionMode] for the list of available modes.
     * @param extensionsManager The [ExtensionsManager] instance.
     * @param useCases The [UseCase] instances to be attached to the camera and receive camera data.
     * @param viewPort The [ViewPort] to be applied on the camera session. If not set, the default
     *   is no viewport.
     * @param effects The list of [CameraEffect] to be applied on the camera session. If not set,
     *   the default is no effects.
     * @throws IllegalArgumentException if the given mode is not a valid extension mode.
     */
    @JvmOverloads
    public constructor(
        @ExtensionMode.Mode mode: Int,
        extensionsManager: ExtensionsManager,
        useCases: List<UseCase> = emptyList(),
        viewPort: ViewPort? = null,
        effects: List<CameraEffect> = emptyList(),
    ) : this(mode, extensionsManager, useCases, viewPort, effects, isAutoRotationEnabled = false)

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

        private var _viewPort: ViewPort? = null

        /** The [ViewPort] for the session. */
        // This property uses `@JvmSynthetic` for both the getter and setter to support idiomatic
        // Kotlin assignment in the DSL while preventing visibility to Java callers. This satisfies
        // the AndroidX `GetterOnBuilder` lint rule and avoids polluting the Java Builder API.
        @get:Nullable
        @get:JvmSynthetic
        @set:JvmSynthetic
        public var viewPort: ViewPort?
            get() = _viewPort
            set(@Nullable value) {
                _viewPort = value
            }

        private val effects: MutableList<CameraEffect> = mutableListOf()

        private var _isAutoRotationEnabled: Boolean = false

        /** Whether to use auto rotation. */
        // This property uses `@JvmSynthetic` for both the getter and setter to support idiomatic
        // Kotlin assignment in the DSL while preventing visibility to Java callers. This satisfies
        // the AndroidX `GetterOnBuilder` lint rule and avoids polluting the Java Builder API.
        @get:NonNull
        @get:JvmSynthetic
        @set:JvmSynthetic
        public var isAutoRotationEnabled: Boolean
            get() = _isAutoRotationEnabled
            set(@NonNull value) {
                _isAutoRotationEnabled = value
            }

        /** Adds a [UseCase] to the session. */
        public fun addUseCase(useCase: UseCase): Builder {
            useCases.add(useCase)
            return this
        }

        /** Sets the [ViewPort] for the session. */
        public fun setViewPort(viewPort: ViewPort): Builder {
            this._viewPort = viewPort
            return this
        }

        /** Adds a [CameraEffect] for the session. */
        public fun addEffect(effect: CameraEffect): Builder {
            effects.add(effect)
            return this
        }

        /**
         * Sets whether to use auto rotation.
         *
         * When enabled, CameraX will monitor the device motion sensor and set the target rotation
         * for ImageCapture and VideoCapture.
         */
        public fun setAutoRotationEnabled(autoRotationEnabled: Boolean): Builder {
            this._isAutoRotationEnabled = autoRotationEnabled
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
                viewPort = _viewPort,
                effects = effects.toList(),
                isAutoRotationEnabled = _isAutoRotationEnabled,
            )
        }
    }
}

/**
 * Creates an [ExtensionSessionConfig] using a Kotlin DSL.
 *
 * Example usage:
 * ```
 * val extensionSessionConfig = extensionSessionConfig(mode, extensionsManager) {
 *     isAutoRotationEnabled = true
 *     viewPort = viewPort
 * }
 * ```
 *
 * @param mode The extension mode. See [ExtensionMode] for the list of available modes.
 * @param extensionsManager The [ExtensionsManager] instance.
 * @param block A lambda to configure the [ExtensionSessionConfig.Builder].
 */
@JvmSynthetic
public inline fun extensionSessionConfig(
    @ExtensionMode.Mode mode: Int,
    extensionsManager: ExtensionsManager,
    crossinline block: ExtensionSessionConfig.Builder.() -> Unit,
): ExtensionSessionConfig =
    ExtensionSessionConfig.Builder(mode, extensionsManager).apply(block).build()
