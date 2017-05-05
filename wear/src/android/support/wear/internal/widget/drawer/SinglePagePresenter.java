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

package android.support.wear.internal.widget.drawer;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView.WearableNavigationDrawerAdapter;

/**
 * Provides a {@link WearableNavigationDrawerPresenter} implementation that is designed for the
 * single page navigation drawer.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class SinglePagePresenter extends WearableNavigationDrawerPresenter {

    private static final long DRAWER_CLOSE_DELAY_MS = 500;

    private final Ui mUi;
    private final boolean mIsAccessibilityEnabled;
    @Nullable
    private WearableNavigationDrawerAdapter mAdapter;
    private int mCount = 0;
    private int mSelected = 0;

    /**
     * Controls the user interface of a single-page {@link WearableNavigationDrawerView}.
     */
    public interface Ui {

        /**
         * Associates a {@link WearableNavigationDrawerPresenter} with this {@link Ui}.
         */
        void setPresenter(WearableNavigationDrawerPresenter presenter);

        /**
         * Initializes the {@link Ui} with {@code count} items.
         */
        void initialize(int count);

        /**
         * Sets the item's {@link Drawable} icon and its {@code contentDescription}.
         */
        void setIcon(int index, Drawable drawable, CharSequence contentDescription);

        /**
         * Displays {@code itemText} in a {@link android.widget.TextView} used to indicate which
         * item is selected. When the {@link Ui} doesn't have space, it should show a {@link
         * android.widget.Toast} if {@code showToastIfNoTextView} is {@code true}.
         */
        void setText(CharSequence itemText, boolean showToastIfNoTextView);

        /**
         * Indicates that the item at {@code index} has been selected.
         */
        void selectItem(int index);

        /**
         * Removes the indication that the item at {@code index} has been selected.
         */
        void deselectItem(int index);

        /**
         * Closes the drawer after the given delay.
         */
        void closeDrawerDelayed(long delayMs);

        /**
         * Peeks the {@link WearableNavigationDrawerView}.
         */
        void peekDrawer();
    }

    public SinglePagePresenter(Ui ui, boolean isAccessibilityEnabled) {
        if (ui == null) {
            throw new IllegalArgumentException("Received null ui.");
        }

        mIsAccessibilityEnabled = isAccessibilityEnabled;
        mUi = ui;
        mUi.setPresenter(this);
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        if (mAdapter == null) {
            return;
        }
        int count = mAdapter.getCount();
        if (mCount != count) {
            mCount = count;
            mSelected = Math.min(mSelected, count - 1);
            mUi.initialize(count);
        }
        for (int i = 0; i < count; i++) {
            mUi.setIcon(i, mAdapter.getItemDrawable(i), mAdapter.getItemText(i));
        }

        mUi.setText(mAdapter.getItemText(mSelected), false /* showToastIfNoTextView */);
        mUi.selectItem(mSelected);
    }

    @Override
    public void onNewAdapter(WearableNavigationDrawerAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("Received null adapter.");
        }
        mAdapter = adapter;
        mAdapter.setPresenter(this);
        onDataSetChanged();
    }

    @Override
    public void onSelected(int index) {
        mUi.deselectItem(mSelected);
        mUi.selectItem(index);
        mSelected = index;
        if (mIsAccessibilityEnabled) {
            // When accessibility gestures are enabled, the user can't access a closed nav drawer,
            // so peek it instead.
            mUi.peekDrawer();
        } else {
            mUi.closeDrawerDelayed(DRAWER_CLOSE_DELAY_MS);
        }

        if (mAdapter != null) {
            mUi.setText(mAdapter.getItemText(index), true /* showToastIfNoTextView */);
        }
        notifyItemSelectedListeners(index);
    }

    @Override
    public void onSetCurrentItemRequested(int index, boolean smoothScrollTo) {
        mUi.deselectItem(mSelected);
        mUi.selectItem(index);
        mSelected = index;
        if (mAdapter != null) {
            mUi.setText(mAdapter.getItemText(index), false /* showToastIfNoTextView */);
        }
        notifyItemSelectedListeners(index);
    }

    @Override
    public boolean onDrawerTapped() {
        // Do nothing. Use onSelected as our tap trigger so that we get which index was tapped on.
        return false;
    }
}
