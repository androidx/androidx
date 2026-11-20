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

package androidx.camera.compose

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.ForwardingCameraControl
import androidx.camera.core.impl.utils.TransformUtils
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.viewfinder.core.FocusState
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@SuppressLint("RestrictedApiAndroidX")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class CameraXViewfinderTapToFocusTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val surfaceRequests = mutableListOf<SurfaceRequest>()
    private var showViewfinder by mutableStateOf(true)
    private var isTapToFocusEnabled by mutableStateOf(true)

    @After
    fun tearDown() {
        showViewfinder = false
        composeTestRule.waitForIdle()
        surfaceRequests.forEach { request ->
            request.willNotProvideSurface()
            request.deferrableSurface.close()
        }
        surfaceRequests.clear()
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun tapToFocus_focusSuccessful_emitsStartedThenFocused() {
        val harness = setupTapToFocusHarness()

        composeTestRule.onNodeWithTag(VIEWFINDER_TAG).performTouchInput { click(center) }
        composeTestRule.waitForIdle()

        assertThat(harness.submittedActions).hasSize(1)
        val afPoint = harness.submittedActions.single().meteringPointsAf.single()
        assertThat(afPoint.x).isWithin(0.01f).of(0.5f)
        assertThat(afPoint.y).isWithin(0.01f).of(0.5f)
        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()

        harness.completeLatest(FocusMeteringResult.create(true))
        composeTestRule.waitForIdle()

        assertThat(harness.events.map { it.second })
            .containsExactly(FocusState.STARTED, FocusState.FOCUSED)
            .inOrder()
    }

    @Test
    fun tapToFocus_focusUnsuccessful_emitsStartedThenNotFocused() {
        val harness = setupTapToFocusHarness()

        composeTestRule.onNodeWithTag(VIEWFINDER_TAG).performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()

        harness.completeLatest(FocusMeteringResult.create(false))
        composeTestRule.waitForIdle()

        assertThat(harness.events.map { it.second })
            .containsExactly(FocusState.STARTED, FocusState.NOT_FOCUSED)
            .inOrder()
    }

    @Test
    fun tapToFocus_operationCanceledException_emitsStartedOnly() {
        val harness = setupTapToFocusHarness()

        composeTestRule.onNodeWithTag(VIEWFINDER_TAG).performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()

        harness.failLatest(CameraControl.OperationCanceledException("Superseded"))
        composeTestRule.waitForIdle()

        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()
    }

    @Test
    fun tapToFocus_failureException_emitsStartedThenFailed() {
        val harness = setupTapToFocusHarness()

        composeTestRule.onNodeWithTag(VIEWFINDER_TAG).performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()

        harness.failLatest(IllegalArgumentException("Unsupported metering region"))
        composeTestRule.waitForIdle()

        assertThat(harness.events.map { it.second })
            .containsExactly(FocusState.STARTED, FocusState.FAILED)
            .inOrder()
    }

    @Test
    fun tapToFocus_disposedWhileAwaiting_cancelsUnderlyingFuture() {
        val harness = setupTapToFocusHarness()

        composeTestRule.onNodeWithTag(VIEWFINDER_TAG).performTouchInput { click(center) }
        composeTestRule.waitForIdle()

        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()
        val future = checkNotNull(harness.latestFuture)
        assertThat(future.isDone).isFalse()

        showViewfinder = false
        composeTestRule.waitForIdle()

        assertThat(future.isCancelled).isTrue()
        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()
    }

    @Test
    fun tapToFocus_disabledWhileAwaiting_cancelsFutureWithoutEmittingFailed() {
        val harness = setupTapToFocusHarness()

        composeTestRule.onNodeWithTag(VIEWFINDER_TAG).performTouchInput { click(center) }
        composeTestRule.waitForIdle()

        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()
        val future = checkNotNull(harness.latestFuture)
        assertThat(future.isDone).isFalse()

        isTapToFocusEnabled = false
        composeTestRule.waitForIdle()

        assertThat(future.isCancelled).isTrue()
        assertThat(harness.events.map { it.second }).containsExactly(FocusState.STARTED).inOrder()
    }

    private fun setupTapToFocusHarness(): TapToFocusHarness {
        val cameraInfo = FakeCameraInfoInternal(90, CameraSelector.LENS_FACING_BACK)
        val harness = TapToFocusHarness()
        val cameraControl =
            object : ForwardingCameraControl(FakeCameraControl()) {
                override fun startFocusAndMetering(
                    action: FocusMeteringAction
                ): ListenableFuture<FocusMeteringResult> {
                    harness.submittedActions.add(action)
                    val future = CallbackToFutureAdapter.getFuture { completer ->
                        harness.latestCompleter = completer
                        "tapToFocusTestFuture"
                    }
                    harness.latestFuture = future
                    return future
                }
            }
        val surfaceSize = Size(640, 480)
        val surfaceRequest =
            SurfaceRequest(surfaceSize, FakeCamera(cameraControl, cameraInfo)) {}
                .also {
                    surfaceRequests.add(it)
                }
        surfaceRequest.updateTransformationInfo(
            SurfaceRequest.TransformationInfo.of(
                Rect(0, 0, surfaceSize.width, surfaceSize.height),
                /* rotationDegrees= */ 0,
                Surface.ROTATION_0,
                /* hasCameraTransform= */ true,
                TransformUtils.getRectToRect(
                    RectF(cameraInfo.sensorRect),
                    RectF(0f, 0f, surfaceSize.width.toFloat(), surfaceSize.height.toFloat()),
                    /* rotationDegrees= */ 0,
                ),
                /* mirroring= */ false,
            )
        )

        showViewfinder = true
        isTapToFocusEnabled = true
        composeTestRule.setContent {
            if (showViewfinder) {
                CameraXViewfinder(
                    surfaceRequest = surfaceRequest,
                    implementationMode = ImplementationMode.EMBEDDED,
                    isTapToFocusEnabled = isTapToFocusEnabled,
                    onTapToFocus = { offset, state -> harness.events.add(offset to state) },
                    modifier = Modifier.size(200.dp, 200.dp).testTag(VIEWFINDER_TAG),
                )
            }
        }
        composeTestRule.waitForIdle()
        return harness
    }

    private class TapToFocusHarness {
        val events = CopyOnWriteArrayList<Pair<Offset, Int>>()
        val submittedActions = CopyOnWriteArrayList<FocusMeteringAction>()
        var latestCompleter: CallbackToFutureAdapter.Completer<FocusMeteringResult>? = null
        var latestFuture: ListenableFuture<FocusMeteringResult>? = null

        fun completeLatest(result: FocusMeteringResult) {
            checkNotNull(latestCompleter).set(result)
        }

        fun failLatest(error: Throwable) {
            checkNotNull(latestCompleter).setException(error)
        }
    }

    private companion object {
        private const val VIEWFINDER_TAG = "CameraXViewfinderTapToFocusTestTag"
    }
}
