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

package android.arch.navigation;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.HashMap;

/**
 * Navigator that navigates through {@link FragmentTransaction fragment transactions}. Every
 * destination using this Navigator must set a valid Fragment class name with
 * <code>android:name</code> or {@link Destination#setFragmentClass}.
 */
@Navigator.Name("fragment")
public class FragmentNavigator extends Navigator<FragmentNavigator.Destination> {
    private Context mContext;
    private FragmentManager mFragmentManager;
    private int mContainerId;
    private int mBackStackCount;
    private HashMap<String, Class<? extends Fragment>> mFragmentClasses = new HashMap<>();

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {
                    int newCount = mFragmentManager.getBackStackEntryCount();
                    @BackStackEffect int backStackEffect;
                    if (newCount < mBackStackCount) {
                        backStackEffect = BACK_STACK_DESTINATION_POPPED;
                    } else if (newCount > mBackStackCount) {
                        backStackEffect = BACK_STACK_DESTINATION_ADDED;
                    } else {
                        backStackEffect = BACK_STACK_UNCHANGED;
                    }
                    mBackStackCount = newCount;

                    int destId = 0;
                    StateFragment state = getState();
                    if (state != null) {
                        destId = state.mCurrentDestId;
                    }
                    dispatchOnNavigatorNavigated(destId, backStackEffect);
                }
            };

    public FragmentNavigator(Context context, FragmentManager manager, int containerId) {
        mContext = context;
        mFragmentManager = manager;
        mContainerId = containerId;

        mBackStackCount = mFragmentManager.getBackStackEntryCount();
        mFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);
    }

    @Override
    public boolean popBackStack() {
        return mFragmentManager.popBackStackImmediate();
    }

    @Override
    public Destination createDestination() {
        return new Destination(this);
    }

    Class<? extends Fragment> getFragmentClassByName(String name) {
        Class<? extends Fragment> clazz = mFragmentClasses.get(name);
        if (clazz == null) {
            try {
                clazz = (Class<? extends Fragment>) Class.forName(name, true,
                        mContext.getClassLoader());
                mFragmentClasses.put(name, clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return clazz;
    }

    private String getBackStackName(@IdRes int destinationId) {
        String name = mContext.getResources().getResourceName(destinationId);
        if (TextUtils.isEmpty(name)) {
            // Fallback to the id if there isn't a resource name
            return Integer.toString(destinationId);
        }
        return name;
    }

    @Override
    public void navigate(Destination destination, Bundle args,
                            NavOptions navOptions) {
        final Fragment frag = destination.createFragment(args);
        final FragmentTransaction ft = mFragmentManager.beginTransaction();

        int enterAnim = navOptions != null ? navOptions.getEnterAnim() : -1;
        int exitAnim = navOptions != null ? navOptions.getExitAnim() : -1;
        if (enterAnim != -1 || exitAnim != -1) {
            enterAnim = enterAnim != -1 ? enterAnim : 0;
            exitAnim = exitAnim != -1 ? exitAnim : 0;
            ft.setCustomAnimations(enterAnim, exitAnim);
        }

        ft.replace(mContainerId, frag);

        final StateFragment oldState = getState();
        if (oldState != null) {
            ft.remove(oldState);
        }

        final @IdRes int destId = destination.getId();
        final StateFragment newState = new StateFragment();
        newState.mCurrentDestId = destId;
        ft.add(newState, StateFragment.FRAGMENT_TAG);

        final boolean initialNavigation = mFragmentManager.getFragments().isEmpty();
        final boolean isClearTask = navOptions != null && navOptions.shouldClearTask();
        // TODO Build first class singleTop behavior for fragments
        final boolean isSingleTopReplacement = navOptions != null && oldState != null
                && navOptions.shouldLaunchSingleTop()
                && oldState.mCurrentDestId == destId;
        if (!initialNavigation && !isClearTask && !isSingleTopReplacement) {
            ft.addToBackStack(getBackStackName(destId));
        } else {
            ft.runOnCommit(new Runnable() {
                @Override
                public void run() {
                    dispatchOnNavigatorNavigated(destId, isSingleTopReplacement
                            ? BACK_STACK_UNCHANGED
                            : BACK_STACK_DESTINATION_ADDED);
                }
            });
        }
        ft.commit();
        mFragmentManager.executePendingTransactions();
    }

    private StateFragment getState() {
        return (StateFragment) mFragmentManager.findFragmentByTag(StateFragment.FRAGMENT_TAG);
    }

    /**
     * NavDestination specific to {@link FragmentNavigator}
     */
    public static class Destination extends NavDestination {
        private Class<? extends Fragment> mFragmentClass;

        /**
         * Construct a new fragment destination to navigate to the given Fragment.
         *
         * @param navigatorProvider The {@link NavController} which this destination
         *                          will be associated with.
         * @param clazz Fragment this destination should create when navigated to
         */
        public Destination(@NonNull NavigatorProvider navigatorProvider,
                Class<? extends Fragment> clazz) {
            super(navigatorProvider.getNavigator(FragmentNavigator.class));
            setFragmentClass(clazz);
        }

        Destination(@NonNull FragmentNavigator fragmentNavigator) {
            super(fragmentNavigator);
        }

        @Override
        public void onInflate(Context context, AttributeSet attrs) {
            super.onInflate(context, attrs);
            TypedArray a = context.getResources().obtainAttributes(attrs,
                    R.styleable.FragmentNavigator);
            setFragmentClass(((FragmentNavigator) getNavigator())
                    .getFragmentClassByName(a.getString(
                            R.styleable.FragmentNavigator_android_name)));
            a.recycle();
        }

        /**
         * Set the Fragment associated with this destination
         * @param clazz The class name of the Fragment to show when you navigate to this
         *              destination
         */
        public void setFragmentClass(Class<? extends Fragment> clazz) {
            mFragmentClass = clazz;
        }

        /**
         * Gets the Fragment associated with this destination
         * @return
         */
        public Class<? extends Fragment> getFragmentClass() {
            return mFragmentClass;
        }

        Fragment createFragment(Bundle args) {
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

    /**
     * An internal fragment used by FragmentNavigator to track additional navigation state.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class StateFragment extends Fragment {
        static final String FRAGMENT_TAG = "android-support-nav:FragmentNavigator.StateFragment";

        private static final String KEY_CURRENT_DEST_ID = "currentDestId";

        int mCurrentDestId;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mCurrentDestId = savedInstanceState.getInt(KEY_CURRENT_DEST_ID);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(KEY_CURRENT_DEST_ID, mCurrentDestId);
        }
    }
}
