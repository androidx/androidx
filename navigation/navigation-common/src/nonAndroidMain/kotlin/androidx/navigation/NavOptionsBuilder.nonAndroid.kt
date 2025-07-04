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

import kotlin.reflect.KClass

@NavOptionsDsl
public actual class NavOptionsBuilder {
    public actual var launchSingleTop: Boolean
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual var restoreState: Boolean
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual var popUpToRoute: String?
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual var popUpToRouteClass: KClass<*>?
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual var popUpToRouteObject: Any?
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual fun popUpTo(route: String, popUpToBuilder: PopUpToBuilder.() -> Unit) {
        implementedInJetBrainsFork()
    }

    @Suppress("BuilderSetStyle")
    public actual inline fun <reified T : Any> popUpTo(
        noinline popUpToBuilder: PopUpToBuilder.() -> Unit
    ) {}

    public actual fun <T : Any> popUpTo(
        route: KClass<T>,
        popUpToBuilder: PopUpToBuilder.() -> Unit,
    ) {
        implementedInJetBrainsFork()
    }

    @Suppress("BuilderSetStyle", "MissingJvmstatic")
    public actual fun <T : Any> popUpTo(route: T, popUpToBuilder: PopUpToBuilder.() -> Unit) {
        implementedInJetBrainsFork()
    }

    internal actual fun build(): NavOptions {
        implementedInJetBrainsFork()
    }
}
