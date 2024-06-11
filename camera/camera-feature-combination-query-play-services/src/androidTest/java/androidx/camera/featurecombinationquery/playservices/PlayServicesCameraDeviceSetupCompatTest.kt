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

package androidx.camera.featurecombinationquery.playservices

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.params.SessionConfiguration
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.SOURCE_PLAY_SERVICES
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class PlayServicesCameraDeviceSetupCompatTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun queryResult_resultSourceIsPlayServices() {
        // Arrange.
        val factory = CameraDeviceSetupCompatFactory(instrumentation.context)
        val impl = factory.getCameraDeviceSetupCompat("1")
        val sessionConfiguration =
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(),
                directExecutor(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(p0: CameraCaptureSession) {
                        // no-op
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        // no-op
                    }
                }
            )
        // Act.
        val result = impl.isSessionConfigurationSupported(sessionConfiguration)
        // Assert.
        assertThat(result.source).isEqualTo(SOURCE_PLAY_SERVICES)
        assertThat(result.supported).isEqualTo(SupportQueryResult.RESULT_UNSUPPORTED)
        assertThat(result.timestampMillis).isEqualTo(0)
    }
}
