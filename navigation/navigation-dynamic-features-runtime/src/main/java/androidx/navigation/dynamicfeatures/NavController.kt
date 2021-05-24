/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavGraph

/**
 * Construct a new [androidx.navigation.NavGraph] that supports dynamic navigation destinations
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your dynamic NavGraph instead",
    ReplaceWith(
        "createGraph(startDestination = startDestination.toString(), route = id.toString()) " +
            "{ builder.invoke() }"
    )
)
public inline fun NavController.createGraph(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    builder: DynamicNavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(id, startDestination, builder)

/**
 * Construct a new [androidx.navigation.NavGraph] that supports dynamic navigation destinations
 */
public inline fun NavController.createGraph(
    startDestination: String,
    route: String? = null,
    builder: DynamicNavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(startDestination, route, builder)
