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

package androidx.preference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;

/**
 * The accessibility delegate used by the {@link RecyclerView} that displays views for
 * {@link Preference}s.
 *
 * Used by Leanback.
 *
 * @hide
 * @deprecated This class is used to set AccessibilityNodeInfo for {@link Preference}. Preference
 * class deprecated the API onInitializeAccessibilityNodeInfo.
 */
@Deprecated
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class PreferenceRecyclerViewAccessibilityDelegate
        extends RecyclerViewAccessibilityDelegate {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final RecyclerView mRecyclerView;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final AccessibilityDelegateCompat mDefaultItemDelegate = super.getItemDelegate();

    public PreferenceRecyclerViewAccessibilityDelegate(@NonNull RecyclerView recyclerView) {
        super(recyclerView);
        mRecyclerView = recyclerView;
    }

    @NonNull
    @Override
    public AccessibilityDelegateCompat getItemDelegate() {
        return mItemDelegate;
    }
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final AccessibilityDelegateCompat mItemDelegate = new AccessibilityDelegateCompat() {
        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            mDefaultItemDelegate.onInitializeAccessibilityNodeInfo(host, info);
            int position = mRecyclerView.getChildAdapterPosition(host);

            RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            if (!(adapter instanceof PreferenceGroupAdapter)) {
                return;
            }

            PreferenceGroupAdapter preferenceGroupAdapter = (PreferenceGroupAdapter) adapter;
            Preference preference = preferenceGroupAdapter.getItem(position);
            if (preference == null) {
                return;
            }

            preference.onInitializeAccessibilityNodeInfo(info);
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            // Must forward actions since the default delegate will handle actions.
            return mDefaultItemDelegate.performAccessibilityAction(host, action, args);
        }
    };
}
