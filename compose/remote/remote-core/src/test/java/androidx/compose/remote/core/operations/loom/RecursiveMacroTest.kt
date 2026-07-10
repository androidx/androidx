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

package androidx.compose.remote.core.operations.loom

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.DebugMessage
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecursiveMacroTest {

    private class MockDocument : CoreDocument()

    @Test
    fun testSelfReferencingMacro_throwsRuntimeException() {
        val doc = MockDocument()
        val loomManager = LoomManager()
        loomManager.setSafeMode(false)

        val macroId = 10
        val patternDefine = PatternDefine(macroId, intArrayOf())

        val bodyBuffer = RemoteComposeBuffer()
        val selfCall = PatternInflation(macroId, intArrayOf())
        selfCall.write(bodyBuffer.buffer)
        patternDefine.setBody(bodyBuffer.buffer.cloneBytes())

        loomManager.add(patternDefine, "recursiveMacro")

        val operations = ArrayList<Operation>()
        operations.add(selfCall)

        assertThrows(RuntimeException::class.java) { loomManager.expandAll(operations, doc) }
    }

    @Test
    fun testSelfReferencingMacro_withSafeMode_returnsDebugMessage() {
        val doc = MockDocument()
        val loomManager = LoomManager()
        loomManager.setSafeMode(true)

        val macroId = 10
        val patternDefine = PatternDefine(macroId, intArrayOf())

        val bodyBuffer = RemoteComposeBuffer()
        val selfCall = PatternInflation(macroId, intArrayOf())
        selfCall.write(bodyBuffer.buffer)
        patternDefine.setBody(bodyBuffer.buffer.cloneBytes())

        loomManager.add(patternDefine, "recursiveMacro")

        val operations = ArrayList<Operation>()
        operations.add(selfCall)

        val expanded = loomManager.expandAll(operations, doc)

        assertTrue(expanded.any { it is DebugMessage })
    }
}
