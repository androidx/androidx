/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("Camera2InteropEx")

package androidx.camera.camera2.interop

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureScope
import androidx.camera.core.InteropConfigurableScope
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.concurrent.futures.await
import com.google.common.util.concurrent.ListenableFuture

/**
 * Applies Camera2 options to a UseCase builder.
 *
 * Configures Camera2 options on builders implementing [UseCase.InteropConfigurable] (such as
 * [androidx.camera.core.Preview.Builder], [androidx.camera.core.ImageAnalysis.Builder], or
 * [androidx.camera.video.VideoCapture.Builder]). For [ImageCapture.Builder], use the dedicated
 * [ImageCapture.Builder.camera2Interop] extension function.
 *
 * Options configured via [UseCaseCamera2Interop] include:
 * - Physical camera ID ([UseCaseCamera2Interop.setPhysicalCameraId])
 * - Stream use case ([UseCaseCamera2Interop.setStreamUseCase])
 * - Mirror mode ([UseCaseCamera2Interop.setMirrorMode])
 * - Timestamp base ([UseCaseCamera2Interop.setTimestampBase])
 * - Dynamic range profile ([UseCaseCamera2Interop.setDynamicRangeProfile])
 * - Surface group ID ([UseCaseCamera2Interop.setSurfaceGroupId])
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior.
 *
 * @sample androidx.camera.camera2.samples.useCaseBuilderCamera2InteropSample
 * @param block configuration block setting Camera2 options on [UseCaseCamera2Interop]
 * @return this builder
 */
@JvmSynthetic
public inline fun <B : UseCase.InteropConfigurable<B>> B.camera2Interop(
    crossinline block: UseCaseCamera2Interop.() -> Unit
): B {
    this.setInterop(Camera2Interop.forUseCase { interop -> interop.block() })
    return this
}

/**
 * Applies Camera2 options to an [ImageCapture.Builder].
 *
 * Configures Camera2 options specific to [ImageCapture.Builder].
 *
 * Options configured via [ImageCaptureCamera2Interop] include:
 * - Physical camera ID ([ImageCaptureCamera2Interop.setPhysicalCameraId])
 * - Stream use case ([ImageCaptureCamera2Interop.setStreamUseCase])
 * - Mirror mode ([ImageCaptureCamera2Interop.setMirrorMode])
 * - Timestamp base ([ImageCaptureCamera2Interop.setTimestampBase])
 * - Dynamic range profile ([ImageCaptureCamera2Interop.setDynamicRangeProfile])
 * - Surface group ID ([ImageCaptureCamera2Interop.setSurfaceGroupId])
 * - Still capture request key-value pairs
 *   ([ImageCaptureCamera2Interop.setStillCaptureRequestOption])
 * - Still capture request template type
 *   ([ImageCaptureCamera2Interop.setStillCaptureRequestTemplateType])
 * - Still capture callbacks ([ImageCaptureCamera2Interop.setStillCaptureCallback])
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. The capture
 * request keys for one-shot still captures (such as
 * [androidx.camera.core.ImageCapture.takePicture]) are determined by copying all repeating request
 * keys (which may include keys added via [androidx.camera.core.SessionConfig.Builder.setInterop] or
 * [androidx.camera.core.CameraControl.applyInteropAsync]) and then overriding them with the still
 * capture request keys configured here. If an option configured via interop conflicts with options
 * required by CameraX internally, the option from Camera2Interop will override, which may result in
 * unexpected behavior.
 *
 * **Warning:** Callbacks configured via interop receive raw
 * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking state-altering
 * methods on these raw objects (such as [android.hardware.camera2.CameraCaptureSession.close] or
 * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
 * management and may cause state desynchronization, stream interruption, or application crashes.
 *
 * @sample androidx.camera.camera2.samples.imageCaptureBuilderCamera2InteropSample
 * @param block configuration block setting Camera2 options on [ImageCaptureCamera2Interop]
 * @return this builder
 */
@JvmSynthetic
public inline fun ImageCapture.Builder.camera2Interop(
    crossinline block: ImageCaptureCamera2Interop.() -> Unit
): ImageCapture.Builder {
    this.setInterop(Camera2Interop.forImageCapture { interop -> interop.block() })
    return this
}

