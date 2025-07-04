/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.testing

import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.ConfigurationNotSupportedException
import androidx.xr.runtime.internal.PermissionNotGrantedException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeLifecycleManagerTest {

    lateinit var underTest: FakeLifecycleManager

    @Before
    fun setUp() {
        underTest = FakeLifecycleManager()
    }

    @Test
    fun create_setsStateToInitialized() {
        underTest.create()

        assertThat(underTest.state).isEqualTo(FakeLifecycleManager.State.INITIALIZED)
    }

    @Test
    fun create_calledTwice_throwsIllegalStateException() {
        underTest.create()

        assertFailsWith<IllegalStateException> { underTest.create() }
    }

    @Test
    fun create_afterResume_throwsIllegalStateException() {
        underTest.create()
        underTest.resume()

        assertFailsWith<IllegalStateException> { underTest.create() }
    }

    @Test
    fun create_afterPause_throwsIllegalStateException() {
        underTest.create()
        underTest.resume()
        underTest.pause()

        assertFailsWith<IllegalStateException> { underTest.create() }
    }

    @Test
    fun create_afterStop_throwsIllegalStateException() {
        underTest.create()
        underTest.stop()

        assertFailsWith<IllegalStateException> { underTest.create() }
    }

    @Test
    fun create_hasMissingPermission_throwsPermissionNotGrantedException() {
        underTest.hasCreatePermission = false

        assertFailsWith<PermissionNotGrantedException> { underTest.create() }
    }

    @Test
    fun configure_beforeCreate_doesNotThrowsIllegalStateException() {
        underTest.configure(Config())
    }

    @Test
    fun configure_afterStop_throwsIllegalStateException() {
        underTest.create()
        underTest.stop()

        assertFailsWith<IllegalStateException> { underTest.configure(Config()) }
    }

    @Test
    fun configure_hasMissingPermission_throwsPermissionNotGrantedException() {
        underTest.create()
        underTest.hasMissingPermission = true

        assertFailsWith<PermissionNotGrantedException> { underTest.configure(Config()) }
    }

    @Test
    fun configure_withFaceTrackingEnabled_doesNotSupportFaceTracking_throwsConfigurationNotSupported() {
        underTest.create()
        underTest.shouldSupportFaceTracking = false
        assertFailsWith<ConfigurationNotSupportedException> {
            underTest.configure(Config(faceTracking = Config.FaceTrackingMode.USER))
        }
    }

    @Test
    fun resume_afterCreate_setsStateToResumed() {
        underTest.create()

        underTest.resume()

        assertThat(underTest.state).isEqualTo(FakeLifecycleManager.State.RESUMED)
    }

    @Test
    fun resume_calledTwice_throwsIllegalStateException() {
        underTest.create()
        underTest.resume()

        assertFailsWith<IllegalStateException> { underTest.resume() }
    }

    @Test
    fun resume_beforeCreate_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> { underTest.resume() }
    }

    @Test
    fun resume_afterStop_throwsIllegalStateException() {
        underTest.create()
        underTest.stop()

        assertFailsWith<IllegalStateException> { underTest.resume() }
    }

    @Test
    fun update_beforeCreate_throwsIllegalStateException() = runTest {
        assertFailsWith<IllegalStateException> { underTest.update() }
    }

    @Test
    fun update_afterCreate_throwsIllegalStateException() = runTest {
        underTest.create()

        assertFailsWith<IllegalStateException> { underTest.update() }
    }

    @Test
    fun update_afterPause_throwsIllegalStateException() = runTest {
        underTest.create()
        underTest.resume()
        underTest.pause()

        assertFailsWith<IllegalStateException> { underTest.update() }
    }

    @Test
    fun update_afterStop_throwsIllegalStateException() = runTest {
        underTest.create()
        underTest.stop()

        assertFailsWith<IllegalStateException> { underTest.update() }
    }

    @Test
    fun update_returnsTimeMarkFromTimeSource() = runTest {
        val testDuration = 5.seconds
        underTest.create()
        underTest.resume()

        val timeMark = underTest.update()
        check(timeMark.elapsedNow().inWholeSeconds == 0L)
        underTest.timeSource += testDuration

        assertThat(timeMark.elapsedNow()).isEqualTo(testDuration)
    }

    @Test
    fun update_calledTwiceAfterAllowOneMoreCallToUpdate_resumesExecution() = runTest {
        val testDuration = 5.seconds
        underTest.create()
        underTest.resume()

        val firstTimeMark = underTest.update()
        underTest.timeSource += testDuration
        underTest.allowOneMoreCallToUpdate()
        val secondTimeMark = underTest.update()

        assertThat(secondTimeMark - firstTimeMark).isEqualTo(testDuration)
    }

    @Test
    fun pause_afterResume_setsStateToPaused() {
        underTest.create()
        underTest.resume()

        underTest.pause()

        assertThat(underTest.state).isEqualTo(FakeLifecycleManager.State.PAUSED)
    }

    @Test
    fun pause_calledTwice_throwsIllegalStateException() {
        underTest.create()
        underTest.resume()
        underTest.pause()

        assertFailsWith<IllegalStateException> { underTest.pause() }
    }

    @Test
    fun pause_beforeCreate_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> { underTest.pause() }
    }

    @Test
    fun pause_afterCreate_throwsIllegalStateException() {
        underTest.create()

        assertFailsWith<IllegalStateException> { underTest.pause() }
    }

    @Test
    fun pause_afterStop_throwsIllegalStateException() {
        underTest.create()
        underTest.stop()

        assertFailsWith<IllegalStateException> { underTest.pause() }
    }

    @Test
    fun stop_afterCreate_setsStateToStopped() {
        underTest.create()

        underTest.stop()

        assertThat(underTest.state).isEqualTo(FakeLifecycleManager.State.DESTROYED)
    }

    @Test
    fun stop_afterPause_setsStateToStopped() {
        underTest.create()
        underTest.resume()
        underTest.pause()

        underTest.stop()

        assertThat(underTest.state).isEqualTo(FakeLifecycleManager.State.DESTROYED)
    }

    @Test
    fun stop_calledTwice_throwsIllegalStateException() {
        underTest.create()
        underTest.stop()

        assertFailsWith<IllegalStateException> { underTest.stop() }
    }

    @Test
    fun stop_beforeCreate_throwsIllegalStateException() {
        assertFailsWith<IllegalStateException> { underTest.stop() }
    }

    @Test
    fun stop_afterResume_throwsIllegalStateException() {
        underTest.create()
        underTest.resume()

        assertFailsWith<IllegalStateException> { underTest.stop() }
    }
}
