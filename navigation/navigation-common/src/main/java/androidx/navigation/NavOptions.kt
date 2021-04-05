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

/**
 * NavOptions stores special options for navigate actions
 */
public class NavOptions internal constructor(
    private val singleTop: Boolean,
    /**
     * The destination to pop up to before navigating. When set, all non-matching destinations
     * should be popped from the back stack.
     * @return the destinationId to pop up to, clearing all intervening destinations
     * @see Builder.setPopUpTo
     *
     * @see isPopUpToInclusive
     */
    @field:IdRes @get:IdRes @param:IdRes
    public val popUpTo: Int,
    private val popUpToInclusive: Boolean,
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
     * Whether the destination set in [.getPopUpTo] should be popped from the back stack.
     * @see Builder.setPopUpTo
     *
     * @see NavOptions.getPopUpTo
     */
    public fun isPopUpToInclusive(): Boolean {
        return popUpToInclusive
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NavOptions
        return singleTop == that.singleTop &&
            popUpTo == that.popUpTo &&
            popUpToInclusive == that.popUpToInclusive &&
            enterAnim == that.enterAnim &&
            exitAnim == that.exitAnim &&
            popEnterAnim == that.popEnterAnim &&
            popExitAnim == that.popExitAnim
    }

    override fun hashCode(): Int {
        var result = if (shouldLaunchSingleTop()) 1 else 0
        result = 31 * result + popUpTo
        result = 31 * result + if (isPopUpToInclusive()) 1 else 0
        result = 31 * result + enterAnim
        result = 31 * result + exitAnim
        result = 31 * result + popEnterAnim
        result = 31 * result + popExitAnim
        return result
    }

    /**
     * Builder for constructing new instances of NavOptions.
     */
    public class Builder {
        private var singleTop = false

        @IdRes
        private var popUpTo = -1
        private var popUpToInclusive = false

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
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param destinationId The destination to pop up to, clearing all intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @return this Builder
         *
         * @see NavOptions.popUpTo
         * @see NavOptions.isPopUpToInclusive
         */
        public fun setPopUpTo(@IdRes destinationId: Int, inclusive: Boolean): Builder {
            popUpTo = destinationId
            popUpToInclusive = inclusive
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
            return NavOptions(
                singleTop, popUpTo, popUpToInclusive, enterAnim, exitAnim, popEnterAnim,
                popExitAnim
            )
        }
    }
}
