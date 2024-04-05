/*
 * Copyright 2018 The Android Open Source Project
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

@file:JvmName("NavHostKt")

package androidx.navigation

import androidx.annotation.IdRes

/**
 * Construct a new [NavGraph]
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your NavGraph instead",
    ReplaceWith(
        "createGraph(startDestination = startDestination.toString(), route = id.toString()) " +
            "{ builder.invoke() }"
    )
)
public inline fun NavHost.createGraph(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navController.createGraph(id, startDestination, builder)

/**
 * Construct a new [NavGraph]
 */
public actual inline fun NavHost.createGraph(
    startDestination: String,
    route: String?,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navController.createGraph(startDestination, route, builder)