/**
 * Applies Camera2 options inside an [InteropConfigurableScope] DSL block.
 *
 * Configures Camera2 options on the underlying UseCase builder within Kotlin DSL blocks such as
 * `preview { ... }`, `imageAnalysis { ... }`, or `videoCapture { ... }`. For `imageCapture { ...
 * }`, use the dedicated [ImageCaptureScope.camera2Interop] extension function.
 *
 * Options configured via [UseCaseCamera2Interop] include:
 * - Physical camera ID ([UseCaseCamera2Interop.setPhysicalCameraId])
 * - Stream use case ([UseCaseCamera2Interop.setStreamUseCase])
 * - Mirror mode ([UseCaseCamera2Interop.setMirrorMode])
 * - Timestamp base ([UseCaseCamera2Interop.setTimestampBase])
 * - Dynamic range profile ([UseCaseCamera2Interop.setDynamicRangeProfile])
 * - Surface group ID ([UseCaseCamera2Interop.setSurfaceGroupId])
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior.
 *
 * @sample androidx.camera.camera2.samples.useCaseDslCamera2InteropSample
 * @param block configuration block setting Camera2 options on [UseCaseCamera2Interop]
 */
@JvmSynthetic
public inline fun <B : UseCase.InteropConfigurable<B>> InteropConfigurableScope<B>.camera2Interop(
    crossinline block: UseCaseCamera2Interop.() -> Unit
) {
    this.builder.camera2Interop(block)
}

/**
 * Applies Camera2 options inside an [ImageCaptureScope] DSL block.
 *
 * Configures Camera2 options on the underlying [ImageCapture.Builder] within an `imageCapture { ...
 * }` DSL block.
 *
 * Options configured via [ImageCaptureCamera2Interop] include:
 * - Physical camera ID ([ImageCaptureCamera2Interop.setPhysicalCameraId])
 * - Stream use case ([ImageCaptureCamera2Interop.setStreamUseCase])
 * - Mirror mode ([ImageCaptureCamera2Interop.setMirrorMode])
 * - Timestamp base ([ImageCaptureCamera2Interop.setTimestampBase])
 * - Dynamic range profile ([ImageCaptureCamera2Interop.setDynamicRangeProfile])
 * - Surface group ID ([ImageCaptureCamera2Interop.setSurfaceGroupId])
 * - Still capture request key-value pairs
 *   ([ImageCaptureCamera2Interop.setStillCaptureRequestOption])
 * - Still capture request template type
 *   ([ImageCaptureCamera2Interop.setStillCaptureRequestTemplateType])
 * - Still capture callbacks ([ImageCaptureCamera2Interop.setStillCaptureCallback])
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. The capture
 * request keys for one-shot still captures (such as
 * [androidx.camera.core.ImageCapture.takePicture]) are determined by copying all repeating request
 * keys (which may include keys added via [androidx.camera.core.SessionConfig.Builder.setInterop] or
 * [androidx.camera.core.CameraControl.applyInteropAsync]) and then overriding them with the still
 * capture request keys configured here. If an option configured via interop conflicts with options
 * required by CameraX internally, the option from Camera2Interop will override, which may result in
 * unexpected behavior.
 *
 * **Warning:** Callbacks configured via interop receive raw
 * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking state-altering
 * methods on these raw objects (such as [android.hardware.camera2.CameraCaptureSession.close] or
 * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
 * management and may cause state desynchronization, stream interruption, or application crashes.
 *
 * @sample androidx.camera.camera2.samples.imageCaptureDslCamera2InteropSample
 * @param block configuration block setting Camera2 options on [ImageCaptureCamera2Interop]
 */
@JvmSynthetic
public inline fun ImageCaptureScope.camera2Interop(
    crossinline block: ImageCaptureCamera2Interop.() -> Unit
) {
    this.builder.camera2Interop(block)
}

