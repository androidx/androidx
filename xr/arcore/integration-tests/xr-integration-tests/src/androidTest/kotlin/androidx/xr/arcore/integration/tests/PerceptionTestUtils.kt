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

import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.arcore.perceptionState
import androidx.xr.arcore.runtime.PerceptionManager
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

// TODO(b/584835486): Consolidate shared ARCore perception test helpers with
// ArCoreTestUtils once in-flight ARCore integration test CLs land.
internal val SESSION_STATE_TIMEOUT = 5.seconds
internal val PERCEPTION_DISCOVERY_TIMEOUT = 10.seconds
internal const val FLOAT_TOLERANCE = 1e-3f
internal const val EXTENTS_TOLERANCE_METERS = 0.02f
private val DEFAULT_POLL_INTERVAL = 50.milliseconds

internal fun Session.requirePerceptionManager(): PerceptionManager =
    runtimes.filterIsInstance<PerceptionRuntime>().single().perceptionManager

internal fun waitForPerceptionStateUpdate(
    session: Session,
    description: String = "perception state update",
) {
    val stateBeforeWait = session.state.value
    waitUntil(description, timeout = SESSION_STATE_TIMEOUT) {
        val currentState = session.state.value
        currentState !== stateBeforeWait && currentState.perceptionState != null
    }
}

internal fun configureSessionAndWaitForPerception(session: Session, config: Config) {
    val configResult = session.configure(config)
    check(configResult is SessionConfigureSuccess) {
        "Failed to configure session: $configResult"
    }
    waitForPerceptionStateUpdate(session)
}

internal fun <T : Any> waitUntilFoundOrNull(
    timeout: Duration = PERCEPTION_DISCOVERY_TIMEOUT,
    pollInterval: Duration = DEFAULT_POLL_INTERVAL,
    probe: () -> T?,
): T? {
    require(!timeout.isNegative()) { "timeout must be non-negative, was $timeout" }
    require(!pollInterval.isNegative()) { "pollInterval must be non-negative, was $pollInterval" }
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "waitUntilFoundOrNull must be called from the instrumentation thread, not the main thread."
    }
    val start = TimeSource.Monotonic.markNow()
    while (true) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val result = probe()
        if (result != null) {
            return result
        }
        if (start.elapsedNow() >= timeout) {
            return null
        }
        CountDownLatch(1).await(pollInterval.inWholeNanoseconds, TimeUnit.NANOSECONDS)
    }
}

internal fun assertFinitePose(pose: Pose) {
    assertThat(pose.translation.x).isFinite()
    assertThat(pose.translation.y).isFinite()
    assertThat(pose.translation.z).isFinite()
    assertThat(pose.rotation.x).isFinite()
    assertThat(pose.rotation.y).isFinite()
    assertThat(pose.rotation.z).isFinite()
    assertThat(pose.rotation.w).isFinite()
}
