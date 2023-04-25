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

package androidx.camera.extensions.internal.compat.workaround

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.util.Pair
import android.util.Size
import androidx.camera.extensions.impl.CaptureStageImpl
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = arrayOf("androidx.camera.extensions.internal")
)
class AvailableKeysRetrieverTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val availableKeys = listOf<CaptureRequest.Key<out Any>>(
        CaptureRequest.CONTROL_AE_REGIONS, CaptureRequest.CONTROL_AF_MODE
    )
    private val fakeImageCaptureExtenderImpl = FakeImageCaptureExtenderImpl(availableKeys)
    private val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()

    @Test
    fun shouldInvokeOnInit() {
        // 1. Arrange
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "SAMSUNG")
        val retriever = AvailableKeysRetriever()

        // 2. Act
        val resultKeys = retriever.getAvailableCaptureRequestKeys(
            fakeImageCaptureExtenderImpl, "0", characteristics, context)

        // 3. Assert
        assertThat(resultKeys).containsExactlyElementsIn(availableKeys)
        assertThat(fakeImageCaptureExtenderImpl.invokeList).containsExactly(
            "onInit", "getAvailableCaptureRequestKeys", "onDeInit"
        )
    }

    @Test
    fun shouldNotInvokeOnInit() {
        // 1. Arrange
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "OTHER")
        val retriever = AvailableKeysRetriever()

        // 2. Act
        val resultKeys = retriever.getAvailableCaptureRequestKeys(
            fakeImageCaptureExtenderImpl, "0", characteristics, context)

        // 3. Assert
        assertThat(resultKeys).containsExactlyElementsIn(availableKeys)
        assertThat(fakeImageCaptureExtenderImpl.invokeList).containsExactly(
            "getAvailableCaptureRequestKeys"
        )
    }

    class FakeImageCaptureExtenderImpl(
        private var availableRequestKeys: List<CaptureRequest.Key<out Any>>
    ) : ImageCaptureExtenderImpl {
        val invokeList = mutableListOf<String>()
        override fun isExtensionAvailable(
            cameraId: String,
            cameraCharacteristics: CameraCharacteristics
        ): Boolean = true
        override fun init(cameraId: String, cameraCharacteristics: CameraCharacteristics) {
            invokeList.add("init")
        }
        override fun getCaptureProcessor() = null
        override fun getCaptureStages(): List<CaptureStageImpl> = emptyList()
        override fun getMaxCaptureStage() = 2
        override fun getSupportedResolutions() = null
        override fun getEstimatedCaptureLatencyRange(size: Size?) = null
        override fun getAvailableCaptureRequestKeys(): List<CaptureRequest.Key<out Any>> {
            invokeList.add("getAvailableCaptureRequestKeys")

            return availableRequestKeys
        }

        override fun getAvailableCaptureResultKeys(): List<CaptureResult.Key<Any>> {
            return mutableListOf()
        }

        override fun getSupportedPostviewResolutions(
            captureSize: Size
        ): MutableList<Pair<Int, Array<Size>>>? = null

        override fun isCaptureProcessProgressAvailable() = false

        override fun getRealtimeCaptureLatency(): Pair<Long, Long>? = null
        override fun isPostviewAvailable() = false
        override fun onInit(
            cameraId: String,
            cameraCharacteristics: CameraCharacteristics,
            context: Context
        ) {
            invokeList.add("onInit")
        }

        override fun onDeInit() {
            invokeList.add("onDeInit")
        }
        override fun onPresetSession(): CaptureStageImpl? = null
        override fun onEnableSession(): CaptureStageImpl? = null
        override fun onDisableSession(): CaptureStageImpl? = null
        override fun onSessionType(): Int = SessionConfiguration.SESSION_REGULAR
    }
}