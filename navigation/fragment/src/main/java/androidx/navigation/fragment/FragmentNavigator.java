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

package androidx.navigation.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.NavigatorProvider;

import java.util.ArrayDeque;
import java.util.HashMap;

/**
 * Navigator that navigates through {@link FragmentTransaction fragment transactions}. Every
 * destination using this Navigator must set a valid Fragment class name with
 * <code>android:name</code> or {@link Destination#setFragmentClass}.
 */
@Navigator.Name("fragment")
public class FragmentNavigator extends Navigator<FragmentNavigator.Destination> {
    private static final String KEY_BACK_STACK_IDS = "androidx-nav-fragment:navigator:backStackIds";

    private Context mContext;
    private FragmentManager mFragmentManager;
    private int mContainerId;
    private ArrayDeque<Integer> mBackStack = new ArrayDeque<>();

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {
                    // The initial Fragment won't be on the back stack, so the
                    // real count of destinations is the back stack entry count + 1
                    int newCount = mFragmentManager.getBackStackEntryCount() + 1;
                    if (newCount < mBackStack.size()) {
                        // Handle cases where the user hit the system back button
                        while (mBackStack.size() > newCount) {
                            mBackStack.removeLast();
                        }
                        int destId = mBackStack.isEmpty() ? 0 : mBackStack.peekLast();
                        dispatchOnNavigatorNavigated(destId, BACK_STACK_DESTINATION_POPPED);
                    }
                }
            };

    public FragmentNavigator(@NonNull Context context, @NonNull FragmentManager manager,
            int containerId) {
        mContext = context;
        mFragmentManager = manager;
        mContainerId = containerId;

        mFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);
    }

    @Override
    public boolean popBackStack() {
        if (mFragmentManager.getBackStackEntryCount() == 0) {
            return false;
        }
        mFragmentManager.popBackStack();
        mBackStack.removeLast();
        int destId = mBackStack.isEmpty() ? 0 : mBackStack.peekLast();
        dispatchOnNavigatorNavigated(destId, BACK_STACK_DESTINATION_POPPED);
        return true;
    }

    @NonNull
    @Override
    public Destination createDestination() {
        return new Destination(this);
    }

    @NonNull
    private String getBackStackName(@IdRes int destinationId) {
        // This gives us the resource name if it exists,
        // or just the destinationId if it doesn't exist
        try {
            return mContext.getResources().getResourceName(destinationId);
        } catch (Resources.NotFoundException e) {
            return Integer.toString(destinationId);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void navigate(@NonNull Destination destination, @Nullable Bundle args,
                            @Nullable NavOptions navOptions) {
        final Fragment frag = destination.createFragment(args);
        final FragmentTransaction ft = mFragmentManager.beginTransaction();

        int enterAnim = navOptions != null ? navOptions.getEnterAnim() : -1;
        int exitAnim = navOptions != null ? navOptions.getExitAnim() : -1;
        int popEnterAnim = navOptions != null ? navOptions.getPopEnterAnim() : -1;
        int popExitAnim = navOptions != null ? navOptions.getPopExitAnim() : -1;
        if (enterAnim != -1 || exitAnim != -1 || popEnterAnim != -1 || popExitAnim != -1) {
            enterAnim = enterAnim != -1 ? enterAnim : 0;
            exitAnim = exitAnim != -1 ? exitAnim : 0;
            popEnterAnim = popEnterAnim != -1 ? popEnterAnim : 0;
            popExitAnim = popExitAnim != -1 ? popExitAnim : 0;
            ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim);
        }

        ft.replace(mContainerId, frag);

        final @IdRes int destId = destination.getId();
        final boolean initialNavigation = mBackStack.isEmpty();
        final boolean isClearTask = navOptions != null && navOptions.shouldClearTask();
        // TODO Build first class singleTop behavior for fragments
        final boolean isSingleTopReplacement = navOptions != null && !initialNavigation
                && navOptions.shouldLaunchSingleTop()
                && mBackStack.peekLast() == destId;

        int backStackEffect;
        if (!initialNavigation && !isClearTask && !isSingleTopReplacement) {
            ft.addToBackStack(getBackStackName(destId));
            backStackEffect = BACK_STACK_DESTINATION_ADDED;
        } else if (isSingleTopReplacement) {
            // Single Top means we only want one instance on the back stack
            if (mBackStack.size() > 1) {
                // If the Fragment to be replaced is on the FragmentManager's
                // back stack, a simple replace() isn't enough so we
                // remove it from the back stack and put our replacement
                // on the back stack in its place
                mFragmentManager.popBackStack();
                ft.addToBackStack(getBackStackName(destId));
            }
            backStackEffect = BACK_STACK_UNCHANGED;
        } else {
            backStackEffect = BACK_STACK_DESTINATION_ADDED;
        }
        ft.setReorderingAllowed(true);
        ft.commit();
        // The commit succeeded, update our view of the world
        if (initialNavigation || !isSingleTopReplacement) {
            mBackStack.add(destId);
        }
        dispatchOnNavigatorNavigated(destId, backStackEffect);
    }

    @Override
    @Nullable
    public Bundle onSaveState() {
        Bundle b = new Bundle();
        int[] backStack = new int[mBackStack.size()];
        int index = 0;
        for (Integer id : mBackStack) {
            backStack[index++] = id;
        }
        b.putIntArray(KEY_BACK_STACK_IDS, backStack);
        return b;
    }

    @Override
    public void onRestoreState(@Nullable Bundle savedState) {
        if (savedState != null) {
            int[] backStack = savedState.getIntArray(KEY_BACK_STACK_IDS);
            if (backStack != null) {
                for (int destId : backStack) {
                    mBackStack.add(destId);
                }
            }
        }
    }

    /**
     * NavDestination specific to {@link FragmentNavigator}
     */
    public static class Destination extends NavDestination {
        private static final HashMap<String, Class<? extends Fragment>> sFragmentClasses =
                new HashMap<>();

        private Class<? extends Fragment> mFragmentClass;

        /**
         * Construct a new fragment destination. This destination is not valid until you set the
         * Fragment via {@link #setFragmentClass(Class)}.
         *
         * @param navigatorProvider The {@link NavController} which this destination
         *                          will be associated with.
         */
        public Destination(@NonNull NavigatorProvider navigatorProvider) {
            this(navigatorProvider.getNavigator(FragmentNavigator.class));
        }

        /**
         * Construct a new fragment destination. This destination is not valid until you set the
         * Fragment via {@link #setFragmentClass(Class)}.
         *
         * @param fragmentNavigator The {@link FragmentNavigator} which this destination
         *                          will be associated with. Generally retrieved via a
         *                          {@link NavController}'s
         *                          {@link NavigatorProvider#getNavigator(Class)} method.
         */
        public Destination(@NonNull Navigator<? extends Destination> fragmentNavigator) {
            super(fragmentNavigator);
        }

        @Override
        public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs) {
            super.onInflate(context, attrs);
            TypedArray a = context.getResources().obtainAttributes(attrs,
                    R.styleable.FragmentNavigator);
            setFragmentClass(getFragmentClassByName(context, a.getString(
                            R.styleable.FragmentNavigator_android_name)));
            a.recycle();
        }

        @SuppressWarnings("unchecked")
        @NonNull
        private Class<? extends Fragment> getFragmentClassByName(Context context, String name) {
            if (name != null && name.charAt(0) == '.') {
                name = context.getPackageName() + name;
            }
            Class<? extends Fragment> clazz = sFragmentClasses.get(name);
            if (clazz == null) {
                try {
                    clazz = (Class<? extends Fragment>) Class.forName(name, true,
                            context.getClassLoader());
                    sFragmentClasses.put(name, clazz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            return clazz;
        }

        /**
         * Set the Fragment associated with this destination
         * @param clazz The class name of the Fragment to show when you navigate to this
         *              destination
         * @return this {@link Destination}
         */
        @NonNull
        public Destination setFragmentClass(@NonNull Class<? extends Fragment> clazz) {
            mFragmentClass = clazz;
            return this;
        }

        /**
         * Gets the Fragment associated with this destination
         * @return
         */
        public Class<? extends Fragment> getFragmentClass() {
            return mFragmentClass;
        }

        /**
         * Create a new instance of the {@link Fragment} associated with this destination.
         * @param args optional args to set on the new Fragment
         * @return an instance of the {@link #getFragmentClass() Fragment class} associated
         * with this destination
         */
        @SuppressWarnings("ClassNewInstance")
        @NonNull
        public Fragment createFragment(@Nullable Bundle args) {
            Class<? extends Fragment> clazz = getFragmentClass();
            if (clazz == null) {
                throw new IllegalStateException("fragment class not set");
            }

            Fragment f;
            try {
                f = clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (args != null) {
                f.setArguments(args);
            }
            return f;
        }
    }
}