/**
 * Applies Camera2 options to a [SessionConfig.Builder].
 *
 * Configures Camera2 options for a capture session builder. Can be called on a
 * [SessionConfig.Builder] directly or within a [androidx.camera.core.sessionConfig] DSL block.
 *
 * Options configured via [SessionConfigCamera2Interop] include:
 * - Session parameters ([SessionConfigCamera2Interop.setSessionParameter])
 * - Session type ([SessionConfigCamera2Interop.setSessionType])
 * - Color space ([SessionConfigCamera2Interop.setColorSpace])
 * - Session state callbacks ([SessionConfigCamera2Interop.setSessionStateCallback])
 * - Device state callbacks ([SessionConfigCamera2Interop.setDeviceStateCallback])
 * - Capture request key-value pairs ([SessionConfigCamera2Interop.setCaptureRequestOption])
 * - Clearing specific capture request keys
 *   ([SessionConfigCamera2Interop.clearCaptureRequestOption])
 * - Clearing all capture request keys ([SessionConfigCamera2Interop.clearAllCaptureRequestOptions])
 * - Capture request template type
 *   ([SessionConfigCamera2Interop.setRepeatingCaptureRequestTemplate])
 * - Repeating capture callbacks ([SessionConfigCamera2Interop.setRepeatingCaptureCallback])
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior.
 *
 * **Warning:** Callbacks configured via interop receive raw [android.hardware.camera2.CameraDevice]
 * and [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking state-altering
 * methods on these raw objects (such as [android.hardware.camera2.CameraCaptureSession.close],
 * [android.hardware.camera2.CameraCaptureSession.abortCaptures], or
 * [android.hardware.camera2.CameraDevice.close]) bypasses CameraX pipeline management and may cause
 * state desynchronization, stream interruption, or application crashes.
 *
 * @sample androidx.camera.camera2.samples.sessionConfigBuilderCamera2InteropSample
 * @sample androidx.camera.camera2.samples.sessionConfigDslCamera2InteropSample
 * @param block configuration block setting Camera2 options on [SessionConfigCamera2Interop]
 * @return this builder
 */
@JvmSynthetic
public inline fun SessionConfig.Builder.camera2Interop(
    crossinline block: SessionConfigCamera2Interop.() -> Unit
): SessionConfig.Builder {
    return this.setInterop(Camera2Interop.forSessionConfig { interop -> interop.block() })
}

/**
 * Applies Camera2 options to a [CameraControl] asynchronously.
 *
 * Dynamically updates repeating capture request parameters or capture request keys on an active
 * camera session. Options configured within a single [block] are applied atomically in a single
 * repeating capture request update. Subsequent calls to [applyCamera2InteropAsync] update
 * parameters incrementally without clearing previously set keys, unless explicitly cleared using
 * [CameraControlCamera2Interop.clearCaptureRequestOption] or
 * [CameraControlCamera2Interop.clearAllCaptureRequestOptions]. This overwrites options set with
 * [SessionConfigCamera2Interop] via [androidx.camera.core.SessionConfig.Builder.setInterop] (or
 * [SessionConfig.Builder.camera2Interop]).
 *
 * Options configured via [CameraControlCamera2Interop] include:
 * - Capture request key-value pairs ([CameraControlCamera2Interop.setCaptureRequestOption])
 * - Clearing specific capture request keys
 *   ([CameraControlCamera2Interop.clearCaptureRequestOption])
 * - Clearing all capture request keys ([CameraControlCamera2Interop.clearAllCaptureRequestOptions])
 * - Capture request template type ([CameraControlCamera2Interop.setRepeatingCaptureRequestTemplate]
 *   or [CameraControlCamera2Interop.repeatingCaptureRequestTemplate])
 * - Repeating capture callbacks ([CameraControlCamera2Interop.setRepeatingCaptureCallback] or
 *   [CameraControlCamera2Interop.repeatingCaptureCallback])
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior or interfere with 3A
 * routines and camera control APIs.
 *
 * **Warning:** Callbacks configured via interop receive raw
 * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking state-altering
 * methods on these raw objects (such as [android.hardware.camera2.CameraCaptureSession.close] or
 * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
 * management and may cause state desynchronization, stream interruption, or application crashes.
 *
 * @sample androidx.camera.camera2.samples.applyCamera2InteropAsyncSample
 * @param block configuration block setting Camera2 options on [CameraControlCamera2Interop]
 * @return [ListenableFuture] completing with `null` when all specified interoperability options
 *   have been successfully updated in the underlying repeating capture request. The future fails
 *   with [CameraControl.OperationCanceledException] if a newer configuration is applied before this
 *   operation takes effect or if the camera is closed. Cancelling the future is a no-op.
 */
