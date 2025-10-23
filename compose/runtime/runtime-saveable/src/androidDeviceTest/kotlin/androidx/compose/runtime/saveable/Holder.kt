/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.saveable

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal data class Holder(var value: Int)

internal val HolderSaver = Saver<Holder, Int>(save = { it.value }, restore = { Holder(it) })

internal object HolderSerializer : KSerializer<Holder> {

    private val valueSerializer = Int.serializer()

    override val descriptor =
        buildClassSerialDescriptor(Holder::class.qualifiedName!!) {
            element("value", PrimitiveSerialDescriptor("value", PrimitiveKind.INT))
        }

    override fun serialize(encoder: Encoder, value: Holder) {
        encoder.encodeSerializableValue(valueSerializer, value.value)
    }

    override fun deserialize(decoder: Decoder): Holder =
        Holder(decoder.decodeSerializableValue(valueSerializer))
}
