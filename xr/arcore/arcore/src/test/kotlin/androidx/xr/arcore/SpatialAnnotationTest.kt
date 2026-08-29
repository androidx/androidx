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

@file:OptIn(ExperimentalSpatialAnnotationsApi::class)

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestSpatialAnnotation
import androidx.xr.runtime.Config
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SpatialAnnotationTrackingMode
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_FINE
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quad
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import java.nio.ByteBuffer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
// TODO(b/554174620) - Gate methods like subscribe if startTracking has not been called.
class SpatialAnnotationTest {

    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    private val TEST_ANNOTATION_ID = SpatialAnnotationId.fromString("test_annotation")

    @Before
    fun setUp(): Unit = runBlocking {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_FINE)

        activityController.create().start().resume()

        session =
            (Session.create(
                    context = activity,
                    coroutineContext = testDispatcher,
                    lifecycleOwner = activity,
                ) as SessionCreateSuccess)
                .session
        session.configure(
            Config.Builder()
                .setSpatialAnnotationTracking(SpatialAnnotationTrackingMode.QUAD)
                .build()
        )
    }

    @Test
    fun subscribe_collectReturnsSpatialAnnotation() =
        runTest(testDispatcher) {
            activityController.resume()
            val testAnnotation = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            arCoreTestRule.addTrackables(testAnnotation)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.size).isEqualTo(1)
            assertThat(underTest.single().id).isEqualTo(TEST_ANNOTATION_ID)
        }

    @Test
    fun subscribe_trackingDisabled_throwsIllegalStateException() {
        session.configure(
            Config.Builder()
                .setSpatialAnnotationTracking(SpatialAnnotationTrackingMode.DISABLED)
                .build()
        )
        assertFailsWith<IllegalStateException> { SpatialAnnotation.subscribe(session) }
    }

    @Test
    fun update_trackingStateMatchesTestAnnotationVisibility() =
        runTest(testDispatcher) {
            activityController.resume()
            val testAnnotation = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            arCoreTestRule.addTrackables(testAnnotation)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.TRACKING)

            testAnnotation.isVisible = false
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState).isEqualTo(TrackingState.PAUSED)
        }

    @Test
    fun update_trackingDisabled_trackingStops() =
        runTest(testDispatcher) {
            val testAnnotation = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            arCoreTestRule.addTrackables(testAnnotation)

            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            activityController.pause()
            advanceUntilIdle()
            session.configure(
                Config.Builder()
                    .setSpatialAnnotationTracking(SpatialAnnotationTrackingMode.DISABLED)
                    .build()
            )
            ShadowLooper.idleMainLooper()
            activityController.resume()
            advanceUntilIdle()

            assertThat(underTest.single().state.value.trackingState)
                .isEqualTo(TrackingState.STOPPED)
        }

    @Test
    fun update_centerPoseMatchesTestAnnotationCenterPose() =
        runTest(testDispatcher) {
            activityController.resume()
            val testAnnotation = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            arCoreTestRule.addTrackables(testAnnotation)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.centerPose).isEqualTo(Pose())

            val newPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
            testAnnotation.centerPose = newPose
            advanceUntilIdle()

            assertThat(underTest.single().state.value.centerPose).isEqualTo(newPose)
        }

    @Test
    fun update_upperLeftMatchesTestAnnotationUpperLeft() =
        runTest(testDispatcher) {
            activityController.resume()
            val testAnnotation = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            arCoreTestRule.addTrackables(testAnnotation)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.quad!!.upperLeft)
                .isEqualTo(testAnnotation.quad!!.upperLeft)

            val newCorner = Vector2(-2.5f, 2.5f)
            testAnnotation.quad =
                Quad.createFromCorners(
                    newCorner,
                    testAnnotation.quad!!.upperRight,
                    testAnnotation.quad!!.lowerRight,
                    testAnnotation.quad!!.lowerLeft,
                )
            advanceUntilIdle()
            assertThat(underTest.single().state.value.quad!!.upperLeft).isEqualTo(newCorner)
        }

    @Test
    fun update_upperRightMatchesTestAnnotationUpperRight() =
        runTest(testDispatcher) {
            activityController.resume()
            val testAnnotation = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            arCoreTestRule.addTrackables(testAnnotation)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.quad!!.upperRight)
                .isEqualTo(testAnnotation.quad!!.upperRight)

            val newCorner = Vector2(2.5f, 2.5f)
            testAnnotation.quad =
                Quad.createFromCorners(
                    testAnnotation.quad!!.upperLeft,
                    newCorner,
                    testAnnotation.quad!!.lowerRight,
                    testAnnotation.quad!!.lowerLeft,
                )
            advanceUntilIdle()
            assertThat(underTest.single().state.value.quad!!.upperRight).isEqualTo(newCorner)
        }

    @Test
    fun update_lowerRightMatchesTestAnnotationLowerRight() =
        runTest(testDispatcher) {
            activityController.resume()
            val testAnnotation = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            arCoreTestRule.addTrackables(testAnnotation)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.quad!!.lowerRight)
                .isEqualTo(testAnnotation.quad!!.lowerRight)

            val newCorner = Vector2(2.5f, -2.5f)
            testAnnotation.quad =
                Quad.createFromCorners(
                    testAnnotation.quad!!.upperLeft,
                    testAnnotation.quad!!.upperRight,
                    newCorner,
                    testAnnotation.quad!!.lowerLeft,
                )
            advanceUntilIdle()
            assertThat(underTest.single().state.value.quad!!.lowerRight).isEqualTo(newCorner)
        }

    @Test
    fun update_lowerLeftMatchesTestAnnotationLowerLeft() =
        runTest(testDispatcher) {
            activityController.resume()
            val testAnnotation = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            arCoreTestRule.addTrackables(testAnnotation)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            assertThat(underTest.single().state.value.quad!!.lowerLeft)
                .isEqualTo(testAnnotation.quad!!.lowerLeft)

            val newCorner = Vector2(-2.5f, -2.5f)
            testAnnotation.quad =
                Quad.createFromCorners(
                    testAnnotation.quad!!.upperLeft,
                    testAnnotation.quad!!.upperRight,
                    testAnnotation.quad!!.lowerRight,
                    newCorner,
                )
            advanceUntilIdle()
            assertThat(underTest.single().state.value.quad!!.lowerLeft).isEqualTo(newCorner)
        }

    @Test
    fun builder_constructsValidOptionsCorrectly() =
        runTest(testDispatcher) {
            val fakeBuffer = ByteBuffer.allocateDirect(1226880)
            val quadMap =
                mapOf(
                    SpatialAnnotationId.fromString("teapot") to
                        Quad.createFromCorners(
                            Vector2(0f, 0f),
                            Vector2(0f, 0f),
                            Vector2(0f, 0f),
                            Vector2(0f, 0f),
                        )
                )

            val options =
                SpatialAnnotationTrackingOptions.Builder(
                        fakeBuffer,
                        IntSize2d(640, 480),
                        123456789L,
                    )
                    .setRowStride(2560)
                    .setFormat(SpatialAnnotationImageFormat.GRAYSCALE)
                    .setAlignment(SpatialAnnotationQuadAlignment.OBJECT)
                    .setQuads(quadMap)
                    .build()

            assertThat(options.imageBuffer).isEqualTo(fakeBuffer)
            assertThat(options.imageSize.width).isEqualTo(640)
            assertThat(options.format).isEqualTo(SpatialAnnotationImageFormat.GRAYSCALE)
            assertThat(options.quads.keys.single())
                .isEqualTo(SpatialAnnotationId.fromString("teapot"))
        }

    @Test
    fun stopTracking_withEmptyList_throwsIllegalArgumentException() =
        runTest(testDispatcher) {
            assertFailsWith<IllegalArgumentException> {
                SpatialAnnotation.stopTracking(session, emptyList())
            }
        }

    @Test
    fun stopTrackingAllAnnotations_haltsAllTracking() =
        runTest(testDispatcher) {
            val testAnnotation1 = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            val testAnnotation2 = TestSpatialAnnotation(SpatialAnnotationId.fromString("box-2"))
            arCoreTestRule.addTrackables(testAnnotation1)
            arCoreTestRule.addTrackables(testAnnotation2)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()

            SpatialAnnotation.stopTrackingAllAnnotations(session)
            advanceUntilIdle()

            assertThat(
                    underTest
                        .first { it.id == SpatialAnnotationId.fromString("box-2") }
                        .state
                        .value
                        .trackingState
                )
                .isEqualTo(TrackingState.STOPPED)
            assertThat(underTest.first { it.id == TEST_ANNOTATION_ID }.state.value.trackingState)
                .isEqualTo(TrackingState.STOPPED)
        }

    @Test
    fun stopTracking_haltsSpecificAnnotationTracking() =
        runTest(testDispatcher) {
            val testAnnotation1 = TestSpatialAnnotation(TEST_ANNOTATION_ID)
            val testAnnotation2 = TestSpatialAnnotation(SpatialAnnotationId.fromString("box-2"))
            arCoreTestRule.addTrackables(testAnnotation1)
            arCoreTestRule.addTrackables(testAnnotation2)
            advanceUntilIdle()

            var underTest = emptyList<SpatialAnnotation>()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                SpatialAnnotation.subscribe(session).collect { underTest = it.toList() }
            }
            advanceUntilIdle()
            SpatialAnnotation.stopTracking(session, listOf(SpatialAnnotationId.fromString("box-2")))
            advanceUntilIdle()

            assertThat(
                    underTest
                        .first { it.id == SpatialAnnotationId.fromString("box-2") }
                        .state
                        .value
                        .trackingState
                )
                .isEqualTo(TrackingState.STOPPED)
            assertThat(underTest.first { it.id == TEST_ANNOTATION_ID }.state.value.trackingState)
                .isEqualTo(TrackingState.TRACKING)
        }
}
