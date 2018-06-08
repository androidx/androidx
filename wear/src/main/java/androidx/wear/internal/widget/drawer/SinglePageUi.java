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

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.R;
import androidx.wear.widget.CircledImageView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;

/**
 * Handles view logic for the single page style {@link WearableNavigationDrawerView}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class SinglePageUi implements SinglePagePresenter.Ui {

    @IdRes
    private static final int[] SINGLE_PAGE_BUTTON_IDS =
            new int[]{
                    R.id.ws_nav_drawer_icon_0,
                    R.id.ws_nav_drawer_icon_1,
                    R.id.ws_nav_drawer_icon_2,
                    R.id.ws_nav_drawer_icon_3,
                    R.id.ws_nav_drawer_icon_4,
                    R.id.ws_nav_drawer_icon_5,
                    R.id.ws_nav_drawer_icon_6,
            };

    @LayoutRes
    private static final int[] SINGLE_PAGE_LAYOUT_RES =
            new int[]{
                    0,
                    R.layout.ws_single_page_nav_drawer_1_item,
                    R.layout.ws_single_page_nav_drawer_2_item,
                    R.layout.ws_single_page_nav_drawer_3_item,
                    R.layout.ws_single_page_nav_drawer_4_item,
                    R.layout.ws_single_page_nav_drawer_5_item,
                    R.layout.ws_single_page_nav_drawer_6_item,
                    R.layout.ws_single_page_nav_drawer_7_item,
            };

    private final WearableNavigationDrawerView mDrawer;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCloseDrawerRunnable =
            new Runnable() {
                @Override
                public void run() {
                    mDrawer.getController().closeDrawer();
                }
            };
    private WearableNavigationDrawerPresenter mPresenter;
    private CircledImageView[] mSinglePageImageViews;
    /**
     * Indicates currently selected item. {@code null} when the layout lacks space to display it.
     */
    @Nullable
    private TextView mTextView;

    public SinglePageUi(WearableNavigationDrawerView navigationDrawer) {
        if (navigationDrawer == null) {
            throw new IllegalArgumentException("Received null navigationDrawer.");
        }
        mDrawer = navigationDrawer;
    }

    @Override
    public void setPresenter(WearableNavigationDrawerPresenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void initialize(int count) {
        if (count < 0 || count >= SINGLE_PAGE_LAYOUT_RES.length
                || SINGLE_PAGE_LAYOUT_RES[count] == 0) {
            mDrawer.setDrawerContent(null);
            return;
        }

        @LayoutRes int layoutRes = SINGLE_PAGE_LAYOUT_RES[count];
        LayoutInflater inflater = LayoutInflater.from(mDrawer.getContext());
        View content = inflater.inflate(layoutRes, mDrawer, false /* attachToRoot */);
        final View peek =
                inflater.inflate(
                        R.layout.ws_single_page_nav_drawer_peek_view, mDrawer,
                        false /* attachToRoot */);

        mTextView = content.findViewById(R.id.ws_nav_drawer_text);
        mSinglePageImageViews = new CircledImageView[count];
        for (int i = 0; i < count; i++) {
            mSinglePageImageViews[i] = content.findViewById(SINGLE_PAGE_BUTTON_IDS[i]);
            mSinglePageImageViews[i].setOnClickListener(new OnSelectedClickHandler(i, mPresenter));
            mSinglePageImageViews[i].setCircleHidden(true);
        }

        mDrawer.setDrawerContent(content);
        mDrawer.setPeekContent(peek);
    }

    @Override
    public void setIcon(int index, Drawable drawable, CharSequence contentDescription) {
        mSinglePageImageViews[index].setImageDrawable(drawable);
        mSinglePageImageViews[index].setContentDescription(contentDescription);
    }

    @Override
    public void setText(CharSequence itemText, boolean showToastIfNoTextView) {
        if (mTextView != null) {
            mTextView.setText(itemText);
        } else if (showToastIfNoTextView) {
            Toast toast = Toast.makeText(mDrawer.getContext(), itemText, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0 /* xOffset */, 0 /* yOffset */);
            toast.show();
        }
    }

    @Override
    public void selectItem(int index) {
        mSinglePageImageViews[index].setCircleHidden(false);
    }

    @Override
    public void deselectItem(int index) {
        mSinglePageImageViews[index].setCircleHidden(true);
    }

    @Override
    public void closeDrawerDelayed(long delayMs) {
        mMainThreadHandler.removeCallbacks(mCloseDrawerRunnable);
        mMainThreadHandler.postDelayed(mCloseDrawerRunnable, delayMs);
    }

    @Override
    public void peekDrawer() {
        mDrawer.getController().peekDrawer();
    }

    /**
     * Notifies the {@code presenter} that the item at the given {@code index} has been selected.
     */
    private static class OnSelectedClickHandler implements View.OnClickListener {

        private final int mIndex;
        private final WearableNavigationDrawerPresenter mPresenter;

        private OnSelectedClickHandler(int index, WearableNavigationDrawerPresenter presenter) {
            mIndex = index;
            mPresenter = presenter;
        }

        @Override
        public void onClick(View v) {
            mPresenter.onSelected(mIndex);
        }
    }
}
