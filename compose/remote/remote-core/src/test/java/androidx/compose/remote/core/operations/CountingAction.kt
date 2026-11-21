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
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.ActionOperation
import androidx.compose.remote.core.operations.utilities.StringSerializer
import androidx.compose.remote.core.serialize.MapSerializer

/** An [ActionOperation] that counts how many times it ran, for pointer input tests. */
internal class CountingAction : Operation(), ActionOperation {
    var count = 0

    override fun runAction(context: RemoteContext, document: CoreDocument) {
        count++
    }

    override fun serializeToString(indent: Int, serializer: StringSerializer) {}

    override fun serialize(serializer: MapSerializer) {}

    override fun write(buffer: WireBuffer) {}

    override fun apply(context: RemoteContext) {}

    override fun deepToString(indent: String): String = "CountingAction"
}
