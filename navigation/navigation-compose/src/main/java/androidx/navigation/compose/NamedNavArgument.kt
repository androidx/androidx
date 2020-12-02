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

package androidx.navigation.compose

import androidx.navigation.NavArgument
import androidx.navigation.NavArgumentBuilder
import androidx.navigation.NavDestinationDsl

/**
 * Construct a new [NavArgument]
 */
@NavDestinationDsl
public fun navArgument(
    name: String,
    builder: NavArgumentBuilder.() -> Unit
): NamedNavArgument = NamedNavArgument(name, NavArgumentBuilder().apply(builder).build())

/**
 * Construct a named [NavArgument] by using the [navArgument] method.
 */
public class NamedNavArgument internal constructor(
    private val name: String,
    private val argument: NavArgument
) {
    internal operator fun component1(): String = name
    internal operator fun component2(): NavArgument = argument
}
