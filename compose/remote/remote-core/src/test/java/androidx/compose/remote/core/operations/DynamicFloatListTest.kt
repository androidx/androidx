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

package androidx.compose.remote.core.operations

import androidx.compose.remote.core.RemoteComposeState
import androidx.compose.remote.core.RemoteContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DynamicFloatListTest {

    @Test
    fun updateVariables_invalidLength_throwsException() {
        val varId = 100
        val nanLength = Utils.asNan(varId)
        val list = DataDynamicListFloat(1, nanLength)

        val context = mock<RemoteContext>()
        whenever(context.getFloat(varId)).thenReturn(2001f) // Exceeds MAX_FLOAT_ARRAY = 2000

        assertThrows(RuntimeException::class.java) { list.updateVariables(context) }
    }

    @Test
    fun updateVariables_negativeLength_throwsException() {
        val varId = 100
        val nanLength = Utils.asNan(varId)
        val list = DataDynamicListFloat(1, nanLength)

        val context = mock<RemoteContext>()
        whenever(context.getFloat(varId)).thenReturn(-5f)

        assertThrows(RuntimeException::class.java) { list.updateVariables(context) }
    }

    @Test
    fun updateVariables_validLength_resizesCorrectly() {
        val varId = 100
        val nanLength = Utils.asNan(varId)
        val list = DataDynamicListFloat(1, nanLength)

        val context = mock<RemoteContext>()
        whenever(context.getFloat(varId)).thenReturn(10f)

        list.updateVariables(context)
        assertEquals(10, list.length)
    }

    @Test
    fun updateDynamicFloatList_apply_negativeIndex_doesNotThrow() {
        val state = RemoteComposeState()
        val listId = 1
        val list = DataDynamicListFloat(listId, 10f)
        state.addCollection(listId, list)

        val context = mock<RemoteContext>()
        context.mRemoteComposeState = state

        // 1. Negative index should be ignored (no exception)
        val opNegative = UpdateDynamicFloatList(listId, -1f, 99f)
        opNegative.apply(context)

        // 2. Out of bounds index should be ignored (no exception)
        val opOutOfBounds = UpdateDynamicFloatList(listId, 10f, 99f)
        opOutOfBounds.apply(context)
    }
}
