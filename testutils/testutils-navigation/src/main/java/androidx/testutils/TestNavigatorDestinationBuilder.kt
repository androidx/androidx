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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.testutils

import androidx.annotation.IdRes
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraphBuilder
import androidx.navigation.get

/**
 * Construct a new [TestNavigator.Destination]
 */
inline fun NavGraphBuilder.test(@IdRes id: Int) = test(id) {}

/**
 * Construct a new [TestNavigator.Destination]
 */
inline fun NavGraphBuilder.test(route: String) = test(route) {}

/**
 * Construct a new [TestNavigator.Destination]
 */
@Suppress("DEPRECATION")
inline fun NavGraphBuilder.test(
    @IdRes id: Int,
    builder: TestNavigatorDestinationBuilder.() -> Unit
) = destination(
    TestNavigatorDestinationBuilder(
        provider[TestNavigator::class],
        id
    ).apply(builder)
)

/**
 * Construct a new [TestNavigator.Destination]
 */
inline fun NavGraphBuilder.test(
    route: String,
    builder: TestNavigatorDestinationBuilder.() -> Unit
) = destination(
    TestNavigatorDestinationBuilder(provider[TestNavigator::class], route).apply(builder)
)

/**
 * DSL for constructing a new [TestNavigator.Destination]
 */
@NavDestinationDsl
class TestNavigatorDestinationBuilder : NavDestinationBuilder<TestNavigator.Destination> {
    @Suppress("DEPRECATION")
    constructor(navigator: TestNavigator, @IdRes id: Int = 0) : super(navigator, id)
    constructor(navigator: TestNavigator, route: String) : super(navigator, route)
}
