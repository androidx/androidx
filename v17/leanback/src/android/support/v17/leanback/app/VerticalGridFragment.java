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

import android.support.v17.leanback.R;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.widget.BrowseFrameLayout;
import android.support.v17.leanback.widget.OnChildLaidOutListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.TitleView;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.SearchOrbView;
import android.support.v4.view.ViewCompat;
import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A fragment for creating leanback vertical grids.
 *
 * <p>Renders a vertical grid of objects given a {@link VerticalGridPresenter} and
 * an {@link ObjectAdapter}.
 */
public class VerticalGridFragment extends Fragment {
    private static final String TAG = "VerticalGridFragment";
    private static boolean DEBUG = false;

    private BrowseFrameLayout mBrowseFrame;
    private String mTitle;
    private Drawable mBadgeDrawable;
    private ObjectAdapter mAdapter;
    private VerticalGridPresenter mGridPresenter;
    private VerticalGridPresenter.ViewHolder mGridViewHolder;
    private OnItemSelectedListener mOnItemSelectedListener;
    private OnItemClickedListener mOnItemClickedListener;
    private OnItemViewSelectedListener mOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    private View.OnClickListener mExternalOnSearchClickedListener;
    private int mSelectedPosition = -1;

    private TitleView mTitleView;
    private SearchOrbView.Colors mSearchAffordanceColors;
    private boolean mSearchAffordanceColorSet;
    private boolean mShowingTitle = true;

    // transition related
    private static TransitionHelper sTransitionHelper = TransitionHelper.getInstance();
    private Object mTitleUpTransition;
    private Object mTitleDownTransition;
    private Object mSceneWithTitle;
    private Object mSceneWithoutTitle;

    /**
     * Sets the badge drawable displayed in the title area.
     */
    public void setBadgeDrawable(Drawable drawable) {
        if (drawable != mBadgeDrawable) {
            mBadgeDrawable = drawable;
            if (mTitleView != null) {
                mTitleView.setBadgeDrawable(drawable);
            }
        }
    }

    /**
     * Returns the badge drawable.
     */
    public Drawable getBadgeDrawable() {
        return mBadgeDrawable;
    }

    /**
     * Sets a title for the fragment.
     */
    public void setTitle(String title) {
        mTitle = title;
        if (mTitleView != null) {
            mTitleView.setTitle(mTitle);
        }
    }

    /**
     * Returns the title for the fragment.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(VerticalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        mGridPresenter = gridPresenter;
        mGridPresenter.setOnItemViewSelectedListener(mViewSelectedListener);
        if (mOnItemViewClickedListener != null) {
            mGridPresenter.setOnItemViewClickedListener(mOnItemViewClickedListener);
        }
        if (mOnItemClickedListener != null) {
            mGridPresenter.setOnItemClickedListener(mOnItemClickedListener);
        }
    }

    /**
     * Returns the grid presenter.
     */
    public VerticalGridPresenter getGridPresenter() {
        return mGridPresenter;
    }

    /**
     * Sets the object adapter for the fragment.
     */
    public void setAdapter(ObjectAdapter adapter) {
        mAdapter = adapter;
        updateAdapter();
    }

    /**
     * Returns the object adapter.
     */
    public ObjectAdapter getAdapter() {
        return mAdapter;
    }

