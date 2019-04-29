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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceRecyclerViewAccessibilityDelegate;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This fragment provides a preference fragment with leanback-style behavior, suitable for
 * embedding into broader UI elements.
 */
public abstract class BaseLeanbackPreferenceFragmentCompat extends PreferenceFragmentCompat {

    private Context mThemedContext;

    @Nullable
    @Override
    public Context getContext() {
        if (mThemedContext == null && getActivity() != null) {
            final TypedValue tv = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.preferenceTheme, tv, true);
            int theme = tv.resourceId;
            if (theme == 0) {
                // Fallback to default theme.
                theme = R.style.PreferenceThemeOverlayLeanback;
            }
            // aosp/821989 has forced PreferenceFragment to use the theme of activity and only
            // override theme attribute value when it's not defined in activity theme.
            // However, a side panel preference fragment can use different values than main content.
            // So a ContextThemeWrapper is required, overrides getContext() before
            // super.onCreate() call to use the ContextThemeWrapper in creating PreferenceManager
            // and onCreateView().
            // super.onCreate() will apply() the theme to activity in non-force way, which shouldn't
            // affect activity as the theme attributes of PreferenceThemeOverlayLeanback is already
            // in the activity's theme (in framework)
            mThemedContext = new ContextThemeWrapper(super.getContext(), theme);
        }
        return mThemedContext;
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        VerticalGridView verticalGridView = (VerticalGridView) inflater
                .inflate(R.layout.leanback_preferences_list, parent, false);
        verticalGridView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_BOTH_EDGE);
        verticalGridView.setFocusScrollStrategy(VerticalGridView.FOCUS_SCROLL_ALIGNED);
        verticalGridView.setAccessibilityDelegateCompat(
                new PreferenceRecyclerViewAccessibilityDelegate(verticalGridView));
        return verticalGridView;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public Fragment getCallbackFragment() {
        return getParentFragment();
    }
}
