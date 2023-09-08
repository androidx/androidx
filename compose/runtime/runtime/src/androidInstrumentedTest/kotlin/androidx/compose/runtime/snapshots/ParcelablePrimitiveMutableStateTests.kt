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

package androidx.compose.runtime.snapshots

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import kotlin.test.assertEquals
import org.junit.Test

class ParcelablePrimitiveMutableStateTests {
    @Test
    fun saveAndRestoreMutableIntState() {
        val state = mutableIntStateOf(0)
        state.intValue = 1

        val restored = recreateViaParcel(state)
        assertEquals(1, restored.intValue)
    }

    @Test
    fun saveAndRestoreMutableLongState() {
        val state = mutableLongStateOf(0L)
        state.longValue = 1

        val restored = recreateViaParcel(state)
        assertEquals(1, restored.longValue)
    }

    @Test
    fun saveAndRestoreMutableFloatState() {
        val state = mutableFloatStateOf(0f)
        state.floatValue = 1.5f

        val restored = recreateViaParcel(state)
        assertEquals(1.5f, restored.floatValue)
    }

    @Test
    fun saveAndRestoreMutableDoubleState() {
        val state = mutableDoubleStateOf(0.0)
        state.doubleValue = 1.5

        val restored = recreateViaParcel(state)
        assertEquals(1.5, restored.doubleValue)
    }

    private inline fun <reified T> recreateViaParcel(value: T): T {
        val parcel = Parcel.obtain().apply {
            writeParcelable(value as Parcelable, 0)
            setDataPosition(0)
        }
        @Suppress("DEPRECATION")
        return parcel.readParcelable<Parcelable>(javaClass.classLoader) as T
    }
}
