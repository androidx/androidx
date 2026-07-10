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
package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeRepository
import com.google.common.truth.Truth
import com.google.common.util.concurrent.MoreExecutors
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class MainPanelEntityImplTest : AndroidXrEntityImplTest() {
    override val xrExtensions = SpatialCoreXrExtensionsHolderProvider.extensionsLegacy
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    override val activity: Activity = activityController.create().start().get()
    override val fakeExecutor = FakeScheduledExecutorService()
    override val sceneNodeRegistry = SceneNodeRegistry()
    private lateinit var sceneRuntime: SpatialSceneRuntime
    private lateinit var mainPanelEntity: MainPanelEntityImpl

    /** The default pixels per meter. */
    private val pixelsPerMeter = 2000f

    override fun createEntity(node: Node): AndroidXrEntity {
        // MainPanelEntityImpl is usually created via SpatialSceneRuntime.
        // But for testing GC we can create it directly if constructor is accessible.
        return MainPanelEntityImpl(activity, node, xrExtensions, sceneNodeRegistry, fakeExecutor)
    }

    @Before
    fun setUp() {
        sceneRuntime =
            SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions, sceneNodeRegistry)

        mainPanelEntity = sceneRuntime.mainPanelEntity as MainPanelEntityImpl
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        sceneRuntime.destroy()
    }

    @Test
    fun runtimeGetMainPanelEntity_returnsPanelEntityImpl() {
        Truth.assertThat(mainPanelEntity).isNotNull()
    }

    @Test
    fun mainPanelEntitySetSizeInPixels_callsExtensions() {
        val kTestPixelDimensions = PixelDimensions(14, 14)
        mainPanelEntity.sizeInPixels = kTestPixelDimensions

        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        Truth.assertThat(shadowXrExtensions.getMainWindowWidth(activity))
            .isEqualTo(kTestPixelDimensions.width)
        Truth.assertThat(shadowXrExtensions.getMainWindowHeight(activity))
            .isEqualTo(kTestPixelDimensions.height)
    }

    @Test
    fun mainPanelEntitySetSize_callsExtensions() {
        val kTestDimensions = Dimensions(0.123f, 0.123f, 0.123f)
        // The (FakeXrExtensions) test default pixel density is 2000 pixel per meter.
        val expectPixels =
            Dimensions(0.123f * pixelsPerMeter, 0.123f * pixelsPerMeter, 0.123f * pixelsPerMeter)
        mainPanelEntity.size = kTestDimensions

        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        Truth.assertThat(shadowXrExtensions.getMainWindowWidth(activity))
            .isEqualTo(expectPixels.width.toInt())
        Truth.assertThat(shadowXrExtensions.getMainWindowHeight(activity))
            .isEqualTo(expectPixels.height.toInt())
    }

    @Test
    fun createActivityPanelEntity_setsCornerRadiusToDefaultSize() {
        // The (FakeXrExtensions) test default pixel density is 2000 pixel per meter. Validate that
        // the corner radius is set to (DEFAULT_CORNER_RADIUS_DP = 32) / 2000 = 0.016.
        Truth.assertThat(mainPanelEntity.cornerRadius).isEqualTo(32f / pixelsPerMeter)
        Truth.assertThat(NodeRepository.getInstance().getCornerRadius(mainPanelEntity.getNode()))
            .isEqualTo(32f / pixelsPerMeter)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_topLeft_returnsCorrectPosition() {
        val position = mainPanelEntity.transformPixelCoordinatesToLocalPosition(Vector2(0f, 0f))
        val expected =
            Vector3(mainPanelEntity.size.width * -0.5f, mainPanelEntity.size.height * 0.5f, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_center_returnsZeroVector() {
        val position =
            mainPanelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f))
        Truth.assertThat(position).isEqualTo(Vector3.Zero)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_topLeft_returnsCorrectPosition() {
        val position =
            mainPanelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, 1f))
        val expected =
            Vector3(mainPanelEntity.size.width * -0.5f, mainPanelEntity.size.height * 0.5f, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun mainPanelEntitySetSize_notifyListenersOnCompletion() {
        val shadowExtensions =
            android.extensions.xr.ShadowXrExtensions.extract(xrExtensions.underlyingObject)

        var listenerInvoked = false
        val listener = Runnable { listenerInvoked = true }
        mainPanelEntity.addOnSetSizeCompleteListener(MoreExecutors.directExecutor(), listener)

        // Defer the underlying IPC callback to simulate an asynchronous in-flight request.
        shadowExtensions.deferSetMainWindowSizeCallbacks(true)

        val kTestDimensions100 = Dimensions(100f, 100f, 100f)
        mainPanelEntity.size = kTestDimensions100

        // Verify: While the IPC call is in-flight, the listener must not be invoked,
        // and the entity should be in a pending state.
        Truth.assertThat(listenerInvoked).isFalse()
        Truth.assertThat(mainPanelEntity.isWaitingForSetSize()).isTrue()

        // Manually trigger the completion of the IPC call.
        shadowExtensions.flushSetMainWindowSizeCallbacks(activity)

        // Verify: Once the IPC completes, the listener is synchronously notified
        // and the pending state is cleared.
        Truth.assertThat(listenerInvoked).isTrue()
        Truth.assertThat(mainPanelEntity.isWaitingForSetSize()).isFalse()

        // Cleanup
        mainPanelEntity.removeOnSetSizeCompleteListener(listener)
        shadowExtensions.deferSetMainWindowSizeCallbacks(false)
    }

    @Test
    fun mainPanelEntitySetSize_rapidUpdates_coalescesIntermediateRequests() {
        val shadowExtensions =
            android.extensions.xr.ShadowXrExtensions.extract(xrExtensions.underlyingObject)

        val kTestDimensions100 = Dimensions(100f, 100f, 100f)
        val kTestDimensions200 = Dimensions(200f, 200f, 200f)
        val kTestDimensions300 = Dimensions(300f, 300f, 300f)
        val kTestDimensions400 = Dimensions(400f, 400f, 400f)

        // Defer the underlying IPC callback to simulate an asynchronous in-flight request.
        shadowExtensions.deferSetMainWindowSizeCallbacks(true)

        // 1. Send the initial size request.
        mainPanelEntity.size = kTestDimensions100

        // Verify: The first request is sent to the underlying API immediately.
        Truth.assertThat(shadowExtensions.getMainWindowWidth(activity))
            .isEqualTo((kTestDimensions100.width * pixelsPerMeter).toInt())
        Truth.assertThat(mainPanelEntity.isWaitingForSetSize()).isTrue()

        // 2. Simulate rapid, consecutive size updates from the user before the first request
        // completes.
        mainPanelEntity.size = kTestDimensions200
        mainPanelEntity.size = kTestDimensions300
        mainPanelEntity.size = kTestDimensions400

        // Verify: The underlying API width is STILL 100m or (100 * 2000)px because the
        // IPC is in-flight.
        // The intermediate requests (200, 300, 400) are deferred and coalesced.
        Truth.assertThat(shadowExtensions.getMainWindowWidth(activity))
            .isEqualTo((kTestDimensions100.width * pixelsPerMeter).toInt())
        Truth.assertThat(mainPanelEntity.isWaitingForSetSize()).isTrue()

        // 3. Manually complete the FIRST request (100).
        // This triggers handleSetSizeComplete(), which discovers the coalesced deferred
        // request (400) and automatically sends it out.
        shadowExtensions.flushSetMainWindowSizeCallbacks(activity)

        // Verify: The system automatically skipped 200 and 300, and immediately
        // sent the latest requested size (400) to the underlying API.
        Truth.assertThat(shadowExtensions.getMainWindowWidth(activity)).isEqualTo(800000)
        Truth.assertThat(mainPanelEntity.isWaitingForSetSize()).isTrue()

        // 4. Manually complete the SECOND request (400).
        shadowExtensions.flushSetMainWindowSizeCallbacks(activity)

        // Verify: The system has finished all resize operations and is now idle.
        Truth.assertThat(mainPanelEntity.isWaitingForSetSize()).isFalse()

        // Cleanup
        shadowExtensions.deferSetMainWindowSizeCallbacks(false)
    }

    @Test
    fun mainPanelEntitySetSize_resetsStateOnException() {
        // Register a listener to verify it is invoked even when an exception occurs.
        var listenerInvoked = false
        val listener = Runnable { listenerInvoked = true }
        mainPanelEntity.addOnSetSizeCompleteListener(MoreExecutors.directExecutor(), listener)

        // Trigger an intentional failure by passing negative dimensions.
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            mainPanelEntity.size = Dimensions(-1f, -1f, -1f)
        }

        // Verify: Even if the underlying API throws an exception synchronously,
        // our error handling ensures the pending flag is correctly recovered to false.
        Truth.assertThat(mainPanelEntity.isWaitingForSetSize()).isFalse()

        // Verify: The listener MUST be invoked on failure to prevent the UI
        // from getting permanently stuck in a hidden state.
        Truth.assertThat(listenerInvoked).isTrue()

        // Verify: Ensure the internal execution lock is also released by sending
        // a valid request and confirming it reaches the underlying API.
        val kTestDimensions01 = Dimensions(0.1f, 0.1f, 0.1f)
        mainPanelEntity.size = kTestDimensions01
        val shadowExtensions = ShadowXrExtensions.extract(xrExtensions)

        Truth.assertThat(shadowExtensions.getMainWindowWidth(activity)).isEqualTo(200)

        // Cleanup
        mainPanelEntity.removeOnSetSizeCompleteListener(listener)
    }
}
