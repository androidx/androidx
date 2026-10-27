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

package androidx.camera.camera2.impl

import android.hardware.camera2.CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR
import android.hardware.camera2.CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR_OFF
import android.hardware.camera2.CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR_ON
import android.hardware.camera2.CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR_UNKNOWN
import android.os.Build
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.common.CameraFrameNumber
import androidx.camera.core.NightModeIndicator
import androidx.camera.core.impl.utils.Threads
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/** Monitor that observes the night mode indicator from capture results. */
@CameraScope
public class NightModeIndicatorMonitor
@Inject
constructor(cameraMetadata: CameraMetadata?, requestListener: ComboRequestListener) :
    Request.Listener, UseCaseCameraControl {

    private val stateLock = Any()

    @Volatile private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            val changed =
                synchronized(stateLock) {
                    _requestControl = value
                    value == null &&
                        nightModeIndicatorAtomic.getAndSet(NightModeIndicator.UNKNOWN) !=
                            NightModeIndicator.UNKNOWN
                }
            if (changed) {
                updateLiveData()
            }
        }

    override fun reset() {
        val changed =
            synchronized(stateLock) {
                nightModeIndicatorAtomic.getAndSet(NightModeIndicator.UNKNOWN) !=
                    NightModeIndicator.UNKNOWN
            }
        if (changed) {
            updateLiveData()
        }
    }

    public val isSupported: Boolean =
        cameraMetadata?.let {
            Build.VERSION.SDK_INT >= 36 && it.resultKeys.contains(EXTENSION_NIGHT_MODE_INDICATOR)
        } ?: false

    private val nightModeIndicatorAtomic = AtomicInteger(NightModeIndicator.UNKNOWN)
    private val _nightModeIndicator =
        object : MutableLiveData<Int>(NightModeIndicator.UNKNOWN) {
            override fun onActive() {
                if (isSupported) {
                    // Register listener only when something is watching
                    requestListener.addListener(
                        this@NightModeIndicatorMonitor,
                        CameraXExecutors.directExecutor(),
                    )
                }
            }

            override fun onInactive() {
                if (isSupported) {
                    // Unregister to stop processing metadata for every frame
                    requestListener.removeListener(this@NightModeIndicatorMonitor)
                }
            }
        }
    public val nightModeIndicatorLiveData: LiveData<Int> = _nightModeIndicator

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: CameraFrameNumber,
        totalCaptureResult: FrameInfo,
    ) {
        if (Build.VERSION.SDK_INT < 36 || _requestControl == null) {
            return
        }
        val indicator = totalCaptureResult.metadata[EXTENSION_NIGHT_MODE_INDICATOR] ?: return
        val newState =
            when (indicator) {
                EXTENSION_NIGHT_MODE_INDICATOR_ON -> NightModeIndicator.RECOMMENDED
                EXTENSION_NIGHT_MODE_INDICATOR_OFF -> NightModeIndicator.NOT_RECOMMENDED
                EXTENSION_NIGHT_MODE_INDICATOR_UNKNOWN -> NightModeIndicator.UNKNOWN
                else -> NightModeIndicator.UNKNOWN
            }
        if (nightModeIndicatorAtomic.get() == newState) {
            return
        }
        val changed =
            synchronized(stateLock) {
                _requestControl != null && nightModeIndicatorAtomic.getAndSet(newState) != newState
            }
        if (changed) {
            updateLiveData()
        }
    }

    private fun updateLiveData() {
        Threads.runOnMain {
            val currentState = nightModeIndicatorAtomic.get()
            if (_nightModeIndicator.value != currentState) {
                _nightModeIndicator.value = currentState
            }
        }
    }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(
            nightModeIndicatorMonitor: NightModeIndicatorMonitor
        ): UseCaseCameraControl
    }
}
