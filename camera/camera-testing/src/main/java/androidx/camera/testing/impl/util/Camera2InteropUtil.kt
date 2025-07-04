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

package androidx.camera.testing.impl.util

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl as CPCamera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo as CPCamera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.Camera2Interop as CPCamera2Interop
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions as CPCaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop as CPExperimentalCamera2Interop
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ExtendableBuilder
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

@kotlin.OptIn(CPExperimentalCamera2Interop::class)
@OptIn(markerClass = [ExperimentalCamera2Interop::class])
public object Camera2InteropUtil {

    @JvmStatic
    public fun <T> setCamera2InteropOptions(
        implName: String,
        builder: ExtendableBuilder<T>,
        captureCallback: CameraCaptureSession.CaptureCallback? = null,
        deviceStateCallback: CameraDevice.StateCallback? = null,
        sessionStateCallback: CameraCaptureSession.StateCallback? = null,
        physicalCameraId: String? = null,
    ) {
        if (implName == CameraPipeConfig::class.simpleName) {
            val extendedBuilder = CPCamera2Interop.Extender(builder)
            captureCallback?.let { extendedBuilder.setSessionCaptureCallback(it) }
            deviceStateCallback?.let { extendedBuilder.setDeviceStateCallback(it) }
            sessionStateCallback?.let { extendedBuilder.setSessionStateCallback(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && physicalCameraId != null) {
                extendedBuilder.setPhysicalCameraId(physicalCameraId)
            }
        } else {
            val extendedBuilder = Camera2Interop.Extender(builder)
            captureCallback?.let { extendedBuilder.setSessionCaptureCallback(it) }
            deviceStateCallback?.let { extendedBuilder.setDeviceStateCallback(it) }
            sessionStateCallback?.let { extendedBuilder.setSessionStateCallback(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && physicalCameraId != null) {
                extendedBuilder.setPhysicalCameraId(physicalCameraId)
            }
        }
    }

    @JvmStatic
    public fun <T> setCameraCaptureSessionCallback(
        implName: String,
        builder: ExtendableBuilder<T>,
        captureCallback: CameraCaptureSession.CaptureCallback,
    ) {
        setCamera2InteropOptions(
            implName = implName,
            builder = builder,
            captureCallback = captureCallback,
        )
    }

    @JvmStatic
    public fun <T> setDeviceStateCallback(
        implName: String,
        builder: ExtendableBuilder<T>,
        stateCallback: CameraDevice.StateCallback,
    ) {
        setCamera2InteropOptions(
            implName = implName,
            builder = builder,
            deviceStateCallback = stateCallback,
        )
    }

    @JvmStatic
    public fun <T> setSessionStateCallback(
        implName: String,
        builder: ExtendableBuilder<T>,
        stateCallback: CameraCaptureSession.StateCallback,
    ) {
        setCamera2InteropOptions(
            implName = implName,
            builder = builder,
            sessionStateCallback = stateCallback,
        )
    }

    @JvmStatic
    public fun getCameraId(implName: String, cameraInfo: CameraInfo): String =
        if (implName == CameraPipeConfig::class.simpleName) {
            CPCamera2CameraInfo.from(cameraInfo).getCameraId()
        } else {
            Camera2CameraInfo.from(cameraInfo).cameraId
        }

    @JvmStatic
    public fun <T> getCamera2CameraInfoCharacteristics(
        implName: String,
        cameraInfo: CameraInfo,
        key: CameraCharacteristics.Key<T>,
    ): T? =
        if (implName == Camera2Config::class.simpleName) {
            Camera2CameraInfo.from(cameraInfo).getCameraCharacteristic(key)
        } else {
            CPCamera2CameraInfo.from(cameraInfo).getCameraCharacteristic(key)
        }

    public interface Camera2CameraInfoWrapper {
        public fun <T> getCameraCharacteristic(key: CameraCharacteristics.Key<T>): T?

        public companion object
    }

    public interface Camera2CameraControlWrapper {
        public fun setCaptureRequestOptions(
            bundle: CaptureRequestOptionsWrapper
        ): ListenableFuture<Void?>

        public fun clearCaptureRequestOptions(): ListenableFuture<Void?>

        public companion object
    }

    @JvmStatic
    public fun Camera2CameraInfoWrapper.Companion.from(
        implName: String,
        cameraInfo: CameraInfo,
    ): Camera2CameraInfoWrapper {
        return when (implName) {
            CameraPipeConfig::class.simpleName ->
                object : Camera2CameraInfoWrapper {
                    private val wrappedCameraInfo = CPCamera2CameraInfo.from(cameraInfo)

                    override fun <T> getCameraCharacteristic(
                        key: CameraCharacteristics.Key<T>
                    ): T? {
                        return wrappedCameraInfo.getCameraCharacteristic(key)
                    }
                }
            Camera2Config::class.simpleName ->
                object : Camera2CameraInfoWrapper {
                    private val wrappedCameraInfo = Camera2CameraInfo.from(cameraInfo)

                    override fun <T> getCameraCharacteristic(
                        key: CameraCharacteristics.Key<T>
                    ): T? {
                        return wrappedCameraInfo.getCameraCharacteristic(key)
                    }
                }
            else -> throw IllegalArgumentException("Unexpected implementation: $implName")
        }
    }

    public interface CaptureRequestOptionsWrapper {

        public fun unwrap(): Any

        public interface Builder {
            public fun <ValueT : Any> setCaptureRequestOption(
                key: CaptureRequest.Key<ValueT>,
                value: ValueT,
            ): Builder

            public fun build(): CaptureRequestOptionsWrapper
        }

        public companion object
    }

    @JvmStatic
    public fun CaptureRequestOptionsWrapper.Companion.builder(
        implName: String
    ): CaptureRequestOptionsWrapper.Builder {
        return when (implName) {
            CameraPipeConfig::class.simpleName ->
                object : CaptureRequestOptionsWrapper.Builder {
                    private val wrappedBuilder = CPCaptureRequestOptions.Builder()

                    override fun <ValueT : Any> setCaptureRequestOption(
                        key: CaptureRequest.Key<ValueT>,
                        value: ValueT,
                    ): CaptureRequestOptionsWrapper.Builder {
                        wrappedBuilder.setCaptureRequestOption(key, value)
                        return this
                    }

                    override fun build(): CaptureRequestOptionsWrapper {
                        val wrappedOptions = wrappedBuilder.build()
                        return object : CaptureRequestOptionsWrapper {
                            override public fun unwrap() = wrappedOptions
                        }
                    }
                }
            Camera2Config::class.simpleName ->
                object : CaptureRequestOptionsWrapper.Builder {
                    private val wrappedBuilder = CaptureRequestOptions.Builder()

                    override fun <ValueT : Any> setCaptureRequestOption(
                        key: CaptureRequest.Key<ValueT>,
                        value: ValueT,
                    ): CaptureRequestOptionsWrapper.Builder {
                        wrappedBuilder.setCaptureRequestOption(key, value)
                        return this
                    }

                    override fun build(): CaptureRequestOptionsWrapper {
                        val wrappedOptions = wrappedBuilder.build()
                        return object : CaptureRequestOptionsWrapper {
                            override fun unwrap() = wrappedOptions
                        }
                    }
                }
            else -> throw IllegalArgumentException("Unexpected implementation: $implName")
        }
    }

    @JvmStatic
    public fun Camera2CameraControlWrapper.Companion.from(
        implName: String,
        cameraControl: CameraControl,
    ): Camera2CameraControlWrapper {
        return when (implName) {
            CameraPipeConfig::class.simpleName ->
                object : Camera2CameraControlWrapper {
                    private val wrappedCameraControl = CPCamera2CameraControl.from(cameraControl)

                    override fun setCaptureRequestOptions(
                        bundle: CaptureRequestOptionsWrapper
                    ): ListenableFuture<Void?> {
                        return wrappedCameraControl.setCaptureRequestOptions(
                            bundle.unwrap() as CPCaptureRequestOptions
                        )
                    }

                    override fun clearCaptureRequestOptions(): ListenableFuture<Void?> {
                        return wrappedCameraControl.clearCaptureRequestOptions()
                    }
                }
            Camera2Config::class.simpleName ->
                object : Camera2CameraControlWrapper {
                    private val wrappedCameraControl = Camera2CameraControl.from(cameraControl)

                    override fun setCaptureRequestOptions(
                        bundle: CaptureRequestOptionsWrapper
                    ): ListenableFuture<Void?> {
                        return wrappedCameraControl.setCaptureRequestOptions(
                            bundle.unwrap() as CaptureRequestOptions
                        )
                    }

                    override fun clearCaptureRequestOptions(): ListenableFuture<Void?> {
                        return wrappedCameraControl.clearCaptureRequestOptions()
                    }
                }
            else -> throw IllegalArgumentException("Unexpected implementation: $implName")
        }
    }

    public class CaptureCallback(
        private val _timeout: Long = TimeUnit.SECONDS.toMillis(5),
        private val _numOfCaptures: Int = 30,
    ) : CameraCaptureSession.CaptureCallback() {

        private val waitingList: MutableList<CaptureContainer> = mutableListOf()

        /** Wait for the specified number of captures, then verify the results. */
        public suspend fun waitFor(timeout: Long = _timeout, numOfCaptures: Int = _numOfCaptures) {
            verifyFor(timeout = timeout, numOfCaptures = numOfCaptures, breakWhenSuccess = false)
        }

        /**
         * Verify the results until the specified number of captures reaches.
         *
         * @param timeout the timeout for waiting for the captures.
         * @param numOfCaptures the number of captures to wait.
         * @param verifyBlock the block for verifying the capture requests and results. It should
         *   return `true` if the requests and results is expected, otherwise `false`.
         */
        public suspend fun verifyFor(
            timeout: Long = _timeout,
            numOfCaptures: Int = _numOfCaptures,
            breakWhenSuccess: Boolean = true,
            verifyBlock:
                (
                    captureRequests: List<CaptureRequest>, captureResults: List<TotalCaptureResult>,
                ) -> Boolean =
                { _, _ ->
                    true
                },
        ) {
            val resultContainer =
                CaptureContainer(
                    count = numOfCaptures,
                    breakWhenSuccess = breakWhenSuccess,
                    verifyBlock = verifyBlock,
                )
            waitingList.add(resultContainer)
            withTimeout(timeout) { resultContainer.signal.await() }
            waitingList.remove(resultContainer)
        }

        public fun <T> verifyLastCaptureRequest(
            keyValueMap: Map<CaptureRequest.Key<T>, T>,
            numOfCaptures: Int = 30,
        ): Unit = runBlocking {
            verifyFor(numOfCaptures = numOfCaptures) { captureRequests, _ ->
                keyValueMap.forEach {
                    if (captureRequests.last()[it.key] != it.value) return@verifyFor false
                }
                true
            }
        }

        public fun <T> verifyLastCaptureResult(
            keyValueMap: Map<CaptureResult.Key<T>, T>,
            numOfCaptures: Int = 30,
        ): Unit = runBlocking {
            verifyFor(numOfCaptures = numOfCaptures) { _, captureResults ->
                keyValueMap.forEach {
                    if (captureResults.last()[it.key] != it.value) return@verifyFor false
                }
                true
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            val copyList = waitingList
            copyList.toList().forEach {
                it.apply {
                    captureRequests.add(request)
                    captureResults.add(result)
                    val success = verifyBlock(captureRequests, captureResults)
                    if (success && breakWhenSuccess) {
                        signal.complete(Unit)
                        return@forEach
                    }
                    if (count-- <= 0) {
                        if (success) {
                            signal.complete(Unit)
                        } else {
                            signal.completeExceptionally(
                                TimeoutException(
                                    "Test doesn't complete after waiting for $_numOfCaptures frames."
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    public data class CaptureContainer(
        var count: Int,
        val breakWhenSuccess: Boolean = true,
        val signal: CompletableDeferred<Unit> = CompletableDeferred(),
        val captureRequests: MutableList<CaptureRequest> = mutableListOf(),
        val captureResults: MutableList<TotalCaptureResult> = mutableListOf(),
        val verifyBlock:
            (
                captureRequests: List<CaptureRequest>, captureResults: List<TotalCaptureResult>,
            ) -> Boolean =
            { _, _ ->
                true
            },
    )
}
