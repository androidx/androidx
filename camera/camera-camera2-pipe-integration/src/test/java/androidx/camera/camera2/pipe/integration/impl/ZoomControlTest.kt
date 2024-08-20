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

package androidx.camera.camera2.pipe.integration.impl

import android.os.Build
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.testing.FakeZoomCompat
import androidx.camera.core.CameraControl
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class ZoomControlTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule(MoreExecutors.directExecutor().asCoroutineDispatcher())

    private val fakeUseCaseThreads by lazy {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope = CoroutineScope(Job() + dispatcher)

        UseCaseThreads(
            cameraScope,
            executor,
            dispatcher,
        )
    }

    private val zoomCompat = FakeZoomCompat(1.0f, 5.0f)
    private lateinit var zoomControl: ZoomControl

    @Before
    fun setUp() {
        zoomControl =
            ZoomControl(fakeUseCaseThreads, zoomCompat).apply {
                requestControl = FakeUseCaseCameraRequestControl()
            }
    }

    @Test
    fun canUpdateZoomRatioInCompat() {
        zoomControl.setZoomRatio(3.0f)[3, TimeUnit.SECONDS]

        assertWithMessage("zoomCompat not updated with correct zoom ratio")
            .that(zoomCompat.zoomRatio)
            .isEqualTo(3.0f)
    }

    @Test
    fun setZoomRatioResultWaits_whenZoomCompatIncomplete() {
        zoomCompat.applyAsyncResult = CompletableDeferred() // never completing deferred
        val result = zoomControl.setZoomRatio(3.0f)

        assertWithMessage("setZoomRatio result completed before ZoomCompat work is done")
            .that(result.isDone)
            .isFalse()
    }

    @Test
    fun setZoomRatioResultCompletes_afterZoomCompatCompletes() {
        zoomCompat.applyAsyncResult = CompletableDeferred() // incomplete deferred

        val result = zoomControl.setZoomRatio(3.0f)
        zoomCompat.applyAsyncResult.complete(Unit)

        assertWithMessage("setZoomRatio result not completed after ZoomCompat work is done")
            .that(result.isDone)
            .isTrue()
    }

    @Test
    fun setZoomRatioResultPropagates_whenUseCaseCameraUpdated(): Unit = runBlocking {
        zoomCompat.applyAsyncResult = CompletableDeferred() // initial incomplete deferred
        zoomControl.setZoomRatio(3.0f)

        // Act. Simulate the UseCaseCamera is recreated before applying zoom.
        zoomCompat.applyAsyncResult = CompletableDeferred() // incomplete deferred of new camera
        zoomControl.requestControl = FakeUseCaseCameraRequestControl()
        zoomCompat.applyAsyncResult.complete(Unit)

        // Assert. The setZoomRatio task should be completed.
        assertWithMessage("zoomCompat not updated with correct zoom ratio")
            .that(zoomCompat.zoomRatio)
            .isEqualTo(3.0f)
    }

    @Test
    fun onlyLatestRequestCompletes_whenMultipleZoomRatiosSet(): Unit = runBlocking {
        zoomCompat.applyAsyncResult = CompletableDeferred() // incomplete deferred

        val result1 = zoomControl.setZoomRatio(3.0f)
        val result2 = zoomControl.setZoomRatio(2.0f)
        zoomControl.setZoomRatio(4.0f)
        zoomCompat.applyAsyncResult.complete(Unit)

        zoomCompat.applyAsyncResult.complete(Unit)

        assertFutureFailedWithOperationCancellation(result1)
        assertFutureFailedWithOperationCancellation(result2)
        // Assert. Only the last setZoomRatio value should be applied.
        assertWithMessage("zoomCompat not updated with correct zoom ratio")
            .that(zoomCompat.zoomRatio)
            .isEqualTo(4.0f)
    }

    @Test
    fun onlyLatestRequestCompletes_whenUseCaseCameraUpdated(): Unit = runBlocking {
        zoomCompat.applyAsyncResult = CompletableDeferred() // incomplete deferred
        val result1 = zoomControl.setZoomRatio(3.0f)

        // Act. Simulate the UseCaseCamera is recreated,
        zoomControl.requestControl = FakeUseCaseCameraRequestControl()
        // Act. Submit a new zoom ratio.
        val result2 = zoomControl.setZoomRatio(2.0f)
        zoomCompat.applyAsyncResult.complete(Unit)

        // Assert 1. The previous setZoomRatio task should be cancelled
        assertFutureFailedWithOperationCancellation(result1)
        // Assert 2. The latest setZoomRatio task should be completed.
        assertWithMessage("setZoomRatio result not completed after ZoomCompat work is done")
            .that(result2.isDone)
            .isTrue()
    }

    @Test
    fun setZoomRatioResultCancels_whenUseCaseCameraClosed(): Unit = runBlocking {
        zoomCompat.applyAsyncResult = CompletableDeferred() // incomplete deferred
        val result = zoomControl.setZoomRatio(3.0f)

        zoomControl.requestControl = null

        assertFutureFailedWithOperationCancellation(result)
    }

    @Test
    fun previousSetZoomRatioResultCancels_whenZoomControlReset(): Unit = runBlocking {
        zoomCompat.applyAsyncResult = CompletableDeferred() // incomplete deferred
        val result = zoomControl.setZoomRatio(3.0f)

        zoomControl.reset()

        assertFutureFailedWithOperationCancellation(result)
    }

    @Test
    fun zoomValueUpdatesToDefault_whenZoomControlReset(): Unit = runBlocking {
        zoomControl.reset()

        assertWithMessage("zoomCompat not updated with correct zoom ratio")
            .that(zoomCompat.zoomRatio)
            .isEqualTo(DEFAULT_ZOOM_RATIO)
    }

    // TODO: port tests from camera-camera2

    private fun <T> assertFutureFailedWithOperationCancellation(future: ListenableFuture<T>) {
        Assert.assertThrows(ExecutionException::class.java) { future[3, TimeUnit.SECONDS] }
            .apply {
                assertThat(cause).isInstanceOf(CameraControl.OperationCanceledException::class.java)
            }
    }
}
