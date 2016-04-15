/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v7.preference;

import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerViewAccessibilityDelegate;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

/**
 * The AccessibilityDelegate used by the RecyclerView that displays Views for Preferences.
 *
 * @hide
 */
public class PreferenceRecyclerViewAccessibilityDelegate
        extends RecyclerViewAccessibilityDelegate {
    final RecyclerView mRecyclerView;
    final AccessibilityDelegateCompat mDefaultItemDelegate = super.getItemDelegate();

    public PreferenceRecyclerViewAccessibilityDelegate(RecyclerView recyclerView) {
        super(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public AccessibilityDelegateCompat getItemDelegate() {
        return mItemDelegate;
    }

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
