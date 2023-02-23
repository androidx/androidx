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

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Change
import androidx.compose.runtime.RememberManager
import androidx.compose.runtime.SlotWriter
import androidx.compose.runtime.changelist.Operation.BackwardsCompatOp

internal class ChangeList : OperationsDebugStringFormattable {

    private val operations = Operations()

    val size: Int get() = operations.size
    fun isEmpty() = operations.isEmpty()
    fun isNotEmpty() = operations.isNotEmpty()

    fun clear() {
        operations.clear()
    }

    fun executeAndFlushAllPendingChanges(
        applier: Applier<*>,
        slots: SlotWriter,
        rememberManager: RememberManager
    ) = operations.executeAndFlushAllPendingOperations(applier, slots, rememberManager)

    fun pushBackwardsCompatChange(change: Change) {
        operations.push(BackwardsCompatOp) {
            setObject(BackwardsCompatOp.Change, change)
        }
    }

    override fun toDebugString(linePrefix: String): String {
        return buildString {
            append("ChangeList instance containing")
            append(size)
            append(" operations")
            if (isNotEmpty()) {
                append(":\n")
                append(operations.toDebugString(linePrefix))
            }
        }
    }
}