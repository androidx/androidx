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
package androidx.camera.camera2.internal

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat.CAMERA_UNAVAILABLE_DO_NOT_DISTURB
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.CameraState.ERROR_CAMERA_IN_USE
import androidx.camera.core.CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED
import androidx.camera.core.CameraState.create
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraStateRegistry
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.MainThreadAsyncHandler
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeCamera
import androidx.core.os.HandlerCompat
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.AfterClass
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
internal class Camera2CameraImplStateTest {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    private lateinit var cameraId: String
    private lateinit var camera: Camera2CameraImpl
    private lateinit var cameraStateRegistry: CameraStateRegistry

    @Before
    fun setCameraId() {
        val nullableCameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK)
        assumeFalse("Device doesn't have an available back facing camera", nullableCameraId == null)
        cameraId = nullableCameraId!!
    }

    @After
    fun releaseCameraResources() {
        if (::camera.isInitialized) {
            camera.release().get()
        }
    }

    @Test
    fun shouldEmitClosedStateInitially() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        assertCameraStateAfterAction(
            action = {},
            expectedState = create(CameraState.Type.CLOSED)
        )
    }

    @Test
    fun shouldEmitPendingOpenState_whenOpeningClosedCamera_andCameraUnavailable() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        // Open fake camera
        val fakeCamera = FakeCamera()
        cameraStateRegistry.registerCamera(fakeCamera, CameraXExecutors.directExecutor(), {})
        cameraStateRegistry.tryOpenCamera(fakeCamera)
        cameraStateRegistry.markCameraState(fakeCamera, CameraInternal.State.OPEN)

        // Try to open camera. This should be prevented since fakeCamera is already open.
        assertCameraStateAfterAction(
            action = { camera.open() },
            expectedState = create(CameraState.Type.PENDING_OPEN)
        )
    }

    @Test
    fun shouldEmitOpeningState_whenOpeningClosedCamera_andCameraAvailable() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        assertCameraStateAfterAction(
            action = { camera.open() },
            expectedState = create(CameraState.Type.OPENING)
        )
    }

    @Test
    fun shouldEmitOpenState_whenCameraOpened() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        assertCameraStateAfterAction(
            action = {
                camera.open()
                camera.awaitCameraOpen()
            },
            expectedState = create(CameraState.Type.OPEN)
        )
    }

    @Test
    fun shouldEmitClosedState_afterOpeningCameraThrowsDNDException() {
        val cameraManager = TestCameraManager(
            onOpenCamera = {
                throw CameraAccessExceptionCompat(CAMERA_UNAVAILABLE_DO_NOT_DISTURB)
            }
        )
        initializeCamera(cameraManager)

        assertCameraStateAfterAction(
            action = { camera.open() },
            expectedStatePredicate = { state ->
                state.type == CameraState.Type.CLOSED &&
                    state.error?.code == ERROR_DO_NOT_DISTURB_MODE_ENABLED
            }
        )
    }

    @Test
    fun shouldEmitOpeningState_whenOpeningCameraThrowsSecurityException() {
        val cameraManager = TestCameraManager(onOpenCamera = { throw SecurityException() })
        initializeCamera(cameraManager)

        assertCameraStateAfterAction(
            action = { camera.open() },
            expectedState = create(CameraState.Type.OPENING)
        )
    }

    @Test
    fun shouldEmitOpeningState_whenOpeningCameraEncountersRecoverableError() {
        val cameraManager = TestCameraManager(onOpenCamera = { })
        initializeCamera(cameraManager)

        assertCameraStateAfterAction(
            action = {
                camera.open()
                cameraManager.triggerRecoverableError()
            },
            expectedStatePredicate = { state ->
                state.type == CameraState.Type.OPENING && state.error != null
            }
        )

        // Clean up
        camera.close()
        cameraManager.triggerClose()
    }

    @Test
    fun shouldEmitClosingState_whenOpeningCameraEncountersCriticalError() {
        val cameraManager = TestCameraManager(onOpenCamera = {})
        initializeCamera(cameraManager)

        assertCameraStateAfterAction(
            action = {
                camera.open()
                cameraManager.triggerCriticalError()
            },
            expectedStatePredicate = { state ->
                state.type == CameraState.Type.CLOSING && state.error != null
            }
        )

        // Clean up
        camera.close()
        cameraManager.triggerClose()
    }

    @Test
    fun shouldEmitPendingOpenState_afterReachingMaxReopenAttempts() {
        val semaphore = Semaphore(0)
        val cameraManager = TestCameraManager(
            onOpenCamera = {
                semaphore.release()
                throw SecurityException()
            }
        )
        initializeCamera(cameraManager)

        assertCameraStateAfterAction(
            action = {
                camera.open()
                awaitMaxReopenAttemptsReached(semaphore)
            },
            expectedState = create(CameraState.Type.PENDING_OPEN)
        )
    }

    @Test
    fun shouldEmitOpeningState_whenOpenCameraEncountersRecoverableError() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        camera.open()
        camera.awaitCameraOpen()

        assertCameraStateAfterAction(
            action = { cameraManager.triggerDisconnect() },
            expectedState = create(
                CameraState.Type.OPENING,
                CameraState.StateError.create(ERROR_CAMERA_IN_USE)
            )
        )

        // Clean up
        camera.close()
        cameraManager.triggerClose()
    }

    @Test
    fun shouldEmitClosingState_whenOpenCameraEncountersCriticalError() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        camera.open()
        camera.awaitCameraOpen()

        assertCameraStateAfterAction(
            action = { cameraManager.triggerCriticalError() },
            expectedStatePredicate = { state ->
                state.type == CameraState.Type.CLOSING && state.error != null
            }
        )

        // Clean up
        camera.close()
        cameraManager.triggerClose()
    }

    @Test
    fun shouldEmitClosingState_whenOpenCameraGetsCloseSignal() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        camera.open()
        camera.awaitCameraOpen()

        assertCameraStateAfterAction(
            action = { camera.close() },
            expectedState = create(CameraState.Type.CLOSING)
        )
    }

    @Test
    fun shouldEmitClosedState_whenCameraClosed() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        camera.open()
        camera.awaitCameraOpen()

        assertCameraStateAfterAction(
            action = {
                camera.close()
                camera.awaitCameraClosed()
            },
            expectedState = create(CameraState.Type.CLOSED)
        )
    }

    @Test
    fun shouldEmitOpeningState_whenPendingOpenCameraReceivesOpenSignal() {
        val cameraManager = TestCameraManager()
        initializeCamera(cameraManager)

        // Open fake camera
        val fakeCamera = FakeCamera()
        cameraStateRegistry.registerCamera(fakeCamera, CameraXExecutors.directExecutor(), {})
        cameraStateRegistry.tryOpenCamera(fakeCamera)
        cameraStateRegistry.markCameraState(fakeCamera, CameraInternal.State.OPEN)

        // Try to open camera. This should be prevented since fakeCamera is already open.
        assertCameraStateAfterAction(
            action = { camera.open() },
            expectedState = create(CameraState.Type.PENDING_OPEN)
        )

        // The camera should start opening when fakeCamera is closed
        assertCameraStateAfterAction(
            action = {
                // Close fake camera
                fakeCamera.close()
                cameraStateRegistry.markCameraState(fakeCamera, CameraInternal.State.CLOSED)
            },
            expectedState = create(CameraState.Type.OPENING)
        )
    }

    private fun initializeCamera(cameraManager: TestCameraManager) {
        // Build camera manager wrapper
        val cameraManagerCompat = CameraManagerCompat.from(cameraManager)

        // Build camera info
        val camera2CameraInfo = Camera2CameraInfoImpl(
            cameraId,
            cameraManagerCompat
        )

        // Initialize camera state registry and only allow 1 open camera at most inside CameraX
        cameraStateRegistry = CameraStateRegistry(1)

        // Initialize camera instance
        camera = Camera2CameraImpl(
            cameraManagerCompat,
            cameraId,
            camera2CameraInfo,
            cameraStateRegistry,
            CameraXExecutors.directExecutor(),
            cameraHandler
        )
    }

    private fun Camera2CameraImpl.awaitCameraOpen() {
        awaitInternalState(CameraInternal.State.OPEN)
    }

    private fun Camera2CameraImpl.awaitCameraClosed() {
        awaitInternalState(CameraInternal.State.CLOSED)
    }

    private fun Camera2CameraImpl.awaitInternalState(state: CameraInternal.State) = runBlocking {
        val receivedState = CompletableDeferred<Unit>()
        val observer = object : Observable.Observer<CameraInternal.State> {
            override fun onNewData(value: CameraInternal.State?) {
                if (value == state) {
                    receivedState.complete(Unit)
                }
            }

            override fun onError(t: Throwable) {
                // No-op
            }
        }
        cameraState.addObserver(CameraXExecutors.directExecutor(), observer)

        try {
            withTimeout(CAMERA_OPEN_CLOSE_WAIT) { receivedState.await() }
        } finally {
            cameraState.removeObserver(observer)
        }
    }

    private fun awaitMaxReopenAttemptsReached(semaphore: Semaphore) {
        while (true) {
            val cameraOpenAttempted =
                semaphore.tryAcquire(CAMERA_REOPEN_WAIT, TimeUnit.MILLISECONDS)
            if (!cameraOpenAttempted) {
                return
            }
        }
    }

    private fun assertCameraStateAfterAction(action: () -> Unit, expectedState: CameraState) {
        assertCameraStateAfterAction(action, { state -> state == expectedState })
    }

    private fun assertCameraStateAfterAction(
        action: () -> Unit,
        expectedStatePredicate: ((CameraState) -> Boolean)
    ) = runBlocking {
        val nextStateReceived = CompletableDeferred<Unit>()
        val stateObserver = Observer<CameraState> { state ->
            if (expectedStatePredicate.invoke(state)) {
                nextStateReceived.complete(Unit)
            }
        }

        withContext(Dispatchers.Main) {
            camera.cameraInfo.cameraState.observeForever(stateObserver)
        }

        action.invoke()

        try {
            withTimeout(CAMERA_STATE_WAIT) { nextStateReceived.await() }
        } finally {
            withContext(Dispatchers.Main) {
                camera.cameraInfo.cameraState.removeObserver(stateObserver)
            }
        }
    }

    class TestCameraManager(private val onOpenCamera: (() -> Unit)? = null) :
        CameraManagerCompat.CameraManagerCompatImpl {

        private val forwardCameraManager = CameraManagerCompat.CameraManagerCompatImpl.from(
            ApplicationProvider.getApplicationContext(),
            MainThreadAsyncHandler.getInstance()
        )
        private var stateCallback: CameraDevice.StateCallback? = null

        override fun getCameraIdList(): Array<String> {
            return forwardCameraManager.cameraIdList
        }

        override fun registerAvailabilityCallback(
            executor: Executor,
            callback: CameraManager.AvailabilityCallback
        ) {
            // No-op
        }

        override fun unregisterAvailabilityCallback(callback: CameraManager.AvailabilityCallback) {
            // No-op
        }

        override fun getCameraCharacteristics(cameraId: String): CameraCharacteristics {
            return forwardCameraManager.getCameraCharacteristics(cameraId)
        }

        override fun openCamera(
            cameraId: String,
            executor: Executor,
            callback: CameraDevice.StateCallback
        ) {
            stateCallback = callback
            if (onOpenCamera == null) {
                forwardCameraManager.openCamera(cameraId, executor, callback)
            } else {
                onOpenCamera.invoke()
            }
        }

        override fun getCameraManager(): CameraManager {
            return forwardCameraManager.cameraManager
        }

        fun triggerRecoverableError() {
            stateCallback?.onError(
                Mockito.mock(CameraDevice::class.java),
                CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE
            )
        }

        fun triggerCriticalError() {
            stateCallback?.onError(
                Mockito.mock(CameraDevice::class.java),
                CameraDevice.StateCallback.ERROR_CAMERA_DISABLED
            )
        }

        fun triggerDisconnect() {
            stateCallback?.onDisconnected(Mockito.mock(CameraDevice::class.java))
        }

        fun triggerClose() {
            stateCallback?.onClosed(Mockito.mock(CameraDevice::class.java))
        }
    }

    companion object {
        private const val CAMERA_OPEN_CLOSE_WAIT = 5000.toLong() // 5 seconds
        private const val CAMERA_REOPEN_WAIT = 3000.toLong() // 3 seconds
        private const val CAMERA_STATE_WAIT = 1000.toLong() // 1 second

        private lateinit var cameraHandlerThread: HandlerThread
        private lateinit var cameraHandler: Handler

        @JvmStatic
        @BeforeClass
        fun classSetup() {
            cameraHandlerThread = HandlerThread("CameraThread")
            cameraHandlerThread.start()
            cameraHandler = HandlerCompat.createAsync(cameraHandlerThread.looper)
        }

        @JvmStatic
        @AfterClass
        fun classTeardown() {
            cameraHandlerThread.quitSafely()
        }
    }
}