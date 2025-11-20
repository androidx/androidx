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
import androidx.navigation.internal.NavContext
import androidx.navigation.internal.NavDestinationImpl
import androidx.navigation.serialization.generateHashCode
import androidx.savedstate.SavedState
import androidx.savedstate.read
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

public actual open class NavDestination
actual constructor(public actual val navigatorName: String) {

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    public actual annotation class ClassType(actual val value: KClass<*>)

    private val impl = NavDestinationImpl(this)

    public actual constructor(navigator: Navigator<out NavDestination>) : this(navigator.name)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual class DeepLinkMatch
    actual constructor(
        public actual val destination: NavDestination,
        @get:Suppress("NullableCollection") // Needed for nullable savedState
        public actual val matchingArgs: SavedState?,
        private val isExactDeepLink: Boolean,
        private val matchingPathSegments: Int,
        private val hasMatchingAction: Boolean,
        private val mimeTypeMatchLevel: Int,
    ) : Comparable<DeepLinkMatch> {
        public actual fun hasMatchingArgs(arguments: SavedState?): Boolean {
            if (arguments == null || matchingArgs == null) return false

            matchingArgs
                .read { toMap().keys }
                .forEach { key ->
                    // the arguments must at least contain every argument stored in this deep link
                    if (!arguments.read { contains(key) }) return false

                    val type = destination.arguments[key]?.type
                    val matchingArgValue = type?.get(matchingArgs, key)
                    val entryArgValue = type?.get(arguments, key)
                    if (type?.valueEquals(matchingArgValue, entryArgValue) == false) {
                        return false
                    }
                }
            return true
        }

        actual override fun compareTo(other: DeepLinkMatch): Int {
            // Prefer exact deep links
            if (isExactDeepLink && !other.isExactDeepLink) {
                return 1
            } else if (!isExactDeepLink && other.isExactDeepLink) {
                return -1
            }
            if (matchingArgs != null && other.matchingArgs == null) {
                return 1
            } else if (matchingArgs == null && other.matchingArgs != null) {
                return -1
            }
            if (matchingArgs != null) {
                val sizeDifference =
                    matchingArgs.read { size() } - other.matchingArgs!!.read { size() }
                if (sizeDifference > 0) {
                    return 1
                } else if (sizeDifference < 0) {
                    return -1
                }
            }
            return 0
        }
    }

    public actual var parent: NavGraph? = null
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public set

    public actual var label: CharSequence? = null

    public actual val arguments: Map<String, NavArgument>
        get() = impl.arguments.toMap()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @delegate:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual var id: Int by impl::id

    public actual var route: String? by impl::route

    private var idName: String? by impl::idName

    public actual open val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = idName ?: id.toString()

    public actual open fun hasDeepLink(deepLink: NavUri): Boolean {
        return hasDeepLink(NavDeepLinkRequest(deepLink, null, null))
    }

    public actual open fun hasDeepLink(deepLinkRequest: NavDeepLinkRequest): Boolean {
        return matchDeepLink(deepLinkRequest) != null
    }

    public actual fun addDeepLink(uriPattern: String) {
        addDeepLink(NavDeepLink.Builder().setUriPattern(uriPattern).build())
    }

    public actual fun addDeepLink(navDeepLink: NavDeepLink) {
        impl.addDeepLink(navDeepLink)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchRoute(route: String): DeepLinkMatch? {
        return impl.matchRoute(route)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open fun matchDeepLink(navDeepLinkRequest: NavDeepLinkRequest): DeepLinkMatch? {
        return impl.matchDeepLink(navDeepLinkRequest)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun buildDeepLinkDestinations(
        previousDestination: NavDestination? = null
    ): List<NavDestination> {
        val hierarchy = ArrayDeque<NavDestination>()
        var current: NavDestination? = this
        do {
            val parent = current!!.parent
            if (
                // If the current destination is a sibling of the previous, just add it straightaway
                previousDestination?.parent != null &&
                    previousDestination.parent!!.findNode(current.id) === current
            ) {
                hierarchy.addFirst(current)
                break
            }
            if (parent == null || parent.startDestinationId != current.id) {
                hierarchy.addFirst(current)
            }
            if (parent == previousDestination) {
                break
            }
            current = parent
        } while (current != null)
        return hierarchy.toList()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun hasRoute(route: String, arguments: SavedState?): Boolean {
        return impl.hasRoute(route, arguments)
    }

    public actual fun addArgument(argumentName: String, argument: NavArgument) {
        impl.addArgument(argumentName, argument)
    }

    public actual fun removeArgument(argumentName: String) {
        impl.removeArgument(argumentName)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("NullableCollection")
    public actual fun addInDefaultArgs(args: SavedState?): SavedState? {
        return impl.addInDefaultArgs(args)
    }

    public actual companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun getDisplayName(context: NavContext, id: Int): String {
            return context.getResourceName(id)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun createRoute(route: String?): String =
            if (route != null) "multiplatform-app://androidx.navigation/$route" else ""

        @JvmStatic
        public actual val NavDestination.hierarchy: Sequence<NavDestination>
            get() = generateSequence(this) { it.parent }

        @JvmStatic
        public actual inline fun <reified T : Any> NavDestination.hasRoute(): Boolean =
            hasRoute(T::class)

        @OptIn(InternalSerializationApi::class)
        @JvmStatic
        public actual fun <T : Any> NavDestination.hasRoute(route: KClass<T>): Boolean =
            route.serializer().generateHashCode() == id
    }
}
