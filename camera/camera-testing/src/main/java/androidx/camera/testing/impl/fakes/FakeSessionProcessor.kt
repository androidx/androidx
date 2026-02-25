/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes

import android.hardware.camera2.CameraCharacteristics
import android.os.SystemClock
import android.util.Pair
import android.util.Size
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfo
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.OutputSurfaceConfiguration
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeout

private const val FAKE_CAPTURE_SEQUENCE_ID = 1

@RequiresApi(23) // ImageWriter requires API 23+
public class FakeSessionProcessor(
    private val postviewSupportedSizes: Map<Int, List<Size>>? = null,
    private val supportedCameraOperations: Set<Int> = emptySet(),
    private val extensionSpecificChars: List<Pair<CameraCharacteristics.Key<*>, Any>>? = emptyList(),
) : SessionProcessor {

    // Values of these Deferred are the timestamp to complete.
    private val initSessionCalled = CompletableDeferred<Long>()
    private val deInitSessionCalled = CompletableDeferred<Long>()

    @AdapterCameraInfo.CameraOperation public var restrictedCameraOperations: Set<Int> = emptySet()

    @OptIn(ExperimentalGetImage::class)
    override fun initSession(
        cameraInfo: CameraInfo,
        outputSurfaceConfig: OutputSurfaceConfiguration?,
    ): SessionConfig? {
        initSessionCalled.complete(SystemClock.elapsedRealtimeNanos())
        return null
    }

    override fun deInitSession() {
        deInitSessionCalled.complete(SystemClock.elapsedRealtimeNanos())
    }

    @AdapterCameraInfo.CameraOperation
    override fun getSupportedCameraOperations(): Set<Int> {
        return supportedCameraOperations
    }

    override fun getSupportedPostviewSize(captureSize: Size): Map<Int, List<Size>> {
        return postviewSupportedSizes ?: emptyMap()
    }

    override fun getAvailableCharacteristicsKeyValues():
        List<Pair<CameraCharacteristics.Key<*>, Any>> = extensionSpecificChars!!

    public suspend fun assertInitSessionInvoked(): Long {
        return initSessionCalled.awaitWithTimeout(3000)
    }

    public suspend fun assertDeInitSessionInvoked(): Long {
        return deInitSessionCalled.awaitWithTimeout(3000)
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(timeMillis: Long): T {
        return withTimeout(timeMillis) { await() }
    }
}
