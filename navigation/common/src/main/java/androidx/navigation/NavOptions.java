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

package androidx.navigation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.annotation.AnimatorRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * NavOptions stores special options for navigate actions
 */
public class NavOptions {
    private static final String KEY_NAV_OPTIONS = "android-support-nav:navOptions";
    private static final String KEY_SINGLE_TOP = "singleTop";
    private static final String KEY_POP_UP_TO = "popUpTo";
    private static final String KEY_POP_UP_TO_INCLUSIVE = "popUpToInclusive";
    private static final String KEY_ENTER_ANIM = "enterAnim";
    private static final String KEY_EXIT_ANIM = "exitAnim";
    private static final String KEY_POP_ENTER_ANIM = "popEnterAnim";
    private static final String KEY_POP_EXIT_ANIM = "popExitAnim";

    /**
     * Add the {@link #getPopEnterAnim() pop enter} and {@link #getPopExitAnim() pop exit}
     * animation to an Intent for later usage with
     * {@link #applyPopAnimationsToPendingTransition(Activity)}.
     * <p>
     * This is automatically called for you by {@link ActivityNavigator}.
     * </p>
     *
     * @param intent Intent being started with the given NavOptions
     * @param navOptions NavOptions containing the pop animations.
     * @see #applyPopAnimationsToPendingTransition(Activity)
     * @see #getPopEnterAnim()
     * @see #getPopExitAnim()
     */
    public static void addPopAnimationsToIntent(@NonNull Intent intent,
            @Nullable NavOptions navOptions) {
        if (navOptions != null) {
            intent.putExtra(KEY_NAV_OPTIONS, navOptions.toBundle());
        }
    }

    /**
     * Apply any pop animations in the Intent of the given Activity to a pending transition.
     * This should be used in place of  {@link Activity#overridePendingTransition(int, int)}
     * to get the appropriate pop animations.
     * @param activity An activity started from the {@link ActivityNavigator}.
     * @see #addPopAnimationsToIntent(Intent, NavOptions)
     * @see #getPopEnterAnim()
     * @see #getPopExitAnim()
     */
    public static void applyPopAnimationsToPendingTransition(@NonNull Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        Bundle bundle = intent.getBundleExtra(KEY_NAV_OPTIONS);
        if (bundle != null) {
            NavOptions navOptions = NavOptions.fromBundle(bundle);
            int popEnterAnim = navOptions.getPopEnterAnim();
            int popExitAnim = navOptions.getPopExitAnim();
            if (popEnterAnim != -1 || popExitAnim != -1) {
                popEnterAnim = popEnterAnim != -1 ? popEnterAnim : 0;
                popExitAnim = popExitAnim != -1 ? popExitAnim : 0;
                activity.overridePendingTransition(popEnterAnim, popExitAnim);
            }
        }
    }

    private boolean mSingleTop;
    @IdRes
    private int mPopUpTo;
    private boolean mPopUpToInclusive;
    @AnimRes @AnimatorRes
    private int mEnterAnim;
    @AnimRes @AnimatorRes
    private int mExitAnim;
    @AnimRes @AnimatorRes
    private int mPopEnterAnim;
    @AnimRes @AnimatorRes
    private int mPopExitAnim;

    NavOptions(boolean singleTop, @IdRes int popUpTo, boolean popUpToInclusive,
            @AnimRes @AnimatorRes int enterAnim, @AnimRes @AnimatorRes int exitAnim,
            @AnimRes @AnimatorRes int popEnterAnim, @AnimRes @AnimatorRes int popExitAnim) {
        mSingleTop = singleTop;
        mPopUpTo = popUpTo;
        mPopUpToInclusive = popUpToInclusive;
        mEnterAnim = enterAnim;
        mExitAnim = exitAnim;
        mPopEnterAnim = popEnterAnim;
        mPopExitAnim = popExitAnim;
    }

    /**
     * Whether this navigation action should launch as single-top (i.e., there will be at most
     * one copy of a given destination on the top of the back stack).
     * <p>
     * This functions similarly to how {@link android.content.Intent#FLAG_ACTIVITY_SINGLE_TOP}
     * works with activites.
     */
    public boolean shouldLaunchSingleTop() {
        return mSingleTop;
    }

    /**
     * The destination to pop up to before navigating. When set, all non-matching destinations
     * should be popped from the back stack.
     * @return the destinationId to pop up to, clearing all intervening destinations
     * @see Builder#setPopUpTo
     * @see #isPopUpToInclusive
     */
    @IdRes
    public int getPopUpTo() {
        return mPopUpTo;
    }

    /**
     * Whether the destination set in {@link #getPopUpTo} should be popped from the back stack.
     * @see Builder#setPopUpTo
     * @see #getPopUpTo
     */
    public boolean isPopUpToInclusive() {
        return mPopUpToInclusive;
    }

    /**
     * The custom enter Animation/Animator that should be run.
     * @return the resource id of a Animation or Animator or -1 if none.
     */
    @AnimRes @AnimatorRes
    public int getEnterAnim() {
        return mEnterAnim;
    }

    /**
     * The custom exit Animation/Animator that should be run.
     * @return the resource id of a Animation or Animator or -1 if none.
     */
    @AnimRes @AnimatorRes
    public int getExitAnim() {
        return mExitAnim;
    }

