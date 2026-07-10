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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.layout.ComponentStart
import androidx.compose.remote.core.operations.layout.ContainerEnd
import org.junit.Assert.assertThrows
import org.junit.Test

class ContainerNestingTest {

    @Test
    fun nestContainers_exceedsMaxDepth_throwsException() {
        val operations = ArrayList<Operation>()
        // Add MAX_NESTING_DEPTH + 1 ComponentStart operations
        for (i in 0..Limits.MAX_NESTING_DEPTH) {
            operations.add(ComponentStart(1, i, 0f, 0f))
        }

        assertThrows(RuntimeException::class.java) {
            CoreDocument.nestContainers(operations, true, null)
        }
    }

    @Test
    fun nestContainers_withinLimits_doesNotThrow() {
        val operations = ArrayList<Operation>()
        // Add MAX_NESTING_DEPTH ComponentStart operations
        for (i in 0 until Limits.MAX_NESTING_DEPTH) {
            operations.add(ComponentStart(1, i, 0f, 0f))
        }
        // Add matching ContainerEnds
        for (i in 0 until Limits.MAX_NESTING_DEPTH) {
            operations.add(ContainerEnd())
        }

        CoreDocument.nestContainers(operations, true, null)
    }
}
