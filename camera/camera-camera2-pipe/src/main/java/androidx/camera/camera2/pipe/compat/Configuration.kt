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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.graphics.SurfaceTexture
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.OutputStream.DynamicRangeProfile
import androidx.camera.camera2.pipe.OutputStream.MirrorMode
import androidx.camera.camera2.pipe.OutputStream.OutputType
import androidx.camera.camera2.pipe.OutputStream.StreamUseCase
import androidx.camera.camera2.pipe.OutputStream.TimestampBase
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper.Companion.SURFACE_GROUP_ID_NONE
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.checkNOrHigher
import androidx.camera.camera2.pipe.core.checkOOrHigher
import androidx.camera.camera2.pipe.core.checkPOrHigher
import java.util.concurrent.Executor
import kotlin.reflect.KClass

/**
 * A data class that mirrors the fields in [android.hardware.camera2.params.SessionConfiguration] so
 * that a real instance can be constructed when creating a
 * [android.hardware.camera2.CameraCaptureSession] on newer versions of the OS.
 */
internal data class SessionConfigData(
    val sessionType: Int,
    val inputConfiguration: InputConfigData?,
    val outputConfigurations: List<OutputConfigurationWrapper>,
    val executor: Executor,
    val stateCallback: CameraCaptureSessionWrapper.StateCallback,
    val sessionTemplateId: Int,
    val sessionParameters: Map<*, Any?>,
    val extensionMode: Int? = null,
    val extensionStateCallback: CameraExtensionSessionWrapper.StateCallback? = null
) {
    companion object {
        /* NOTE: These must keep in sync with their SessionConfiguration values. */
        const val SESSION_TYPE_REGULAR = 0
        const val SESSION_TYPE_HIGH_SPEED = 1
        const val SESSION_TYPE_EXTENSION = 2
    }
}

/**
 * A data class that mirrors the fields in [android.hardware.camera2.params.InputConfiguration] so
 * that a real instance can be constructed when creating a
 * [android.hardware.camera2.CameraCaptureSession] on newer versions of the OS.
 */
internal data class InputConfigData(val width: Int, val height: Int, val format: Int)

/**
 * An interface for [OutputConfiguration] with minor modifications.
 *
 * The primary modifications to this class are to make it harder to accidentally changes things that
 * cannot be modified after the [android.hardware.camera2.CameraCaptureSession] has been created.
 *
 * [OutputConfiguration]'s are NOT immutable, and changing state of an [OutputConfiguration] may
 * require the CameraCaptureSession to be finalized or updated.
 */
internal interface OutputConfigurationWrapper : UnsafeWrapper {
    /**
     * This method will return null if the output configuration was created without a Surface, and
     * until addSurface is called for the first time.
     *
     * @see OutputConfiguration.getSurface
     */
    val surface: Surface?

    /**
     * This method returns the current list of surfaces for this [OutputConfiguration]. Since the
     * [OutputConfiguration] is stateful, this value may change as a result of calling addSurface or
     * removeSurface.
     *
     * @see OutputConfiguration.getSurfaces
     */
    val surfaces: List<Surface>

    /** @see OutputConfiguration.addSurface */
    fun addSurface(surface: Surface)

    /** @see OutputConfiguration.removeSurface */
    fun removeSurface(surface: Surface)

    /** @see OutputConfiguration.setPhysicalCameraId */
    val physicalCameraId: CameraId?

    /** @see OutputConfiguration.enableSurfaceSharing */
    val surfaceSharing: Boolean

    /** @see OutputConfiguration.getMaxSharedSurfaceCount */
    val maxSharedSurfaceCount: Int

    /** @see OutputConfiguration.getSurfaceGroupId */
    val surfaceGroupId: Int

    companion object {
        const val SURFACE_GROUP_ID_NONE = -1
    }
}

