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
import androidx.navigation.serialization.generateRoutePattern
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

public actual class NavOptions internal constructor(
    private val singleTop: Boolean,
    private val restoreState: Boolean,
    popUpToRoute: String?,
    private val popUpToInclusive: Boolean,
    private val popUpToSaveState: Boolean,
) {
    public actual var popUpToRoute: String? = popUpToRoute
        private set

    public actual var popUpToRouteClass: KClass<*>? = null
        private set

    public actual var popUpToRouteObject: Any? = null
        private set

    /**
     * NavOptions stores special options for navigate actions
     */
    @OptIn(InternalSerializationApi::class)
    internal constructor(
        singleTop: Boolean,
        restoreState: Boolean,
        popUpToRouteClass: KClass<*>?,
        popUpToInclusive: Boolean,
        popUpToSaveState: Boolean
    ) : this(
        singleTop = singleTop,
        restoreState = restoreState,
        popUpToRoute = popUpToRouteClass!!.serializer().generateRoutePattern(),
        popUpToInclusive = popUpToInclusive,
        popUpToSaveState = popUpToSaveState
    ) {
        this.popUpToRouteClass = popUpToRouteClass
    }

    /**
     * NavOptions stores special options for navigate actions
     */
    @OptIn(InternalSerializationApi::class)
    internal constructor(
        singleTop: Boolean,
        restoreState: Boolean,
        popUpToRouteObject: Any,
        popUpToInclusive: Boolean,
        popUpToSaveState: Boolean
    ) : this(
        singleTop = singleTop,
        restoreState = restoreState,
        popUpToRoute = popUpToRouteObject::class.serializer().generateRoutePattern(),
        popUpToInclusive = popUpToInclusive,
        popUpToSaveState = popUpToSaveState
    ) {
        this.popUpToRouteObject = popUpToRouteObject
    }

    public actual fun shouldLaunchSingleTop(): Boolean {
        return singleTop
    }

    public actual fun shouldRestoreState(): Boolean {
        return restoreState
    }

    public actual fun isPopUpToInclusive(): Boolean {
        return popUpToInclusive
    }

    public actual fun shouldPopUpToSaveState(): Boolean {
        return popUpToSaveState
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavOptions) return false
        return singleTop == other.singleTop &&
            restoreState == other.restoreState &&
            popUpToRoute == other.popUpToRoute &&
            popUpToRouteClass == other.popUpToRouteClass &&
            popUpToRouteObject == other.popUpToRouteObject &&
            popUpToInclusive == other.popUpToInclusive &&
            popUpToSaveState == other.popUpToSaveState
    }

    override fun hashCode(): Int {
        var result = if (shouldLaunchSingleTop()) 1 else 0
        result = 31 * result + if (shouldRestoreState()) 1 else 0
        result = 31 * result + popUpToRoute.hashCode()
        result = 31 * result + popUpToRouteClass.hashCode()
        result = 31 * result + popUpToRouteObject.hashCode()
        result = 31 * result + if (isPopUpToInclusive()) 1 else 0
        result = 31 * result + if (shouldPopUpToSaveState()) 1 else 0
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(this::class.simpleName)
        sb.append("(")
        if (singleTop) {
            sb.append("launchSingleTop ")
        }
        if (restoreState) {
            sb.append("restoreState ")
        }
        if (popUpToRoute != null) {
            sb.append("popUpTo(")
            if (popUpToRoute != null) {
                sb.append(popUpToRoute)
            } else if (popUpToRouteClass != null) {
                sb.append(popUpToRouteClass)
            } else if (popUpToRouteObject != null) {
                sb.append(popUpToRouteObject)
            } else {
                sb.append("null")
            }
            if (popUpToInclusive) {
                sb.append(" inclusive")
            }
            if (popUpToSaveState) {
                sb.append(" saveState")
            }
            sb.append(")")
        }
        return sb.toString()
    }

    public actual class Builder {
        private var singleTop = false
        private var restoreState = false
        private var popUpToRoute: String? = null
        private var popUpToRouteClass: KClass<*>? = null
        private var popUpToRouteObject: Any? = null
        private var popUpToInclusive = false
        private var popUpToSaveState = false

        public actual fun setLaunchSingleTop(singleTop: Boolean): Builder {
            this.singleTop = singleTop
            return this
        }

        public actual fun setRestoreState(restoreState: Boolean): Builder {
            this.restoreState = restoreState
            return this
        }

        @JvmOverloads
        public actual fun setPopUpTo(
            route: String?,
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            popUpToRoute = route
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        @JvmOverloads
        @Suppress("MissingGetterMatchingBuilder") // no need for getter
        public actual inline fun <reified T : Any> setPopUpTo(
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            setPopUpTo(T::class, inclusive, saveState)
            return this
        }

        // this restricted public is needed so that the public reified [popUpTo] can call
        // private popUpToRouteClass setter
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun setPopUpTo(
            klass: KClass<*>,
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            popUpToRouteClass = klass
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        @JvmOverloads
        @Suppress("MissingGetterMatchingBuilder")
        @OptIn(InternalSerializationApi::class)
        public actual fun <T : Any> setPopUpTo(
            route: T,
            inclusive: Boolean,
            saveState: Boolean
        ): Builder {
            popUpToRouteObject = route
            setPopUpTo(route::class.serializer().hashCode(), inclusive, saveState)
            return this
        }

        public actual fun build(): NavOptions {
            return if (popUpToRoute != null) {
                NavOptions(
                    singleTop, restoreState,
                    popUpToRoute, popUpToInclusive, popUpToSaveState,
                )
            } else if (popUpToRouteClass != null) {
                NavOptions(
                    singleTop, restoreState,
                    popUpToRouteClass, popUpToInclusive, popUpToSaveState,
                )
            } else if (popUpToRouteObject != null) {
                NavOptions(
                    singleTop, restoreState,
                    popUpToRouteObject!!, popUpToInclusive, popUpToSaveState,
                )
            } else {
                throw IllegalStateException()
            }
        }
    }
}
