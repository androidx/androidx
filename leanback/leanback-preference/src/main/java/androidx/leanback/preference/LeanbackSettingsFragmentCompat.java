/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.leanback.preference;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

/**
 * This fragment provides a container for displaying a {@link LeanbackPreferenceFragmentCompat}
 *
 * <p>The following sample code shows a simple leanback preference fragment that is
 * populated from a resource.  The resource it loads is:</p>
 *
 * {@sample frameworks/support/samples/SupportPreferenceDemos/src/main/res/xml/preferences.xml preferences}
 *
 * <p>The sample implements
 * {@link PreferenceFragmentCompat.OnPreferenceStartFragmentCallback#onPreferenceStartFragment(
 * PreferenceFragmentCompat, Preference)},
 * {@link PreferenceFragmentCompat.OnPreferenceStartScreenCallback#onPreferenceStartScreen(
 * PreferenceFragmentCompat, PreferenceScreen)},
 * and {@link #onPreferenceStartInitialScreen()}:</p>
 *
 * {@sample frameworks/support/samples/SupportPreferenceDemos/src/main/java/com/example/androidx/preference/LeanbackPreferences.java leanback_preferences}
 */
public abstract class LeanbackSettingsFragmentCompat extends Fragment
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
        PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    private static final String PREFERENCE_FRAGMENT_TAG =
            "androidx.leanback.preference.LeanbackSettingsFragment.PREFERENCE_FRAGMENT";

    private final RootViewOnKeyListener mRootViewOnKeyListener = new RootViewOnKeyListener();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.leanback_settings_fragment, container, false);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            onPreferenceStartInitialScreen();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Trap back button presses
        final LeanbackSettingsRootView rootView = (LeanbackSettingsRootView) getView();
        if (rootView != null) {
            rootView.setOnBackKeyListener(mRootViewOnKeyListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        final LeanbackSettingsRootView rootView = (LeanbackSettingsRootView) getView();
        if (rootView != null) {
            rootView.setOnBackKeyListener(null);
        }
    }

    @Override
    public boolean onPreferenceDisplayDialog(@NonNull PreferenceFragmentCompat caller,
            Preference pref) {
        if (caller == null) {
            throw new IllegalArgumentException("Cannot display dialog for preference " + pref
                    + ", Caller must not be null!");
        }
        final Fragment f;
        if (pref instanceof ListPreference) {
            final ListPreference listPreference = (ListPreference) pref;
            f = LeanbackListPreferenceDialogFragmentCompat.newInstanceSingle(
                    listPreference.getKey());
            f.setTargetFragment(caller, 0);
            startPreferenceFragment(f);
        } else if (pref instanceof MultiSelectListPreference) {
            MultiSelectListPreference listPreference = (MultiSelectListPreference) pref;
            f = LeanbackListPreferenceDialogFragmentCompat.newInstanceMulti(
                    listPreference.getKey());
            f.setTargetFragment(caller, 0);
            startPreferenceFragment(f);
        } else if (pref instanceof EditTextPreference) {
            f = LeanbackEditTextPreferenceDialogFragmentCompat.newInstance(pref.getKey());
            f.setTargetFragment(caller, 0);
            startPreferenceFragment(f);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Called to instantiate the initial {@link PreferenceFragment}
     * to be shown in this fragment. Implementations are expected to call
     * {@link #startPreferenceFragment(Fragment)}.
     */
    public abstract void onPreferenceStartInitialScreen();

    /**
     * Displays a preference fragment to the user. This method can also be used to display
     * list-style fragments on top of the stack of preference fragments.
     *
     * @param fragment Fragment instance to be added.
     */
    public void startPreferenceFragment(@NonNull Fragment fragment) {
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        final Fragment prevFragment =
                getChildFragmentManager().findFragmentByTag(PREFERENCE_FRAGMENT_TAG);
        if (prevFragment != null) {
            transaction
                    .addToBackStack(null)
                    .replace(R.id.settings_preference_fragment_container, fragment,
                            PREFERENCE_FRAGMENT_TAG);
        } else {
            transaction
                    .add(R.id.settings_preference_fragment_container, fragment,
                            PREFERENCE_FRAGMENT_TAG);
        }
        transaction.commit();
    }

    /**
     * Displays a fragment to the user, temporarily replacing the contents of this fragment.
     *
     * @param fragment Fragment instance to be added.
     */
    public void startImmersiveFragment(@NonNull Fragment fragment) {
        final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        final Fragment preferenceFragment =
                getChildFragmentManager().findFragmentByTag(PREFERENCE_FRAGMENT_TAG);
        if (preferenceFragment != null && !preferenceFragment.isHidden()) {
            transaction.remove(preferenceFragment);
        }
        transaction
                .add(R.id.settings_dialog_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private class RootViewOnKeyListener implements View.OnKeyListener {
        RootViewOnKeyListener() {
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return getChildFragmentManager().popBackStackImmediate();
            } else {
                return false;
            }
        }
    }
}
