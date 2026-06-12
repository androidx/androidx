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

package androidx.compose.remote.player.core

import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.ComponentStart
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteDocumentLimitsTest {

    @Test
    fun constructor_exceedsNestingDepth_throwsException() {
        val buffer = RemoteComposeBuffer()
        // Write standard header using apiLevel = 8
        Header.apply(buffer.buffer, 8, shortArrayOf(), arrayOf())
        // Write too many nested component starts
        for (i in 0..Limits.MAX_NESTING_DEPTH) {
            ComponentStart.apply(buffer.buffer, 1, i, 0f, 0f)
        }
        val bytes = buffer.buffer.cloneBytes()

        // Constructing RemoteDocument should throw RuntimeException due to nesting limit
        assertThrows(RuntimeException::class.java) { RemoteDocument(bytes) }
    }
}
