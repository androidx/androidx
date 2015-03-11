/* This file is auto-generated from BrandedFragment.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.app;

import android.support.v4.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.SearchOrbView;
import android.support.v17.leanback.widget.TitleHelper;
import android.support.v17.leanback.widget.TitleView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment support for managing branding on a
 * {@link android.support.v17.leanback.widget.TitleView}.
 * @hide
 */
class BrandedSupportFragment extends Fragment {

    // BUNDLE attribute for title is showing
    private static final String TITLE_SHOW = "titleShow";

    private boolean mShowingTitle = true;
    private String mTitle;
    private Drawable mBadgeDrawable;
    private TitleView mTitleView;
    private SearchOrbView.Colors mSearchAffordanceColors;
    private boolean mSearchAffordanceColorSet;
    private View.OnClickListener mExternalOnSearchClickedListener;
    private TitleHelper mTitleHelper;

    /**
     * Sets the {@link TitleView}.
     */
    void setTitleView(TitleView titleView) {
        mTitleView = titleView;
        if (mTitleView == null) {
            mTitleHelper = null;
        } else {
            mTitleView.setTitle(mTitle);
            mTitleView.setBadgeDrawable(mBadgeDrawable);
            if (mSearchAffordanceColorSet) {
                mTitleView.setSearchAffordanceColors(mSearchAffordanceColors);
            }
            if (mExternalOnSearchClickedListener != null) {
                mTitleView.setOnSearchClickedListener(mExternalOnSearchClickedListener);
            }
            if (getView() instanceof ViewGroup) {
                mTitleHelper = new TitleHelper((ViewGroup) getView(), mTitleView);
            }
        }
    }

    /**
     * Returns the {@link TitleView}.
     */
    TitleView getTitleView() {
        return mTitleView;
    }

    /**
     * Returns the {@link TitleHelper}.
     */
    TitleHelper getTitleHelper() {
        return mTitleHelper;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TITLE_SHOW, mShowingTitle);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            mShowingTitle = savedInstanceState.getBoolean(TITLE_SHOW);
        }
        if (mTitleView != null && view instanceof ViewGroup) {
            mTitleHelper = new TitleHelper((ViewGroup) view, mTitleView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTitleHelper = null;
    }

    /**
     * Shows or hides the {@link android.support.v17.leanback.widget.TitleView}.
     */
    void showTitle(boolean show) {
        // TODO: handle interruptions?
        if (show == mShowingTitle) {
            return;
        }
        mShowingTitle = show;
        if (mTitleHelper != null) {
            mTitleHelper.showTitle(show);
        }
    }

    /**
     * Sets the drawable displayed in the browse fragment title.
     *
     * @param drawable The Drawable to display in the browse fragment title.
     */
    public void setBadgeDrawable(Drawable drawable) {
        if (mBadgeDrawable != drawable) {
            mBadgeDrawable = drawable;
            if (mTitleView != null) {
                mTitleView.setBadgeDrawable(drawable);
            }
        }
    }

    /**
     * Returns the badge drawable used in the fragment title.
     */
    public Drawable getBadgeDrawable() {
        return mBadgeDrawable;
    }

    /**
     * Sets a title for the browse fragment.
     *
     * @param title The title of the browse fragment.
     */
    public void setTitle(String title) {
        mTitle = title;
        if (mTitleView != null) {
            mTitleView.setTitle(title);
        }
    }

    /**
     * Returns the title for the browse fragment.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Sets a click listener for the search affordance.
     *
     * <p>The presence of a listener will change the visibility of the search
     * affordance in the fragment title. When set to non-null, the title will
     * contain an element that a user may click to begin a search.
     *
     * <p>The listener's {@link View.OnClickListener#onClick onClick} method
     * will be invoked when the user clicks on the search element.
     *
     * @param listener The listener to call when the search element is clicked.
     */
    public void setOnSearchClickedListener(View.OnClickListener listener) {
        mExternalOnSearchClickedListener = listener;
        if (mTitleView != null) {
            mTitleView.setOnSearchClickedListener(listener);
        }
    }

    /**
     * Sets the {@link android.support.v17.leanback.widget.SearchOrbView.Colors} used to draw the search affordance.
     */
    public void setSearchAffordanceColors(SearchOrbView.Colors colors) {
        mSearchAffordanceColors = colors;
        mSearchAffordanceColorSet = true;
        if (mTitleView != null) {
            mTitleView.setSearchAffordanceColors(mSearchAffordanceColors);
        }
    }

    /**
     * Returns the {@link android.support.v17.leanback.widget.SearchOrbView.Colors}
     * used to draw the search affordance.
     */
    public SearchOrbView.Colors getSearchAffordanceColors() {
        if (mSearchAffordanceColorSet) {
            return mSearchAffordanceColors;
        }
        if (mTitleView == null) {
            throw new IllegalStateException("Fragment views not yet created");
        }
        return mTitleView.getSearchAffordanceColors();
    }

    /**
     * Sets the color used to draw the search affordance.
     * A default brighter color will be set by the framework.
     *
     * @param color The color to use for the search affordance.
     */
    public void setSearchAffordanceColor(int color) {
        setSearchAffordanceColors(new SearchOrbView.Colors(color));
    }

    /**
     * Returns the color used to draw the search affordance.
     */
    public int getSearchAffordanceColor() {
        return getSearchAffordanceColors().color;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mTitleView != null) {
            mTitleView.setVisibility(mShowingTitle ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void onPause() {
        if (mTitleView != null) {
            mTitleView.enableAnimation(false);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTitleView != null) {
            mTitleView.enableAnimation(true);
        }
    }
}
