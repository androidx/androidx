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

import androidx.camera.camera2.pipe.integration.adapter.ZoomValue
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.internal.ZoomMath.getLinearZoomFromZoomRatio
import androidx.camera.camera2.pipe.integration.internal.ZoomMath.getZoomRatioFromLinearZoom
import androidx.camera.core.CameraControl
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.utils.Threads
import androidx.camera.core.impl.utils.futures.Futures
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.ListenableFuture
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

public const val DEFAULT_ZOOM_RATIO: Float = 1.0f

@CameraScope
public class ZoomControl @Inject constructor(private val zoomCompat: ZoomCompat) :
    UseCaseCameraControl {
    // NOTE: minZoom may be lower than 1.0
    // NOTE: Default zoom ratio is 1.0 (DEFAULT_ZOOM_RATIO)
    public val minZoomRatio: Float = zoomCompat.minZoomRatio
    public val maxZoomRatio: Float = zoomCompat.maxZoomRatio

    public val defaultZoomState: ZoomValue by lazy {
        ZoomValue(DEFAULT_ZOOM_RATIO, minZoomRatio, maxZoomRatio)
    }

    private val _zoomState by lazy { MutableLiveData<ZoomState>(defaultZoomState) }

    public val zoomStateLiveData: LiveData<ZoomState>
        get() = _zoomState

    private var isInitialized = false

    /** Linear zoom is between 0.0f and 1.0f */
    public fun toLinearZoom(zoomRatio: Float): Float =
        getLinearZoomFromZoomRatio(
            zoomRatio = zoomRatio,
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio,
        )

    /** Zoom ratio is commonly used as the "1x, 2x, 5x" zoom ratio. Zoom ratio may be less than 1 */
    private fun toZoomRatio(linearZoom: Float) =
        getZoomRatioFromLinearZoom(
            linearZoom = linearZoom,
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio,
        )

    private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value
            val zoomState = _zoomState.value ?: defaultZoomState
            val shouldUpdateParameters = isInitialized || zoomState.zoomRatio != DEFAULT_ZOOM_RATIO
            applyZoomState(zoomState, false, shouldUpdateParameters)
            isInitialized = true
        }

    private var updateSignal: CompletableDeferred<Unit>? = null

    override fun reset() {
        // TODO: 1.0 may not be a reasonable value to reset the zoom state to.
        applyZoomState(defaultZoomState)
    }

    private fun setZoomState(value: ZoomState) {
        if (Threads.isMainThread()) {
            _zoomState.value = value
        } else {
            _zoomState.postValue(value)
        }
    }

    public fun setLinearZoom(linearZoom: Float): ListenableFuture<Void> {
        if (linearZoom > 1.0f || linearZoom < 0f) {
            val outOfRangeDesc = "Requested linearZoom $linearZoom is not within valid range [0, 1]"
            return Futures.immediateFailedFuture(IllegalArgumentException(outOfRangeDesc))
        }

        val zoomValue = ZoomValue(ZoomValue.LinearZoom(linearZoom), minZoomRatio, maxZoomRatio)
        return applyZoomState(zoomValue)
    }

    public fun setZoomRatio(zoomRatio: Float): ListenableFuture<Void> {
        if (zoomRatio > maxZoomRatio || zoomRatio < minZoomRatio) {
            val outOfRangeDesc =
                "Requested zoomRatio $zoomRatio is not within valid range" +
                    " [$minZoomRatio, $maxZoomRatio]"
            return Futures.immediateFailedFuture(IllegalArgumentException(outOfRangeDesc))
        }

        val zoomValue = ZoomValue(zoomRatio, minZoomRatio, maxZoomRatio)
        return applyZoomState(zoomValue)
    }

    public fun applyZoomState(
        zoomState: ZoomState,
        cancelPreviousTask: Boolean = true,
        shouldUpdateParameters: Boolean = true,
    ): ListenableFuture<Void> {
        val signal = CompletableDeferred<Unit>()

        updateSignal?.let { previousUpdateSignal ->
            if (cancelPreviousTask) {
                // Cancel the previous request signal if exist.
                previousUpdateSignal.completeExceptionally(
                    CameraControl.OperationCanceledException(
                        "Cancelled due to another zoom value being set."
                    )
                )
            } else {
                // Propagate the result to the previous updateSignal
                signal.propagateTo(previousUpdateSignal)
            }
        }
        updateSignal = signal

        setZoomState(zoomState)

        requestControl?.let {
            val zoomRatio = zoomState.zoomRatio
            val deferred =
                if (shouldUpdateParameters) {
                    zoomCompat.applyAsync(zoomRatio, it)
                } else {
                    zoomCompat.resetAsync(it)
                }
            deferred.propagateTo(signal)
        }
            ?: signal.completeExceptionally(
                CameraControl.OperationCanceledException("Camera is not active.")
            )

        /**
         * TODO: Use signal.asListenableFuture() directly. Deferred<T>.asListenableFuture() returns
         *   a ListenableFuture<T>, so this currently reports a type mismatch error (Required:
         *   Void!, Found: Unit). Currently, Job.asListenableFuture() is used as a workaround for
         *   this problem.
         */
        return Futures.nonCancellationPropagating((signal as Job).asListenableFuture())
    }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(zoomControl: ZoomControl): UseCaseCameraControl
    }
}
