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
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestAugmentedObject
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AugmentedObjectTest {
    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(
            Config(
                augmentedObjectCategories =
                    setOf(
                        AugmentedObjectCategory.KEYBOARD,
                        AugmentedObjectCategory.MOUSE,
                        AugmentedObjectCategory.LAPTOP,
                    )
            )
        )
    }

    @Test
    fun subscribe_collectReturnsObject() =
        runTest(testDispatcher) {
            val testObject = TestAugmentedObject(AugmentedObjectCategory.KEYBOARD)
            arCoreTestRule.addTrackables(testObject)
            advanceUntilIdle()

            var underTest = emptyList<AugmentedObject>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                AugmentedObject.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.size).isEqualTo(1)
            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.TRACKING)
            assertThat(underTest.single().state.value.category).isEqualTo(testObject.category)
            assertThat(underTest.single().state.value.centerPose).isEqualTo(testObject.centerPose)
            assertThat(underTest.single().state.value.extents).isEqualTo(testObject.extents)
        }

    @Test
    fun subscribe_ignoresObjectsNotIncludedInConfig() =
        runTest(testDispatcher) {
            val testObject = TestAugmentedObject(AugmentedObjectCategory.UNKNOWN)
            arCoreTestRule.addTrackables(testObject)
            advanceUntilIdle()

            var underTest = emptyList<AugmentedObject>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                AugmentedObject.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(session.config.augmentedObjectCategories).doesNotContain(testObject.category)
            assertThat(underTest).isEmpty()
        }

    @Test
    fun subscribe_augmentedObjectTrackingDisabled_throwsIllegalStateException() {
        val configureResult = session.configure(Config(augmentedObjectCategories = emptySet()))
        check(configureResult is SessionConfigureSuccess)
        assertFailsWith<IllegalStateException> { AugmentedObject.subscribe(session) }
    }

    @Test
    fun update_trackingStateMatchesObjectVisibility() =
        runTest(testDispatcher) {
            val testObject = TestAugmentedObject(AugmentedObjectCategory.KEYBOARD)
            arCoreTestRule.addTrackables(testObject)
            advanceUntilIdle()

            var underTest = emptyList<AugmentedObject>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                AugmentedObject.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.TRACKING)

            testObject.isVisible = false
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState).isEqualTo(TrackingState.PAUSED)
        }

    @Test
    fun update_categoryNotConfigured_trackingStops() =
        runTest(testDispatcher) {
            val testObject = TestAugmentedObject(AugmentedObjectCategory.KEYBOARD)
            arCoreTestRule.addTrackables(testObject)
            advanceUntilIdle()

            var underTest = emptyList<AugmentedObject>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                AugmentedObject.subscribe(session).collect { underTest = it.toList() }
            }

            activityController.pause()
            advanceUntilIdle()
            session.configure(Config(augmentedObjectCategories = emptySet()))
            activityController.resume()
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.STOPPED)
        }

    @Test
    fun update_centerPoseMatchesObjectCenterPose() =
        runTest(testDispatcher) {
            val testObject = TestAugmentedObject(AugmentedObjectCategory.KEYBOARD)
            arCoreTestRule.addTrackables(testObject)
            advanceUntilIdle()

            var underTest = emptyList<AugmentedObject>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                AugmentedObject.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.centerPose).isEqualTo(Pose())

            val pose = Pose(Vector3(3f, 4f, 5f), Quaternion(2f, 3f, 4f, 1f))
            testObject.centerPose = pose
            advanceUntilIdle()

            assertThat(underTest.single().state.value.centerPose).isEqualTo(pose)
        }

    @Test
    fun update_extentsMatchesTestPlaneExtents() =
        runTest(testDispatcher) {
            val testObject = TestAugmentedObject(AugmentedObjectCategory.KEYBOARD)
            arCoreTestRule.addTrackables(testObject)
            advanceUntilIdle()

            var underTest = emptyList<AugmentedObject>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                AugmentedObject.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.extents).isEqualTo(FloatSize3d())

            val newExtents = FloatSize3d(3.0f, 4.0f, 5.0f)
            testObject.extents = newExtents
            advanceUntilIdle()

            assertThat(underTest.single().state.value.extents).isEqualTo(newExtents)
        }
}
