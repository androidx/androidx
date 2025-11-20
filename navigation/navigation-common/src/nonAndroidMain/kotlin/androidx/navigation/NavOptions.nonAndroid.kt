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

import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

public actual class NavOptions {
    public actual val popUpToId: Int = 0

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

    public actual fun shouldLaunchSingleTop(): Boolean {
        implementedInJetBrainsFork()
    }

    public actual fun shouldRestoreState(): Boolean {
        implementedInJetBrainsFork()
    }

    public actual fun isPopUpToInclusive(): Boolean {
        implementedInJetBrainsFork()
    }

    public actual fun shouldPopUpToSaveState(): Boolean {
        implementedInJetBrainsFork()
    }

    public actual class Builder actual constructor() {

        public actual fun setLaunchSingleTop(singleTop: Boolean): Builder {
            implementedInJetBrainsFork()
        }

        public actual fun setRestoreState(restoreState: Boolean): Builder {
            implementedInJetBrainsFork()
        }

        @JvmOverloads
        public actual fun setPopUpTo(
            route: String?,
            inclusive: Boolean,
            saveState: Boolean,
        ): Builder {
            implementedInJetBrainsFork()
        }

        @Suppress("MissingGetterMatchingBuilder")
        @JvmOverloads
        public actual inline fun <reified T : Any> setPopUpTo(
            inclusive: Boolean,
            saveState: Boolean,
        ): Builder {
            implementedInJetBrainsFork()
        }

        @JvmOverloads
        public actual fun <T : Any> setPopUpTo(
            route: KClass<T>,
            inclusive: Boolean,
            saveState: Boolean,
        ): Builder {
            implementedInJetBrainsFork()
        }

        @JvmOverloads
        @Suppress("MissingGetterMatchingBuilder")
        public actual fun <T : Any> setPopUpTo(
            route: T,
            inclusive: Boolean,
            saveState: Boolean,
        ): Builder {
            implementedInJetBrainsFork()
        }

        public actual fun build(): NavOptions {
            implementedInJetBrainsFork()
        }
    }
}
