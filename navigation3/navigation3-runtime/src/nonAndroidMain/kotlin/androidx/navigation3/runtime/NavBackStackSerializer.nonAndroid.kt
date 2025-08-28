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

package androidx.navigation3.runtime

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

@Suppress("FunctionName") // Factory function.
public actual inline fun <reified T : Any> NavBackStackSerializer(
    configuration: SavedStateConfiguration
): KSerializer<SnapshotStateList<T>> {
    // Non-Android targets always use the provided `serializersModule`. You MUST also
    // pass the same configuration (or at least its serializersModule) to your
    // encode/decode calls. This is because polymorphic dispatch looks up subtypes
    // in the Encoder/Decoder’s module, not in the KSerializer instance itself.
    return SnapshotStateListSerializer(
        elementSerializer = configuration.serializersModule.serializer<T>()
    )
}
