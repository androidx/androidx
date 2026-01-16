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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.fakes.FakeUseCase
import com.google.common.truth.Truth.assertThat
import javax.inject.Provider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.annotation.Config as RoboConfig
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@RoboConfig(sdk = [RoboConfig.ALL_SDKS])
class DeferredUseCaseCameraRequestControlImplTest {

    @Mock private lateinit var mockImplProvider: Provider<UseCaseCameraRequestControlImpl>

    @Mock private lateinit var mockImpl: UseCaseCameraRequestControlImpl

    private lateinit var useCaseThreads: UseCaseThreads
    private lateinit var deferredControl: DeferredUseCaseCameraRequestControl

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val sequentialExecutor =
            CameraXExecutors.newSequentialExecutor(CameraXExecutors.directExecutor())

        val sequentialDispatcher = sequentialExecutor.asCoroutineDispatcher()

        useCaseThreads = UseCaseThreads(testScope, sequentialExecutor, sequentialDispatcher)

        `when`(mockImplProvider.get()).thenReturn(mockImpl)

        deferredControl = DeferredUseCaseCameraRequestControl(mockImplProvider, useCaseThreads)
    }

    @Test
    fun initializationFailure_propagatesException() =
        testScope.runTest {
            `when`(mockImplProvider.get()).thenThrow(RuntimeException("Init failed"))
            val deferred = deferredControl.setTorchOnAsync()
            advanceUntilIdle()
            assertThat(deferred.isCompleted).isTrue()
            assertThat(deferred.getCompletionExceptionOrNull()).isNotNull()
        }

    @Test
    fun constructor_doesNotInitializeImpl() {
        // Assert: Simply creating the instance should not trigger the Provider
        verify(mockImplProvider, never()).get()
    }

    @Test
    fun setParametersAsync_initializesImplAndDelegates() =
        testScope.runTest {
            // Arrange
            val values =
                mapOf<CaptureRequest.Key<*>, Any>(
                    CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON
                )
            val deferredResult = CompletableDeferred(Unit)

            `when`(mockImpl.setParametersAsync(anyMap(), any(), any())).thenReturn(deferredResult)

            // Act
            deferredControl.setParametersAsync(
                values,
                UseCaseCameraRequestControl.Type.DEFAULT,
                Config.OptionPriority.OPTIONAL,
            )

            advanceUntilIdle() // Ensure the sequential coroutine runs

            // Assert
            verify(mockImplProvider, times(1)).get()
            verify(mockImpl)
                .setParametersAsync(
                    values,
                    UseCaseCameraRequestControl.Type.DEFAULT,
                    Config.OptionPriority.OPTIONAL,
                )
        }

    @Test
    fun subsequentCalls_doNotReinitializeImpl() =
        testScope.runTest {
            // Arrange
            `when`(mockImpl.setTorchOnAsync())
                .thenReturn(CompletableDeferred(Result3A(Result3A.Status.OK)))

            // Act
            deferredControl.setTorchOnAsync()
            advanceUntilIdle()

            deferredControl.setTorchOnAsync()
            advanceUntilIdle()

            // Assert
            verify(mockImplProvider, times(1)).get()
            verify(mockImpl, times(2)).setTorchOnAsync()
        }

    @Test
    fun updateRepeatingRequestAsync_delegatesWithCorrectArguments() =
        testScope.runTest {
            // Arrange
            val fakeUseCase = FakeUseCase()
            val runningUseCases = listOf(fakeUseCase)

            `when`(mockImpl.updateRepeatingRequestAsync(anyBoolean(), anyList()))
                .thenReturn(CompletableDeferred(Unit))

            // Act
            deferredControl.updateRepeatingRequestAsync(
                isPrimary = true,
                runningUseCases = runningUseCases,
            )
            advanceUntilIdle()

            // Assert
            verify(mockImpl).updateRepeatingRequestAsync(true, runningUseCases)
        }

    @Test
    fun issueSingleCaptureAsync_delegatesAndReturnsMappedDeferreds() =
        testScope.runTest {
            // Arrange
            val captureConfig = CaptureConfig.Builder().build()
            val sequence = listOf(captureConfig, captureConfig)

            val mockDeferred1 = CompletableDeferred<Void?>().apply { complete(null) }
            val mockDeferred2 = CompletableDeferred<Void?>().apply { complete(null) }
            val implDeferreds = listOf(mockDeferred1, mockDeferred2)

            `when`(mockImpl.issueSingleCaptureAsync(anyList(), anyInt(), anyInt(), anyInt()))
                .thenReturn(implDeferreds)

            // Act
            val resultDeferreds = deferredControl.issueSingleCaptureAsync(sequence, 0, 0, 0)
            advanceUntilIdle()

            // Assert
            verify(mockImplProvider).get()
            verify(mockImpl).issueSingleCaptureAsync(sequence, 0, 0, 0)

            assertThat(resultDeferreds).hasSize(2)
            assertThat(resultDeferreds[0].isCompleted).isTrue()
        }

    @Test
    fun close_callsCloseOnImpl_onlyIfInitialized() =
        testScope.runTest {
            // Arrange: Initialize it first
            `when`(mockImpl.setTorchOnAsync())
                .thenReturn(CompletableDeferred(Result3A(Result3A.Status.OK)))
            deferredControl.setTorchOnAsync()
            advanceUntilIdle()

            // Act
            deferredControl.close()
            advanceUntilIdle()

            // Assert
            verify(mockImpl).close()
        }

    @Test
    fun close_doesNotInitializeImpl_ifNotCalledBefore() =
        testScope.runTest {
            // Act
            deferredControl.close()
            advanceUntilIdle()

            // Assert
            verify(mockImplProvider, never()).get()
        }

    @Test
    fun awaitSurfaceSetup_initializesAndDelegates() =
        testScope.runTest {
            // Arrange
            `when`(mockImpl.awaitSurfaceSetup()).thenReturn(true)

            // Act
            val result = deferredControl.awaitSurfaceSetup()

            // Assert
            verify(mockImplProvider).get()
            verify(mockImpl).awaitSurfaceSetup()
            assertThat(result).isTrue()
        }

    @Test
    fun submitParameters_runsOnSequentialThread() =
        testScope.runTest {
            val values = mapOf<CaptureRequest.Key<*>, Any>()

            `when`(mockImpl.submitParameters(anyMap(), any(), any()))
                .thenReturn(CompletableDeferred(Unit))

            deferredControl.submitParameters(
                values,
                UseCaseCameraRequestControl.Type.DEFAULT,
                Config.OptionPriority.OPTIONAL,
            )
            advanceUntilIdle()

            verify(mockImplProvider).get()
            verify(mockImpl)
                .submitParameters(
                    values,
                    UseCaseCameraRequestControl.Type.DEFAULT,
                    Config.OptionPriority.OPTIONAL,
                )
        }
}
