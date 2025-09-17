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

package androidx.compose.runtime.changelist

import androidx.compose.runtime.Anchor
import androidx.compose.runtime.Applier
import androidx.compose.runtime.RememberManager
import androidx.compose.runtime.SlotWriter
import androidx.compose.runtime.changelist.Operation.InsertNodeFixup
import androidx.compose.runtime.changelist.Operation.PostInsertNodeFixup
import androidx.compose.runtime.changelist.Operation.UpdateNode
import androidx.compose.runtime.runtimeCheck

internal class FixupList : OperationsDebugStringFormattable() {
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

    fun createAndInsertNode(factory: () -> Any?, insertIndex: Int, groupAnchor: Anchor) {
        operations.push(InsertNodeFixup) {
            setObject(InsertNodeFixup.Factory, factory)
            setInt(InsertNodeFixup.InsertIndex, insertIndex)
            setObject(InsertNodeFixup.GroupAnchor, groupAnchor)
        }

        pendingOperations.push(PostInsertNodeFixup) {
            setInt(PostInsertNodeFixup.InsertIndex, insertIndex)
            setObject(PostInsertNodeFixup.GroupAnchor, groupAnchor)
        }
    }

    fun endNodeInsert() {
        runtimeCheck(pendingOperations.isNotEmpty()) {
            "Cannot end node insertion, there are no pending operations that can be realized."
        }
        pendingOperations.popInto(operations)
    }

    fun <V, T> updateNode(value: V, block: T.(V) -> Unit) {
        operations.push(UpdateNode) {
            setObject(UpdateNode.Value, value)
            setObject(UpdateNode.Block, @Suppress("UNCHECKED_CAST") (block as Any?.(Any?) -> Unit))
        }
    }

    override fun toDebugString(linePrefix: String): String {
        return buildString {
            append("FixupList instance containing $size operations")
            if (isNotEmpty()) append(":\n${operations.toDebugString(linePrefix)}")
        }
    }
}
