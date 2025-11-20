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

package androidx.camera.extensions.internal.sessionprocessor

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Pair
import android.util.Size
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.OutputSurfaceConfiguration
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SessionProcessor.CaptureSessionRequestProcessor
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.CameraExtensionsControl
import androidx.camera.extensions.CameraExtensionsInfo
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.internal.Camera2ExtensionsUtil.convertCamera2ModeToCameraXMode
import androidx.camera.extensions.internal.Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode
import androidx.camera.extensions.internal.ExtensionsUtils
import androidx.camera.extensions.internal.VendorExtender
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(31)
public class Camera2ExtensionsSessionProcessor(
    private val availableCaptureRequestKeys: List<CaptureRequest.Key<*>>,
    @ExtensionMode.Mode private val mode: Int,
    private val vendorExtender: VendorExtender,
) : SessionProcessor, CameraExtensionsInfo, CameraExtensionsControl {

    private val camera2ExtensionMode = convertCameraXModeToCamera2Mode(mode)

    private var extensionStrengthLiveData: MutableLiveData<Int>? = null
    private var currentExtensionTypeLiveData: MutableLiveData<Int>? = null
    private val extensionStrength: AtomicInteger = AtomicInteger(100)
    private val currentExtensionType: AtomicInteger = AtomicInteger(mode)

    @AdapterCameraInfo.CameraOperation
    private val supportedCameraOperations: Set<Int> =
        ExtensionsUtils.getSupportedCameraOperations(availableCaptureRequestKeys)

    private var cameraInfoInternal: CameraInfoInternal? = null
    private var cameraCaptureCallback: CameraCaptureCallback? = null

    private val lock = Any()

    @GuardedBy("lock")
    private var captureSessionRequestProcessor: CaptureSessionRequestProcessor? = null

    init {
        if (isCurrentExtensionModeAvailable()) {
            currentExtensionTypeLiveData = MutableLiveData<Int>(mode)
        }
        if (isExtensionStrengthAvailable()) {
            extensionStrengthLiveData = MutableLiveData<Int>(100)
        }
    }

    override fun initSession(
        cameraInfo: CameraInfo,
        outputSurfaceConfig: OutputSurfaceConfiguration?,
    ): SessionConfig? {
        cameraInfoInternal = cameraInfo as CameraInfoInternal
        // Sets up the CameraCaptureCallback to receive the extensions related info
        cameraCaptureCallback =
            object : CameraCaptureCallback() {
                override fun onCaptureCompleted(
                    captureConfigId: Int,
                    cameraCaptureResult: CameraCaptureResult,
                ) {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                            isCurrentExtensionModeAvailable()
                    ) {
                        cameraCaptureResult.captureResult
                            ?.get(CaptureResult.EXTENSION_CURRENT_TYPE)
                            ?.let {
                                val cameraXMode = convertCamera2ModeToCameraXMode(it)
                                if (currentExtensionType.getAndSet(cameraXMode) != cameraXMode) {
                                    extensionStrengthLiveData?.postValue(it)
                                }
                            }
                    }

                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                            isExtensionStrengthAvailable()
                    ) {
                        cameraCaptureResult.captureResult
                            ?.get(CaptureResult.EXTENSION_STRENGTH)
                            ?.let {
                                if (extensionStrength.getAndSet(it) != it) {
                                    extensionStrengthLiveData?.postValue(it)
                                }
                            }
                    }
                }
            }

        cameraInfoInternal!!.addSessionCaptureCallback(
            CameraXExecutors.directExecutor(),
            cameraCaptureCallback!!,
        )

        return null
    }

    override fun deInitSession() {
        cameraInfoInternal?.apply {
            cameraCaptureCallback?.let { removeSessionCaptureCallback(it) }
        }
    }

    override fun getSupportedPostviewSize(captureSize: Size): Map<Int, List<Size>> {
        return vendorExtender.getSupportedPostviewResolutions(captureSize)
    }

    override fun getSupportedCameraOperations(): Set<Int> {
        return supportedCameraOperations
    }

    override fun getAvailableCharacteristicsKeyValues():
        List<Pair<CameraCharacteristics.Key<*>, in Any>> {
        return vendorExtender.availableCharacteristicsKeyValues
    }

    override fun getExtensionAvailableStabilizationModes(): IntArray? {
        return super.getExtensionAvailableStabilizationModes()
    }

    override fun getImplementationType(): Pair<Int, Int> {
        return Pair.create(SessionProcessor.TYPE_CAMERA2_EXTENSION, camera2ExtensionMode)
    }

    override fun setParameters(config: Config) {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#setParameters should not be invoked!"
        )
    }

    override fun onCaptureSessionStart(requestProcessor: RequestProcessor) {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#onCaptureSessionStart should not be invoked!"
        )
    }

    override fun onCaptureSessionEnd() {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#onCaptureSessionEnd should not be invoked!"
        )
    }

    override fun startRepeating(
        tagBundle: TagBundle,
        callback: SessionProcessor.CaptureCallback,
    ): Int {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#startRepeating should not be invoked!"
        )
    }

    override fun stopRepeating() {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#stopRepeating should not be invoked!"
        )
    }

    override fun startCapture(
        postviewEnabled: Boolean,
        tagBundle: TagBundle,
        callback: SessionProcessor.CaptureCallback,
    ): Int {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#startCapture should not be invoked!"
        )
    }

    override fun abortCapture(captureSequenceId: Int) {
        throw UnsupportedOperationException(
            "Camera2ExtensionsSessionProcessor#abortCapture should not be invoked!"
        )
    }

    override fun isExtensionStrengthAvailable(): Boolean =
        vendorExtender.isExtensionStrengthAvailable

    override fun getExtensionStrength(): LiveData<Int>? = extensionStrengthLiveData

    override fun isCurrentExtensionModeAvailable(): Boolean =
        vendorExtender.isCurrentExtensionModeAvailable

    override fun getCurrentExtensionMode(): LiveData<Int>? = currentExtensionTypeLiveData

    override fun getRealtimeCaptureLatency(): Pair<Long, Long>? {
        synchronized(lock) {
            return captureSessionRequestProcessor?.realtimeStillCaptureLatency
        }
    }

    override fun setExtensionStrength(strength: Int) {
        synchronized(lock) { captureSessionRequestProcessor?.setExtensionStrength(strength) }
    }

    override fun setCaptureSessionRequestProcessor(processor: CaptureSessionRequestProcessor?) {
        synchronized(lock) { captureSessionRequestProcessor = processor }
    }
}