    final private OnItemViewSelectedListener mViewSelectedListener =
            new OnItemViewSelectedListener() {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            int position = mGridViewHolder.getGridView().getSelectedPosition();
            if (DEBUG) Log.v(TAG, "grid selected position " + position);
            gridOnItemSelected(position);
            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(item, row);
            }
            if (mOnItemViewSelectedListener != null) {
                mOnItemViewSelectedListener.onItemSelected(itemViewHolder, item,
                        rowViewHolder, row);
            }
        }
    };

    final private OnChildLaidOutListener mChildLaidOutListener =
            new OnChildLaidOutListener() {
        @Override
        public void onChildLaidOut(ViewGroup parent, View view, int position, long id) {
            if (position == 0) {
                showOrHideTitle();
            }
        }
    };

    /**
     * Sets an item selection listener.
     * @deprecated Use {@link #setOnItemViewSelectedListener(OnItemViewSelectedListener)}
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    /**
     * Sets an item selection listener.
     */
    public void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        mOnItemViewSelectedListener = listener;
    }

    private void gridOnItemSelected(int position) {
        if (position != mSelectedPosition) {
            mSelectedPosition = position;
            showOrHideTitle();
        }
    }

    private void showOrHideTitle() {
        if (mGridViewHolder.getGridView().findViewHolderForAdapterPosition(mSelectedPosition)
                == null) {
            return;
        }
        if (!mGridViewHolder.getGridView().hasPreviousViewInSameRow(mSelectedPosition)) {
            // if has no sibling in front of it,  show title
            if (!mShowingTitle) {
                sTransitionHelper.runTransition(mSceneWithTitle, mTitleDownTransition);
                mShowingTitle = true;
            }
        } else if (mShowingTitle) {
            sTransitionHelper.runTransition(mSceneWithoutTitle, mTitleUpTransition);
            mShowingTitle = false;
        }
    }

    /**
     * Sets an item clicked listener.
     * @deprecated Use {@link #setOnItemViewClickedListener(OnItemViewClickedListener)}
     */
    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
        if (mGridPresenter != null) {
            mGridPresenter.setOnItemClickedListener(mOnItemClickedListener);
        }
    }

    /**
     * Returns the item clicked listener.
     * @deprecated Use {@link #getOnItemViewClickedListener()}
     */
    public OnItemClickedListener getOnItemClickedListener() {
        return mOnItemClickedListener;
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        mOnItemViewClickedListener = listener;
        if (mGridPresenter != null) {
            mGridPresenter.setOnItemViewClickedListener(mOnItemViewClickedListener);
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public OnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    /**
     * Sets a click listener for the search affordance.
     *
     * <p>The presence of a listener will change the visibility of the search
     * affordance in the title area. When set to non-null, the title area will
     * contain a call to search action.
     *
     * <p>The listener's onClick method will be invoked when the user clicks on
     * the search action.
     *
     * @param listener The listener to invoke when the search affordance is
     *        clicked, or null to hide the search affordance.
     */
    public void setOnSearchClickedListener(View.OnClickListener listener) {
        mExternalOnSearchClickedListener = listener;
        if (mTitleView != null) {
            mTitleView.setOnSearchClickedListener(listener);
        }
    }

    /**
     * Sets the {@link SearchOrbView.Colors} used to draw the search affordance.
     */
    public void setSearchAffordanceColors(SearchOrbView.Colors colors) {
        mSearchAffordanceColors = colors;
        mSearchAffordanceColorSet = true;
        if (mTitleView != null) {
            mTitleView.setSearchAffordanceColors(mSearchAffordanceColors);
        }
    }

    /**
     * Returns the {@link SearchOrbView.Colors} used to draw the search affordance.
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

    private final BrowseFrameLayout.OnFocusSearchListener mOnFocusSearchListener =
            new BrowseFrameLayout.OnFocusSearchListener() {
        @Override
        public View onFocusSearch(View focused, int direction) {
            if (DEBUG) Log.v(TAG, "onFocusSearch focused " + focused + " + direction " + direction);

            final View searchOrbView = mTitleView.getSearchAffordanceView();
            final boolean isRtl = ViewCompat.getLayoutDirection(focused) ==
                    View.LAYOUT_DIRECTION_RTL;
            final int forward = isRtl ? View.FOCUS_LEFT : View.FOCUS_RIGHT;
            if (focused == searchOrbView && (
                    direction == View.FOCUS_DOWN || direction == forward)) {
                return mGridViewHolder.view;

            } else if (focused != searchOrbView && searchOrbView.getVisibility() == View.VISIBLE
                    && direction == View.FOCUS_UP) {
                return searchOrbView;

            } else {
                return null;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.lb_vertical_grid_fragment,
                container, false);

        mBrowseFrame = (BrowseFrameLayout) root.findViewById(R.id.browse_frame);
        mBrowseFrame.setOnFocusSearchListener(mOnFocusSearchListener);

        mTitleView = (TitleView) root.findViewById(R.id.browse_title_group);
        mTitleView.setBadgeDrawable(mBadgeDrawable);
        mTitleView.setTitle(mTitle);
        if (mSearchAffordanceColorSet) {
            mTitleView.setSearchAffordanceColors(mSearchAffordanceColors);
        }
        if (mExternalOnSearchClickedListener != null) {
            mTitleView.setOnSearchClickedListener(mExternalOnSearchClickedListener);
        }

        mSceneWithTitle = sTransitionHelper.createScene(root, new Runnable() {
            @Override
            public void run() {
                mTitleView.setVisibility(View.VISIBLE);
            }
        });
        mSceneWithoutTitle = sTransitionHelper.createScene(root, new Runnable() {
            @Override
            public void run() {
                mTitleView.setVisibility(View.INVISIBLE);
            }
        });
        Context context = getActivity();
        mTitleUpTransition = sTransitionHelper.loadTransition(context, R.transition.lb_title_out);
        mTitleDownTransition = sTransitionHelper.loadTransition(context, R.transition.lb_title_in);

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ViewGroup gridDock = (ViewGroup) view.findViewById(R.id.browse_grid_dock);
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock);
        gridDock.addView(mGridViewHolder.view);
        mGridViewHolder.getGridView().setOnChildLaidOutListener(mChildLaidOutListener);

        updateAdapter();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGridViewHolder.getGridView().requestFocus();
    }

    @Override
    public void onPause() {
        mTitleView.enableAnimation(false);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleView.enableAnimation(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGridViewHolder = null;
    }

    /**
     * Sets the selected item position.
     */
    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
        if(mGridViewHolder != null && mGridViewHolder.getGridView().getAdapter() != null) {
            mGridViewHolder.getGridView().setSelectedPositionSmooth(position);
        }
    }

    private void updateAdapter() {
        if (mGridViewHolder != null) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter);
            if (mSelectedPosition != -1) {
                mGridViewHolder.getGridView().setSelectedPosition(mSelectedPosition);
            }
        }
    }
}
