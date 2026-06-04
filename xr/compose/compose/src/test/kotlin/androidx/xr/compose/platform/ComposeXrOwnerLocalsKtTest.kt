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
// TODO(b/502276582): Remove Suppression once the rest of aosp/4029203 are submitted
@file:Suppress("DEPRECATION")

package androidx.xr.compose.platform

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeXrOwnerLocalsKtTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun composeXrOwnerLocals_nonActivityContext_sessionIsNull() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalContext provides ApplicationProvider.getApplicationContext()
            ) {
                assertNull(LocalComposeXrOwners.current.session)
            }
        }
    }

    @Test
    fun composeXrOwnerLocals_activityContext_returnsNonNull() {
        composeTestRule.setContent { assertNotNull(LocalComposeXrOwners.current) }
    }

    @Test
    fun composeXrOwnerLocals_nonXr_sessionIsNull() {
        composeTestRule.activity.disableXr()

        composeTestRule.setContent { assertNull(LocalComposeXrOwners.current.session) }
    }

    @Test
    fun composeXrOwnerLocals_sessionCannotBeCreated_sessionIsNull() {
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session_factory,
            { null },
        )

        composeTestRule.setContent { assertNull(LocalComposeXrOwners.current.session) }
    }

    @Test
    fun composeXrOwnerLocals_sessionThrowsException_sessionIsNull() {
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session_factory,
            { throw IllegalStateException() },
        )

        composeTestRule.setContent { assertNull(LocalComposeXrOwners.current.session) }
    }

    @Test
    fun composeXrOwnerLocals_asyncSessionCreation_resolvesEventually() {
        val fakeSession = composeTestRule.activity.configureFakeSession()
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session,
            null,
        )
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session_factory_dispatcher,
            Dispatchers.IO,
        )

        val latch = java.util.concurrent.CountDownLatch(1)
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session_factory,
            {
                latch.await()
                fakeSession
            },
        )

        var initialSession: androidx.xr.runtime.Session? = null
        var locals: ComposeXrOwnerLocals? = null
        composeTestRule.setContent {
            locals = LocalComposeXrOwners.current
            initialSession = locals?.session
        }

        // Initially, session should be null because the coroutine is scheduled asynchronously on IO
        // thread and factory is delayed waiting on the latch.
        assertNull(initialSession)

        // Release the latch so that the asynchronous session creation can proceed.
        latch.countDown()

        val resolvedLocals = assertNotNull(locals)

        // Wait for the looper and background tasks to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            resolvedLocals.session != null
        }

        // Eventually, the session is resolved to non-null
        assertNotNull(resolvedLocals.session)
    }

    @Test
    fun getOrCreatesClearedAndRecreated_onActivityRecreation() {
        var locals1: ComposeXrOwnerLocals? = null
        var locals2: ComposeXrOwnerLocals? = null

        composeTestRule.setContent {
            locals1 = LocalComposeXrOwners.current
            locals2 = LocalComposeXrOwners.current
        }

        val resolvedLocals1 = assertNotNull(locals1)
        val resolvedLocals2 = assertNotNull(locals2)

        assertThat(resolvedLocals1).isSameInstanceAs(resolvedLocals2)

        val activity1 = composeTestRule.activity
        val decorView1 = activity1.window.decorView

        // Verify that the tags are correctly established in the View hierarchy
        assertThat(decorView1.getTag(androidx.xr.compose.R.id.compose_xr_owner_locals))
            .isSameInstanceAs(resolvedLocals1)

        // Phase 2: Recreate the activity. This destroys the old activity and its
        // lifecycle, which should trigger our observer to clear the cache.
        composeTestRule.activityRule.scenario.recreate()

        // Verify that the cache has been cleared.
        assertNull(decorView1.getTag(androidx.xr.compose.R.id.compose_xr_owner_locals))

        // Phase 3: Verify that a new, distinct instance is created for the new activity.
        var locals3: ComposeXrOwnerLocals? = null
        composeTestRule.setContent { locals3 = LocalComposeXrOwners.current }

        val resolvedLocals3 = assertNotNull(locals3)
        assertThat(resolvedLocals3).isNotSameInstanceAs(resolvedLocals1)
    }

    // TODO(b/502276582): Remove Suppression once the rest of aosp/4029203 are submitted
    @Suppress("DEPRECATION")
    @Test
    fun getOrCreate_spatialModeChanged_updatesStateAndNode() {
        val session = composeTestRule.configureFakeSession()
        val fakeSceneRuntime =
            session.runtimes
                .filterIsInstance<androidx.xr.scenecore.testing.FakeSceneRuntime>()
                .first()

        var locals: ComposeXrOwnerLocals? = null

        composeTestRule.setContent { locals = LocalComposeXrOwners.current }
        composeTestRule.waitForIdle()

        val xrLocals = assertNotNull(locals)

        val spatialConfiguration =
            assertNotNull(xrLocals.spatialConfiguration as? SessionSpatialConfiguration)

        assertThat(spatialConfiguration.recommendedScale).isEqualTo(1f)
        assertThat(spatialConfiguration.recommendedPose).isEqualTo(Pose.Identity)

        val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)
        val expectedScale = 2.5f

        composeTestRule.runOnIdle {
            fakeSceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(
                recommendedPose = expectedPose,
                recommendedScale = Vector3(expectedScale, expectedScale, expectedScale),
            )
        }
        composeTestRule.waitForIdle()

        // Verify Compose State updated successfully
        assertThat(spatialConfiguration.recommendedScale).isEqualTo(expectedScale)
        assertThat(spatialConfiguration.recommendedPose).isEqualTo(expectedPose)

        // Verify subspaceRootNode scaled and translated correctly in SceneCore
        val rootNode = xrLocals.subspaceRootNode

        assertThat(rootNode?.getScale(Space.ACTIVITY)).isEqualTo(expectedScale)
        assertThat(rootNode?.getPose(Space.ACTIVITY)).isEqualTo(expectedPose)
    }

    @Test
    fun getOrCreateSession_calledConcurrently_initializesSessionOnlyOnce() {
        var factoryCallCount = 0
        val activity = composeTestRule.activity
        val fakeSession = composeTestRule.activity.configureFakeSession()
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session,
            null,
        )
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session_factory_dispatcher,
            Dispatchers.IO,
        )
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session_factory,
            {
                factoryCallCount++
                fakeSession
            },
        )

        val sessions =
            java.util.Collections.synchronizedList(mutableListOf<androidx.xr.runtime.Session?>())

        // Call getOrCreateSession concurrently 10 times utilizing lifecycleScope
        (1..10).forEach {
            activity.lifecycleScope.launch(Dispatchers.Main) {
                val session = activity.getOrCreateSession()
                sessions.add(session)
            }
        }

        // Wait for the looper and background tasks to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            sessions.size == 10
        }

        // All 10 calls should return the same session
        assertThat(sessions).containsExactlyElementsIn(List(10) { fakeSession })

        // The factory should only be invoked once
        assertThat(factoryCallCount).isEqualTo(1)
    }

    @Suppress("DEPRECATION")
    @Test
    fun composeXrOwnerLocals_outsideSubspace_mainPanelInheritsRecommendedScale() {
        val session = composeTestRule.configureFakeSession()
        val fakeSceneRuntime = session.runtimes.filterIsInstance<FakeSceneRuntime>().first()
        var locals: ComposeXrOwnerLocals? = null

        composeTestRule.setContent { locals = LocalComposeXrOwners.current }
        composeTestRule.waitForIdle()

        val xrLocals = assertNotNull(actual = locals)

        val expectedScale = 2.5f
        composeTestRule.runOnIdle {
            fakeSceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(
                recommendedPose = Pose.Identity,
                recommendedScale = Vector3(x = expectedScale, y = expectedScale, z = expectedScale),
            )
        }
        composeTestRule.waitForIdle()

        // Verify the mainPanelEntity's parent is set to the subspaceRootNode by default
        assertThat(session.scene.mainPanelEntity.parent).isEqualTo(xrLocals.subspaceRootNode)

        // Because mainPanelEntity is parented to subspaceRootNode, it will passively inherit this
        // scale.
        val spatialConfiguration =
            assertNotNull(actual = xrLocals.spatialConfiguration as? SessionSpatialConfiguration)
        assertThat(spatialConfiguration.recommendedScale).isEqualTo(expectedScale)
        assertThat(xrLocals.subspaceRootNode?.getScale(relativeTo = Space.ACTIVITY))
            .isEqualTo(expectedScale)
    }
}
