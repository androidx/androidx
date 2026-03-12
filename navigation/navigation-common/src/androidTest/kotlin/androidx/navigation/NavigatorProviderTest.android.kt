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

package androidx.navigation

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Navigator.Name(EmptyNavigator.NAME)
internal actual open class EmptyNavigator : Navigator<NavDestination>() {

    actual companion object {
        actual const val NAME = "empty"
    }

    actual override fun createDestination(): NavDestination {
        throw IllegalStateException("createDestination is not supported")
    }

    actual override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class NoNameNavigator : Navigator<NavDestination>() {
    actual override fun createDestination(): NavDestination {
        throw IllegalStateException("createDestination is not supported")
    }

    actual override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}

@Navigator.Name(EmptyNavigator.NAME)
internal actual class EmptyNavigator2 actual constructor() : EmptyNavigator()
