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