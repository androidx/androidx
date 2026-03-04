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

package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.PixelDimensions as RtPixelDimensions
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakePanelEntity
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class MainPanelEntityTest {
    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
    private val activity = activityController.create().start().get()
    private lateinit var sceneRuntime: SceneRuntime

    lateinit var session: Session

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
    }

    @Test
    fun addPerceivedResolutionChangedListener_callsRuntimeAddPerceivedResolutionChangedListener() {
        val listener = Consumer<IntSize2d> {}
        val executor = directExecutor()
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(1)
        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap.values.toList()[0])
            .isEqualTo(executor)
    }

    @Test
    fun addPerceivedResolutionChangedListener_withNoExecutor_callsRuntimeWithMainThreadExecutor() {
        val listener = Consumer<IntSize2d> {}
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(listener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(1)
        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap.values.toList()[0])
            .isEqualTo(HandlerExecutor.mainThreadExecutor)
    }

    @Test
    fun addPerceivedResolutionChangedListener_withoutDeviceTracking_throwsIllegalStateException() {
        // Disable head tracking
        session.configure(Config(deviceTracking = DeviceTrackingMode.DISABLED))

        val listener = Consumer<IntSize2d> {}
        val exception =
            assertFailsWith<IllegalStateException> {
                session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(listener)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("Config.DeviceTrackingMode is not set to SpatialLastKnown.")
    }

    @Test
    fun removePerceivedResolutionChangedListener_callsRuntimeRemovePerceivedResolutionChangedListener() {
        val listener = Consumer<IntSize2d> {}
        // Add the listener first so there's something to remove
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(
            directExecutor(),
            listener,
        )
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(1)

        session.scene.mainPanelEntity.removePerceivedResolutionChangedListener(listener)

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(0)
    }

    @Test
    fun perceivedResolutionChangedListener_isCalledWithConvertedValues() {
        var receivedDimensions: IntSize2d? = null
        val listener = Consumer<IntSize2d> { dims -> receivedDimensions = dims }
        val executor = directExecutor()

        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(1)

        val testRtDimensions = RtPixelDimensions(100, 200)
        fakeSceneRuntime.perceivedResolutionChangedMap.keys.toList()[0].accept(testRtDimensions)

        assertThat(receivedDimensions).isNotNull()
        assertThat(receivedDimensions!!.width).isEqualTo(100)
        assertThat(receivedDimensions.height).isEqualTo(200)

        // Simulate another callback
        val anotherRtDimensions = RtPixelDimensions(300, 400)
        fakeSceneRuntime.perceivedResolutionChangedMap.keys.toList()[0].accept(anotherRtDimensions)

        assertThat(receivedDimensions.width).isEqualTo(300)
        assertThat(receivedDimensions.height).isEqualTo(400)
    }

    @Test
    fun addMultiplePerceivedResolutionListeners_allAreRegisteredAndCalled() {
        var receivedDimensions1 = IntSize2d(0, 0)
        val listener1 = Consumer<IntSize2d> { dims -> receivedDimensions1 = dims }
        var receivedDimensions2 = IntSize2d(0, 0)
        val listener2 = Consumer<IntSize2d> { dims -> receivedDimensions2 = dims }
        val executor = directExecutor()

        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener1)
        session.scene.mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener2)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(2)

        // Simulate callback for the first registered listener only
        val testRtDimensions1 = RtPixelDimensions(10, 20)
        fakeSceneRuntime.perceivedResolutionChangedMap.keys.toList()[0].accept(testRtDimensions1)

        assertThat(receivedDimensions1).isEqualTo(IntSize2d(10, 20))
        assertThat(receivedDimensions2).isEqualTo(IntSize2d(0, 0))

        // Simulate callback for the second registered listener
        val testRtDimensions2 = RtPixelDimensions(30, 40)
        fakeSceneRuntime.perceivedResolutionChangedMap.keys.toList()[1].accept(testRtDimensions2)

        assertThat(receivedDimensions1).isEqualTo(IntSize2d(10, 20))
        assertThat(receivedDimensions2).isEqualTo(IntSize2d(30, 40))
    }

    @Test
    fun dispose_removesPerceivedResolutionChangedListener() {
        val listener = Consumer<IntSize2d> {}
        val executor = directExecutor()
        val mainPanelEntity = session.scene.mainPanelEntity

        mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(1)

        mainPanelEntity.dispose()

        assertThat(fakeSceneRuntime.perceivedResolutionChangedMap).hasSize(0)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_callsRuntime() {
        val input = Vector2(100f, 100f)
        val result = session.scene.mainPanelEntity.transformPixelCoordinatesToLocalPosition(input)

        val sizeInPixels = (session.scene.mainPanelEntity.rtEntity as FakePanelEntity).sizeInPixels
        val u = input.x / sizeInPixels.width
        val v = input.y / sizeInPixels.height
        val coordinates = Vector2(u * 2 - 1, (1 - v) * 2 - 1)
        val size = (session.scene.mainPanelEntity.rtEntity as FakePanelEntity).size
        val xInLocal3DSpace = coordinates.x * size.width / 2f
        val yInLocal3DSpace = coordinates.y * size.height / 2f
        val expected = Vector3(xInLocal3DSpace, yInLocal3DSpace, 0f)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_callsRuntime() {
        val input = Vector2(0.5f, 0.5f)
        val result =
            session.scene.mainPanelEntity.transformNormalizedCoordinatesToLocalPosition(input)

        val size = (session.scene.mainPanelEntity.rtEntity as FakePanelEntity).size
        val xInLocal3DSpace = input.x * size.width / 2f
        val yInLocal3DSpace = input.y * size.height / 2f
        val expected = Vector3(xInLocal3DSpace, yInLocal3DSpace, 0f)

        assertThat(result).isEqualTo(expected)
    }
}
