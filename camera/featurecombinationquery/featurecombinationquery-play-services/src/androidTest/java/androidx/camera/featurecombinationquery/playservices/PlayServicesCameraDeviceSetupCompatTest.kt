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

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.SOURCE_PLAY_SERVICES
import androidx.camera.featurecombinationquery.SessionConfigurationLegacy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.camera.feature.combination.query.CombinationQuery
import com.google.android.gms.camera.feature.combination.query.QueryResult
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class PlayServicesCameraDeviceSetupCompatTest {
    companion object {
        private const val RETURN_TIMESTAMP_MS = 123456L
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var queryClient: CombinationQuery

    @Test
    fun queryResult_resultUnknownIsMappedCorrectly() {
        // Arrange.
        queryClient = mock<CombinationQuery>()
        whenever(
                queryClient.isSessionConfigurationSupported(
                    /*cameraId=*/ any(),
                    /*sessionConfig=*/ any(),
                )
            )
            .thenReturn(QueryResult(QueryResult.Result.UNKNOWN, RETURN_TIMESTAMP_MS))
        val impl = PlayServicesCameraDeviceSetupCompat(queryClient, "0")
        val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.PRIVATE, 1)
        val sessionConfiguration =
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(OutputConfiguration(imageReader.surface)),
                directExecutor(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(p0: CameraCaptureSession) {
                        // no-op
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        // no-op
                    }
                },
            )
        // Act.
        val result = impl.isSessionConfigurationSupported(sessionConfiguration)
        // Assert.
        assertThat(result.source).isEqualTo(SOURCE_PLAY_SERVICES)
        assertThat(result.supported).isEqualTo(SupportQueryResult.RESULT_UNDEFINED)
        assertThat(result.timestampMillis).isEqualTo(RETURN_TIMESTAMP_MS)
    }

    @Test
    fun queryResultLegacy_resultUnknownIsMappedCorrectly() {
        // Arrange.
        queryClient = mock<CombinationQuery>()
        whenever(
                queryClient.isSessionConfigurationSupportedLegacy(
                    /*cameraId=*/ any(),
                    /*outputConfigs=*/ any(),
                    /*sessionParams=*/ any(),
                )
            )
            .thenReturn(QueryResult(QueryResult.Result.UNKNOWN, RETURN_TIMESTAMP_MS))
        val impl = PlayServicesCameraDeviceSetupCompat(queryClient, "0")
        val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.PRIVATE, 1)
        val sessionConfig =
            SessionConfigurationLegacy.Builder()
                .addOutputConfiguration(OutputConfiguration(imageReader.surface))
                .build()
        // Act.
        val result = impl.isSessionConfigurationSupportedLegacy(sessionConfig)
        // Assert.
        assertThat(result.source).isEqualTo(SOURCE_PLAY_SERVICES)
        assertThat(result.supported).isEqualTo(SupportQueryResult.RESULT_UNDEFINED)
        assertThat(result.timestampMillis).isEqualTo(RETURN_TIMESTAMP_MS)
    }

    @Test
    fun queryResult_resultSupportedIsMappedCorrectly() {
        // Arrange.
        queryClient = mock<CombinationQuery>()
        whenever(
                queryClient.isSessionConfigurationSupported(
                    /*cameraId=*/ any(),
                    /*sessionConfig=*/ any(),
                )
            )
            .thenReturn(QueryResult(QueryResult.Result.SUPPORTED, RETURN_TIMESTAMP_MS))
        val impl = PlayServicesCameraDeviceSetupCompat(queryClient, "0")
        val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.PRIVATE, 1)
        val sessionConfiguration =
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(OutputConfiguration(imageReader.surface)),
                directExecutor(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(p0: CameraCaptureSession) {
                        // no-op
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        // no-op
                    }
                },
            )
        // Act.
        val result = impl.isSessionConfigurationSupported(sessionConfiguration)
        // Assert.
        assertThat(result.source).isEqualTo(SOURCE_PLAY_SERVICES)
        assertThat(result.supported).isEqualTo(SupportQueryResult.RESULT_SUPPORTED)
        assertThat(result.timestampMillis).isEqualTo(RETURN_TIMESTAMP_MS)
    }

    @Test
    fun queryResultLegacy_resultSupportedIsMappedCorrectly() {
        // Arrange.
        queryClient = mock<CombinationQuery>()
        whenever(
                queryClient.isSessionConfigurationSupportedLegacy(
                    /*cameraId=*/ any(),
                    /*outputConfigs=*/ any(),
                    /*sessionParams=*/ any(),
                )
            )
            .thenReturn(QueryResult(QueryResult.Result.SUPPORTED, RETURN_TIMESTAMP_MS))
        val impl = PlayServicesCameraDeviceSetupCompat(queryClient, "0")
        val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.PRIVATE, 1)
        val sessionConfig =
            SessionConfigurationLegacy.Builder()
                .addOutputConfiguration(OutputConfiguration(imageReader.surface))
                .build()
        // Act.
        val result = impl.isSessionConfigurationSupportedLegacy(sessionConfig)
        // Assert.
        assertThat(result.source).isEqualTo(SOURCE_PLAY_SERVICES)
        assertThat(result.supported).isEqualTo(SupportQueryResult.RESULT_SUPPORTED)
        assertThat(result.timestampMillis).isEqualTo(RETURN_TIMESTAMP_MS)
    }

    @Test
    fun queryResult_resultUnSupportedIsMappedCorrectly() {
        // Arrange.
        queryClient = mock<CombinationQuery>()
        whenever(
                queryClient.isSessionConfigurationSupported(
                    /*cameraId=*/ any(),
                    /*sessionConfig=*/ any(),
                )
            )
            .thenReturn(QueryResult(QueryResult.Result.NOT_SUPPORTED, RETURN_TIMESTAMP_MS))
        val impl = PlayServicesCameraDeviceSetupCompat(queryClient, "0")
        val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.PRIVATE, 1)
        val sessionConfiguration =
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(OutputConfiguration(imageReader.surface)),
                directExecutor(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(p0: CameraCaptureSession) {
                        // no-op
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        // no-op
                    }
                },
            )
        // Act.
        val result = impl.isSessionConfigurationSupported(sessionConfiguration)
        // Assert.
        assertThat(result.source).isEqualTo(SOURCE_PLAY_SERVICES)
        assertThat(result.supported).isEqualTo(SupportQueryResult.RESULT_UNSUPPORTED)
        assertThat(result.timestampMillis).isEqualTo(RETURN_TIMESTAMP_MS)
    }

    @Test
    fun queryResultLegacy_resultUnSupportedIsMappedCorrectly() {
        // Arrange.
        queryClient = mock<CombinationQuery>()
        whenever(
                queryClient.isSessionConfigurationSupportedLegacy(
                    /*cameraId=*/ any(),
                    /*outputConfigs=*/ any(),
                    /*sessionParams=*/ any(),
                )
            )
            .thenReturn(QueryResult(QueryResult.Result.NOT_SUPPORTED, RETURN_TIMESTAMP_MS))
        val impl = PlayServicesCameraDeviceSetupCompat(queryClient, "0")
        val imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.PRIVATE, 1)
        val sessionConfig =
            SessionConfigurationLegacy.Builder()
                .addOutputConfiguration(OutputConfiguration(imageReader.surface))
                .build()
        // Act.
        val result = impl.isSessionConfigurationSupportedLegacy(sessionConfig)
        // Assert.
        assertThat(result.source).isEqualTo(SOURCE_PLAY_SERVICES)
        assertThat(result.supported).isEqualTo(SupportQueryResult.RESULT_UNSUPPORTED)
        assertThat(result.timestampMillis).isEqualTo(RETURN_TIMESTAMP_MS)
    }
}
