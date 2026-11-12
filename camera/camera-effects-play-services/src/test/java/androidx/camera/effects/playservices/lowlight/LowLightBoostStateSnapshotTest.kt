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
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class LowLightBoostStateSnapshotTest {

    @Test
    fun sharedSnapshots_haveExpectedStatusAndNoError() {
        assertThat(LowLightBoostStateSnapshot.OFF.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
        assertThat(LowLightBoostStateSnapshot.INACTIVE.status)
            .isEqualTo(LowLightBoostEffect.STATUS_INACTIVE)
        assertThat(LowLightBoostStateSnapshot.ACTIVE.status)
            .isEqualTo(LowLightBoostEffect.STATUS_ACTIVE)
        assertThat(LowLightBoostStateSnapshot.PENDING_INSTALLATION.status)
            .isEqualTo(LowLightBoostEffect.STATUS_PENDING_INSTALLATION)
        listOf(
                LowLightBoostStateSnapshot.OFF,
                LowLightBoostStateSnapshot.INACTIVE,
                LowLightBoostStateSnapshot.ACTIVE,
                LowLightBoostStateSnapshot.PENDING_INSTALLATION,
            )
            .forEach { assertThat(it.error).isNull() }
    }

    @Test
    fun errorSnapshot_hasStatusOffAndError() {
        val error = RuntimeException("Test error")

        val snapshot = LowLightBoostStateSnapshot.error(error)

        assertThat(snapshot.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
        assertThat(snapshot.error).isSameInstanceAs(error)
    }

    @Test
    fun errorWithStatusOtherThanOff_throws() {
        listOf(
                LowLightBoostEffect.STATUS_INACTIVE,
                LowLightBoostEffect.STATUS_ACTIVE,
                LowLightBoostEffect.STATUS_PENDING_INSTALLATION,
            )
            .forEach { status ->
                assertThrows(IllegalArgumentException::class.java) {
                    LowLightBoostStateSnapshot(status, RuntimeException("Invalid"))
                }
            }
    }

    @Test
    fun snapshotsWithSameValues_areEqual() {
        val snapshot = LowLightBoostStateSnapshot(LowLightBoostEffect.STATUS_ACTIVE)

        assertThat(snapshot).isEqualTo(LowLightBoostStateSnapshot.ACTIVE)
        assertThat(snapshot.hashCode()).isEqualTo(LowLightBoostStateSnapshot.ACTIVE.hashCode())
        assertThat(snapshot).isNotEqualTo(LowLightBoostStateSnapshot.INACTIVE)
    }

    @Test
    fun errorSnapshots_areEqualOnlyWithSameErrorInstance() {
        val error = RuntimeException("Error")

        assertThat(LowLightBoostStateSnapshot.error(error))
            .isEqualTo(LowLightBoostStateSnapshot.error(error))
        assertThat(LowLightBoostStateSnapshot.error(error))
            .isNotEqualTo(LowLightBoostStateSnapshot.error(RuntimeException("Error")))
        assertThat(LowLightBoostStateSnapshot.error(error))
            .isNotEqualTo(LowLightBoostStateSnapshot.OFF)
    }

    @Test
    fun toString_containsStatusNameAndError() {
        assertThat(LowLightBoostStateSnapshot.OFF.toString()).contains("status=OFF")
        assertThat(LowLightBoostStateSnapshot.INACTIVE.toString()).contains("status=INACTIVE")
        assertThat(LowLightBoostStateSnapshot.ACTIVE.toString()).contains("status=ACTIVE")
        assertThat(LowLightBoostStateSnapshot.PENDING_INSTALLATION.toString())
            .contains("status=PENDING_INSTALLATION")
        assertThat(LowLightBoostStateSnapshot.error(RuntimeException("Boom")).toString())
            .contains("Boom")
    }
}
