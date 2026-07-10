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

package androidx.compose.remote.integration.demos.main

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Generic screen with a [title] that will be displayed in the list of screens. */
@Serializable
sealed class Screen : NavKey {
    abstract val key: String
    abstract val title: String

    override fun toString() = title
}

/** Screen that displays [androidx.compose.runtime.Composable] content when selected. */
@Serializable
data class ComposableScreen(override val key: String, override val title: String) : Screen()

/** A category of [Screen]s, that will display a list of [screens] when selected. */
@Serializable
data class Category(
    override val key: String,
    override val title: String,
    val screens: List<Screen>,
) : Screen()

/** Flattened recursive [List] of every screen in [Category]. */
fun Category.allScreens(): List<Screen> {
    val allScreens = mutableListOf<Screen>()
    fun Category.addAllScreens() {
        screens.forEach { screen ->
            allScreens += screen
            if (screen is Category) {
                screen.addAllScreens()
            }
        }
    }
    addAllScreens()
    return allScreens
}

/** Flattened recursive [List] of every launchable screen in [Category]. */
fun Category.allLaunchableScreens(): List<Screen> {
    return allScreens().filter { it !is Category }
}