@JvmSynthetic
public inline fun CameraControl.applyCamera2InteropAsync(
    crossinline block: CameraControlCamera2Interop.() -> Unit
): ListenableFuture<Void> {
    return this.applyInteropAsync(Camera2Interop.forCameraControl { interop -> interop.block() })
}

/**
 * Applies Camera2 options to a [CameraControl] asynchronously within a coroutine.
 *
 * Dynamically updates repeating capture request parameters or capture request keys on an active
 * camera session. Options configured within a single [block] are applied atomically in a single
 * repeating capture request update. Subsequent calls to [applyCamera2Interop] update parameters
 * incrementally without clearing previously set keys, unless explicitly cleared using
 * [CameraControlCamera2Interop.clearCaptureRequestOption] or
 * [CameraControlCamera2Interop.clearAllCaptureRequestOptions]. This overwrites options set with
 * [SessionConfigCamera2Interop] via [androidx.camera.core.SessionConfig.Builder.setInterop] (or
 * [SessionConfig.Builder.camera2Interop]).
 *
 * This function suspends until all specified interoperability options have been successfully
 * updated in the underlying repeating capture request. Throws
 * [CameraControl.OperationCanceledException] if a newer configuration is applied before this
 * operation takes effect or if the camera is closed.
 *
 * Options configured via [CameraControlCamera2Interop] include:
 * - Capture request key-value pairs ([CameraControlCamera2Interop.setCaptureRequestOption])
 * - Clearing specific capture request keys
 *   ([CameraControlCamera2Interop.clearCaptureRequestOption])
 * - Clearing all capture request keys ([CameraControlCamera2Interop.clearAllCaptureRequestOptions])
 * - Capture request template type ([CameraControlCamera2Interop.setRepeatingCaptureRequestTemplate]
 *   or [CameraControlCamera2Interop.repeatingCaptureRequestTemplate])
 * - Repeating capture callbacks ([CameraControlCamera2Interop.setRepeatingCaptureCallback] or
 *   [CameraControlCamera2Interop.repeatingCaptureCallback])
 *
 * **Note:** Using Camera2 interop options can override internal CameraX configurations. If an
 * option configured via interop conflicts with options required by CameraX internally, the option
 * from Camera2Interop will override, which may result in unexpected behavior or interfere with 3A
 * routines and camera control APIs.
 *
 * **Warning:** Callbacks configured via interop receive raw
 * [android.hardware.camera2.CameraCaptureSession] instances. Directly invoking state-altering
 * methods on these raw objects (such as [android.hardware.camera2.CameraCaptureSession.close] or
 * [android.hardware.camera2.CameraCaptureSession.abortCaptures]) bypasses CameraX pipeline
 * management and may cause state desynchronization, stream interruption, or application crashes.
 *
 * @sample androidx.camera.camera2.samples.applyCamera2InteropSample
 * @param block configuration block setting Camera2 options on [CameraControlCamera2Interop]
 * @throws CameraControl.OperationCanceledException if a newer configuration is applied before this
 *   operation takes effect or if the camera is closed
 */
@JvmSynthetic
public suspend inline fun CameraControl.applyCamera2Interop(
    crossinline block: CameraControlCamera2Interop.() -> Unit
) {
    this.applyCamera2InteropAsync(block).await()
}

/**
 * Returns the Camera2 camera ID from this [CameraInfo].
 *
 * @throws IllegalArgumentException if this [CameraInfo] does not contain Camera2 information
 */
@get:JvmSynthetic
public val CameraInfo.cameraId: String
    get() = Camera2Interop.getCameraId(this)

/**
 * Returns the [CameraCharacteristics] from this [CameraInfo].
 *
 * @throws IllegalArgumentException if this [CameraInfo] does not contain Camera2 information
 */
@get:JvmSynthetic
public val CameraInfo.cameraCharacteristics: CameraCharacteristics
    get() = Camera2Interop.getCameraCharacteristics(this)

/**
 * Creates a [CameraSelector] targeting this camera ID string.
 *
 * @return [CameraSelector] matching this camera ID
 */
@JvmSynthetic
public fun String.toCameraSelector(): CameraSelector =
    Camera2Interop.getCameraSelectorFromCameraId(this)
