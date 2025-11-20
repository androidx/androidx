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

import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import kotlin.reflect.KClass

public actual open class NavigatorProvider actual constructor() {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual val navigators: Map<String, Navigator<out NavDestination>>
        get() = implementedInJetBrainsFork()

    @Suppress("UNCHECKED_CAST")
    @CallSuper
    public actual open fun <T : Navigator<*>> getNavigator(name: String): T {
        implementedInJetBrainsFork()
    }

    public actual fun addNavigator(
        navigator: Navigator<out NavDestination>
    ): Navigator<out NavDestination>? {
        implementedInJetBrainsFork()
    }

    @CallSuper
    public actual open fun addNavigator(
        name: String,
        navigator: Navigator<out NavDestination>,
    ): Navigator<out NavDestination>? {
        implementedInJetBrainsFork()
    }
}

@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun <T : Navigator<out NavDestination>> NavigatorProvider.get(
    clazz: KClass<T>
): T = implementedInJetBrainsFork()
