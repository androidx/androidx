/*
 * Copyright 2021 The Android Open Source Project
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

@file:RequiresApi(21)

package androidx.camera.camera2.pipe.integration.adapter

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.media.MediaCodec
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.impl.STREAM_USE_CASE_OPTION
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import junit.framework.TestCase
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class SessionConfigAdapterTest {

    private val mockSurface = Mockito.mock(DeferrableSurface::class.java)
    private val mockSessionConfig = Mockito.mock(SessionConfig::class.java)
    private val mockImplementationOption =
        Mockito.mock(androidx.camera.core.impl.Config::class.java)
    private val sessionConfigAdapter = SessionConfigAdapter(listOf())

    @get:Rule
    val dispatcherRule = MainDispatcherRule(MoreExecutors.directExecutor().asCoroutineDispatcher())

    @Test
    fun invalidSessionConfig() {
        // Arrange
        val testDeferrableSurface = createTestDeferrableSurface()

        // Create an invalid SessionConfig which doesn't set the template
        val fakeTestUseCase = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.addSurface(testDeferrableSurface)
                }
            )
        }

        // Act
        val sessionConfigAdapter = SessionConfigAdapter(
            useCases = listOf(fakeTestUseCase)
        )

        // Assert
        assertThat(sessionConfigAdapter.isSessionConfigValid()).isFalse()
        assertThat(sessionConfigAdapter.getValidSessionConfigOrNull()).isNull()

        // Clean up
        testDeferrableSurface.close()
    }

    @Test
    fun reportInvalidSurfaceTest() {
        // Arrange
        val testDeferrableSurface = createTestDeferrableSurface().apply { close() }

        val errorListener = object : SessionConfig.ErrorListener {
            val results = mutableListOf<Pair<SessionConfig, SessionConfig.SessionError>>()
            override fun onError(sessionConfig: SessionConfig, error: SessionConfig.SessionError) {
                results.add(Pair(sessionConfig, error))
            }
        }

        val fakeTestUseCase1 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(testDeferrableSurface)
                    sessionConfigBuilder.addErrorListener(errorListener)
                }
            )
        }
        val fakeTestUseCase2 = createFakeTestUseCase {
            it.setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    sessionConfigBuilder.addSurface(createTestDeferrableSurface().apply { close() })
                    sessionConfigBuilder.addErrorListener(errorListener)
                }
            )
        }

        // Act
        SessionConfigAdapter(
            useCases = listOf(fakeTestUseCase1, fakeTestUseCase2)
        ).reportSurfaceInvalid(testDeferrableSurface)

        // Assert, verify it only reports the SURFACE_NEEDS_RESET error on one SessionConfig
        // at a time.
        assertThat(errorListener.results.size).isEqualTo(1)
        assertThat(errorListener.results[0].second).isEqualTo(
            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
        )
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingEmptyUseCase() {
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(listOf(), true)
        TestCase.assertTrue(mapping.isEmpty())
    }

    @Test
    fun populateSurfaceToStreamUseHintMappingEmptyUseCase() {
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseHintMapping(listOf())
        TestCase.assertTrue(mapping.isEmpty())
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingNoAppropriateContainerClass() {
        Mockito.`when`(mockSurface.containerClass).thenReturn(FakeUseCase::class.java)
        Mockito.`when`(mockSessionConfig.surfaces).thenReturn(listOf(mockSurface))
        Mockito.`when`(mockSessionConfig.implementationOptions).thenReturn(mockImplementationOption)
        val sessionConfigs: MutableCollection<SessionConfig> = ArrayList()
        sessionConfigs.add(mockSessionConfig)
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(sessionConfigs, true)
        TestCase.assertTrue(mapping[mockSurface] == 0L)
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingPreview() {
        Mockito.`when`(mockSurface.containerClass).thenReturn(Preview::class.java)
        Mockito.`when`(mockSessionConfig.surfaces).thenReturn(listOf(mockSurface))
        Mockito.`when`(mockSessionConfig.implementationOptions).thenReturn(mockImplementationOption)
        val sessionConfigs: MutableCollection<SessionConfig> = ArrayList()
        sessionConfigs.add(mockSessionConfig)
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(sessionConfigs, true)
        TestCase.assertTrue(mapping.isNotEmpty())
        TestCase.assertTrue(mapping[mockSurface] == 1L)
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingZSL() {
        Mockito.`when`(mockSurface.containerClass).thenReturn(Preview::class.java)
        Mockito.`when`(mockSessionConfig.surfaces).thenReturn(listOf(mockSurface))
        Mockito.`when`(mockSessionConfig.implementationOptions)
            .thenReturn(mockImplementationOption)
        Mockito.`when`(mockSessionConfig.templateType)
            .thenReturn(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
        val sessionConfigs: MutableCollection<SessionConfig> = ArrayList()
        sessionConfigs.add(mockSessionConfig)
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(sessionConfigs, true)
        TestCase.assertTrue(mapping.isEmpty())
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingImageAnalysis() {
        Mockito.`when`(mockSurface.containerClass).thenReturn(ImageAnalysis::class.java)
        Mockito.`when`(mockSessionConfig.surfaces).thenReturn(listOf(mockSurface))
        Mockito.`when`(mockSessionConfig.implementationOptions)
            .thenReturn(mockImplementationOption)
        val sessionConfigs: MutableCollection<SessionConfig> = ArrayList()
        sessionConfigs.add(mockSessionConfig)
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(sessionConfigs, true)
        TestCase.assertTrue(mapping.isNotEmpty())
        TestCase.assertTrue(mapping[mockSurface] == 1L)
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingImageCapture() {
        Mockito.`when`(mockSurface.containerClass).thenReturn(ImageCapture::class.java)
        Mockito.`when`(mockSessionConfig.surfaces).thenReturn(listOf(mockSurface))
        Mockito.`when`(mockSessionConfig.implementationOptions).thenReturn(mockImplementationOption)
        val sessionConfigs: MutableCollection<SessionConfig> = ArrayList()
        sessionConfigs.add(mockSessionConfig)
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(sessionConfigs, true)
        TestCase.assertTrue(mapping.isNotEmpty())
        TestCase.assertTrue(mapping[mockSurface] == 2L)
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingVideoCapture() {
        Mockito.`when`(mockSurface.containerClass).thenReturn(MediaCodec::class.java)
        Mockito.`when`(mockSessionConfig.surfaces).thenReturn(listOf(mockSurface))
        Mockito.`when`(mockSessionConfig.implementationOptions).thenReturn(mockImplementationOption)
        val sessionConfigs: MutableCollection<SessionConfig> = ArrayList()
        sessionConfigs.add(mockSessionConfig)
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(sessionConfigs, true)
        TestCase.assertTrue(mapping.isNotEmpty())
        TestCase.assertTrue(mapping[mockSurface] == 3L)
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingStreamSharing() {
        Mockito.`when`(mockSurface.containerClass).thenReturn(StreamSharing::class.java)
        Mockito.`when`(mockSessionConfig.surfaces).thenReturn(listOf(mockSurface))
        Mockito.`when`(mockSessionConfig.implementationOptions).thenReturn(mockImplementationOption)
        val sessionConfigs: MutableCollection<SessionConfig> = ArrayList()
        sessionConfigs.add(mockSessionConfig)
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(sessionConfigs, true)
        TestCase.assertTrue(mapping.isNotEmpty())
        TestCase.assertTrue(mapping[mockSurface] == 3L)
    }

    @Test
    fun populateSurfaceToStreamUseCaseMappingCustomized() {
        Mockito.`when`(mockSurface.containerClass).thenReturn(MediaCodec::class.java)
        Mockito.`when`(mockSessionConfig.surfaces).thenReturn(listOf(mockSurface))
        Mockito.`when`(mockSessionConfig.implementationOptions)
            .thenReturn(mockImplementationOption)
        Mockito.`when`(mockImplementationOption.containsOption(STREAM_USE_CASE_OPTION))
            .thenReturn(true)
        Mockito.`when`(mockImplementationOption.retrieveOption(STREAM_USE_CASE_OPTION))
            .thenReturn(0L)
        val sessionConfigs: MutableCollection<SessionConfig> = ArrayList()
        sessionConfigs.add(mockSessionConfig)
        val mapping = sessionConfigAdapter.getSurfaceToStreamUseCaseMapping(sessionConfigs, true)
        TestCase.assertTrue(mapping.isNotEmpty())
        TestCase.assertTrue(mapping[mockSurface] == 0L)
    }

    private fun createFakeTestUseCase(block: (FakeTestUseCase) -> Unit): FakeTestUseCase = run {
        val configBuilder = FakeUseCaseConfig.Builder().setTargetName("UseCase")
        FakeTestUseCase(configBuilder.useCaseConfig).also {
            block(it)
        }
    }

    private fun createTestDeferrableSurface(): TestDeferrableSurface = run {
        TestDeferrableSurface().also {
            it.terminationFuture.addListener({ it.cleanUp() }, MoreExecutors.directExecutor())
        }
    }
}

class FakeTestUseCase(
    config: FakeUseCaseConfig,
) : FakeUseCase(config) {

    fun setupSessionConfig(sessionConfigBuilder: SessionConfig.Builder) {
        updateSessionConfig(sessionConfigBuilder.build())
        notifyActive()
    }
}

class TestDeferrableSurface : DeferrableSurface() {
    private val surfaceTexture = SurfaceTexture(0).also {
        it.setDefaultBufferSize(0, 0)
    }
    val testSurface = Surface(surfaceTexture)

    override fun provideSurface(): ListenableFuture<Surface> {
        return Futures.immediateFuture(testSurface)
    }

    fun cleanUp() {
        testSurface.release()
        surfaceTexture.release()
    }
}
