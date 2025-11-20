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

package androidx.camera.camera2.pipe.integration.interop

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.compat.Camera2CameraControlCompat
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.CameraControl
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.utils.futures.Futures
import androidx.core.util.Preconditions
import com.google.common.util.concurrent.ListenableFuture

/**
 * A class that provides ability to interoperate with the [android.hardware.camera2] APIs.
 *
 * Camera2 specific controls, like capture request options, can be applied through this class. A
 * Camera2CameraControl can be created from a general [CameraControl] which is associated to a
 * camera. Then the controls will affect all use cases that are using that camera.
 *
 * If any option applied by Camera2CameraControl conflicts with the options required by CameraX
 * internally. The options from Camera2CameraControl will override, which may result in unexpected
 * behavior depends on the options being applied.
 */
@SuppressWarnings("HiddenSuperclass")
@ExperimentalCamera2Interop
public class Camera2CameraControl
private constructor(
    private val compat: Camera2CameraControlCompat,
    private val threads: UseCaseThreads,
    @get:VisibleForTesting internal val requestListener: ComboRequestListener,
) : UseCaseCameraControl {

    private var _useCaseCameraRequestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        @RestrictTo(RestrictTo.Scope.LIBRARY) get() = _useCaseCameraRequestControl
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        set(value) {
            _useCaseCameraRequestControl = value
            _useCaseCameraRequestControl?.also {
                requestListener.removeListener(compat)
                requestListener.addListener(compat, threads.sequentialExecutor)
                compat.applyAsync(it, false)
            }
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun reset() {
        // Clear the current task, but don't clear the CaptureRequestOptions. Camera2CameraControl
        // will store the CaptureRequestOptions while use case is detached.
        compat.cancelCurrentTask()
        requestListener.removeListener(compat)
    }

    /**
     * Sets a [CaptureRequestOptions] and updates the session with the options it contains.
     *
     * This will first clear all options that have already been set, then apply the new options.
     *
     * Any values which are in conflict with values already set by CameraX, such as by
     * [androidx.camera.core.CameraControl], will overwrite the existing values. The values will be
     * submitted with every repeating and single capture requests issued by CameraX, which may
     * result in unexpected behavior depending on the values being applied.
     *
     * @param bundle The [CaptureRequestOptions] which will be set.
     * @return a [ListenableFuture] which completes when the repeating
     *   [android.hardware.camera2.CaptureResult] shows the options have be submitted completely.
     *   The future fails with [CameraControl.OperationCanceledException] if newer options are set
     *   or camera is closed before the current request completes. Cancelling the ListenableFuture
     *   is a no-op.
     */
    @SuppressWarnings("AsyncSuffixFuture")
    public fun setCaptureRequestOptions(bundle: CaptureRequestOptions): ListenableFuture<Void?> {
        compat.clearRequestOption()
        compat.addRequestOption(bundle)
        return updateAsync("setCaptureRequestOptions")
    }

    /**
     * Adds a [CaptureRequestOptions] updates the session with the options it contains.
     *
     * The options will be merged with the existing options. If one option is set with a different
     * value, it will overwrite the existing value.
     *
     * Any values which are in conflict with values already set by CameraX, such as by
     * [androidx.camera.core.CameraControl], will overwrite the existing values. The values will be
     * submitted with every repeating and single capture requests issued by CameraX, which may
     * result in unexpected behavior depends on the values being applied.
     *
     * @param bundle The [CaptureRequestOptions] which will be set.
     * @return a [ListenableFuture] which completes when the repeating
     *   [android.hardware.camera2.CaptureResult] shows the options have be submitted completely.
     *   The future fails with [CameraControl.OperationCanceledException] if newer options are set
     *   or camera is closed before the current request completes.
     */
    @SuppressWarnings("AsyncSuffixFuture")
    public fun addCaptureRequestOptions(bundle: CaptureRequestOptions): ListenableFuture<Void?> {
        compat.addRequestOption(bundle)
        return updateAsync("addCaptureRequestOptions")
    }

    /**
     * Gets all the capture request options that is currently applied by the [Camera2CameraControl].
     *
     * It doesn't include the capture request options applied by the
     * [android.hardware.camera2.CameraDevice] templates or by CameraX.
     *
     * @return The [CaptureRequestOptions].
     */
    public fun getCaptureRequestOptions(): CaptureRequestOptions = compat.getRequestOption()

    /**
     * Clears all capture request options that is currently applied by the [Camera2CameraControl].
     *
     * @return a [ListenableFuture] which completes when the repeating
     *   [android.hardware.camera2.CaptureResult] shows the options have be submitted completely.
     *   The future fails with [CameraControl.OperationCanceledException] if newer options are set
     *   or camera is closed before the current request completes.
     */
    @SuppressWarnings("AsyncSuffixFuture")
    public fun clearCaptureRequestOptions(): ListenableFuture<Void?> {
        compat.clearRequestOption()
        return updateAsync("clearCaptureRequestOptions")
    }

    private fun updateAsync(tag: String): ListenableFuture<Void?> =
        Futures.nonCancellationPropagating(
            compat.applyAsync(requestControl).asListenableFuture(tag)
        )

    public companion object {

        /**
         * Gets the [Camera2CameraControl] from a [CameraControl].
         *
         * The [CameraControl] is still usable after a [Camera2CameraControl] is obtained from it.
         * Note that the [Camera2CameraControl] has higher priority than the [CameraControl]. For
         * example, if [android.hardware.camera2.CaptureRequest.FLASH_MODE] is set through the
         * [Camera2CameraControl]. All [CameraControl] features that required
         * [android.hardware.camera2.CaptureRequest.FLASH_MODE] internally like torch may not work
         * properly.
         *
         * @param cameraControl The [CameraControl] to get from.
         * @return The camera control with Camera2 implementation.
         * @throws IllegalArgumentException if the camera control does not contain the camera2
         *   information (e.g., if CameraX was not initialized with a
         *   [androidx.camera.camera2.pipe.integration.CameraPipeConfig]).
         */
        @JvmStatic
        public fun from(cameraControl: CameraControl): Camera2CameraControl {
            var cameraControlImpl = (cameraControl as CameraControlInternal).implementation
            Preconditions.checkArgument(
                cameraControlImpl is CameraControlAdapter,
                "CameraControl doesn't contain Camera2 implementation.",
            )
            return (cameraControlImpl as CameraControlAdapter).camera2cameraControl
        }

        /** This is the workaround to prevent constructor from being added to public API. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        public fun create(
            compat: Camera2CameraControlCompat,
            threads: UseCaseThreads,
            requestListener: ComboRequestListener,
        ): Camera2CameraControl {
            return Camera2CameraControl(compat, threads, requestListener)
        }
    }
}
