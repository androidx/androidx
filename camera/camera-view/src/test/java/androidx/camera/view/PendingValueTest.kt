/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.view

import android.os.Build
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [PendingValue].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class PendingValueTest {

    @Test
    fun assignPendingValueAfterPropagation_valueAssigned() {
        // Arrange: create a pending value.
        val pendingValue = PendingValue<Boolean>()
        var assignedValue: Boolean? = null

        // Act: set the pending value and propagate the result.
        val future1 = pendingValue.setValue(true)
        pendingValue.propagateIfHasValue {
            assignedValue = it
            Futures.immediateFuture(null)
        }
        // Assert: value is propagated to core. App future is done.
        assertThat(assignedValue).isTrue()
        assertThat(future1.isDone).isTrue()

        // Act: set the pending value again.
        val future2 = pendingValue.setValue(false)
        pendingValue.propagateIfHasValue {
            assignedValue = it
            Futures.immediateFuture(null)
        }
        // Assert: value is propagated to core. App future is done.
        assertThat(assignedValue).isFalse()
        assertThat(future2.isDone).isTrue()
    }

    @Test
    fun propagationTwiceForTheSameAssignment_theSecondTimeIsNoOp() {
        // Arrange: create a pending value.
        val pendingValue = PendingValue<Boolean>()

        // Act: set the pending value and propagate the result.
        val future1 = pendingValue.setValue(true)
        var assignedValue: Boolean? = null
        pendingValue.propagateIfHasValue {
            assignedValue = it
            Futures.immediateFuture(null)
        }
        // Assert: value is propagated to core. App future is done.
        assertThat(assignedValue).isTrue()
        assertThat(future1.isDone).isTrue()

        // Act: propagate the result again. e.g. when camera is restarted
        var assignedValue2: Boolean? = null
        pendingValue.propagateIfHasValue {
            assignedValue2 = it
            Futures.immediateFuture(null)
        }
        // Assert: the value set in the previous session will not be propagated.
        assertThat(assignedValue2).isNull()
    }

    @Test
    fun assignPendingValueTwice_theSecondValueIsAssigned() {
        // Arrange.
        val pendingValue = PendingValue<Boolean>()

        // Act: set value twice: false then true.
        val future1 = pendingValue.setValue(false)
        val future2 = pendingValue.setValue(true)

        // Assert: the value is true.
        var assignedValue: Boolean? = null
        pendingValue.propagateIfHasValue {
            assignedValue = it
            Futures.immediateFuture(null)
        }
        assertThat(assignedValue).isTrue()
        assertThat(future1.isCancelled).isTrue()
        assertThat(future2.isCancelled).isFalse()
        assertThat(future2.isDone).isTrue()
    }
}
