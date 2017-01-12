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

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * This fragment provides a fully decorated leanback-style preference fragment, including a
 * list background and header.
 *
 * <p>The following sample code shows a simple leanback preference fragment that is
 * populated from a resource.  The resource it loads is:</p>
 *
 * {@sample frameworks/support/samples/SupportPreferenceDemos/res/xml/preferences.xml preferences}
 *
 * <p>The fragment needs only to implement {@link #onCreatePreferences(Bundle, String)} to populate
 * the list of preference objects:</p>
 *
 * {@sample frameworks/support/samples/SupportPreferenceDemos/src/com/example/android/supportpreference/FragmentSupportPreferencesLeanback.java
 *      support_fragment_leanback}
 */
public abstract class LeanbackPreferenceFragment extends BaseLeanbackPreferenceFragment {

    public LeanbackPreferenceFragment() {
        if (Build.VERSION.SDK_INT >= 21) {
            LeanbackPreferenceFragmentTransitionHelperApi21.addTransitions(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.leanback_preference_fragment, container, false);
        final ViewGroup innerContainer = (ViewGroup) view.findViewById(R.id.main_frame);
        final View innerView = super.onCreateView(inflater, innerContainer, savedInstanceState);
        if (innerView != null) {
            innerContainer.addView(innerView);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(getPreferenceScreen().getTitle());
    }

    /**
     * Set the title to be shown above the preference list
     * @param title Title text to be shown
     */
    public void setTitle(CharSequence title) {
        final View view = getView();
        final TextView decorTitle = view == null
                ? null : (TextView) view.findViewById(R.id.decor_title);
        if (decorTitle != null) {
            decorTitle.setText(title);
        }
    }
}
