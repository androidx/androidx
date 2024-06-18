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

package androidx.navigation.testing

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.CollectionNavType
import androidx.navigation.NavArgument
import androidx.navigation.NavType
import androidx.navigation.serialization.RouteEncoder
import androidx.navigation.serialization.generateNavArguments
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * SavedStateHandle constructor to create a SavedStateHandle with a serializable object.
 *
 * Returns a [SavedStateHandle] populated with arguments from [route].
 *
 * Note: To use this api in non-instrumented tests, run with robolectric. This is because android's
 * [Bundle] is necessarily integrated into the parsing of route arguments.
 *
 * @param route The route to extract argument values from
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 */
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST", "DEPRECATION")
public operator fun SavedStateHandle.Companion.invoke(
    route: Any,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap()
): SavedStateHandle {
    val serializer = route::class.serializer()
    // generate type maps
    val namedArgs: MutableMap<String, NavArgument> =
        mutableMapOf<String, NavArgument>().apply {
            serializer.generateNavArguments(typeMap).map { put(it.name, it.argument) }
        }
    val finalTypeMap = mutableMapOf<String, NavType<Any?>>()
    namedArgs.forEach {
        // replace built-in string navtypes to avoid Uri.encode/decodes, so that the test factory
        // can be used in non-instrumented test
        val finalNavType =
            when (it.value.type) {
                NavType.StringListType -> CommonizedStringListType
                NavType.StringArrayType -> CommonizedStringArrayType
                NavType.StringType -> CommonizedStringType
                else -> it.value.type
            }
                as NavType<Any?>
        finalTypeMap[it.key] = finalNavType
    }
    // encode route to map of arg values
    val argValues = RouteEncoder(serializer, finalTypeMap).encodeToArgMap(route)
    val bundle = bundleOf()
    // parse and put arg values into bundle
    argValues.forEach { entry ->
        val argName = entry.key
        val type = finalTypeMap[entry.key]
        checkNotNull(type) {
            "SavedStateHandleFactory could not locate NavType for argument [$argName]. Please" +
                "provide NavType in typeMap."
        }
        val tempBundle = bundleOf()
        // start collection navtypes with empty list unless it has default
        if (type is CollectionNavType && !namedArgs[argName]?.isDefaultValuePresent!!) {
            type.put(tempBundle, argName, type.emptyCollection())
        }
        entry.value.forEach { value ->
            try {
                if (!tempBundle.containsKey(argName)) {
                    type.parseAndPut(tempBundle, argName, value)
                } else {
                    val previousValue = type[tempBundle, argName]
                    type.parseAndPut(tempBundle, argName, value, previousValue)
                }
            } catch (e: IllegalArgumentException) {
                // parse failed, ignored
            }
        }
        bundle.putAll(tempBundle)
    }
    // convert arg bundle to arg map
    val finalMap = mutableMapOf<String, Any?>()
    bundle.keySet().forEach { name -> finalMap[name] = bundle[name] }
    // populate handle with arg map
    return SavedStateHandle(finalMap)
}

private val CommonizedStringType: NavType<String?> =
    object : NavType<String?>(true) {
        override val name: String
            get() = "string"

        override fun put(bundle: Bundle, key: String, value: String?) {
            bundle.putString(key, value)
        }

        @Suppress("DEPRECATION")
        override fun get(bundle: Bundle, key: String): String? {
            return bundle[key] as String?
        }

        /**
         * Returns input value by default.
         *
         * If input value is "null", returns null as the reversion of Kotlin standard library
         * serializing null receivers of [kotlin.toString] into "null".
         */
        override fun parseValue(value: String): String? {
            return if (value == "null") null else value
        }

        /**
         * Returns default value of Uri.encode(value).
         *
         * If input value is null, returns "null" in compliance with Kotlin standard library parsing
         * null receivers of [kotlin.toString] into "null".
         */
        override fun serializeAsValue(value: String?): String {
            return value ?: "null"
        }
    }

private val CommonizedStringArrayType: NavType<Array<String>?> =
    object : CollectionNavType<Array<String>?>(true) {
        override val name: String
            get() = "string[]"

        override fun put(bundle: Bundle, key: String, value: Array<String>?) {
            bundle.putStringArray(key, value)
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        override fun get(bundle: Bundle, key: String): Array<String>? {
            return bundle[key] as Array<String>?
        }

        override fun parseValue(value: String): Array<String> {
            return arrayOf(value)
        }

        override fun parseValue(value: String, previousValue: Array<String>?): Array<String>? {
            return previousValue?.plus(parseValue(value)) ?: parseValue(value)
        }

        override fun valueEquals(value: Array<String>?, other: Array<String>?) =
            value.contentDeepEquals(other)

        override fun serializeAsValues(value: Array<String>?): List<String> =
            value?.toList() ?: emptyList()

        override fun emptyCollection(): Array<String> = arrayOf()
    }

private val CommonizedStringListType: NavType<List<String>?> =
    object : CollectionNavType<List<String>?>(true) {
        override val name: String
            get() = "List<String>"

        override fun put(bundle: Bundle, key: String, value: List<String>?) {
            bundle.putStringArray(key, value?.toTypedArray())
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        override fun get(bundle: Bundle, key: String): List<String>? {
            return (bundle[key] as Array<String>?)?.toList()
        }

        override fun parseValue(value: String): List<String> {
            return listOf(value)
        }

        override fun parseValue(value: String, previousValue: List<String>?): List<String>? {
            return previousValue?.plus(parseValue(value)) ?: parseValue(value)
        }

        override fun valueEquals(value: List<String>?, other: List<String>?): Boolean {
            val valueArray = value?.toTypedArray()
            val otherArray = other?.toTypedArray()
            return valueArray.contentDeepEquals(otherArray)
        }

        override fun serializeAsValues(value: List<String>?): List<String> =
            value?.toList() ?: emptyList()

        override fun emptyCollection(): List<String> = emptyList()
    }
