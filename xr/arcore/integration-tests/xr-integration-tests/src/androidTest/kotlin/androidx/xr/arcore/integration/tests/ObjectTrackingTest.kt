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

package androidx.xr.arcore.integration.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.AugmentedObject
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [AugmentedObject] subscription and category-based 3D
 * bounding-box tracking.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ObjectTrackingTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HEAD_TRACKING",
            )
        )

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    private suspend fun configureObjectTrackingSession(
        targetCategories: Set<AugmentedObjectCategory>
    ) {
        val session = sessionRule.session
        // TODO: b/581124636 - Include non-object modes in multi-feature config tests once native
        // AbortConfigureSession regression is resolved.
        val config = Config.Builder().setAugmentedObjectCategories(targetCategories).build()
        val configResult =
            try {
                session.configure(config)
            } catch (e: UnsupportedOperationException) {
                throw AssumptionViolatedException(
                    "Skipping test: AugmentedObject tracking is not supported: ${e.message}",
                    e,
                )
            }
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure AugmentedObjectCategories: $configResult"
        }
        checkNotNull(
            withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                session.state.drop(1).first { it.perceptionState != null }
            }
        ) {
            "Timed out waiting for perception state"
        }
    }

    /**
     * Positive configuration and [AugmentedObject.subscribe] no-throw smoke check complementing
     * [augmentedObject_subscribeWhenCategoriesEmpty_throwsIllegalStateException] when no physical
     * objects are in view.
     */
    @Test
    @XrDeviceTest
    fun configureAndSubscribe_whenCategoriesConfigured_enablesConfigAndSubscribesWithoutThrowing() {
        val session = sessionRule.session
        // TODO: b/581124636 - Include non-object modes in multi-feature config tests once native
        // AbortConfigureSession regression is resolved.
        val config = Config.Builder().setAugmentedObjectCategories(TARGET_CATEGORIES).build()

        val configResult =
            try {
                session.configure(config)
            } catch (e: UnsupportedOperationException) {
                throw AssumptionViolatedException(
                    "Skipping test: AugmentedObject tracking is not supported: ${e.message}",
                    e,
                )
            }
        AugmentedObject.subscribe(session)

        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertThat(session.config.augmentedObjectCategories)
            .containsExactlyElementsIn(TARGET_CATEGORIES)
    }

    @Test
    @XrDeviceTest
    fun augmentedObject_whenObjectDetected_emitsTrackedObjectWithBounded3dExtents() {
        runBlocking {
            val session = sessionRule.session
            configureObjectTrackingSession(TARGET_CATEGORIES)
            val objectsFlow = AugmentedObject.subscribe(session)

            // TODO: b/575891923 - Migrate to synthetic/mocked object detection once Pillar 5
            // perception simulation supports augmented objects so extent assertions run
            // deterministically without physical objects in view.
            // Drive discovery from per-frame session.state rather than objectsFlow alone:
            // AugmentedObject uses identity equality, so objectsFlow.stateIn can conflate [obj]
            // when obj transitions PAUSED -> TRACKING without adding or removing list items.
            val trackedState =
                withTimeoutOrNull(OBJECT_DISCOVERY_TIMEOUT_MS) {
                    session.state
                        .mapNotNull { coreState ->
                            coreState.perceptionState
                                ?.trackableStates
                                ?.filterIsInstance<AugmentedObject.State>()
                                ?.firstOrNull { it.trackingState == TrackingState.TRACKING }
                        }
                        .first()
                }
            assumeTrue(
                "Skipping object assertions: No tracked keyboard/mouse/laptop in camera view",
                trackedState != null,
            )
            val state = checkNotNull(trackedState)
            val subscribedObjects =
                withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                    objectsFlow.first { state.owner in it }
                }

            val nonNullSubscribedObjects =
                assertNotNull(
                    subscribedObjects,
                    "Timed out waiting for AugmentedObject.subscribe to emit tracked object",
                )
            for (augmentedObject in nonNullSubscribedObjects) {
                val currentState = augmentedObject.state.value
                assertThat(currentState.owner).isEqualTo(augmentedObject)
                assertThat(currentState.category).isIn(TARGET_CATEGORIES)
            }
            assertThat(state.category).isIn(TARGET_CATEGORIES)
            assertThat(state.centerPose.translation.x).isFinite()
            assertThat(state.centerPose.translation.y).isFinite()
            assertThat(state.centerPose.translation.z).isFinite()
            val rotation = state.centerPose.rotation
            assertThat(rotation.x).isFinite()
            assertThat(rotation.y).isFinite()
            assertThat(rotation.z).isFinite()
            assertThat(rotation.w).isFinite()
            assertThat(state.extents.width).isGreaterThan(0f)
            assertThat(state.extents.width).isLessThan(MAX_OBJECT_EXTENT_METERS)
            assertThat(state.extents.height).isGreaterThan(0f)
            assertThat(state.extents.height).isLessThan(MAX_OBJECT_EXTENT_METERS)
            assertThat(state.extents.depth).isGreaterThan(0f)
            assertThat(state.extents.depth).isLessThan(MAX_OBJECT_EXTENT_METERS)
        }
    }

    @Test
    @XrDeviceTest
    fun augmentedObject_subscribeWhenCategoriesEmpty_throwsIllegalStateException() {
        val session = sessionRule.session
        val config = Config.Builder().setAugmentedObjectCategories(emptySet()).build()

        val configResult = session.configure(config)

        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertFailsWith<IllegalStateException> { AugmentedObject.subscribe(session) }
    }

    companion object {
        private const val FRAME_TIMEOUT_MS = 5_000L
        private const val OBJECT_DISCOVERY_TIMEOUT_MS = 10_000L
        private const val MAX_OBJECT_EXTENT_METERS = 1.0f

        private val TARGET_CATEGORIES: Set<AugmentedObjectCategory> =
            setOf(
                AugmentedObjectCategory.KEYBOARD,
                AugmentedObjectCategory.MOUSE,
                AugmentedObjectCategory.LAPTOP,
            )
    }
}
