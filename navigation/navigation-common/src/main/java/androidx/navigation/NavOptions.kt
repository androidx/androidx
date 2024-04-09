/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.annotation.AnimRes
import androidx.annotation.AnimatorRes
import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import androidx.navigation.NavDestination.Companion.createRoute
import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * NavOptions stores special options for navigate actions
 */
public class NavOptions internal constructor(
    private val singleTop: Boolean,
    private val restoreState: Boolean,
    /**
     * The destination to pop up to before navigating. When set, all non-matching destinations
     * should be popped from the back stack.
     * @return the destinationId to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     *
     * @see isPopUpToInclusive
     * @see shouldPopUpToSaveState
     */
    @field:IdRes @get:IdRes @param:IdRes
    public val popUpToId: Int,
    private val popUpToInclusive: Boolean,
    private val popUpToSaveState: Boolean,
    /**
     * The custom enter Animation/Animator that should be run.
     * @return the resource id of a Animation or Animator or -1 if none.
     */
    @get:AnimatorRes @get:AnimRes @param:AnimRes @param:AnimatorRes
    public val enterAnim: Int,
    /**
     * The custom exit Animation/Animator that should be run.
     * @return the resource id of a Animation or Animator or -1 if none.
     */
    @get:AnimatorRes @get:AnimRes @param:AnimRes @param:AnimatorRes
    public val exitAnim: Int,
    /**
     * The custom enter Animation/Animator that should be run when this destination is
     * popped from the back stack.
     * @return the resource id of a Animation or Animator or -1 if none.
     */
    @get:AnimatorRes @get:AnimRes @param:AnimRes @param:AnimatorRes
    public val popEnterAnim: Int,
    /**
     * The custom exit Animation/Animator that should be run when this destination is
     * popped from the back stack.
     * @return the resource id of a Animation or Animator or -1 if none.
     */
    @get:AnimatorRes @get:AnimRes @param:AnimRes @param:AnimatorRes
    public val popExitAnim: Int
) {
    /**
     * The destination to pop up to before navigating. When set, all non-matching destinations
     * should be popped from the back stack.
     * @return the destinationId to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     *
     * @see isPopUpToInclusive
     * @see shouldPopUpToSaveState
     */
    @IdRes
    @Deprecated("Use popUpToId instead.", ReplaceWith("popUpToId"))
    public fun getPopUpTo(): Int = popUpToId

    /**
     * Route for the destination to pop up to before navigating. When set, all non-matching
     * destinations should be popped from the back stack.
     * @return the destination route to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     *
     * @see isPopUpToInclusive
     * @see shouldPopUpToSaveState
     */
    public var popUpToRoute: String? = null
        private set

    /**
     * Route from a [KClass] for the destination to pop up to before navigating. When set,
     * all non-matching destinations should be popped from the back stack.
     * @return the destination route to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     *
     * @see isPopUpToInclusive
     * @see shouldPopUpToSaveState
     */
    // both ExperimentalSafeArgsApi annotations required for annotation to appear on API declaration
    @get:ExperimentalSafeArgsApi
    // required due to getter with ExperimentalSafeArgsApi annotation
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalSafeArgsApi
    public var popUpToRouteClass: KClass<*>? = null
        private set

    /**
     * Route from an Object for the destination to pop up to before navigating. When set,
     * all non-matching destinations should be popped from the back stack.
     * @return the destination route to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     *
     * @see isPopUpToInclusive
     * @see shouldPopUpToSaveState
     */
    // both ExperimentalSafeArgsApi annotations required for annotation to appear on API declaration
    @get:ExperimentalSafeArgsApi
    // required due to getter with ExperimentalSafeArgsApi annotation
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalSafeArgsApi
    public var popUpToRouteObject: Any? = null
        private set

    /**
     * NavOptions stores special options for navigate actions
     */
    internal constructor(
        singleTop: Boolean,
        restoreState: Boolean,
        popUpToRoute: String?,
        popUpToInclusive: Boolean,
        popUpToSaveState: Boolean,
        enterAnim: Int,
        exitAnim: Int,
        popEnterAnim: Int,
        popExitAnim: Int
    ) : this(
        singleTop,
        restoreState,
        createRoute(popUpToRoute).hashCode(),
        popUpToInclusive,
        popUpToSaveState,
        enterAnim,
        exitAnim,
        popEnterAnim,
        popExitAnim
    ) {
        this.popUpToRoute = popUpToRoute
    }

    /**
     * NavOptions stores special options for navigate actions
     */
    @OptIn(InternalSerializationApi::class, ExperimentalSafeArgsApi::class)
    internal constructor(
        singleTop: Boolean,
        restoreState: Boolean,
        popUpToRouteClass: KClass<*>?,
        popUpToInclusive: Boolean,
        popUpToSaveState: Boolean,
        enterAnim: Int,
        exitAnim: Int,
        popEnterAnim: Int,
        popExitAnim: Int
    ) : this(
        singleTop,
        restoreState,
        popUpToRouteClass!!.serializer().hashCode(),
        popUpToInclusive,
        popUpToSaveState,
        enterAnim,
        exitAnim,
        popEnterAnim,
        popExitAnim
    ) {
        this.popUpToRouteClass = popUpToRouteClass
    }

    /**
     * NavOptions stores special options for navigate actions
     */
    @OptIn(InternalSerializationApi::class, ExperimentalSafeArgsApi::class)
    internal constructor(
        singleTop: Boolean,
        restoreState: Boolean,
        popUpToRouteObject: Any,
        popUpToInclusive: Boolean,
        popUpToSaveState: Boolean,
        enterAnim: Int,
        exitAnim: Int,
        popEnterAnim: Int,
        popExitAnim: Int
    ) : this(
        singleTop,
        restoreState,
        popUpToRouteObject::class.serializer().hashCode(),
        popUpToInclusive,
        popUpToSaveState,
        enterAnim,
        exitAnim,
        popEnterAnim,
        popExitAnim
    ) {
        this.popUpToRouteObject = popUpToRouteObject
    }

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most
     * one copy of a given destination on the top of the back stack).
     *
     *
     * This functions similarly to how [android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP]
     * works with activities.
     */
    public fun shouldLaunchSingleTop(): Boolean {
        return singleTop
    }

    /**
     * Whether this navigation action should restore any state previously saved
     * by [Builder.setPopUpTo] or the `popUpToSaveState` attribute.
     */
    public fun shouldRestoreState(): Boolean {
        return restoreState
    }

    /**
     * Whether the destination set in [getPopUpTo] should be popped from the back stack.
     * @see Builder.setPopUpTo
     *
     * @see NavOptions.getPopUpTo
     */
    public fun isPopUpToInclusive(): Boolean {
        return popUpToInclusive
    }

    /**
     * Whether the back stack and the state of all destinations between the
     * current destination and [popUpToId] should be saved for later restoration via
     * [Builder.setRestoreState] or the `restoreState` attribute using the same ID
     * as [popUpToId] (note: this matching ID is true whether [isPopUpToInclusive] is true or
     * false).
     */
    public fun shouldPopUpToSaveState(): Boolean {
        return popUpToSaveState
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavOptions) return false
        return singleTop == other.singleTop &&
            restoreState == other.restoreState &&
            popUpToId == other.popUpToId &&
            popUpToRoute == other.popUpToRoute &&
            popUpToRouteClass == other.popUpToRouteClass &&
            popUpToRouteObject == other.popUpToRouteObject &&
            popUpToInclusive == other.popUpToInclusive &&
            popUpToSaveState == other.popUpToSaveState &&
            enterAnim == other.enterAnim &&
            exitAnim == other.exitAnim &&
            popEnterAnim == other.popEnterAnim &&
            popExitAnim == other.popExitAnim
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    override fun hashCode(): Int {
        var result = if (shouldLaunchSingleTop()) 1 else 0
        result = 31 * result + if (shouldRestoreState()) 1 else 0
        result = 31 * result + popUpToId
        result = 31 * result + popUpToRoute.hashCode()
        result = 31 * result + popUpToRouteClass.hashCode()
        result = 31 * result + popUpToRouteObject.hashCode()
        result = 31 * result + if (isPopUpToInclusive()) 1 else 0
        result = 31 * result + if (shouldPopUpToSaveState()) 1 else 0
        result = 31 * result + enterAnim
        result = 31 * result + exitAnim
        result = 31 * result + popEnterAnim
        result = 31 * result + popExitAnim
        return result
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(javaClass.simpleName)
        sb.append("(")
        if (singleTop) {
            sb.append("launchSingleTop ")
        }
        if (restoreState) {
            sb.append("restoreState ")
        }
        if (popUpToRoute != null || popUpToId != -1)
        if (popUpToRoute != null) {
            sb.append("popUpTo(")
            if (popUpToRoute != null) {
                sb.append(popUpToRoute)
            } else if (popUpToRouteClass != null) {
                sb.append(popUpToRouteClass)
            } else if (popUpToRouteObject != null) {
                sb.append(popUpToRouteObject)
            } else {
                sb.append("0x")
                sb.append(Integer.toHexString(popUpToId))
            }
            if (popUpToInclusive) {
                sb.append(" inclusive")
            }
            if (popUpToSaveState) {
                sb.append(" saveState")
            }
            sb.append(")")
        }
        if (enterAnim != -1 || exitAnim != -1 || popEnterAnim != -1 || popExitAnim != -1) {
            sb.append("anim(enterAnim=0x")
            sb.append(Integer.toHexString(enterAnim))
            sb.append(" exitAnim=0x")
            sb.append(Integer.toHexString(exitAnim))
            sb.append(" popEnterAnim=0x")
            sb.append(Integer.toHexString(popEnterAnim))
            sb.append(" popExitAnim=0x")
            sb.append(Integer.toHexString(popExitAnim))
            sb.append(")")
        }
        return sb.toString()
    }

    /**
     * Builder for constructing new instances of NavOptions.
     */
    public class Builder {
        private var singleTop = false
        private var restoreState = false

        @IdRes
        private var popUpToId = -1
        private var popUpToRoute: String? = null
        private var popUpToRouteClass: KClass<*>? = null
        private var popUpToRouteObject: Any? = null
        private var popUpToInclusive = false
        private var popUpToSaveState = false

        @AnimRes
        @AnimatorRes
        private var enterAnim = -1

        @AnimRes
        @AnimatorRes
        private var exitAnim = -1

        @AnimRes
        @AnimatorRes
        private var popEnterAnim = -1

        @AnimRes
        @AnimatorRes
        private var popExitAnim = -1

        /**
         * Launch a navigation target as single-top if you are making a lateral navigation
         * between instances of the same target (e.g. detail pages about similar data items)
         * that should not preserve history.
         *
         * @param singleTop true to launch as single-top
         */
        public fun setLaunchSingleTop(singleTop: Boolean): Builder {
            this.singleTop = singleTop
            return this
        }

        /**
         * Whether this navigation action should restore any state previously saved
         * by [setPopUpTo] or the `popUpToSaveState` attribute. If no state was
         * previously saved with the destination ID being navigated to, this has no effect.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        public fun setRestoreState(restoreState: Boolean): Builder {
            this.restoreState = restoreState
            return this
        }

        /**
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param destinationId The destination to pop up to, clearing all intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @param saveState true if the back stack and the state of all destinations between the
         * current destination and [destinationId] should be saved for later restoration via
         * [setRestoreState] or the `restoreState` attribute using the same ID
         * as [popUpToId] (note: this matching ID is true whether [inclusive] is true or
         * false).
         * @return this Builder
         *
         * @see NavOptions.popUpToId
         * @see NavOptions.isPopUpToInclusive
         */
        @JvmOverloads
        public fun setPopUpTo(
            @IdRes destinationId: Int,
            inclusive: Boolean,
            saveState: Boolean = false
        ): Builder {
            popUpToId = destinationId
            popUpToRoute = null
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        /**
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param route route for destination to pop up to, clearing all intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @param saveState true if the back stack and the state of all destinations between the
         * current destination and [route] should be saved for later restoration via
         * [setRestoreState] or the `restoreState` attribute using the same ID
         * as [popUpToRoute] (note: this matching ID is true whether [inclusive] is true or
         * false).
         * @return this Builder
         *
         * @see NavOptions.popUpToId
         * @see NavOptions.isPopUpToInclusive
         */
        @JvmOverloads
        public fun setPopUpTo(
            route: String?,
            inclusive: Boolean,
            saveState: Boolean = false
        ): Builder {
            popUpToRoute = route
            popUpToId = -1
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        /**
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param T route from a [KClass] for destination to pop up to, clearing all
         * intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @param saveState true if the back stack and the state of all destinations between the
         * current destination and [T] should be saved for later restoration via
         * [setRestoreState] or the `restoreState` attribute using the same route from [KClass]
         * as [popUpToRouteClass] (note: this matching route is true whether [inclusive] is true or
         * false).
         * @return this Builder
         *
         * @see NavOptions.popUpToId
         * @see NavOptions.isPopUpToInclusive
         */
        @JvmOverloads
        @Suppress("MissingGetterMatchingBuilder") // no need for getter
        @ExperimentalSafeArgsApi
        public inline fun <reified T : Any> setPopUpTo(
            inclusive: Boolean,
            saveState: Boolean = false
        ): Builder {
            setPopUpTo(T::class, inclusive, saveState)
            return this
        }

        // this restricted public is needed so that the public reified [popUpTo] can call
        // private popUpToRouteClass setter
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun setPopUpTo(
            klass: KClass<*>,
            inclusive: Boolean,
            saveState: Boolean = false
        ): Builder {
            popUpToRouteClass = klass
            popUpToId = -1
            popUpToInclusive = inclusive
            popUpToSaveState = saveState
            return this
        }

        /**
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param route route from an Object for destination to pop up to, clearing all
         * intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @param saveState true if the back stack and the state of all destinations between the
         * current destination and [route] should be saved for later restoration via
         * [setRestoreState] or the `restoreState` attribute using the same route from an Object
         * as [popUpToRouteObject] (note: this matching route is true whether [inclusive] is
         * true or false).
         * @return this Builder
         *
         * @see NavOptions.popUpToId
         * @see NavOptions.isPopUpToInclusive
         */
        @JvmOverloads
        @Suppress("MissingGetterMatchingBuilder")
        @ExperimentalSafeArgsApi
        @OptIn(InternalSerializationApi::class)
        public fun <T : Any> setPopUpTo(
            route: T,
            inclusive: Boolean,
            saveState: Boolean = false
        ): Builder {
            popUpToRouteObject = route
            setPopUpTo(route::class.serializer().hashCode(), inclusive, saveState)
            return this
        }

        /**
         * Sets a custom Animation or Animator resource for the enter animation.
         *
         * Note: Animator resources are not supported for navigating to a new Activity
         * @param enterAnim Custom animation to run
         * @return this Builder
         *
         * @see NavOptions.enterAnim
         */
        public fun setEnterAnim(@AnimRes @AnimatorRes enterAnim: Int): Builder {
            this.enterAnim = enterAnim
            return this
        }

        /**
         * Sets a custom Animation or Animator resource for the exit animation.
         *
         * Note: Animator resources are not supported for navigating to a new Activity
         * @param exitAnim Custom animation to run
         * @return this Builder
         *
         * @see NavOptions.exitAnim
         */
        public fun setExitAnim(@AnimRes @AnimatorRes exitAnim: Int): Builder {
            this.exitAnim = exitAnim
            return this
        }

        /**
         * Sets a custom Animation or Animator resource for the enter animation
         * when popping off the back stack.
         *
         * Note: Animator resources are not supported for navigating to a new Activity
         * @param popEnterAnim Custom animation to run
         * @return this Builder
         *
         * @see NavOptions.popEnterAnim
         */
        public fun setPopEnterAnim(@AnimRes @AnimatorRes popEnterAnim: Int): Builder {
            this.popEnterAnim = popEnterAnim
            return this
        }

        /**
         * Sets a custom Animation or Animator resource for the exit animation
         * when popping off the back stack.
         *
         * Note: Animator resources are not supported for navigating to a new Activity
         * @param popExitAnim Custom animation to run
         * @return this Builder
         *
         * @see NavOptions.popExitAnim
         */
        public fun setPopExitAnim(@AnimRes @AnimatorRes popExitAnim: Int): Builder {
            this.popExitAnim = popExitAnim
            return this
        }

        /**
         * @return a constructed NavOptions
         */
        public fun build(): NavOptions {
            return if (popUpToRoute != null) {
                NavOptions(
                    singleTop, restoreState,
                    popUpToRoute, popUpToInclusive, popUpToSaveState,
                    enterAnim, exitAnim, popEnterAnim, popExitAnim
                )
            } else if (popUpToRouteClass != null) {
                NavOptions(
                    singleTop, restoreState,
                    popUpToRouteClass, popUpToInclusive, popUpToSaveState,
                    enterAnim, exitAnim, popEnterAnim, popExitAnim
                )
            } else if (popUpToRouteObject != null) {
                NavOptions(
                    singleTop, restoreState,
                    popUpToRouteObject!!, popUpToInclusive, popUpToSaveState,
                    enterAnim, exitAnim, popEnterAnim, popExitAnim
                )
            } else {
                NavOptions(
                    singleTop, restoreState,
                    popUpToId, popUpToInclusive, popUpToSaveState,
                    enterAnim, exitAnim, popEnterAnim, popExitAnim
                )
            }
        }
    }
}
