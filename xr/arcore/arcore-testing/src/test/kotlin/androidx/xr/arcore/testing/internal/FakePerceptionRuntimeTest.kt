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

package androidx.xr.arcore.testing.internal

import androidx.kruth.assertThat
import androidx.xr.runtime.Config
import androidx.xr.runtime.FaceTrackingMode
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakePerceptionRuntimeTest {

    internal lateinit var underTest: FakePerceptionRuntime

    @Before
    fun setUp() {
        underTest = FakePerceptionRuntime(FakePerceptionManager())
    }

    @Test
    fun initialize_setsStateToInitialized() {
        underTest.initialize()

        assertThat(underTest.state).isEqualTo(FakePerceptionRuntime.State.INITIALIZED)
    }

    @Test
    fun initialize_calledTwice_throwsIllegalStateException() {
        underTest.initialize()

        assertFailsWith<IllegalStateException> { underTest.initialize() }
    }

    @Test
    fun initialize_afterResume_throwsIllegalStateException() {
        underTest.initialize()
        underTest.resume()

        assertFailsWith<IllegalStateException> { underTest.initialize() }
    }

    @Test
    fun initialize_afterPause_throwsIllegalStateException() {
        underTest.initialize()
        underTest.resume()
        underTest.pause()

        assertFailsWith<IllegalStateException> { underTest.initialize() }
    }

    @Test
    fun initialize_afterDestroy_throwsIllegalStateException() {
        underTest.initialize()
        underTest.destroy()

        assertFailsWith<IllegalStateException> { underTest.initialize() }
    }

    @Test
    fun initialize_hasMissingPermission_throwsSecurityException() {
        underTest.hasCreatePermission = false

        assertFailsWith<SecurityException> { underTest.initialize() }
    }

    @Test
    fun configure_beforeCreate_doesNotThrowsIllegalStateException() {
        underTest.configure(Config())
    }

    @Test
    fun configure_afterDestroy_throwsIllegalStateException() {
        underTest.initialize()
        underTest.destroy()

        assertFailsWith<IllegalStateException> { underTest.configure(Config()) }
    }

    @Test
    fun configure_hasMissingPermission_throwsSecurityException() {
        underTest.initialize()
        underTest.hasMissingPermission = true

        assertFailsWith<SecurityException> { underTest.configure(Config()) }
    }

    @Test
    fun configure_withFaceTrackingEnabled_doesNotSupportFaceTracking_throwsUnsupportedOperationException() {
        underTest.initialize()
        underTest.shouldSupportFaceTracking = false
        assertFailsWith<UnsupportedOperationException> {
            underTest.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        }
    }

    @Test
    fun resume_afterInitialize_setsStateToResumed() {
        underTest.initialize()

        underTest.resume()

        assertThat(underTest.state).isEqualTo(FakePerceptionRuntime.State.RESUMED)
    }

    @Test
    fun resume_calledTwice_throwsIllegalStateException() {
        underTest.initialize()
        underTest.resume()

        assertFailsWith<IllegalStateException> { underTest.resume() }
    }

    @Test
    fun resume_beforeInitialize_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> { underTest.resume() }
    }

    @Test
    fun resume_afterDestroy_throwsIllegalStateException() {
        underTest.initialize()
        underTest.destroy()

        assertFailsWith<IllegalStateException> { underTest.resume() }
    }

    @Test
    fun update_beforeInitialize_throwsIllegalStateException() = runTest {
        assertFailsWith<IllegalStateException> { underTest.update() }
    }

    @Test
    fun update_afterInitialize_throwsIllegalStateException() = runTest {
        underTest.initialize()

        assertFailsWith<IllegalStateException> { underTest.update() }
    }

    @Test
    fun update_afterPause_throwsIllegalStateException() = runTest {
        underTest.initialize()
        underTest.resume()
        underTest.pause()

        assertFailsWith<IllegalStateException> { underTest.update() }
    }

    @Test
    fun update_afterDestroy_throwsIllegalStateException() = runTest {
        underTest.initialize()
        underTest.destroy()

        assertFailsWith<IllegalStateException> { underTest.update() }
    }

    @Test
    fun update_returnsTimeMarkFromTimeSource() = runTest {
        val testDuration = 5.seconds
        underTest.initialize()
        underTest.resume()

        val timeMark = underTest.update()
        check(timeMark.elapsedNow().inWholeSeconds == 0L)
        underTest.timeSource += testDuration

        assertThat(timeMark.elapsedNow()).isEqualTo(testDuration)
    }

    @Test
    fun update_calledTwiceAfterAllowOneMoreCallToUpdate_resumesExecution() = runTest {
        val testDuration = 5.seconds
        underTest.initialize()
        underTest.resume()

        val firstTimeMark = underTest.update()
        underTest.timeSource += testDuration
        FakePerceptionRuntime.allowOneMoreCallToUpdate()
        val secondTimeMark = underTest.update()

        assertThat(secondTimeMark - firstTimeMark).isEqualTo(testDuration)
    }

    @Test
    fun pause_afterResume_setsStateToPaused() {
        underTest.initialize()
        underTest.resume()

        underTest.pause()

        assertThat(underTest.state).isEqualTo(FakePerceptionRuntime.State.PAUSED)
    }

    @Test
    fun pause_calledTwice_throwsIllegalStateException() {
        underTest.initialize()
        underTest.resume()
        underTest.pause()

        assertFailsWith<IllegalStateException> { underTest.pause() }
    }

    @Test
    fun pause_beforeInitialize_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> { underTest.pause() }
    }

    @Test
    fun pause_afterInitialize_throwsIllegalStateException() {
        underTest.initialize()

        assertFailsWith<IllegalStateException> { underTest.pause() }
    }

    @Test
    fun pause_afterDestroy_throwsIllegalStateException() {
        underTest.initialize()
        underTest.destroy()

        assertFailsWith<IllegalStateException> { underTest.pause() }
    }

    @Test
    fun stop_afterInitialize_setsStateToStopped() {
        underTest.initialize()

        underTest.destroy()

        assertThat(underTest.state).isEqualTo(FakePerceptionRuntime.State.DESTROYED)
    }

    @Test
    fun destroy_afterPause_setsStateToStopped() {
        underTest.initialize()
        underTest.resume()
        underTest.pause()

        underTest.destroy()

        assertThat(underTest.state).isEqualTo(FakePerceptionRuntime.State.DESTROYED)
    }

    @Test
    fun destroy_calledTwice_throwsIllegalStateException() {
        underTest.initialize()
        underTest.destroy()

        assertFailsWith<IllegalStateException> { underTest.destroy() }
    }

    @Test
    fun destroy_beforeInitialize_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> { underTest.destroy() }
    }

    @Test
    fun destroy_afterResume_throwsIllegalStateException() {
        underTest.initialize()
        underTest.resume()

        assertFailsWith<IllegalStateException> { underTest.destroy() }
    }
}
