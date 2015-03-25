/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v17.preference;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class LeanbackSettingsFragment extends Fragment
        implements PreferenceFragment.OnPreferenceStartFragmentCallback,
        PreferenceFragment.OnPreferenceStartScreenCallback {

    private static final String SETTINGS_FRAGMENT_INNER_TAG =
            "android.support.v17.preference.LeanbackSettingsFragment.INNER_FRAGMENT";

    private boolean mInitialScreen;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.leanback_settings_fragment, container, false);

        // Trap back button presses
        ((LeanbackSettingsRootView) v).setOnBackKeyListener(new RootViewOnKeyListener());

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            final Fragment f = new LeanbackSettingsFragmentInner();
            getChildFragmentManager().beginTransaction()
                    .add(R.id.settings_fragment_container, f, SETTINGS_FRAGMENT_INNER_TAG)
                    .commit();
            getChildFragmentManager().executePendingTransactions();
            mInitialScreen = true;
            onPreferenceStartInitialScreen();
            mInitialScreen = false;
        }
    }

    /**
     * Called to instantiate the initial {@link android.support.v14.preference.PreferenceFragment}
     * to be shown in this fragment. Implementations are expected to call
     * {@link #startPreferenceFragment(android.app.Fragment, java.lang.String)}.
     */
    public abstract void onPreferenceStartInitialScreen();

    /**
     * Displays a preference fragment to the user. This method can also be used to display
     * list-style fragments on top of the stack of preference fragments.
     *
     * @param fragment Fragment instance to be added.
     * @param tag Fragment tag
     */
    public void startPreferenceFragment(@NonNull Fragment fragment, @Nullable String tag) {
        getInnerFragment().startStackedFragment(fragment, tag, !mInitialScreen);
    }

    /**
     * Displays a fragment to the user, temporarily replacing the contents of this fragment.
     *
     * @param fragment Fragment instance to be added.
     * @param tag Fragment tag
     */
    public void startImmersiveFragment(@NonNull Fragment fragment, @Nullable String tag) {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment_container, fragment, tag)
                .addToBackStack(null)
                .commit();
    }

    private LeanbackSettingsFragmentInner getInnerFragment() {
        return (LeanbackSettingsFragmentInner)
                getChildFragmentManager().findFragmentByTag(SETTINGS_FRAGMENT_INNER_TAG);
    }

    private boolean handleBackPress() {
        final LeanbackSettingsFragmentInner inner = getInnerFragment();
        boolean handled = false;
        if (inner != null && inner.isVisible()) {
            handled = inner.handleBackPress();
        }
        return handled || getChildFragmentManager().popBackStackImmediate();
    }

    /**
     * @hide
     */
    public static class LeanbackSettingsFragmentInner extends Fragment {

        private static final String TARGET_FRAGMENT_TAG =
                "android.support.v17.preference.LeanbackSettingsFragment.TARGET";

        @Override
        public @Nullable View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.leanback_settings_fragment_stack,
                    container, false);
        }

        public void startStackedFragment(@NonNull Fragment fragment, @Nullable String tag,
                boolean addToBackstack) {
            fragment.setTargetFragment(findTarget(), 0);
            final FragmentTransaction transaction = getChildFragmentManager().beginTransaction()
                    .add(R.id.settings_preference_stack, fragment, tag);
            if (addToBackstack) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }

        private Fragment findTarget() {
            Fragment target =
                    getChildFragmentManager().findFragmentByTag(TARGET_FRAGMENT_TAG);
            if (target == null) {
                target = new Target();
                getChildFragmentManager().beginTransaction()
                        .add(target, TARGET_FRAGMENT_TAG)
                        .commit();
                getChildFragmentManager().executePendingTransactions();
            }
            return target;
        }

        public boolean handleBackPress() {
            return getChildFragmentManager().popBackStackImmediate();
        }

        // This looks terrible, and it is. We need this because the target fragment needs to be
        // in the same FragmentManager as the fragment targeting it.
        public static class Target extends Fragment
                implements PreferenceFragment.OnPreferenceStartFragmentCallback,
                PreferenceFragment.OnPreferenceStartScreenCallback {

            @Override
            public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
                final PreferenceFragment.OnPreferenceStartFragmentCallback callback =
                        (PreferenceFragment.OnPreferenceStartFragmentCallback)
                        getParentFragment().getParentFragment();
                return callback.onPreferenceStartFragment(caller, pref);
            }

            @Override
            public boolean onPreferenceStartScreen(PreferenceFragment caller,
                    PreferenceScreen pref) {
                final PreferenceFragment.OnPreferenceStartScreenCallback callback =
                        (PreferenceFragment.OnPreferenceStartScreenCallback)
                        getParentFragment().getParentFragment();
                return callback.onPreferenceStartScreen(caller, pref);
            }
        }
    }

    private class RootViewOnKeyListener implements View.OnKeyListener {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return handleBackPress();
            } else {
                return false;
            }
        }
    }
}
