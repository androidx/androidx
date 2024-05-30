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

package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavType
import kotlin.reflect.KClass
import kotlin.reflect.KType

/** DSL for constructing a new [DialogNavigator.Destination] */
@NavDestinationDsl
public class DialogNavigatorDestinationBuilder :
    NavDestinationBuilder<DialogNavigator.Destination> {

    private val dialogNavigator: DialogNavigator
    private val dialogProperties: DialogProperties
    private val content: @Composable (NavBackStackEntry) -> Unit

    /**
     * DSL for constructing a new [DialogNavigator.Destination]
     *
     * @param navigator navigator used to create the destination
     * @param route the destination's unique route
     * @param dialogProperties properties that should be passed to
     *   [androidx.compose.ui.window.Dialog].
     * @param content composable for the destination
     */
    public constructor(
        navigator: DialogNavigator,
        route: String,
        dialogProperties: DialogProperties,
        content: @Composable (NavBackStackEntry) -> Unit
    ) : super(navigator, route) {
        this.dialogNavigator = navigator
        this.dialogProperties = dialogProperties
        this.content = content
    }

    /**
     * DSL for constructing a new [DialogNavigator.Destination]
     *
     * @param navigator navigator used to create the destination
     * @param route the destination's unique route from a [KClass]
     * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
     *   [NavType]. May be empty if [route] does not use custom NavTypes.
     * @param dialogProperties properties that should be passed to
     *   [androidx.compose.ui.window.Dialog].
     * @param content composable for the destination
     */
    public constructor(
        navigator: DialogNavigator,
        route: KClass<*>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
        dialogProperties: DialogProperties,
        content: @Composable (NavBackStackEntry) -> Unit
    ) : super(navigator, route, typeMap) {
        this.dialogNavigator = navigator
        this.dialogProperties = dialogProperties
        this.content = content
    }

    override fun instantiateDestination(): DialogNavigator.Destination {
        return DialogNavigator.Destination(dialogNavigator, dialogProperties, content)
    }
}
