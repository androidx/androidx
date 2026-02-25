/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation.testing

import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlin.collections.forEach

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Navigator.Name("test")
internal actual class TestNavigator : Navigator<NavDestination>() {
    actual override fun createDestination(): NavDestination = NavDestination(this)
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Navigator.Name("test")
internal actual class TestTransitionNavigator actual constructor() : Navigator<NavDestination>() {
    actual val testLifecycleOwner = TestLifecycleOwner()
    actual val testLifecycle = testLifecycleOwner.lifecycle

    actual override fun createDestination(): NavDestination = NavDestination(this)

    actual override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        entries.forEach { entry -> state.pushWithTransition(entry) }
    }

    actual override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.popWithTransition(popUpTo, savedState)
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Navigator.Name("test")
internal actual class FloatingWindowTestNavigator : Navigator<FloatingTestDestination>() {
    actual override fun createDestination(): FloatingTestDestination = FloatingTestDestination(this)
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Navigator.Name("test")
internal actual class SupportingPaneTestNavigator : Navigator<SupportingPaneTestDestination>() {
    actual override fun createDestination(): SupportingPaneTestDestination =
        SupportingPaneTestDestination(this)
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Navigator.Name("test")
internal actual class SupportingPaneTestTransitionNavigator :
    Navigator<SupportingPaneTestDestination>() {
    actual override fun createDestination(): SupportingPaneTestDestination =
        SupportingPaneTestDestination(this)

    actual override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        entries.forEach { entry -> state.pushWithTransition(entry) }
    }

    actual override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.popWithTransition(popUpTo, savedState)
    }
}
