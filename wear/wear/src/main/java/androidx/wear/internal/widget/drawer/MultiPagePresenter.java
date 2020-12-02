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

package androidx.wear.internal.widget.drawer;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView.WearableNavigationDrawerAdapter;

/**
 * Provides a {@link WearableNavigationDrawerPresenter} implementation that is designed for the
 * multi-page navigation drawer.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class MultiPagePresenter extends WearableNavigationDrawerPresenter {

    private final Ui mUi;
    private final WearableNavigationDrawerView mDrawer;
    private final boolean mIsAccessibilityEnabled;
    @Nullable private WearableNavigationDrawerAdapter mAdapter;

    /**
     * Controls the user interface of a multi-page {@link WearableNavigationDrawerView}.
     */
    public interface Ui {

        /**
         * Initializes the {@code Ui}.
         */
        void initialize(WearableNavigationDrawerView drawer,
                WearableNavigationDrawerPresenter presenter);

        /**
         * Should notify the {@code NavigationPagerAdapter} that the underlying data has changed.
         */
        void notifyNavigationPagerAdapterDataChanged();

        /**
         * Should notify the Page Indicator that the underlying data has changed.
         */
        void notifyPageIndicatorDataChanged();

        /**
         * Associates the given {@code adapter} with this {@link Ui}.
         */
        void setNavigationPagerAdapter(WearableNavigationDrawerAdapter adapter);

        /**
         * Sets which item is selected and optionally smooth scrolls to it.
         */
        void setNavigationPagerSelectedItem(int index, boolean smoothScrollTo);
    }

    public MultiPagePresenter(WearableNavigationDrawerView drawer, Ui ui,
            boolean isAccessibilityEnabled) {
        if (drawer == null) {
            throw new IllegalArgumentException("Received null drawer.");
        }
        if (ui == null) {
            throw new IllegalArgumentException("Received null ui.");
        }
        mDrawer = drawer;
        mUi = ui;
        mUi.initialize(drawer, this);
        mIsAccessibilityEnabled = isAccessibilityEnabled;
    }

    @Override
    public void onDataSetChanged() {
        mUi.notifyNavigationPagerAdapterDataChanged();
        mUi.notifyPageIndicatorDataChanged();
    }

    @Override
    public void onNewAdapter(WearableNavigationDrawerAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("Received null adapter.");
        }
        mAdapter = adapter;
        mAdapter.setPresenter(this);
        mUi.setNavigationPagerAdapter(adapter);
    }

    @Override
    public void onSelected(int index) {
        notifyItemSelectedListeners(index);
    }

    @Override
    public void onSetCurrentItemRequested(int index, boolean smoothScrollTo) {
        mUi.setNavigationPagerSelectedItem(index, smoothScrollTo);
    }

    @Override
    public boolean onDrawerTapped() {
        if (mDrawer.isOpened()) {
            if (mIsAccessibilityEnabled) {
                // When accessibility gestures are enabled, the user can't access a closed nav
                // drawer, so peek it instead.
                mDrawer.getController().peekDrawer();
            } else {
                mDrawer.getController().closeDrawer();
            }
            return true;
        }
        return false;
    }
}
