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

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.CollectionNavType
import androidx.navigation.NavArgument
import androidx.navigation.NavType
import androidx.navigation.parseAndPutFromUri
import androidx.navigation.serialization.RouteEncoder
import androidx.navigation.serialization.generateNavArguments
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlin.jvm.JvmSuppressWildcards
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
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
): SavedStateHandle {
    val serializer = route::class.serializer()
    // generate type maps
    val namedArgs: MutableMap<String, NavArgument> =
        mutableMapOf<String, NavArgument>().apply {
            serializer.generateNavArguments(typeMap).map { put(it.name, it.argument) }
        }
    val finalTypeMap = mutableMapOf<String, NavType<Any?>>()
    namedArgs.forEach { finalTypeMap[it.key] = it.value.type }
    // encode route to map of arg values
    val argValues = RouteEncoder(serializer, finalTypeMap).encodeToArgMap(route)
    val savedState = savedState()
    // parse and put arg values into bundle
    argValues.forEach { entry ->
        val argName = entry.key
        val type = finalTypeMap[entry.key]
        checkNotNull(type) {
            "SavedStateHandleFactory could not locate NavType for argument [$argName]. Please" +
                "provide NavType in typeMap."
        }
        val tempSavedState = savedState()
        // start collection navtypes with empty list unless it has default
        if (type is CollectionNavType && !namedArgs[argName]?.isDefaultValuePresent!!) {
            type.put(tempSavedState, argName, type.emptyCollection())
        }
        entry.value.forEach { value ->
            try {
                if (!tempSavedState.read { contains(argName) }) {
                    type.parseAndPutFromUri(tempSavedState, argName, value)
                } else {
                    val previousValue = type[tempSavedState, argName]
                    type.parseAndPut(tempSavedState, argName, value, previousValue)
                }
            } catch (e: IllegalArgumentException) {
                // parse failed, ignored
            }
        }
        savedState.write { putAll(tempSavedState) }
    }
    // convert arg bundle to arg map
    val finalMap = savedState.read { toMap() }
    // populate handle with arg map
    @Suppress("VisibleForTests") // Safe to use here as this is a test utilities module.
    return SavedStateHandle(initialState = finalMap)
}
