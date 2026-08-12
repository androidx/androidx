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

package androidx.navigation3.runtime.deeplink

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

interface TestInterface

@Serializable object TestInterfaceImplA : TestInterface

@Serializable object TestInterfaceImplB : TestInterface

@Serializable object TestInterfaceImplC : TestInterface

@Serializable
sealed class TestSealedClass {
    @Serializable data object Key1 : TestSealedClass()

    @Serializable data object Key2 : TestSealedClass()
}

@Serializable object TestKey

@Serializable data class SimpleKey(val name: String, val age: Int)

@Serializable data class DefaultKey(val name: String = "default", val age: Int = 0)

@Serializable data class NullableDefaultKey(val name: String? = null, val age: Int? = null)

@Serializable data class NullableKey(val name: String?, val age: Int?)

@Serializable data class NestedKey(val user: SimpleKey, val flag: Boolean)

@Serializable
enum class DirectionEnum {
    NORTH,
    SOUTH,
}

@Serializable data class EnumKey(val direction: DirectionEnum)

@Serializable data class DefaultEnumKey(val direction: DirectionEnum = DirectionEnum.NORTH)

@Serializable data class NestedEnumKey(val direction: EnumKey, val flag: Boolean)

@Serializable data class ListKey(val list: List<Int>)

@Serializable data class SetKey(val set: Set<String>)

@Serializable data class ArrayKey(val array: Array<Boolean>)

@Serializable data class NullableListKey(val list: List<Int?>)

@Serializable data class DefaultListKey(val list: List<Int> = emptyList())

@Serializable data class NonPrimitiveListKey(val list: List<SimpleKey>)

@Serializable data class NonEmptyDefaultListKey(val list: List<Int> = listOf(1, 2, 3))

@Serializable data class MapKey(val map: Map<Int, String>)

@Serializable data class BooleanKey(val bool: Boolean)

@Serializable data class ByteKey(val byte: Byte)

@Serializable data class ShortKey(val short: Short)

@Serializable data class LongKey(val long: Long)

@Serializable data class FloatKey(val float: Float)

@Serializable data class DoubleKey(val double: Double)

@Serializable data class CharKey(val char: Char)

@Serializable data class IntKey(val int: Int)

@Serializable data class TestDefaultArgKey(val name: String = "test")

@Serializable data class TestArgKey(val name: String)

class TestDeepLinkMatcher(filters: List<Filter> = emptyList()) :
    DeepLinkMatcher<TestKey, DeepLinkMatcher.MatchResult<TestKey>>(filters) {
    override fun matchRequest(request: DeepLinkRequest): MatchResult<TestKey> {
        return MatchResult(TestKey)
    }
}

class TestFilter(val filter: String) : DeepLinkMatcher.Filter {
    override fun filterRequest(request: DeepLinkRequest): Boolean {
        return filter == request.extras[MATCHER_STRING_FILTER_KEY]
    }
}

fun DeepLinkRequest.Companion.withStringExtra(uri: String, extra: String) =
    DeepLinkRequest(
        uri = DeepLinkUri(uri),
        extras = requestExtras { put(MATCHER_STRING_FILTER_KEY, extra) },
    )

const val MATCHER_STRING_FILTER_VALUE = "filterString"

public object MATCHER_STRING_FILTER_KEY : RequestExtrasKey<String>

@Serializable
abstract class SerializableAbstract {
    abstract val name: String
}

// interface Project

@Serializable
class SerializableConcrete(val owner: String, override val name: String) : SerializableAbstract()

@Serializable class SerializableConcreteArgKey(val derived: SerializableConcrete) : NavKey
