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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer

/**
 * Provides a [NavBackStack] that is automatically remembered in the Compose hierarchy across
 * process death and config changes.
 *
 * Classes/objects added to the [NavBackStack] should be annotated with [Serializable] to ensure
 * they can be saved and restored properly.
 *
 * @param elements the starting keys of this backStack
 */
@Composable
public fun <T : NavKey> rememberNavBackStack(vararg elements: T): NavBackStack {
    return rememberSaveable(saver = snapshotStateListSaver(serializableListSaver())) {
        elements.toList().toMutableStateList()
    }
}

/** A List of objects that extend the [NavKey] marker class. */
public typealias NavBackStack = SnapshotStateList<NavKey>

internal fun <T : Any> serializableListSaver(
    serializer: KSerializer<T> = UnsafePolymorphicSerializer()
) =
    listSaver<List<T>, SavedState>(
        save = { list -> list.map { encodeToSavedState(serializer, it) } },
        restore = { list -> list.map { decodeFromSavedState(serializer, it) } },
    )

@Suppress("UNCHECKED_CAST")
internal fun <T> snapshotStateListSaver(
    listSaver: Saver<List<T>, out Any> = autoSaver()
): Saver<SnapshotStateList<T>, Any> =
    with(listSaver as Saver<List<T>, Any>) {
        Saver(
            save = { state ->
                // We use toMutableList() here to ensure that save() is
                // sent a list that is saveable by default (e.g., something
                // that autoSaver() can handle)
                save(state.toList().toMutableList())
            },
            restore = { state -> restore(state)?.toMutableStateList() },
        )
    }

@OptIn(InternalSerializationApi::class)
internal class UnsafePolymorphicSerializer<T : Any> : KSerializer<T> {

    override val descriptor =
        buildClassSerialDescriptor("PolymorphicData") {
            element(elementName = "type", serialDescriptor<String>())
            element(elementName = "payload", buildClassSerialDescriptor("Any"))
        }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        return decoder.decodeStructure(descriptor) {
            val className = decodeStringElement(descriptor, decodeElementIndex(descriptor))
            val classRef = Class.forName(className).kotlin
            val serializer = classRef.serializer()

            decodeSerializableElement(descriptor, decodeElementIndex(descriptor), serializer) as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            val className = value::class.java.name
            encodeStringElement(descriptor, index = 0, className)
            val serializer = value::class.serializer() as KSerializer<T>
            encodeSerializableElement(descriptor, index = 1, serializer, value)
        }
    }
}
