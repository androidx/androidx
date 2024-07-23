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

package androidx.compose.material.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl

/** DSL for constructing a new [BottomSheetNavigator.Destination] */
@NavDestinationDsl
class BottomSheetNavigatorDestinationBuilder :
    NavDestinationBuilder<BottomSheetNavigator.Destination> {

    private val bottomSheetNavigator: BottomSheetNavigator
    private val content: @Composable ColumnScope.(NavBackStackEntry) -> Unit

    /**
     * DSL for constructing a new [BottomSheetNavigator.Destination]
     *
     * @param navigator navigator used to create the destination
     * @param route the destination's unique route
     * @param content composable for the destination
     */
    public constructor(
        navigator: BottomSheetNavigator,
        route: String,
        content: @Composable ColumnScope.(NavBackStackEntry) -> Unit
    ) : super(navigator, route) {
        this.bottomSheetNavigator = navigator
        this.content = content
    }

    override fun instantiateDestination(): BottomSheetNavigator.Destination {
        return BottomSheetNavigator.Destination(bottomSheetNavigator, content)
    }
}
