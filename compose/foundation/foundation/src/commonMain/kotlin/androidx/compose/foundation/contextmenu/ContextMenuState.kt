/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.contextmenu

import androidx.compose.foundation.contextmenu.ContextMenuState.Status
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified

private const val UNSPECIFIED_OFFSET_ERROR_MESSAGE =
    "ContextMenuState.Status should never be open with an unspecified offset. " +
        "Use ContextMenuState.Status.Closed instead."

/** Holds state related to the context menu. */
internal class ContextMenuState internal constructor(initialStatus: Status = Status.Closed) {
    var status by mutableStateOf(initialStatus)

    override fun toString(): String = "ContextMenuState(status=$status)"

    override fun hashCode(): Int = status.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ContextMenuState) return false
        return other.status == this.status
    }

    /** The status of the context menu. Can be [Open] or [Closed]. */
    sealed class Status {
        /** An open context menu [Status]. */
        class Open(
            /** The offset to open the menu at. It must be specified. */
            val offset: Offset
        ) : Status() {
            init {
                checkPrecondition(offset.isSpecified) { UNSPECIFIED_OFFSET_ERROR_MESSAGE }
            }

            override fun toString(): String = "Open(offset=$offset)"

            override fun hashCode(): Int = offset.hashCode()

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is Open) return false
                return this.offset == other.offset
            }
        }

        /** A closed context menu [Status]. */
        object Closed : Status() {
            override fun toString(): String = "Closed"
        }
    }
}

/** Convenience method to set the state's status to [Status.Closed]. */
internal fun ContextMenuState.close() {
    status = Status.Closed
}
