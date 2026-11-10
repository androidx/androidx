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

package androidx.compose.remote.creation.compose.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteIntArrayTest {
    val context = TestRemoteContext()
    val creationState = testCreationState()

    @Test
    fun arrayDeref_fetchesValueFromArray() {
        val remoteIntArray = RemoteIntArray(10, 20, 30)

        val result = remoteIntArray[1]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(20)
    }

    @Test
    fun arrayDeref_fetchesVariableFromArray() {
        val remoteIntArray =
            RemoteIntArray(listOf(10.ri, RemoteInt.createNamedRemoteInt("test", 20), 30.ri))

        val result = remoteIntArray[1]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(20)
    }

    @Test
    fun arrayDeref_variableIntIndexFetchesFromArray() {
        val remoteIntArray = RemoteIntArray(10, 20, 30)
        val index = 1.ri

        val result = remoteIntArray[index]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(20)
    }

    @Test
    fun arrayDeref_variableFloatIndexFetchesFromArray() {
        val remoteIntArray = RemoteIntArray(10, 20, 30)
        val index = 1f.rf

        val result = remoteIntArray[index]
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(20)
    }

    @Test
    fun toDebugString_creation() {
        val remoteIntArray = RemoteIntArray(1, 2, 3)
        assertThat(remoteIntArray.toDebugString()).isEqualTo("arrayOf(1, 2, 3)")
    }

    @Test
    fun toDebugString_indexing() {
        val remoteIntArray = RemoteIntArray(1, 2, 3)
        val idx = RemoteInt.createNamedRemoteInt("idx", 1)
        val result = remoteIntArray[idx]
        assertThat(result.toDebugString()).isEqualTo("arrayOf(1, 2, 3)[user:idx]")

        val floatIdx = RemoteFloat.createNamedRemoteFloat("f", 1f)
        assertThat(remoteIntArray[floatIdx].toDebugString()).isEqualTo("arrayOf(1, 2, 3)[user:f]")
    }

    private fun makeAndPaintCoreDocument() = context.loadDocument(creationState)
}
