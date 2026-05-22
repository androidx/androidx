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

package androidx.xr.scenecore.testrule

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MainPanelEntity
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.sceneRuntime
import androidx.xr.scenecore.testing.MainPanelEntityTester
import androidx.xr.scenecore.testing.MemoryUtils
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.lang.ref.WeakReference
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config as RoboConfig

@RunWith(RobolectricTestRunner::class)
@RoboConfig(sdk = [RoboConfig.TARGET_SDK])
class TestRuleMainPanelEntityTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()
    private lateinit var session: Session

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var mainPanelEntity: MainPanelEntity
    private lateinit var mainPanelEntityTester: MainPanelEntityTester

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        session.configure(Config.Builder().setDeviceTracking(DeviceTrackingMode.SPATIAL).build())
        mainPanelEntity = session.scene.mainPanelEntity
        mainPanelEntityTester = scenecoreTestRule.mainPanelEntityTester
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun addPerceivedResolutionChangedListener_callsRuntimeAddPerceivedResolutionChangedListener() {
        var callCount = 0
        val listener = Consumer<IntSize2d> { callCount++ }

        mainPanelEntity.addPerceivedResolutionChangedListener(directExecutor(), listener)

        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(IntSize2d(100, 200))

        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun addPerceivedResolutionChangedListener_withNoExecutor_callsRuntimeWithMainThreadExecutor() {
        var calledOnMainThread = false
        val listener =
            Consumer<IntSize2d> {
                calledOnMainThread = (Looper.myLooper() == Looper.getMainLooper())
            }

        // This should default to MainThreadExecutor
        mainPanelEntity.addPerceivedResolutionChangedListener(listener)

        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(IntSize2d(100, 200))

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(calledOnMainThread).isTrue()
    }

    @Test
    fun addPerceivedResolutionChangedListener_withoutDeviceTracking_throwsIllegalStateException() {
        // Disable head tracking
        session.configure(Config.Builder().setDeviceTracking(DeviceTrackingMode.DISABLED).build())

        val listener = Consumer<IntSize2d> {}
        val exception =
            assertFailsWith<IllegalStateException> {
                mainPanelEntity.addPerceivedResolutionChangedListener(listener)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("Config.DeviceTrackingMode is not set to Spatial.")
    }

    @Test
    fun removePerceivedResolutionChangedListener_callsRuntimeRemovePerceivedResolutionChangedListener() {
        var callCount = 0
        val listener = Consumer<IntSize2d> { callCount++ }
        mainPanelEntity.addPerceivedResolutionChangedListener(directExecutor(), listener)

        // 1. Verify listener is active
        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(IntSize2d(100, 200))
        assertThat(callCount).isEqualTo(1)

        // 2. Remove listener
        mainPanelEntity.removePerceivedResolutionChangedListener(listener)

        // 3. Verify listener is no longer called
        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(IntSize2d(300, 400))
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun perceivedResolutionChangedListener_isCalledWithConvertedValues() {
        var receivedDimensions: IntSize2d? = null
        val listener = Consumer<IntSize2d> { dims -> receivedDimensions = dims }

        mainPanelEntity.addPerceivedResolutionChangedListener(directExecutor(), listener)

        val testDimensions = IntSize2d(100, 200)
        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(testDimensions)

        assertThat(receivedDimensions).isEqualTo(testDimensions)

        // Simulate another callback
        val anotherDimensions = IntSize2d(300, 400)
        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(anotherDimensions)

        assertThat(receivedDimensions).isEqualTo(anotherDimensions)
    }

    @Test
    fun addMultiplePerceivedResolutionListeners_allAreRegisteredAndCalled() {
        var receivedDimensions1 = IntSize2d(0, 0)
        val listener1 = Consumer<IntSize2d> { dims -> receivedDimensions1 = dims }
        var receivedDimensions2 = IntSize2d(0, 0)
        val listener2 = Consumer<IntSize2d> { dims -> receivedDimensions2 = dims }
        val executor = directExecutor()

        mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener1)
        mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener2)

        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(IntSize2d(10, 20))

        assertThat(receivedDimensions1).isEqualTo(IntSize2d(10, 20))
        assertThat(receivedDimensions2).isEqualTo(IntSize2d(10, 20))

        // Simulate another change
        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(IntSize2d(30, 40))

        assertThat(receivedDimensions1).isEqualTo(IntSize2d(30, 40))
        assertThat(receivedDimensions2).isEqualTo(IntSize2d(30, 40))
    }

    @Test
    fun disposeInternal_removesPerceivedResolutionChangedListener() {
        var callCount = 0
        val listener = Consumer<IntSize2d> { callCount++ }
        val executor = directExecutor()

        mainPanelEntity.addPerceivedResolutionChangedListener(executor, listener)
        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(IntSize2d(100, 200))
        assertThat(callCount).isEqualTo(1)

        mainPanelEntity.disposeInternal()

        mainPanelEntityTester.triggerOnPerceivedResolutionChanged(IntSize2d(200, 200))
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_returnsExpectedPositions() {
        // MainPanel default size is 2000x2000 pixels (1.0m x 1.0m @ 2000 dp/m)
        // Center (1000, 1000) should be (0, 0, 0)
        val centerInput = Vector2(1000f, 1000f)
        assertThat(mainPanelEntity.transformPixelCoordinatesToLocalPosition(centerInput))
            .isEqualTo(Vector3.Zero)

        // Top-left (0, 0) should be (-0.5, 0.5, 0)
        val topLeftInput = Vector2(0f, 0f)
        assertVector3(
            mainPanelEntity.transformPixelCoordinatesToLocalPosition(topLeftInput),
            Vector3(-0.5f, 0.5f, 0f),
        )

        // Bottom-right (2000, 2000) should be (0.5, -0.5, 0)
        val bottomRightInput = Vector2(2000f, 2000f)
        assertVector3(
            mainPanelEntity.transformPixelCoordinatesToLocalPosition(bottomRightInput),
            Vector3(0.5f, -0.5f, 0f),
        )
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_returnsExpectedPositions() {
        // Center (0, 0) should be (0, 0, 0)
        assertThat(mainPanelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f)))
            .isEqualTo(Vector3.Zero)

        // (1, 1) should be (0.5, 0.5, 0)
        assertVector3(
            mainPanelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(1f, 1f)),
            Vector3(0.5f, 0.5f, 0f),
        )
    }

    private fun assertVector3(actual: Vector3, expected: Vector3, epsilon: Float = 1e-5f) {
        assertThat(actual.x).isWithin(epsilon).of(expected.x)
        assertThat(actual.y).isWithin(epsilon).of(expected.y)
        assertThat(actual.z).isWithin(epsilon).of(expected.z)
    }

    @Test
    fun garbageCollection_disposesEntity() {
        fun createMainPanelEntity(): WeakReference<MainPanelEntity> {
            val entity =
                MainPanelEntity.create(
                    session.sceneRuntime,
                    session.scene.perceptionSpace,
                    session.scene.entityRegistry,
                )
            return WeakReference(entity)
        }

        val entityRef = createMainPanelEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }
}
