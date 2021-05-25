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

import androidx.annotation.IdRes
import androidx.navigation.FloatingWindow
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraphBuilder
import androidx.navigation.Navigator
import androidx.navigation.get
import androidx.testutils.TestNavigator

@Navigator.Name("dialog")
class FloatingTestNavigator : TestNavigator() {
    override fun createDestination(): Destination {
        return FloatingDestination(this)
    }

    class FloatingDestination(navigator: TestNavigator) :
        Destination(navigator),
        FloatingWindow
}

/**
 * Construct a new [TestNavigator.Destination] from a [FloatingTestNavigator].
 */
inline fun NavGraphBuilder.dialog(@IdRes id: Int) = dialog(id) {}

/**
 * Construct a new [TestNavigator.Destination] from a [FloatingTestNavigator].
 */
inline fun NavGraphBuilder.dialog(
    @IdRes id: Int,
    builder: FloatingTestNavigatorDestinationBuilder.() -> Unit
) = destination(
    FloatingTestNavigatorDestinationBuilder(
        provider[FloatingTestNavigator::class],
        id
    ).apply(builder)
)

/**
 * DSL for constructing a new [TestNavigator.Destination] from a [FloatingTestNavigator].
 */
@Suppress("DEPRECATION")
@NavDestinationDsl
class FloatingTestNavigatorDestinationBuilder(
    navigator: FloatingTestNavigator,
    @IdRes id: Int
) : NavDestinationBuilder<TestNavigator.Destination>(navigator, id)
