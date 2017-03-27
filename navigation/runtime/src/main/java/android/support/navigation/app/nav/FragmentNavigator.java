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

package android.support.navigation.app.nav;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.support.navigation.R;

import java.util.HashMap;

/**
 * Navigator that navigates through {@link FragmentTransaction fragment transactions}. Every
 * destination using this Navigator must set a valid Fragment class name with
 * <code>android:name</code> or {@link Destination#setFragmentClass}.
 */
public class FragmentNavigator extends Navigator<FragmentNavigator.Destination> {
    public static final String NAME = "fragment";

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
                    boolean isPopOperation = newCount < mBackStackCount;
                    mBackStackCount = newCount;

                    int destId = 0;
                    StateFragment state = getState();
                    if (state != null) {
                        destId = state.mCurrentDestId;
                    }
                    dispatchOnNavigatorNavigated(destId, isPopOperation);
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

    @Override
    public boolean navigate(Destination destination, Bundle args,
                            NavOptions navOptions) {
        String flowName = navOptions != null ? navOptions.getFlowName() : null;
        if (flowName == null) {
            flowName = destination.getFlow();
        }

        // If the first non-null back stack entry name we find matches our flow name, we're still
        // part of the same flow and we should pass null as the name of this transaction.
        // This way we can finish a whole flow back to its root by the flow name.
        if (flowName != null) {
            final int stackCount = mFragmentManager.getBackStackEntryCount();
            for (int i = stackCount - 1; i >= 0; i--) {
                final FragmentManager.BackStackEntry bse = mFragmentManager.getBackStackEntryAt(i);
                final String bseName = bse.getName();
                if (bseName != null) {
                    if (TextUtils.equals(bseName, flowName)) {
                        flowName = null;
                    }
                    break;
                }
            }
        }
        final Fragment frag = destination.createFragment(args);
        final FragmentTransaction ft = mFragmentManager.beginTransaction()
                .replace(mContainerId, frag);

        final StateFragment oldState = getState();
        if (oldState != null) {
            ft.remove(oldState);
        }

        final int destId = destination.getId();
        final StateFragment newState = new StateFragment();
        newState.mCurrentDestId = destId;
        ft.add(newState, StateFragment.FRAGMENT_TAG);

        final boolean initialNavigation = mFragmentManager.getFragments().isEmpty();
        if (!initialNavigation) {
            ft.addToBackStack(flowName);
        } else {
            ft.postOnCommit(new Runnable() {
                @Override
                public void run() {
                    dispatchOnNavigatorNavigated(destId, false);
                }
            });
        }
        ft.commit();

        return true;
    }

    private StateFragment getState() {
        return (StateFragment) mFragmentManager.findFragmentByTag(StateFragment.FRAGMENT_TAG);
    }

    /**
     * NavDestination specific to {@link FragmentNavigator}
     */
    public static class Destination extends NavDestination {
        private Class<? extends Fragment> mFragmentClass;
        private String mFlow;

        /**
         * Construct a new fragment destination to navigate to the given Fragment.
         *
         * @param navigatorProvider The {@link NavController} which this destination
         *                          will be associated with.
         * @param clazz Fragment this destination should create when navigated to
         */
        public Destination(@NonNull NavigatorProvider navigatorProvider,
                Class<? extends Fragment> clazz) {
            super(navigatorProvider.getNavigator(NAME));
            setFragmentClass(clazz);
        }

        Destination(@NonNull FragmentNavigator fragmentNavigator) {
            super(fragmentNavigator);
        }

        @Override
        public void onInflate(Context context, AttributeSet attrs) {
            super.onInflate(context, attrs);
            TypedArray a = context.getResources().obtainAttributes(attrs,
                    R.styleable.FragmentDestination);
            setFragmentClass(((FragmentNavigator) getNavigator())
                    .getFragmentClassByName(a.getString(
                            R.styleable.FragmentDestination_android_name)));
            setFlow(a.getString(R.styleable.FragmentDestination_flow));
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

        public String getFlow() {
            return mFlow;
        }

        public void setFlow(String flowName) {
            mFlow = flowName;
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
