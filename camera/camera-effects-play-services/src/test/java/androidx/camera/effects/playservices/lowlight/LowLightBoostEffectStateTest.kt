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
class LowLightBoostEffectStateTest {

    @Test
    fun factories_createExpectedStates() {
        val error = RuntimeException("Test error")

        assertThat(LowLightBoostEffectState.createOff().type)
            .isEqualTo(LowLightBoostEffectState.OFF)
        assertThat(LowLightBoostEffectState.createInactive().type)
            .isEqualTo(LowLightBoostEffectState.INACTIVE)
        assertThat(LowLightBoostEffectState.createActive().type)
            .isEqualTo(LowLightBoostEffectState.ACTIVE)
        assertThat(LowLightBoostEffectState.createPendingInstallation().type)
            .isEqualTo(LowLightBoostEffectState.PENDING_INSTALLATION)
        val errorState = LowLightBoostEffectState.createError(error)
        assertThat(errorState.type).isEqualTo(LowLightBoostEffectState.ERROR)
        assertThat(errorState.error).isSameInstanceAs(error)
    }

    @Test
    fun nonErrorStates_haveNoError() {
        assertThat(LowLightBoostEffectState.createOff().error).isNull()
        assertThat(LowLightBoostEffectState.createActive().error).isNull()
    }

    @Test
    fun errorWithNonErrorState_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LowLightBoostEffectState(LowLightBoostEffectState.ACTIVE, RuntimeException("Invalid"))
        }
    }

    @Test
    fun errorStateWithoutError_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LowLightBoostEffectState(LowLightBoostEffectState.ERROR, null)
        }
    }

    @Test
    fun statesWithSameValue_areEqual() {
        val state = LowLightBoostEffectState(LowLightBoostEffectState.ACTIVE)
        val other = LowLightBoostEffectState(LowLightBoostEffectState.ACTIVE)

        assertThat(state).isEqualTo(other)
        assertThat(state.hashCode()).isEqualTo(other.hashCode())
        assertThat(state).isNotEqualTo(LowLightBoostEffectState.createInactive())
    }

    @Test
    fun errorStates_areEqualOnlyWithSameErrorInstance() {
        val error = RuntimeException("Error")

        assertThat(LowLightBoostEffectState.createError(error))
            .isEqualTo(LowLightBoostEffectState.createError(error))
        assertThat(LowLightBoostEffectState.createError(error))
            .isNotEqualTo(LowLightBoostEffectState.createError(RuntimeException("Error")))
    }

    @Test
    fun toString_containsTypeName() {
        assertThat(LowLightBoostEffectState.createOff().toString()).contains("type=OFF")
        assertThat(LowLightBoostEffectState.createInactive().toString()).contains("type=INACTIVE")
        assertThat(LowLightBoostEffectState.createActive().toString()).contains("type=ACTIVE")
        assertThat(LowLightBoostEffectState.createPendingInstallation().toString())
            .contains("type=PENDING_INSTALLATION")
        assertThat(LowLightBoostEffectState.createError(RuntimeException()).toString())
            .contains("type=ERROR")
    }
}
