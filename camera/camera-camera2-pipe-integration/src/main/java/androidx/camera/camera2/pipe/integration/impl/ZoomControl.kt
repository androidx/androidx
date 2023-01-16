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
import androidx.camera.camera2.pipe.integration.adapter.ZoomValue
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.utils.futures.Futures
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.ListenableFuture
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val DEFAULT_ZOOM_RATIO = 1.0f

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class ZoomControl @Inject constructor(
    private val threads: UseCaseThreads,
    private val zoomCompat: ZoomCompat,
) : UseCaseCameraControl {
    // NOTE: minZoom may be lower than 1.0
    // NOTE: Default zoom ratio is 1.0 (DEFAULT_ZOOM_RATIO)
    val minZoom: Float = zoomCompat.minZoom
    val maxZoom: Float = zoomCompat.maxZoom

    val defaultZoomState by lazy {
        ZoomValue(DEFAULT_ZOOM_RATIO, minZoom, maxZoom)
    }

    private val _zoomState by lazy {
        MutableLiveData<ZoomState>(defaultZoomState)
    }

    val zoomStateLiveData: LiveData<ZoomState>
        get() = _zoomState

    /** Linear zoom is between 0.0f and 1.0f */
    fun toLinearZoom(zoomRatio: Float): Float {
        val range = zoomCompat.maxZoom - zoomCompat.minZoom
        if (range > 0) {
            return (zoomRatio - zoomCompat.minZoom) / range
        }
        return 0.0f
    }

    /** Zoom ratio is commonly used as the "1x, 2x, 5x" zoom ratio. Zoom ratio may be less than 1 */
    fun toZoomRatio(linearZoom: Float): Float {
        val range = zoomCompat.maxZoom - zoomCompat.minZoom
        if (range > 0) {
            return linearZoom * range + zoomCompat.minZoom
        }

        // if minZoom = maxZoom = 2.0f, 2.0f should be returned instead of default 1.0f
        if (nearZero(range)) {
            return zoomCompat.minZoom
        }

        return DEFAULT_ZOOM_RATIO
    }

    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
            update()
        }

    override fun reset() {
        // TODO: 1.0 may not be a reasonable value to reset the zoom state to.
        threads.sequentialScope.launch(start = CoroutineStart.UNDISPATCHED) {
            setZoomState(defaultZoomState)
        }

        update()
    }

    private fun update() {
        _useCaseCamera?.let {
            zoomCompat.apply(_zoomState.value?.zoomRatio ?: DEFAULT_ZOOM_RATIO, it)
        }
    }

    private suspend fun setZoomState(value: ZoomState) {
        // TODO: camera-camera2 updates livedata with setValue if calling thread is main thread,
        //  and updates with postValue otherwise. Need to consider if always using setValue
        //  via main thread is alright in camera-pipe.
        withContext(Dispatchers.Main) {
            _zoomState.value = value
        }
    }

    fun setZoomRatioAsync(ratio: Float): ListenableFuture<Void> {
        // TODO: report IllegalArgumentException if ratio not in range
        return Futures.nonCancellationPropagating(
            useCaseCamera?.let {
                threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    val zoomValue = ZoomValue(
                        ratio,
                        minZoom,
                        maxZoom
                    )
                    setZoomState(zoomValue)
                    update()
                }.asListenableFuture()
            } ?: Futures.immediateFailedFuture(
                CameraControl.OperationCanceledException("Camera is not active.")
            )
        )
    }

    private fun nearZero(num: Float): Boolean {
        return abs(num) < 2.0 * Math.ulp(abs(num))
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(zoomControl: ZoomControl): UseCaseCameraControl
    }
}
