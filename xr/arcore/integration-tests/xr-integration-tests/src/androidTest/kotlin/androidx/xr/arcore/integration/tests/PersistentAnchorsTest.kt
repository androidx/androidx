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

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorInvalidUuidException
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateTimedOut
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore Persistent Anchors ([Anchor] creation, persistence,
 * loading, and deletion).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class PersistentAnchorsTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.SCENE_UNDERSTANDING_FINE",
                "android.permission.HAND_TRACKING",
                "android.permission.HEAD_TRACKING",
                "android.permission.FACE_TRACKING",
                "android.permission.EYE_TRACKING_COARSE",
                "android.permission.EYE_TRACKING_FINE",
                "android.permission.INTERNET",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
            )
        )

    private suspend fun createSessionWithPersistence(
        scenario: ActivityScenario<ComponentActivity>,
        persistenceMode: AnchorPersistenceMode = AnchorPersistenceMode.LOCAL,
    ): Session {
        var activity: ComponentActivity? = null
        scenario.onActivity { activity = it }
        val result = Session.create(context = activity!!)
        when (result) {
            is SessionCreateApkRequired ->
                assumeTrue(
                    "Skipping test: Missing or outdated APK '${result.requiredApk}'",
                    false,
                )
            is SessionCreateUnsupportedDevice ->
                assumeTrue(
                    "Skipping test: Device does not support required XR features",
                    false,
                )
            else -> {}
        }
        check(result is SessionCreateSuccess) {
            when (result) {
                is SessionCreateUnknownError -> "SessionCreateUnknownError: ${result.errorMessage}"
                is SessionCreateTimedOut -> "SessionCreateTimedOut"
                else -> "Session.create failed with result: $result"
            }
        }
        val session = result.session
        val config =
            Config.Builder()
                .setAnchorPersistence(persistenceMode)
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .build()
        val configResult = session.configure(config)
        check(configResult is SessionConfigureSuccess) { "Failed to configure Session" }

        val state = withTimeoutOrNull(5000L) { session.state.first { it.perceptionState != null } }
        checkNotNull(state) { "Timed out waiting for perception state" }
        return session
    }

    private suspend fun unpersistAnchorAndWait(session: Session, uuid: UUID) {
        Anchor.unpersist(session, uuid)
        val result =
            withTimeoutOrNull(5000L) {
                while (Anchor.getPersistedAnchorUuids(session).contains(uuid)) {
                    delay(100)
                }
                true
            }
        checkNotNull(result) { "Timed out waiting for anchor to be unpersisted" }
    }

    @Test
    fun createAnchor_withValidPose_returnsAnchorCreateSuccess() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario)
                val testPose = Pose(Vector3(0f, 0f, -1.5f), Quaternion.Identity)

                val result = Anchor.create(session, testPose)

                check(result is AnchorCreateSuccess)
                val anchor = result.anchor
                assertThat(anchor.state.value.pose).isEqualTo(testPose)
            }
        }
    }

    @Test
    fun persistAnchor_whenAnchorPersistenceEnabled_returnsValidUuid() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario)
                val result = Anchor.create(session, Pose.Identity)
                check(result is AnchorCreateSuccess)
                val anchor = result.anchor

                val uuid = anchor.persist()
                try {
                    val persistedUuids = Anchor.getPersistedAnchorUuids(session)
                    assertThat(persistedUuids).contains(uuid)
                } finally {
                    runCatching { unpersistAnchorAndWait(session, uuid) }
                }
            }
        }
    }

    @Test
    fun getPersistedAnchorUuids_whenPersistenceDisabled_throwsIllegalStateException() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario, AnchorPersistenceMode.DISABLED)

                assertFailsWith<IllegalStateException> {
                    Anchor.getPersistedAnchorUuids(session)
                }
            }
        }
    }

    @Test
    fun persistAnchor_whenPersistenceDisabled_throwsIllegalStateException() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario, AnchorPersistenceMode.DISABLED)
                val result = Anchor.create(session, Pose.Identity)
                check(result is AnchorCreateSuccess)
                val anchor = result.anchor

                assertFailsWith<IllegalStateException> { anchor.persist() }
            }
        }
    }

    @Test
    fun loadAnchor_withPersistedUuid_returnsAnchorCreateSuccess() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario)
                val createResult = Anchor.create(session, Pose.Identity)
                check(createResult is AnchorCreateSuccess)
                val anchor = createResult.anchor
                val uuid = anchor.persist()
                try {
                    val loadResult = Anchor.load(session, uuid)

                    check(loadResult is AnchorCreateSuccess)
                    val loadedAnchor = loadResult.anchor
                    assertThat(loadedAnchor.state.value.pose).isEqualTo(Pose.Identity)
                } finally {
                    runCatching { unpersistAnchorAndWait(session, uuid) }
                }
            }
        }
    }

    @Test
    fun loadAnchor_withInvalidUuid_throwsAnchorInvalidUuidException() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario)

                assertFailsWith<AnchorInvalidUuidException> {
                    Anchor.load(session, UUID.randomUUID())
                }
            }
        }
    }

    @Test
    fun loadAnchor_whenPersistenceDisabled_throwsIllegalStateException() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario, AnchorPersistenceMode.DISABLED)

                assertFailsWith<IllegalStateException> {
                    Anchor.load(session, UUID.randomUUID())
                }
            }
        }
    }

    @Test
    fun unpersistAnchor_removesUuidFromPersistedList() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario)
                val createResult = Anchor.create(session, Pose.Identity)
                check(createResult is AnchorCreateSuccess)
                val anchor = createResult.anchor
                val uuid = anchor.persist()
                try {
                    assertThat(Anchor.getPersistedAnchorUuids(session)).contains(uuid)
                    unpersistAnchorAndWait(session, uuid)
                    assertThat(Anchor.getPersistedAnchorUuids(session)).doesNotContain(uuid)
                } finally {
                    if (Anchor.getPersistedAnchorUuids(session).contains(uuid)) {
                        runCatching { unpersistAnchorAndWait(session, uuid) }
                    }
                }
            }
        }
    }

    // TODO(b/565784971): OpenXrPerceptionManager.unpersistAnchor currently throws
    // IllegalStateException via check(). Update to expect AnchorInvalidUuidException
    // once arcore-openxr is updated.
    @Test
    fun unpersistAnchor_withInvalidUuid_throwsIllegalStateException() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario)

                assertFailsWith<IllegalStateException> {
                    Anchor.unpersist(session, UUID.randomUUID())
                }
            }
        }
    }

    @Test
    fun unpersistAnchor_whenPersistenceDisabled_throwsIllegalStateException() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario, AnchorPersistenceMode.DISABLED)

                assertFailsWith<IllegalStateException> {
                    Anchor.unpersist(session, UUID.randomUUID())
                }
            }
        }
    }

    @Test
    fun detachAnchor_completesSuccessfully() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session = createSessionWithPersistence(scenario)
                val createResult =
                    Anchor.create(session, Pose(Vector3(0f, 0f, -1f), Quaternion.Identity))
                check(createResult is AnchorCreateSuccess)
                val anchor = createResult.anchor

                anchor.detach()
            }
        }
    }
}
