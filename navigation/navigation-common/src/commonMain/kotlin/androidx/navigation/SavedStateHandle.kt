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

package androidx.navigation

import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.serialization.decodeArguments
import androidx.navigation.serialization.generateNavArguments
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * Returns route as an object of type [T]
 *
 * Extrapolates arguments from [SavedStateHandle] and recreates object [T]
 *
 * @param [T] the entry's [NavDestination.route] as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in [T]. May be empty if [T] does not use
 *   custom NavTypes.
 * @return A new instance of this entry's [NavDestination.route] as an object of type [T]
 */
public inline fun <reified T : Any> SavedStateHandle.toRoute(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap()
): T = internalToRoute(T::class, typeMap)

@OptIn(InternalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : Any> SavedStateHandle.internalToRoute(
    route: KClass<T>,
    typeMap: Map<KType, NavType<*>>
): T {
    val map: MutableMap<String, NavType<*>> = mutableMapOf()
    val serializer = route.serializer()
    serializer.generateNavArguments(typeMap).onEach { map[it.name] = it.argument.type }
    return serializer.decodeArguments(this, map)
}
