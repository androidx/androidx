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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.navigation.test

import androidx.navigation.FloatingWindow
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraphBuilder
import androidx.navigation.Navigator
import androidx.navigation.SupportingPane
import androidx.navigation.get
import androidx.testutils.TestNavigator

@Navigator.Name("supporting_dialog")
class SupportingFloatingTestNavigator : TestNavigator() {
    override fun createDestination(): Destination {
        return SupportingFloatingDestination(this)
    }

    class SupportingFloatingDestination(navigator: TestNavigator) :
        Destination(navigator), FloatingWindow, SupportingPane
}

/** Construct a new [TestNavigator.Destination] from a [SupportingFloatingTestNavigator]. */
inline fun NavGraphBuilder.supportingDialog(
    route: String,
    builder: SupportingFloatingTestNavigatorDestinationBuilder.() -> Unit = {}
) =
    destination(
        SupportingFloatingTestNavigatorDestinationBuilder(
                provider[SupportingFloatingTestNavigator::class],
                route
            )
            .apply(builder)
    )

/**
 * DSL for constructing a new [TestNavigator.Destination] from a [SupportingFloatingTestNavigator].
 */
@NavDestinationDsl
class SupportingFloatingTestNavigatorDestinationBuilder(
    navigator: SupportingFloatingTestNavigator,
    route: String
) : NavDestinationBuilder<TestNavigator.Destination>(navigator, route)
