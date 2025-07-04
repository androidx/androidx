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

package androidx.camera.core

import androidx.annotation.RestrictTo
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfigs
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraRepository
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.StreamSpecsCalculator

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraUseCaseAdapterProvider {
    /**
     * Provides a [CameraUseCaseAdapter] for a single camera.
     *
     * @param cameraId The ID of the camera to provide the adapter for.
     * @return A [CameraUseCaseAdapter] instance.
     * @throws IllegalArgumentException If the specified camera id is unavailable.
     */
    @Throws(IllegalArgumentException::class)
    public fun provide(cameraId: String): CameraUseCaseAdapter

    /**
     * Provides a [CameraUseCaseAdapter] for one or two cameras, allowing for advanced multi-camera
     * use cases.
     *
     * @param camera The primary [CameraInternal] instance.
     * @param secondaryCamera The secondary [CameraInternal] instance, or `null` if not used.
     * @param adapterCameraInfo The [AdapterCameraInfo] for the primary camera.
     * @param secondaryAdapterCameraInfo The [AdapterCameraInfo] for the secondary camera, or
     *   `null`.
     * @param compositionSettings The [CompositionSettings] for the primary camera.
     * @param secondaryCompositionSettings The [CompositionSettings] for the secondary camera.
     * @return A [CameraUseCaseAdapter] instance.
     */
    public fun provide(
        camera: CameraInternal,
        secondaryCamera: CameraInternal?,
        adapterCameraInfo: AdapterCameraInfo,
        secondaryAdapterCameraInfo: AdapterCameraInfo?,
        compositionSettings: CompositionSettings,
        secondaryCompositionSettings: CompositionSettings,
    ): CameraUseCaseAdapter
}

/**
 * Provides instances of [CameraUseCaseAdapter].
 *
 * This class is responsible for creating [CameraUseCaseAdapter] objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CameraUseCaseAdapterProviderImpl(
    private val cameraRepository: CameraRepository,
    private val cameraCoordinator: CameraCoordinator,
    private val useCaseConfigFactory: UseCaseConfigFactory,
    private val streamSpecsCalculator: StreamSpecsCalculator,
) : CameraUseCaseAdapterProvider {
    @Throws(IllegalArgumentException::class)
    public override fun provide(cameraId: String): CameraUseCaseAdapter {
        val camera = cameraRepository.getCamera(cameraId)
        return provideInternal(
            camera = camera,
            // TODO: Support extensions camera config.
            adapterCameraInfo =
                AdapterCameraInfo(camera.cameraInfoInternal, CameraConfigs.defaultConfig()),
        )
    }

    public override fun provide(
        camera: CameraInternal,
        secondaryCamera: CameraInternal?,
        adapterCameraInfo: AdapterCameraInfo,
        secondaryAdapterCameraInfo: AdapterCameraInfo?,
        compositionSettings: CompositionSettings,
        secondaryCompositionSettings: CompositionSettings,
    ): CameraUseCaseAdapter {
        return provideInternal(
            camera = camera,
            secondaryCamera = secondaryCamera,
            adapterCameraInfo = adapterCameraInfo,
            secondaryAdapterCameraInfo = secondaryAdapterCameraInfo,
            compositionSettings = compositionSettings,
            secondaryCompositionSettings = secondaryCompositionSettings,
        )
    }

    private fun provideInternal(
        camera: CameraInternal,
        secondaryCamera: CameraInternal? = null,
        adapterCameraInfo: AdapterCameraInfo,
        secondaryAdapterCameraInfo: AdapterCameraInfo? = null,
        compositionSettings: CompositionSettings = CompositionSettings.DEFAULT,
        secondaryCompositionSettings: CompositionSettings = CompositionSettings.DEFAULT,
    ): CameraUseCaseAdapter {
        return CameraUseCaseAdapter(
            camera,
            secondaryCamera,
            adapterCameraInfo,
            secondaryAdapterCameraInfo,
            compositionSettings,
            secondaryCompositionSettings,
            cameraCoordinator,
            streamSpecsCalculator,
            useCaseConfigFactory,
        )
    }
}
