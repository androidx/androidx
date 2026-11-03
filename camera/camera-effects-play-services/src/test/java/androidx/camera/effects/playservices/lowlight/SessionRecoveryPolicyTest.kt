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
package androidx.camera.effects.playservices.lowlight

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SessionRecoveryPolicyTest {

    private val policy = SessionRecoveryPolicy()

    @Test
    fun firstAttempt_isAllowedImmediately() {
        assertThat(policy.canAttemptRecovery(nowMs = START_MS)).isTrue()
    }

    @Test
    fun attempts_areThrottled() {
        // Arrange.
        policy.onRecoveryAttempted(START_MS)

        // Act & Assert.
        assertThat(policy.canAttemptRecovery(START_MS + 1_999L)).isFalse()
        assertThat(policy.canAttemptRecovery(START_MS + 2_000L)).isTrue()
    }

    @Test
    fun attempts_areCappedUntilReset() {
        // Arrange: the cap is reached.
        repeat(3) { policy.onRecoveryAttempted(START_MS + it * 10_000L) }
        val canAttemptAfterCap = policy.canAttemptRecovery(START_MS + 60_000L)

        // Act.
        policy.reset()

        // Assert.
        assertThat(canAttemptAfterCap).isFalse()
        assertThat(policy.canAttemptRecovery(START_MS + 60_000L)).isTrue()
    }

    @Test
    fun reset_allowsImmediateAttempt() {
        // Arrange.
        policy.onRecoveryAttempted(START_MS)

        // Act.
        policy.reset()

        // Assert.
        assertThat(policy.canAttemptRecovery(START_MS + 1L)).isTrue()
    }

    @Test
    fun deathOfStableSession_resetsCap() {
        // Arrange: the cap is reached.
        repeat(3) { policy.onRecoveryAttempted(START_MS + it * 10_000L) }

        // Act: a session that stayed alive for 10 seconds dies.
        policy.onSessionDied(sessionStartTimeMs = START_MS, nowMs = START_MS + 10_000L)

        // Assert.
        assertThat(policy.canAttemptRecovery(START_MS + 60_000L)).isTrue()
    }

    @Test
    fun deathOfShortLivedSession_keepsCap() {
        // Arrange: the cap is reached.
        repeat(3) { policy.onRecoveryAttempted(START_MS + it * 10_000L) }

        // Act: a session dies right after its creation.
        policy.onSessionDied(sessionStartTimeMs = START_MS, nowMs = START_MS + 9_999L)

        // Assert.
        assertThat(policy.canAttemptRecovery(START_MS + 60_000L)).isFalse()
    }

    private companion object {
        private const val START_MS = 100_000L
    }
}
