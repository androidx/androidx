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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeRuntimeAnchor
import androidx.xr.arcore.testing.FakeRuntimeAugmentedObject as FakeRuntimeObject
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class AugmentedObjectTest {
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var xrResourcesManager: XrResourcesManager

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        xrResourcesManager = XrResourcesManager()

        val shadowApplication = shadowOf(activity.application)
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }
        FakePerceptionRuntimeFactory.hasCreatePermission = true
        activityController.create()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(
            Config(
                augmentedObjectCategories =
                    listOf(
                        AugmentedObjectCategory.KEYBOARD,
                        AugmentedObjectCategory.MOUSE,
                        AugmentedObjectCategory.LAPTOP,
                    )
            )
        )
        xrResourcesManager.lifecycleManager = session.perceptionRuntime.lifecycleManager

        FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun subscribe_collectReturnsObject() =
        runTest(testDispatcher) {
            val perceptionManager = getFakePerceptionManager()
            val runtimeObject = FakeRuntimeObject()
            perceptionManager.addTrackable(runtimeObject)
            activityController.resume()
            advanceUntilIdle()
            activityController.pause()
            var underTest = emptyList<AugmentedObject>()

            val job =
                backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    underTest = AugmentedObject.subscribe(session).first().toList()
                }
            advanceUntilIdle()

            assertThat(underTest.size).isEqualTo(1)
            assertThat(underTest.first().runtimeObject).isEqualTo(runtimeObject)
            job.cancel()
        }

    @Test
    fun subscribe_augmentedObjectTrackingDisabled_throwsIllegalStateException() {
        val configureResult = session.configure(Config(augmentedObjectCategories = listOf()))

        assertFailsWith<IllegalStateException> { AugmentedObject.subscribe(session) }
    }

    @Test
    fun createAnchor_usesGivenPose() {
        val runtimeObject = FakeRuntimeObject()
        getFakePerceptionManager().addTrackable(runtimeObject)
        xrResourcesManager.syncTrackables(listOf(runtimeObject))
        val underTest = xrResourcesManager.trackablesMap.values.first() as AugmentedObject
        val pose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))

        val anchorResult = underTest.createAnchor(pose)

        assertThat(anchorResult).isInstanceOf(AnchorCreateSuccess::class.java)
        val anchor = (anchorResult as AnchorCreateSuccess).anchor
        assertThat(anchor.state.value.pose).isEqualTo(pose)
    }

    @Test
    fun createAnchor_anchorLimitReached_returnsAnchorResourcesExhaustedResult() {
        val runtimeObject = FakeRuntimeObject()
        getFakePerceptionManager().addTrackable(runtimeObject)
        xrResourcesManager.syncTrackables(listOf(runtimeObject))
        val underTest = xrResourcesManager.trackablesMap.values.first() as AugmentedObject

        repeat(FakeRuntimeAnchor.ANCHOR_RESOURCE_LIMIT) {
            val result = underTest.createAnchor(Pose())
        }

        assertThat(underTest.createAnchor(Pose()))
            .isInstanceOf(AnchorCreateResourcesExhausted::class.java)
    }

    @Test
    fun createAnchor_augmentedObjectTrackingDisabled_throwsIllegalStateException() {
        val runtimeObject = FakeRuntimeObject()
        getFakePerceptionManager().addTrackable(runtimeObject)
        xrResourcesManager.syncTrackables(listOf(runtimeObject))
        val underTest = xrResourcesManager.trackablesMap.values.first() as AugmentedObject
        session.configure(Config(augmentedObjectCategories = listOf()))

        assertFailsWith<IllegalStateException> { underTest.createAnchor(Pose()) }
    }

    @Test
    fun update_trackingStateMatchesRuntime() = runBlocking {
        val runtimeObject = FakeRuntimeObject()
        runtimeObject.trackingState = TrackingState.STOPPED
        xrResourcesManager.syncTrackables(listOf(runtimeObject))
        val underTest = xrResourcesManager.trackablesMap[runtimeObject] as AugmentedObject
        check(underTest.state.value.trackingState == TrackingState.STOPPED)

        runtimeObject.trackingState = TrackingState.TRACKING
        underTest.update()

        assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun update_centerPoseMatchesRuntime() = runBlocking {
        val runtimeObject = FakeRuntimeObject()
        xrResourcesManager.syncTrackables(listOf(runtimeObject))
        val underTest = xrResourcesManager.trackablesMap[runtimeObject] as AugmentedObject
        check(
            (underTest.state.value.centerPose ==
                Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1.0f)))
        )

        val newPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        runtimeObject.centerPose = newPose
        underTest.update()

        assertThat(underTest.state.value.centerPose).isEqualTo(newPose)
    }

    @Test
    fun update_extentsMatchesRuntime() = runBlocking {
        val runtimeObject = FakeRuntimeObject()
        val extents = FloatSize3d(1.0f, 2.0f, 3.0f)
        runtimeObject.extents = extents
        xrResourcesManager.syncTrackables(listOf(runtimeObject))
        val underTest = xrResourcesManager.trackablesMap[runtimeObject] as AugmentedObject
        underTest.update()
        check(underTest.state.value.extents == extents)

        val newExtents = FloatSize3d(3.0f, 4.0f, 5.0f)
        runtimeObject.extents = newExtents
        underTest.update()

        assertThat(underTest.state.value.extents).isEqualTo(newExtents)
    }

    private fun getFakePerceptionManager(): FakePerceptionManager {
        return session.perceptionRuntime.perceptionManager as FakePerceptionManager
    }
}
