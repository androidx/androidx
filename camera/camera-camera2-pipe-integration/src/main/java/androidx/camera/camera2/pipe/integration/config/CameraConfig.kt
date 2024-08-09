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

package androidx.camera.camera2.pipe.integration.config

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.DoNotDisturbException
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter
import androidx.camera.camera2.pipe.integration.adapter.CameraInternalAdapter
import androidx.camera.camera2.pipe.integration.adapter.ZslControl
import androidx.camera.camera2.pipe.integration.adapter.ZslControlImpl
import androidx.camera.camera2.pipe.integration.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.pipe.integration.compat.Camera2CameraControlCompat
import androidx.camera.camera2.pipe.integration.compat.CameraCompatModule
import androidx.camera.camera2.pipe.integration.compat.EvCompCompat
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.CaptureSessionStuckQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.FinalizeSessionOnCloseQuirk
import androidx.camera.camera2.pipe.integration.impl.CameraPipeCameraProperties
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.FlashControl
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.impl.State3AControl
import androidx.camera.camera2.pipe.integration.impl.StillCaptureRequestControl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.Quirks
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Named
import javax.inject.Scope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

@Scope public annotation class CameraScope

/** Dependency bindings for adapting an individual [CameraInternal] instance to [CameraPipe] */
@OptIn(ExperimentalCamera2Interop::class)
@Module(
    includes =
        [
            Camera2CameraControlCompat.Bindings::class,
            EvCompCompat.Bindings::class,
            EvCompControl.Bindings::class,
            FlashControl.Bindings::class,
            FocusMeteringControl.Bindings::class,
            State3AControl.Bindings::class,
            StillCaptureRequestControl.Bindings::class,
            TorchControl.Bindings::class,
            ZoomCompat.Bindings::class,
            ZoomControl.Bindings::class,
        ],
    subcomponents = [UseCaseCameraComponent::class]
)
public abstract class CameraModule {
    public companion object {

        @CameraScope
        @Provides
        public fun provideUseCaseThreads(
            cameraConfig: CameraConfig,
            cameraThreadConfig: CameraThreadConfig
        ): UseCaseThreads {

            val executor = cameraThreadConfig.cameraExecutor
            val dispatcher = cameraThreadConfig.cameraExecutor.asCoroutineDispatcher()

            val cameraScope =
                CoroutineScope(
                    SupervisorJob() +
                        dispatcher +
                        CoroutineName("CXCP-UseCase-${cameraConfig.cameraId.value}")
                )

            return UseCaseThreads(cameraScope, executor, dispatcher)
        }

        @CameraScope
        @Provides
        public fun provideCamera2CameraControl(
            compat: Camera2CameraControlCompat,
            threads: UseCaseThreads,
            @VisibleForTesting requestListener: ComboRequestListener,
        ): Camera2CameraControl = Camera2CameraControl.create(compat, threads, requestListener)

        @CameraScope
        @Nullable
        @Provides
        public fun provideCameraMetadata(
            cameraPipe: CameraPipe,
            config: CameraConfig
        ): CameraMetadata? {
            try {
                return cameraPipe.cameras().awaitCameraMetadata(config.cameraId)
            } catch (exception: DoNotDisturbException) {
                Log.error { "Failed to inject camera metadata: Do Not Disturb mode is on." }
            }
            return null
        }

        @CameraScope
        @Provides
        @Named("CameraId")
        public fun provideCameraIdString(config: CameraConfig): String = config.cameraId.value

        @CameraScope
        @Nullable
        @Provides
        public fun provideStreamConfigurationMap(
            cameraMetadata: CameraMetadata?
        ): StreamConfigurationMap? {
            return cameraMetadata?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        }

        @CameraScope
        @Provides
        public fun provideCameraGraphFlags(cameraQuirks: CameraQuirks): CameraGraph.Flags {
            if (cameraQuirks.quirks.contains(CaptureSessionStuckQuirk::class.java)) {
                Log.debug { "CameraPipe should be enabling CaptureSessionStuckQuirk" }
            }
            // TODO(b/276354253): Set quirkWaitForRepeatingRequestOnDisconnect flag for overrides.

            // TODO(b/277310425): When creating a CameraGraph, this flag should be turned OFF when
            //  this behavior is not needed based on the use case interaction and the device on
            //  which the test is running.
            val quirkFinalizeSessionOnCloseBehavior = FinalizeSessionOnCloseQuirk.getBehavior()
            return CameraGraph.Flags(
                quirkFinalizeSessionOnCloseBehavior = quirkFinalizeSessionOnCloseBehavior,
            )
        }

        @CameraScope
        @Provides
        @Named("cameraQuirksValues")
        public fun provideCameraQuirksValues(cameraQuirks: CameraQuirks): Quirks =
            cameraQuirks.quirks

        @CameraScope
        @Provides
        public fun provideZslControl(cameraProperties: CameraProperties): ZslControl {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return ZslControlImpl(cameraProperties)
            } else {
                return ZslControlNoOpImpl()
            }
        }
    }

    @Binds
    public abstract fun bindCameraProperties(impl: CameraPipeCameraProperties): CameraProperties

    @Binds public abstract fun bindCameraInternal(adapter: CameraInternalAdapter): CameraInternal

    @Binds
    public abstract fun bindCameraInfoInternal(adapter: CameraInfoAdapter): CameraInfoInternal

    @Binds
    public abstract fun bindCameraControlInternal(
        adapter: CameraControlAdapter
    ): CameraControlInternal
}

/** Configuration properties used when creating a [CameraInternal] instance. */
@Module
public class CameraConfig(public val cameraId: CameraId) {
    @Provides public fun provideCameraConfig(): CameraConfig = this
}

/** Dagger subcomponent for a single [CameraInternal] instance. */
@CameraScope
@Subcomponent(
    modules =
        [
            CameraModule::class,
            CameraConfig::class,
            CameraCompatModule::class,
        ]
)
public interface CameraComponent {
    @Subcomponent.Builder
    public interface Builder {
        public fun config(config: CameraConfig): Builder

        public fun build(): CameraComponent
    }

    public fun getCameraInternal(): CameraInternal
}
