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

package androidx.navigation

import androidx.annotation.IdRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Construct a new [NavGraph]
 */
public inline fun NavController.createGraph(
    @IdRes id: Int = 0,
    @IdRes startDestination: Int,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(id, startDestination, builder)

/**
 * Creates and returns a [Flow] that will emit the currently active [NavBackStackEntry] whenever
 * it changes. If there is no active [NavBackStackEntry], no item will be emitted.
 */
@ExperimentalCoroutinesApi
public val NavController.currentBackStackEntryFlow: Flow<NavBackStackEntry> get() = callbackFlow {
    val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
        controller.currentBackStackEntry?.let(::sendBlocking)
    }

    addOnDestinationChangedListener(listener)
    awaitClose {
        removeOnDestinationChangedListener(listener)
    }
}
