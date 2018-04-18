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

package androidx.leanback.preference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.RestrictTo;
import androidx.leanback.widget.VerticalGridView;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceRecyclerViewAccessibilityDelegate;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This fragment provides a preference fragment with leanback-style behavior, suitable for
 * embedding into broader UI elements.
 */
public abstract class BaseLeanbackPreferenceFragment extends PreferenceFragment {

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
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public Fragment getCallbackFragment() {
        return getParentFragment();
    }
}
