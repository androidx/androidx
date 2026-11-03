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

import android.os.Build
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class BlockingCallTrackerTest {

    private val tracker = BlockingCallTracker()

    @Test
    fun call_returnsResultAndAddsBlockedTime() {
        // Act.
        val result = tracker.call {
            ShadowSystemClock.advanceBy(BLOCKED_MS, TimeUnit.MILLISECONDS)
            "result"
        }

        // Assert.
        assertThat(result).isEqualTo("result")
        assertThat(tracker.blockedMsSinceLastFrame).isEqualTo(BLOCKED_MS)
    }

    @Test
    fun callThatThrows_stillAddsBlockedTime() {
        // Act.
        assertThrows(IllegalStateException::class.java) {
            tracker.call {
                ShadowSystemClock.advanceBy(BLOCKED_MS, TimeUnit.MILLISECONDS)
                throw IllegalStateException("Binder failed")
            }
        }

        // Assert.
        assertThat(tracker.blockedMsSinceLastFrame).isEqualTo(BLOCKED_MS)
    }

    @Test
    fun blockedTime_accumulatesUntilInputFrameIsHandled() {
        // Arrange.
        repeat(2) {
            tracker.call { ShadowSystemClock.advanceBy(BLOCKED_MS, TimeUnit.MILLISECONDS) }
        }
        val blockedMsBeforeFrame = tracker.blockedMsSinceLastFrame

        // Act.
        tracker.onInputFrameHandled()

        // Assert.
        assertThat(blockedMsBeforeFrame).isEqualTo(2 * BLOCKED_MS)
        assertThat(tracker.blockedMsSinceLastFrame).isEqualTo(0L)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun cameraSurfaceOf_returnsSessionCameraSurface() {
        // Arrange.
        val session = FakeLowLightBoostSession()

        // Act & Assert.
        try {
            assertThat(tracker.cameraSurfaceOf(session))
                .isSameInstanceAs(session.cameraSurfaceForTest)
        } finally {
            session.release()
            session.cameraSurfaceTexture.release()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun cameraSurfaceOfDeadSession_isNull() {
        // Arrange: getCameraSurface() throws, e.g. after Google Play Services died.
        val session = FakeLowLightBoostSession().apply { throwOnCall = true }

        // Act & Assert.
        try {
            assertThat(tracker.cameraSurfaceOf(session)).isNull()
        } finally {
            session.release()
            session.cameraSurfaceTexture.release()
        }
    }

    private companion object {
        private const val BLOCKED_MS = 100L
    }
}