@RequiresApi(24)
internal class AndroidOutputConfiguration(
    private val output: OutputConfiguration,
    override val surfaceSharing: Boolean,
    override val maxSharedSurfaceCount: Int,
    override val physicalCameraId: CameraId?
) : OutputConfigurationWrapper {

    @RequiresApi(24)
    companion object {
        /**
         * Create and validate an OutputConfiguration for Camera2. null is returned when a
         * non-exceptional error is encountered when creating the OutputConfiguration.
         */
        fun create(
            surface: Surface?,
            outputType: OutputType = OutputType.SURFACE,
            mirrorMode: MirrorMode? = null,
            timestampBase: TimestampBase? = null,
            dynamicRangeProfile: DynamicRangeProfile? = null,
            streamUseCase: StreamUseCase? = null,
            size: Size? = null,
            surfaceSharing: Boolean = false,
            surfaceGroupId: Int = SURFACE_GROUP_ID_NONE,
            physicalCameraId: CameraId? = null,
            cameraId: CameraId? = null,
            camera2MetadataProvider: Camera2MetadataProvider? = null,
        ): OutputConfigurationWrapper? {
            check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

            // Create the OutputConfiguration using the groupId via the constructor (if set)
            val configuration: OutputConfiguration
            if (outputType == OutputType.SURFACE) {
                check(surface != null) {
                    "OutputConfigurations defined with ${OutputType.SURFACE} must provide a"
                    "non-null surface!"
                }
                // OutputConfiguration will, on some OS versions, attempt to read the surface size
                // from the Surface object. If the Surface has been destroyed, this check will fail.
                // Because there's no way to cleanly synchronize and check the value, we catch the
                // exception for these cases.
                try {
                    configuration =
                        if (surfaceGroupId != SURFACE_GROUP_ID_NONE) {
                            OutputConfiguration(surfaceGroupId, surface)
                        } else {
                            OutputConfiguration(surface)
                        }
                } catch (e: Throwable) {
                    Log.warn(e) { "Failed to create an OutputConfiguration for $surface!" }
                    return null
                }
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    throw IllegalStateException(
                        "Deferred OutputConfigurations are not supported on API " +
                            "${Build.VERSION.SDK_INT} (requires API ${Build.VERSION_CODES.O})"
                    )
                }

                check(size != null) {
                    "Size must defined when creating a deferred OutputConfiguration."
                }
                val outputKlass =
                    when (outputType) {
                        OutputType.SURFACE_TEXTURE -> SurfaceTexture::class.java
                        OutputType.SURFACE_VIEW -> SurfaceHolder::class.java
                        OutputType.SURFACE ->
                            throw IllegalStateException("Unsupported OutputType: $outputType")
                    }
                configuration = Api26Compat.newOutputConfiguration(size, outputKlass)
            }

            // Enable surface sharing, if set.
            if (surfaceSharing) {
                configuration.enableSurfaceSharingCompat()
            }

            // Pass along the physicalCameraId, if set.
            if (physicalCameraId != null) {
                configuration.setPhysicalCameraIdCompat(physicalCameraId)
            }

            if (mirrorMode != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Api33Compat.setMirrorMode(configuration, mirrorMode.value)
                } else {
                    if (mirrorMode != MirrorMode.MIRROR_MODE_AUTO) {
                        Log.warn {
                            "Cannot set mirrorMode to a non-default value on " +
                                "API ${Build.VERSION.SDK_INT}. This may result in unexpected " +
                                "behavior. Requested $mirrorMode"
                        }
                    }
                }
            }

            if (timestampBase != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Api33Compat.setTimestampBase(configuration, timestampBase.value)
                } else {
                    if (timestampBase != TimestampBase.TIMESTAMP_BASE_DEFAULT) {
                        Log.info {
                            "The timestamp base on API ${Build.VERSION.SDK_INT} will " +
                                "default to TIMESTAMP_BASE_DEFAULT, with which the camera device" +
                                " adjusts timestamps based on the output target. " +
                                "Requested $timestampBase"
                        }
                    }
                }
            }

            if (dynamicRangeProfile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Api33Compat.setDynamicRangeProfile(configuration, dynamicRangeProfile.value)
                } else {
                    if (dynamicRangeProfile != DynamicRangeProfile.STANDARD) {
                        Log.warn {
                            "Cannot set dynamicRangeProfile to a non-default value on API " +
                                "${Build.VERSION.SDK_INT}. This may result in unexpected " +
                                "behavior. Requested $dynamicRangeProfile"
                        }
                    }
                }
            }

            if (streamUseCase != null && cameraId != null && camera2MetadataProvider != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val cameraMetadata = camera2MetadataProvider.awaitCameraMetadata(cameraId)
                    val availableStreamUseCases =
                        Api33Compat.getAvailableStreamUseCases(cameraMetadata)
                    if (availableStreamUseCases?.contains(streamUseCase.value) == true) {
                        Api33Compat.setStreamUseCase(configuration, streamUseCase.value)
                    }
                }
            }

            // Create and return the Android
            return AndroidOutputConfiguration(
                configuration,
                surfaceSharing,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Api28Compat.getMaxSharedSurfaceCount(configuration)
                } else {
                    1
                },
                physicalCameraId
            )
        }

        private fun OutputConfiguration.enableSurfaceSharingCompat() {
            checkNOrHigher("surfaceSharing")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Api26Compat.enableSurfaceSharing(this)
            }
        }

        private fun OutputConfiguration.setPhysicalCameraIdCompat(physicalCameraId: CameraId) {
            checkPOrHigher("physicalCameraId")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Compat.setPhysicalCameraId(this, physicalCameraId.value)
            }
        }
    }

    override val surface: Surface? = output.surface
    override val surfaces: List<Surface>
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return Api26Compat.getSurfaces(output)
            }

            // On older versions of the OS, only one surface is allowed, and if an output
            // configuration is in a deferred state it may not have a surface when it's first
            // created.
            return listOfNotNull(output.surface)
        }

    override fun addSurface(surface: Surface) {
        checkOOrHigher("addSurface")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Api26Compat.addSurfaces(output, surface)
        }
    }

    override fun removeSurface(surface: Surface) {
        checkPOrHigher("removeSurface")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Api28Compat.removeSurface(output, surface)
        }
    }

    override val surfaceGroupId: Int
        get() = output.surfaceGroupId

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            OutputConfiguration::class -> output as T
            else -> null
        }

    override fun toString(): String = output.toString()
}
