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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * This fragment provides a fully decorated leanback-style preference fragment, including a
 * list background and header.
 *
 * <p>The following sample code shows a simple leanback preference fragment that is
 * populated from a resource.  The resource it loads is:</p>
 *
 * {@sample frameworks/support/samples/SupportPreferenceDemos/src/main/res/xml/preferences.xml preferences}
 *
 * <p>The fragment needs only to implement {@link #onCreatePreferences(Bundle, String)} to populate
 * the list of preference objects:</p>
 *
 * {@sample frameworks/support/samples/SupportPreferenceDemos/src/main/java/com/example/androidx/preference/LeanbackPreferences.java leanback_preferences}
 */
public abstract class LeanbackPreferenceFragmentCompat extends
        BaseLeanbackPreferenceFragmentCompat {

    public LeanbackPreferenceFragmentCompat() {
        LeanbackPreferenceFragmentTransitionHelperApi21.addTransitions(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View innerView = super.onCreateView(inflater, container, savedInstanceState);
        // parent class would create a themed context based the preferenceTheme attr.
        LayoutInflater themedInflater = LayoutInflater.from(innerView.getContext());
        final View view = themedInflater.inflate(R.layout.leanback_preference_fragment, container,
                false);
        final ViewGroup innerContainer = (ViewGroup) view.findViewById(R.id.main_frame);
        if (innerView != null) {
            innerContainer.addView(innerView);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(getPreferenceScreen().getTitle());
    }

    /**
     * Set the title to be shown above the preference list
     *
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
