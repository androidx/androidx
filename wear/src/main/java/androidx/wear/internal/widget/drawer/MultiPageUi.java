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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.wear.R;
import androidx.wear.widget.drawer.PageIndicatorView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView.WearableNavigationDrawerAdapter;

/**
 * Handles view logic for the multi page style {@link WearableNavigationDrawerView}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class MultiPageUi implements MultiPagePresenter.Ui {

    private static final String TAG = "MultiPageUi";

    private WearableNavigationDrawerPresenter mPresenter;

    @Nullable private ViewPager mNavigationPager;
    @Nullable private PageIndicatorView mPageIndicatorView;

    @Override
    public void initialize(
            WearableNavigationDrawerView drawer, WearableNavigationDrawerPresenter presenter) {
        if (drawer == null) {
            throw new IllegalArgumentException("Received null drawer.");
        }
        if (presenter == null) {
            throw new IllegalArgumentException("Received null presenter.");
        }
        mPresenter = presenter;

        LayoutInflater inflater = LayoutInflater.from(drawer.getContext());
        final View content = inflater.inflate(R.layout.ws_navigation_drawer_view, drawer,
                false /* attachToRoot */);

        mNavigationPager = content.findViewById(R.id.ws_navigation_drawer_view_pager);
        mPageIndicatorView = content.findViewById(R.id.ws_navigation_drawer_page_indicator);

        drawer.setDrawerContent(content);
    }

    @Override
    public void setNavigationPagerAdapter(final WearableNavigationDrawerAdapter adapter) {
        if (mNavigationPager == null || mPageIndicatorView == null) {
            Log.w(TAG, "setNavigationPagerAdapter was called before initialize.");
            return;
        }

        NavigationPagerAdapter navigationPagerAdapter = new NavigationPagerAdapter(adapter);
        mNavigationPager.setAdapter(navigationPagerAdapter);

        // Clear out the old page listeners and add a new one for this adapter.
        mNavigationPager.clearOnPageChangeListeners();
        mNavigationPager.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        mPresenter.onSelected(position);
                    }
                });
        // PageIndicatorView adds itself as a page change listener here, so this must come after
        // they are cleared.
        mPageIndicatorView.setPager(mNavigationPager);
    }

    @Override
    public void notifyPageIndicatorDataChanged() {
        if (mPageIndicatorView != null) {
            mPageIndicatorView.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyNavigationPagerAdapterDataChanged() {
        if (mNavigationPager != null) {
            PagerAdapter adapter = mNavigationPager.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void setNavigationPagerSelectedItem(int index, boolean smoothScrollTo) {
        if (mNavigationPager != null) {
            mNavigationPager.setCurrentItem(index, smoothScrollTo);
        }
    }

    /**
     * Adapter for {@link ViewPager} used in the multi-page UI.
     */
    private static final class NavigationPagerAdapter extends PagerAdapter {

        private final WearableNavigationDrawerAdapter mAdapter;

        NavigationPagerAdapter(WearableNavigationDrawerAdapter adapter) {
            mAdapter = adapter;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            // Do not attach to root in the inflate method. The view needs to returned at the end
            // of this method. Attaching to root will cause view to point to container instead.
            final View view =
                    LayoutInflater.from(container.getContext())
                            .inflate(R.layout.ws_navigation_drawer_item_view, container, false);
            container.addView(view);
            final ImageView iconView =
                    view.findViewById(R.id.ws_navigation_drawer_item_icon);
            final TextView textView =
                    view.findViewById(R.id.ws_navigation_drawer_item_text);
            iconView.setImageDrawable(mAdapter.getItemDrawable(position));
            textView.setText(mAdapter.getItemText(position));
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,
                @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return mAdapter.getCount();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }
}
