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

package androidx.navigation

@NavOptionsDsl
public actual class NavOptionsBuilder {
    private val builder = NavOptions.Builder()
    public actual var launchSingleTop: Boolean = false

    @get:Suppress("GetterOnBuilder", "GetterSetterNames")
    @set:Suppress("SetterReturnsThis", "GetterSetterNames")
    public actual var restoreState: Boolean = false

    public actual var popUpToRoute: String? = null
        private set(value) {
            if (value != null) {
                require(value.isNotBlank()) { "Cannot pop up to an empty route" }
                field = value
                inclusive = false
            }
        }
    private var inclusive = false
    private var saveState = false

    public actual fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit) {
        popUpToRoute = route
        val builder = PopUpToBuilder().apply(popUpToBuilder)
        inclusive = builder.inclusive
        saveState = builder.saveState
    }

    internal actual fun build() = builder.apply {
        setLaunchSingleTop(launchSingleTop)
        setRestoreState(restoreState)
        setPopUpTo(popUpToRoute, inclusive, saveState)
    }.build()
}
