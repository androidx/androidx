/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.annotation.SuppressLint
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ZoomState
import androidx.lifecycle.LiveData
import kotlin.reflect.KClass

/**
 * Implementation of [CameraInfo] for physical camera. In comparison,
 * [CameraInfoAdapter] is the version of logical camera.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class PhysicalCameraInfoAdapter(
    private val cameraProperties: CameraProperties
) : CameraInfo, UnsafeWrapper {

    @OptIn(ExperimentalCamera2Interop::class)
    internal val camera2CameraInfo: Camera2CameraInfo by lazy {
        Camera2CameraInfo.create(cameraProperties)
    }

    override fun getSensorRotationDegrees(): Int {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getSensorRotationDegrees(relativeRotation: Int): Int {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun hasFlashUnit(): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getTorchState(): LiveData<Int> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getZoomState(): LiveData<ZoomState> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getExposureState(): ExposureState {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getCameraState(): LiveData<CameraState> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getImplementationType(): String {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getCameraSelector(): CameraSelector {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getLensFacing(): Int {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getIntrinsicZoomRatio(): Float {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun isFocusMeteringSupported(action: FocusMeteringAction): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    @SuppressLint("NullAnnotationGroup")
    @ExperimentalZeroShutterLag
    override fun isZslSupported(): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getSupportedFrameRateRanges(): Set<Range<Int>> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun isLogicalMultiCameraSupported(): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun isPrivateReprocessingSupported(): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun querySupportedDynamicRanges(
        candidateDynamicRanges: Set<DynamicRange>
    ): Set<DynamicRange> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getPhysicalCameraInfos(): Set<CameraInfo> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return when (type) {
            Camera2CameraInfo::class -> camera2CameraInfo as T
            else -> cameraProperties.metadata.unwrapAs(type)
        }
    }
}
