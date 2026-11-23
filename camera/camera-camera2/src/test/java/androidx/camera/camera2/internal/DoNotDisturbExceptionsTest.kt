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

package androidx.camera.camera2.internal

import androidx.camera.camera2.pipe.DoNotDisturbException
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.CameraUnavailableException.CAMERA_UNAVAILABLE_DO_NOT_DISTURB
import androidx.camera.core.impl.CameraUpdateException
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class DoNotDisturbExceptionsTest {

    @Test
    fun directDoNotDisturbException_mapsToDoNotDisturbInitializationException() {
        val dnd = DoNotDisturbException("Do Not Disturb mode is on")

        val initException = checkNotNull(dnd.toDoNotDisturbInitializationExceptionOrNull())

        val unavailable = initException.cause as CameraUnavailableException
        assertThat(unavailable.reason).isEqualTo(CAMERA_UNAVAILABLE_DO_NOT_DISTURB)
        assertThat(unavailable.cause).isSameInstanceAs(dnd)
    }

    @Test
    fun wrappedDoNotDisturbException_unwrapsCauseChain() {
        val dnd = DoNotDisturbException("Do Not Disturb mode is on")
        val wrapped = CameraUpdateException("Failed to query camera metadata", dnd)

        val initException = checkNotNull(wrapped.toDoNotDisturbInitializationExceptionOrNull())

        val unavailable = initException.cause as CameraUnavailableException
        assertThat(unavailable.reason).isEqualTo(CAMERA_UNAVAILABLE_DO_NOT_DISTURB)
        assertThat(unavailable.cause).isSameInstanceAs(dnd)
    }

    @Test
    fun alreadyMappedInitializationException_returnsSameInstanceWithoutDoubleWrapping() {
        val dnd = DoNotDisturbException("Do Not Disturb mode is on")
        val existing = checkNotNull(dnd.toDoNotDisturbInitializationExceptionOrNull())

        assertThat(existing.toDoNotDisturbInitializationExceptionOrNull())
            .isSameInstanceAs(existing)
    }

    @Test
    fun cyclicCauseChainWithoutDoNotDisturb_terminatesAndReturnsNull() {
        val first = RuntimeException("first")
        val second = RuntimeException("second", first)
        first.initCause(second)

        assertThat(first.toDoNotDisturbInitializationExceptionOrNull()).isNull()
        assertThat(first.toDoNotDisturbCameraUnavailableExceptionOrNull()).isNull()
    }

    @Test
    fun unrelatedRuntimeException_returnsNull() {
        val unrelated = RuntimeException("Unrelated failure")

        assertThat(unrelated.toDoNotDisturbInitializationExceptionOrNull()).isNull()
        assertThat(unrelated.toDoNotDisturbCameraUnavailableExceptionOrNull()).isNull()
    }
}
