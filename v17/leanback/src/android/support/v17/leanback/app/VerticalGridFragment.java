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
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SearchOrbView;
import android.support.v17.leanback.widget.VerticalGridView;
import android.util.Log;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Leanback fragment for a vertical grid.
 *
 * Renders a vertical grid of objects given a {@link VerticalGridPresenter} and
 * an {@link ObjectAdapter}.
 */
public class VerticalGridFragment extends Fragment {
    private static final String TAG = "VerticalGridFragment";
    private static boolean DEBUG = false;

    private Params mParams;
    private ObjectAdapter mAdapter;
    private VerticalGridPresenter mGridPresenter;
    private VerticalGridPresenter.ViewHolder mGridViewHolder;
    private OnItemSelectedListener mOnItemSelectedListener;
    private OnItemClickedListener mOnItemClickedListener;
    private View.OnClickListener mExternalOnSearchClickedListener;
    private int mSelectedPosition = -1;

    private ImageView mBadgeView;
    private TextView mTitleView;
    private ViewGroup mBrowseTitle;
    private SearchOrbView mSearchOrbView;
    private boolean mShowingTitle = true;

    // transition related
    private static TransitionHelper sTransitionHelper = TransitionHelper.getInstance();
    private Object mTitleTransition;
    private Object mSceneWithTitle;
    private Object mSceneWithoutTitle;

    public static class Params {
        private String mTitle;
        private Drawable mBadgeDrawable;

        /**
         * Sets the badge image.
         */
        public void setBadgeImage(Drawable drawable) {
            mBadgeDrawable = drawable;
        }

        /**
         * Returns the badge image.
         */
        public Drawable getBadgeImage() {
            return mBadgeDrawable;
        }

        /**
         * Sets a title for the browse fragment.
         */
        public void setTitle(String title) {
            mTitle = title;
        }

        /**
         * Returns the title for the browse fragment.
         */
        public String getTitle() {
            return mTitle;
        }
    }

    /**
     * Set fragment parameters.
     */
    public void setParams(Params params) {
        mParams = params;
        setBadgeDrawable(mParams.mBadgeDrawable);
        setTitle(mParams.mTitle);
    }

    /**
     * Returns fragment parameters.
     */
    public Params getParams() {
        return mParams;
    }

    /**
     * Set the grid presenter.
     */
    public void setGridPresenter(VerticalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        mGridPresenter = gridPresenter;
        mGridPresenter.setOnItemSelectedListener(mRowSelectedListener);
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

    final private OnItemSelectedListener mRowSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(Object item, Row row) {
            int position = mGridViewHolder.getGridView().getSelectedPosition();
            if (DEBUG) Log.v(TAG, "row selected position " + position);
            onRowSelected(position);
            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(item, row);
            }
        }
    };

    /**
     * Sets an item selection listener.
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    private void onRowSelected(int position) {
        if (position != mSelectedPosition) {
            if (!mGridViewHolder.getGridView().hasPreviousViewInSameRow(position)) {
                // if has no sibling in front of it,  show title
                if (!mShowingTitle) {
                    sTransitionHelper.runTransition(mSceneWithTitle, mTitleTransition);
                    mShowingTitle = true;
                }
            } else if (mShowingTitle) {
                sTransitionHelper.runTransition(mSceneWithoutTitle, mTitleTransition);
                mShowingTitle = false;
            }
            mSelectedPosition = position;
        }
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
        if (mGridPresenter != null) {
            mGridPresenter.setOnItemClickedListener(mOnItemClickedListener);
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public OnItemClickedListener getOnItemClickedListener() {
        return mOnItemClickedListener;
    }

    /**
     * Sets a click listener for the search affordance.
     *
     * The presence of a listener will change the visibility of the search affordance in the
     * title area. When set to non-null the title area will contain a call to search action.
     *
     * The listener onClick method will be invoked when the user click on the search action.
     *
     * @param listener The listener.
     */
    public void setOnSearchClickedListener(View.OnClickListener listener) {
        mExternalOnSearchClickedListener = listener;
        if (mSearchOrbView != null) {
            mSearchOrbView.setOnOrbClickedListener(listener);
        }
    }

    private void setBadgeDrawable(Drawable drawable) {
        if (mBadgeView == null) {
            return;
        }
        mBadgeView.setImageDrawable(drawable);
        if (drawable != null) {
            mBadgeView.setVisibility(View.VISIBLE);
        } else {
            mBadgeView.setVisibility(View.GONE);
        }
    }

    private void setTitle(String title) {
        if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.lb_vertical_grid_fragment,
                container, false);

        mBrowseTitle = (ViewGroup) root.findViewById(R.id.browse_title_group);
        mBadgeView = (ImageView) mBrowseTitle.findViewById(R.id.browse_badge);
        mTitleView = (TextView) mBrowseTitle.findViewById(R.id.browse_title);
        mSearchOrbView = (SearchOrbView) mBrowseTitle.findViewById(R.id.browse_orb);
        if (mExternalOnSearchClickedListener != null) {
            mSearchOrbView.setOnOrbClickedListener(mExternalOnSearchClickedListener);
        }

        if (mParams != null) {
            setBadgeDrawable(mParams.mBadgeDrawable);
            setTitle(mParams.mTitle);
        }

        mSceneWithTitle = sTransitionHelper.createScene(root, new Runnable() {
            @Override
            public void run() {
                mBrowseTitle.setVisibility(View.VISIBLE);
            }
        });
        mSceneWithoutTitle = sTransitionHelper.createScene(root, new Runnable() {
            @Override
            public void run() {
                mBrowseTitle.setVisibility(View.GONE);
            }
        });
        mTitleTransition = sTransitionHelper.createTransitionSet(false);
        Object fade = sTransitionHelper.createFadeTransition(
                TransitionHelper.FADE_IN | TransitionHelper.FADE_OUT);
        Object changeBounds = sTransitionHelper.createChangeBounds(false);
        sTransitionHelper.addTransition(mTitleTransition, fade);
        sTransitionHelper.addTransition(mTitleTransition, changeBounds);
        sTransitionHelper.excludeChildren(mTitleTransition, R.id.browse_grid_dock, true);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ViewGroup gridDock = (ViewGroup) view.findViewById(R.id.browse_grid_dock);
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock);
        gridDock.addView(mGridViewHolder.view);

        updateAdapter();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGridViewHolder.getGridView().requestFocus();
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
