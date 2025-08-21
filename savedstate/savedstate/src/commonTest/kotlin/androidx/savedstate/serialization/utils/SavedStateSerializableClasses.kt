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

package androidx.savedstate.serialization.utils

import androidx.savedstate.serialization.CLASS_DISCRIMINATOR_KEY
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable internal data class IntData(val value: Int)

@Serializable internal data class LongData(val value: Long)

@Serializable internal data class ShortData(val value: Short)

@Serializable internal data class ByteData(val value: Byte)

@Serializable internal data class BooleanData(val value: Boolean)

@Serializable internal data class CharData(val value: Char)

@Serializable internal data class FloatData(val value: Float)

@Serializable internal data class DoubleData(val value: Double)

@Serializable internal data class StringData(val value: String)

@Serializable internal data class NullData(val value: String?)

@Serializable internal data class EnumData(val base1: Enum, val base2: Enum)

@Serializable
internal enum class Enum {
    OptionA,
    OptionB,
}

@Serializable internal data class BoxData<T>(val value: T)

@Serializable internal sealed class Sealed

@Serializable internal data class SealedImpl1(val value: Int) : Sealed()

@Serializable internal data class SealedImpl2(val value: String) : Sealed()

@Serializable internal data class SealedData(val base1: Sealed, val base2: Sealed)

@Serializable
internal object ObjectData {
    val value: String = "myState"
}

@Serializable
internal data class ClassDiscriminatorConflict(@SerialName(CLASS_DISCRIMINATOR_KEY) val type: Int)

@Serializable @SerialName("SerialName1") internal data class SerialNameType(val value: Int)

@Serializable
internal data class SerialNameData(@SerialName("SerialName2") val value: SerialNameType)
