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
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavType
import androidx.navigation.Navigator
import androidx.navigation.get
import androidx.navigation.serialization.generateRouteWithArgs
import kotlin.reflect.KType

/**
 * SavedStateHandle constructor to create a SavedStateHandle with a serializable object.
 *
 * Returns a [SavedStateHandle] populated with arguments from [route].
 *
 * @param route The route to extract argument values from
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 */
@Suppress("DEPRECATION")
public operator fun SavedStateHandle.Companion.invoke(
    route: Any,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap()
): SavedStateHandle {
    val dest =
        NavDestinationBuilder(
                TestNavigatorProvider().get<Navigator<NavDestination>>("test"),
                route::class,
                typeMap
            )
            .build()
    val map = dest.arguments.mapValues { it.value.type }
    val deeplink = generateRouteWithArgs(route, map)
    val matching = dest.matchDeepLink(deeplink)
    checkNotNull(matching) { "Cannot match route [$deeplink] to [${route::class.simpleName}]" }
    if (dest.arguments.isNotEmpty()) {
        checkNotNull(matching.matchingArgs) { "Missing arguments from route [$deeplink]" }
    }
    val finalMap: MutableMap<String, Any?> = mutableMapOf()
    matching.matchingArgs?.keySet()?.forEach { key -> finalMap[key] = matching.matchingArgs!![key] }
    return SavedStateHandle(finalMap)
}
