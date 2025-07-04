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

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass
import kotlin.reflect.KType

@NavDestinationDsl
public actual open class NavDestinationBuilder<out D : NavDestination> {
    public actual constructor(navigator: Navigator<out D>, route: String?) {
        implementedInJetBrainsFork()
    }

    public actual constructor(
        navigator: Navigator<out D>,
        @Suppress("OptionalBuilderConstructorArgument") route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    ) {
        implementedInJetBrainsFork()
    }

    protected actual val navigator: Navigator<out D>
        get() = implementedInJetBrainsFork()

    public actual val route: String?
        get() = implementedInJetBrainsFork()

    public actual var label: CharSequence?
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual fun argument(name: String, argumentBuilder: NavArgumentBuilder.() -> Unit) {
        implementedInJetBrainsFork()
    }

    @Suppress("BuilderSetStyle")
    public actual fun argument(name: String, argument: NavArgument) {
        implementedInJetBrainsFork()
    }

    public actual fun deepLink(uriPattern: String) {
        implementedInJetBrainsFork()
    }

    @JvmName("deepLinkSafeArgs")
    @Suppress("BuilderSetStyle")
    public actual inline fun <reified T : Any> deepLink(basePath: String) {
        implementedInJetBrainsFork()
    }

    public actual fun deepLink(navDeepLink: NavDeepLinkDslBuilder.() -> Unit) {
        implementedInJetBrainsFork()
    }

    @Suppress("BuilderSetStyle")
    public actual inline fun <reified T : Any> deepLink(
        basePath: String,
        noinline navDeepLink: NavDeepLinkDslBuilder.() -> Unit,
    ) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun <T : Any> deepLink(
        route: KClass<T>,
        basePath: String,
        navDeepLink: NavDeepLinkDslBuilder.() -> Unit,
    ) {
        implementedInJetBrainsFork()
    }

    @Suppress("BuilderSetStyle")
    public actual fun deepLink(navDeepLink: NavDeepLink) {
        implementedInJetBrainsFork()
    }

    @Suppress("BuilderSetStyle")
    protected actual open fun instantiateDestination(): D {
        implementedInJetBrainsFork()
    }

    public actual open fun build(): D {
        implementedInJetBrainsFork()
    }
}
