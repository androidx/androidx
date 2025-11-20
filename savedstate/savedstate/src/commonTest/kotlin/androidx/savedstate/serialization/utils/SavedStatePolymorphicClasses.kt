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

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable internal abstract class PolymorphicClass

@Serializable internal data class PolymorphicClassImpl1(val value: Int) : PolymorphicClass()

@Serializable internal data class PolymorphicClassImpl2(val value: String) : PolymorphicClass()

@Serializable
internal data class PolymorphicClassData(val base1: PolymorphicClass, val base2: PolymorphicClass)

internal interface PolymorphicInterface

@Serializable internal data class PolymorphicInterfaceImpl1(val value: Int) : PolymorphicInterface

@Serializable
internal data class PolymorphicInterfaceImpl2(val value: String) : PolymorphicInterface

@Serializable
internal data class PolymorphicInterfaceData(
    val base1: PolymorphicInterface,
    val base2: PolymorphicInterface,
)

@Serializable
internal data class PolymorphicNullMixedData(
    val base1: PolymorphicClass?,
    val base2: PolymorphicInterface?,
)

@Serializable
internal data class PolymorphicMixedData(
    val base1: PolymorphicClass,
    val base2: PolymorphicInterface,
)

internal val polymorphicTestModule = SerializersModule {
    polymorphic(PolymorphicClass::class) {
        subclass(PolymorphicClassImpl1.serializer())
        subclass(PolymorphicClassImpl2.serializer())
    }

    polymorphic(PolymorphicInterface::class) {
        subclass(PolymorphicInterfaceImpl1.serializer())
        subclass(PolymorphicInterfaceImpl2.serializer())
    }
}
