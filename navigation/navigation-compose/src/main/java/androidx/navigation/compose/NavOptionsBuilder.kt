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

import androidx.navigation.NavOptionsBuilder
import androidx.navigation.PopUpToBuilder

/**
 * Pop up to a given destination before navigating. This pops all non-matching destination routes
 * from the back stack until the destination with a matching route is found.
 *
 * @param route route for the destination
 * @param popUpToBuilder builder used to construct a popUpTo operation
 */
fun NavOptionsBuilder.popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit) {
    popUpTo(createRoute(route).hashCode(), popUpToBuilder)
}
