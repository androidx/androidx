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

package androidx.wear.widget;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.wear.widget.SwipeDismissFrameLayout.Callback;

/**
 * {@link PreferenceFragment} that supports swipe-to-dismiss.
 *
 * <p>Unlike a regular PreferenceFragment, this Fragment has a solid color background using the
 * background color from the theme. This allows the fragment to be layered on top of other
 * fragments so that the previous layer is seen when this fragment is swiped away.
 */
public class SwipeDismissPreferenceFragment extends PreferenceFragment {

    private SwipeDismissFrameLayout mSwipeLayout;

    private final Callback mCallback =
            new Callback() {
                @Override
                public void onSwipeStarted(SwipeDismissFrameLayout layout) {
                    SwipeDismissPreferenceFragment.this.onSwipeStart();
                }

                @Override
                public void onSwipeCanceled(SwipeDismissFrameLayout layout) {
                    SwipeDismissPreferenceFragment.this.onSwipeCancelled();
                }

                @Override
                public void onDismissed(SwipeDismissFrameLayout layout) {
                    SwipeDismissPreferenceFragment.this.onDismiss();
                }
            };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mSwipeLayout = new SwipeDismissFrameLayout(getActivity());
        mSwipeLayout.addCallback(mCallback);

        View contents = super.onCreateView(inflater, mSwipeLayout, savedInstanceState);

        mSwipeLayout.setBackgroundColor(getBackgroundColor());
        mSwipeLayout.addView(contents);

        return mSwipeLayout;
    }

    /** Called when the fragment is dismissed with a swipe. */
    public void onDismiss() {
    }

    /** Called when a swipe-to-dismiss gesture is started. */
    public void onSwipeStart() {
    }

    /** Called when a swipe-to-dismiss gesture is cancelled. */
    public void onSwipeCancelled() {
    }

    /**
     * Sets whether or not the preferences list can be focused. If {@code focusable} is false, any
     * existing focus will be cleared.
     */
    public void setFocusable(boolean focusable) {
        if (focusable) {
            mSwipeLayout.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            mSwipeLayout.setFocusable(true);
        } else {
            // Prevent any child views from receiving focus.
            mSwipeLayout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

            mSwipeLayout.setFocusable(false);
            mSwipeLayout.clearFocus();
        }
    }

    private int getBackgroundColor() {
        TypedValue value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
        return value.data;
    }
}
