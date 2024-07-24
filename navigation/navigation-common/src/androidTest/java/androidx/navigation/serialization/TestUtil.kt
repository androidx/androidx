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

package androidx.navigation.serialization

import android.os.Bundle
import androidx.navigation.CollectionNavType
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable internal data class CustomTypeWithArg(val id: Int)

@Serializable
@SerialName(PATH_SERIAL_NAME)
internal class TestClassCollectionArg(val list: List<CustomTypeWithArg>)

internal val collectionNavType =
    object : CollectionNavType<List<CustomTypeWithArg>>(false) {
        override fun put(bundle: Bundle, key: String, value: List<CustomTypeWithArg>) {
            bundle.putStringArrayList(key, ArrayList(value.map { it.id.toString() }))
        }

        override fun serializeAsValues(value: List<CustomTypeWithArg>): List<String> =
            value.map { it.id.toString() }

        override fun emptyCollection(): List<CustomTypeWithArg> = emptyList()

        override fun get(bundle: Bundle, key: String): List<CustomTypeWithArg> {
            return bundle.getStringArrayList(key)?.map { CustomTypeWithArg(it.toInt()) }
                ?: emptyList()
        }

        override fun parseValue(value: String): List<CustomTypeWithArg> = listOf()

        override fun serializeAsValue(value: List<CustomTypeWithArg>) = "CustomTypeWithArg"
    }

internal fun stringArgument(name: String, hasDefaultValue: Boolean = false) =
    navArgument(name) {
        type = NavType.StringType
        nullable = false
        unknownDefaultValuePresent = hasDefaultValue
    }

internal fun nullableStringArgument(name: String, hasDefaultValue: Boolean = false) =
    navArgument(name) {
        type = NavType.StringType
        nullable = true
        unknownDefaultValuePresent = hasDefaultValue
    }

internal fun intArgument(name: String, hasDefaultValue: Boolean = false) =
    navArgument(name) {
        type = NavType.IntType
        nullable = false
        unknownDefaultValuePresent = hasDefaultValue
    }

internal fun <D : Enum<*>> enumArgument(
    name: String,
    clazz: Class<D>,
    hasDefaultValue: Boolean = false
) =
    navArgument(name) {
        type = NavType.EnumType(clazz)
        nullable = false
        unknownDefaultValuePresent = hasDefaultValue
    }

@OptIn(InternalSerializationApi::class)
internal fun <T> KSerializer<T>.expectedSafeArgsId(): Int = generateHashCode()
