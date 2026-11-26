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

package androidx.compose.remote.creation.compose.intervals

import androidx.compose.remote.creation.compose.state.BooleanOperationKey

/** External interval semantics for [BooleanOperationKey]. */
internal object RemoteBooleanIntervalSemantics : RemoteOperationIntervalSemantics {
    override fun isBooleanSelect(op: Any): Boolean =
        when (op as BooleanOperationKey) {
            BooleanOperationKey.SelectString,
            BooleanOperationKey.SelectFloat,
            BooleanOperationKey.SelectInt,
            BooleanOperationKey.SelectBoolean -> true
            else -> false
        }
}
