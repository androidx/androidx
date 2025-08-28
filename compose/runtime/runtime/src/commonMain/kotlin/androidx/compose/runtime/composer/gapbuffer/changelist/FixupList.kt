/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.composer.gapbuffer.changelist

import androidx.compose.runtime.Applier
import androidx.compose.runtime.composer.DebugStringFormattable
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.composer.gapbuffer.GapAnchor
import androidx.compose.runtime.composer.gapbuffer.SlotWriter
import androidx.compose.runtime.runtimeCheck
import androidx.compose.runtime.tooling.OperationErrorContext

internal class FixupList : DebugStringFormattable() {
    private val operations = Operations()
    private val pendingOperations = Operations()

    val size: Int
        get() = operations.size

    fun isEmpty() = operations.isEmpty()

    fun isNotEmpty() = operations.isNotEmpty()

    fun clear() {
        pendingOperations.clear()
        operations.clear()
    }

    fun executeAndFlushAllPendingFixups(
        applier: Applier<*>,
        slots: SlotWriter,
        rememberManager: RememberManager,
        errorContext: OperationErrorContext?,
    ) {
        runtimeCheck(pendingOperations.isEmpty()) {
            "FixupList has pending fixup operations that were not realized. " +
                "Were there mismatched insertNode() and endNodeInsert() calls?"
        }
        operations.executeAndFlushAllPendingOperations(
            applier,
            slots,
            rememberManager,
            errorContext,
        )
    }

    fun createAndInsertNode(factory: () -> Any?, insertIndex: Int, groupAnchor: GapAnchor) {
        operations.push(Operation.InsertNodeFixup) {
            setObject(Operation.InsertNodeFixup.Factory, factory)
            setInt(Operation.InsertNodeFixup.InsertIndex, insertIndex)
            setObject(Operation.InsertNodeFixup.GroupAnchor, groupAnchor)
        }

        pendingOperations.push(Operation.PostInsertNodeFixup) {
            setInt(Operation.PostInsertNodeFixup.InsertIndex, insertIndex)
            setObject(Operation.PostInsertNodeFixup.GroupAnchor, groupAnchor)
        }
    }

    fun endNodeInsert() {
        runtimeCheck(pendingOperations.isNotEmpty()) {
            "Cannot end node insertion, there are no pending operations that can be realized."
        }
        pendingOperations.popInto(operations)
    }

    fun <V, T> updateNode(value: V, block: T.(V) -> Unit) {
        operations.push(Operation.UpdateNode) {
            setObject(Operation.UpdateNode.Value, value)
            setObject(
                Operation.UpdateNode.Block,
                @Suppress("UNCHECKED_CAST") (block as Any?.(Any?) -> Unit),
            )
        }
    }

    override fun toDebugString(linePrefix: String): String {
        return buildString {
            append("FixupList instance containing $size operations")
            if (isNotEmpty()) append(":\n${operations.toDebugString(linePrefix)}")
        }
    }
}
