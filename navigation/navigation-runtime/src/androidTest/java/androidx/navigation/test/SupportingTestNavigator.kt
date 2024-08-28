/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.test

import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraphBuilder
import androidx.navigation.Navigator
import androidx.navigation.SupportingPane
import androidx.navigation.get
import androidx.testutils.TestNavigator

@Navigator.Name("supporting_pane")
class SupportingTestNavigator : TestNavigator() {
    override fun createDestination(): Destination {
        return SupportingDestination(this)
    }

    class SupportingDestination(navigator: TestNavigator) : Destination(navigator), SupportingPane
}

/** Construct a new [TestNavigator.Destination] from a [SupportingTestNavigator]. */
inline fun NavGraphBuilder.supportingPane(
    route: String,
    builder: SupportingTestNavigatorDestinationBuilder.() -> Unit = {}
) =
    destination(
        SupportingTestNavigatorDestinationBuilder(provider[SupportingTestNavigator::class], route)
            .apply(builder)
    )

/** DSL for constructing a new [TestNavigator.Destination] from a [SupportingTestNavigator]. */
@NavDestinationDsl
class SupportingTestNavigatorDestinationBuilder(navigator: SupportingTestNavigator, route: String) :
    NavDestinationBuilder<TestNavigator.Destination>(navigator, route)