    /**
     * The custom enter Animation/Animator that should be run when this destination is
     * popped from the back stack.
     * @return the resource id of a Animation or Animator or -1 if none.
     * @see #applyPopAnimationsToPendingTransition(Activity)
     */
    @AnimRes @AnimatorRes
    public int getPopEnterAnim() {
        return mPopEnterAnim;
    }

    /**
     * The custom exit Animation/Animator that should be run when this destination is
     * popped from the back stack.
     * @return the resource id of a Animation or Animator or -1 if none.
     * @see #applyPopAnimationsToPendingTransition(Activity)
     */
    @AnimRes @AnimatorRes
    public int getPopExitAnim() {
        return mPopExitAnim;
    }

    @NonNull
    private Bundle toBundle() {
        Bundle b = new Bundle();
        b.putBoolean(KEY_SINGLE_TOP, mSingleTop);
        b.putInt(KEY_POP_UP_TO, mPopUpTo);
        b.putBoolean(KEY_POP_UP_TO_INCLUSIVE, mPopUpToInclusive);
        b.putInt(KEY_ENTER_ANIM, mEnterAnim);
        b.putInt(KEY_EXIT_ANIM, mExitAnim);
        b.putInt(KEY_POP_ENTER_ANIM, mPopEnterAnim);
        b.putInt(KEY_POP_EXIT_ANIM, mPopExitAnim);
        return b;
    }

    @NonNull
    private static NavOptions fromBundle(@NonNull Bundle b) {
        return new NavOptions(b.getBoolean(KEY_SINGLE_TOP, false),
                b.getInt(KEY_POP_UP_TO, 0), b.getBoolean(KEY_POP_UP_TO_INCLUSIVE, false),
                b.getInt(KEY_ENTER_ANIM, -1), b.getInt(KEY_EXIT_ANIM, -1),
                b.getInt(KEY_POP_ENTER_ANIM, -1), b.getInt(KEY_POP_EXIT_ANIM, -1));
    }

    /**
     * Builder for constructing new instances of NavOptions.
     */
    public static class Builder {
        boolean mSingleTop;
        @IdRes
        int mPopUpTo;
        boolean mPopUpToInclusive;
        @AnimRes @AnimatorRes
        int mEnterAnim = -1;
        @AnimRes @AnimatorRes
        int mExitAnim = -1;
        @AnimRes @AnimatorRes
        int mPopEnterAnim = -1;
        @AnimRes @AnimatorRes
        int mPopExitAnim = -1;

        public Builder() {
        }

        /**
         * Launch a navigation target as single-top if you are making a lateral navigation
         * between instances of the same target (e.g. detail pages about similar data items)
         * that should not preserve history.
         *
         * @param singleTop true to launch as single-top
         */
        @NonNull
        public Builder setLaunchSingleTop(boolean singleTop) {
            mSingleTop = singleTop;
            return this;
        }

        /**
         * Pop up to a given destination before navigating. This pops all non-matching destinations
         * from the back stack until this destination is found.
         *
         * @param destinationId The destination to pop up to, clearing all intervening destinations.
         * @param inclusive true to also pop the given destination from the back stack.
         * @return this Builder
         * @see NavOptions#getPopUpTo
         * @see NavOptions#isPopUpToInclusive
         */
        @NonNull
        public Builder setPopUpTo(@IdRes int destinationId, boolean inclusive) {
            mPopUpTo = destinationId;
            mPopUpToInclusive = inclusive;
            return this;
        }

        /**
         * Sets a custom Animation or Animator resource for the enter animation.
         *
         * <p>Note: Animator resources are not supported for navigating to a new Activity</p>
         * @param enterAnim Custom animation to run
         * @return this Builder
         * @see NavOptions#getEnterAnim()
         */
        @NonNull
        public Builder setEnterAnim(@AnimRes @AnimatorRes int enterAnim) {
            mEnterAnim = enterAnim;
            return this;
        }

        /**
         * Sets a custom Animation or Animator resource for the exit animation.
         *
         * <p>Note: Animator resources are not supported for navigating to a new Activity</p>
         * @param exitAnim Custom animation to run
         * @return this Builder
         * @see NavOptions#getExitAnim()
         */
        @NonNull
        public Builder setExitAnim(@AnimRes @AnimatorRes int exitAnim) {
            mExitAnim = exitAnim;
            return this;
        }

        /**
         * Sets a custom Animation or Animator resource for the enter animation
         * when popping off the back stack.
         *
         * <p>Note: Animator resources are not supported for navigating to a new Activity</p>
         * @param popEnterAnim Custom animation to run
         * @return this Builder
         * @see NavOptions#getPopEnterAnim()
         */
        @NonNull
        public Builder setPopEnterAnim(@AnimRes @AnimatorRes int popEnterAnim) {
            mPopEnterAnim = popEnterAnim;
            return this;
        }

        /**
         * Sets a custom Animation or Animator resource for the exit animation
         * when popping off the back stack.
         *
         * <p>Note: Animator resources are not supported for navigating to a new Activity</p>
         * @param popExitAnim Custom animation to run
         * @return this Builder
         * @see NavOptions#getPopExitAnim()
         */
        @NonNull
        public Builder setPopExitAnim(@AnimRes @AnimatorRes int popExitAnim) {
            mPopExitAnim = popExitAnim;
            return this;
        }

        /**
         * @return a constructed NavOptions
         */
        @NonNull
        public NavOptions build() {
            return new NavOptions(mSingleTop, mPopUpTo, mPopUpToInclusive,
                    mEnterAnim, mExitAnim, mPopEnterAnim, mPopExitAnim);
        }
    }
}
