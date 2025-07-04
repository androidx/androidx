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

import android.content.ContentResolver
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.FaceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.testing.FakeLifecycleManager
import androidx.xr.runtime.testing.FakePerceptionManager
import androidx.xr.runtime.testing.FakeRuntimeFace
import androidx.xr.runtime.testing.FakeRuntimeFactory
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class FaceTest {
    private lateinit var xrResourcesManager: XrResourcesManager
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @get:Rule
    val grantPermissionRule = GrantPermissionRule.grant("android.permission.FACE_TRACKING")

    private lateinit var mockContentResolver: ContentResolver

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)

        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        xrResourcesManager = XrResourcesManager()
        mockContentResolver = activity.contentResolver

        val shadowApplication = shadowOf(activity.application)
        shadowApplication.grantPermissions("android.permission.FACE_TRACKING")
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }

        FakeRuntimeFactory.hasCreatePermission = true

        activityController.create()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(faceTracking = FaceTrackingMode.USER))
        xrResourcesManager.lifecycleManager = session.runtime.lifecycleManager
    }

    @After
    fun tearDown() {
        xrResourcesManager.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun userFace_returnsFaceWithUpdatedTrackingStateAndBlendShapes() {
        runTest(testDispatcher) {
            val perceptionManager = session.runtime.perceptionManager as FakePerceptionManager
            val userFace = Face.getUserFace(session)
            check(userFace != null)
            check(userFace.state.value.trackingState != TrackingState.TRACKING)
            check(userFace.state.value.blendShapeValues.isEmpty())
            check(userFace.state.value.confidenceValues.isEmpty())

            val runtimeFace = perceptionManager.userFace!! as FakeRuntimeFace
            runtimeFace.trackingState = TrackingState.TRACKING
            val expectedBlendShapeValues = floatArrayOf(0.1f, 0.2f, 0.3f)
            val expectedConfidenceValues = floatArrayOf(0.4f, 0.5f, 0.6f)
            runtimeFace.blendShapeValues = expectedBlendShapeValues
            runtimeFace.confidenceValues = expectedConfidenceValues

            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(userFace.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
            assertThat(userFace.state.value.blendShapeValues).isEqualTo(expectedBlendShapeValues)
            assertThat(userFace.state.value.confidenceValues).isEqualTo(expectedConfidenceValues)
        }
    }

    @Test
    fun userFace_faceTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(faceTracking = FaceTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Face.getUserFace(session) }
    }

    @Test
    fun update_stateMachesRuntimeFace() = runBlocking {
        val runtimeFace = FakeRuntimeFace()
        val underTest = Face(runtimeFace)
        check(underTest.state.value.trackingState != TrackingState.TRACKING)
        check(underTest.state.value.blendShapeValues.isEmpty())
        check(underTest.state.value.confidenceValues.isEmpty())
        runtimeFace.trackingState = TrackingState.TRACKING
        val expectedBlendShapeValues = floatArrayOf(0.1f, 0.2f, 0.3f)
        val expectedConfidenceValues = floatArrayOf(0.4f, 0.5f, 0.6f)
        runtimeFace.blendShapeValues = expectedBlendShapeValues
        runtimeFace.confidenceValues = expectedConfidenceValues

        underTest.update()

        assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(underTest.state.value.blendShapeValues).isEqualTo(expectedBlendShapeValues)
        assertThat(underTest.state.value.confidenceValues).isEqualTo(expectedConfidenceValues)
        assertThat(underTest.state.value.blendShapes.keys.size)
            .isEqualTo(expectedBlendShapeValues.size)
        assertThat(underTest.state.value.blendShapes.values.size)
            .isEqualTo(expectedBlendShapeValues.size)
    }
}
