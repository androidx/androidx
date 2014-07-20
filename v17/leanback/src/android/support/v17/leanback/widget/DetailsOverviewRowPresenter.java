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
package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.support.v17.leanback.R;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Collection;

/**
 * A DetailsOverviewRowPresenter renders a {@link DetailsOverviewRow} to display an
 * overview of an item. Typically this row will be the first row in a fragment
 * such as the {@link android.support.v17.leanback.app.DetailsFragment
 * DetailsFragment}.
 *
 * <p>The detailed description is rendered using a {@link Presenter}.
 */
public class DetailsOverviewRowPresenter extends RowPresenter {

    private static final String TAG = "DetailsOverviewRowPresenter";
    private static final boolean DEBUG = false;

    private static final int MORE_ACTIONS_FADE_MS = 100;

    /**
     * A ViewHolder for the DetailsOverviewRow.
     */
    public static class ViewHolder extends RowPresenter.ViewHolder {
        final ViewGroup mOverviewView;
        final ImageView mImageView;
        final FrameLayout mDetailsDescriptionFrame;
        final HorizontalGridView mActionsRow;
        Presenter.ViewHolder mDetailsDescriptionViewHolder;
        int mNumItems;
        boolean mShowMoreRight;
        boolean mShowMoreLeft;

        void bind(ItemBridgeAdapter bridgeAdapter) {
            mNumItems = bridgeAdapter.getItemCount();
            bridgeAdapter.setAdapterListener(mAdapterListener);

            mShowMoreRight = false;
            mShowMoreLeft = true;
            showMoreLeft(false);
        }

        final View.OnLayoutChangeListener mLayoutChangeListener =
                new View.OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View v, int left, int top, int right,
                    int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (DEBUG) Log.v(TAG, "onLayoutChange " + v);
                checkFirstAndLastPosition(false);
            }
        };

        final ItemBridgeAdapter.AdapterListener mAdapterListener =
                new ItemBridgeAdapter.AdapterListener() {

            @Override
            public void onAttachedToWindow(ItemBridgeAdapter.ViewHolder viewHolder) {
                // Remove first to ensure we don't add ourselves more than once.
                viewHolder.itemView.removeOnLayoutChangeListener(mLayoutChangeListener);
                viewHolder.itemView.addOnLayoutChangeListener(mLayoutChangeListener);
            }
            @Override
            public void onDetachedFromWindow(ItemBridgeAdapter.ViewHolder viewHolder) {
                viewHolder.itemView.removeOnLayoutChangeListener(mLayoutChangeListener);
                checkFirstAndLastPosition(false);
            }
        };

        final RecyclerView.OnScrollListener mScrollListener =
                new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                checkFirstAndLastPosition(true);
            }
        };

        private int getViewCenter(View view) {
            return (view.getRight() - view.getLeft()) / 2;
        }

        private void checkFirstAndLastPosition(boolean fromScroll) {
            RecyclerView.ViewHolder viewHolder;

            viewHolder = mActionsRow.findViewHolderForPosition(mNumItems - 1);
            boolean showRight = (viewHolder == null ||
                    viewHolder.itemView.getRight() > mActionsRow.getWidth());

            viewHolder = mActionsRow.findViewHolderForPosition(0);
            boolean showLeft = (viewHolder == null || viewHolder.itemView.getLeft() < 0);

            if (DEBUG) Log.v(TAG, "checkFirstAndLast fromScroll " + fromScroll +
                    " showRight " + showRight + " showLeft " + showLeft);

            showMoreRight(showRight);
            showMoreLeft(showLeft);
        }

        private void showMoreLeft(boolean show) {
            if (show != mShowMoreLeft) {
                mActionsRow.setFadingLeftEdge(show);
                mShowMoreLeft = show;
            }
        }

        private void showMoreRight(boolean show) {
            if (show != mShowMoreRight) {
                mActionsRow.setFadingRightEdge(show);
                mShowMoreRight = show;
            }
        }

        /**
         * Constructor for the ViewHolder.
         *
         * @param rootView The root View that this view holder will be attached
         *        to.
         */
        public ViewHolder(View rootView) {
            super(rootView);
            mOverviewView = (ViewGroup) rootView.findViewById(R.id.details_overview);
            mImageView = (ImageView) rootView.findViewById(R.id.details_overview_image);
            mDetailsDescriptionFrame =
                    (FrameLayout) rootView.findViewById(R.id.details_overview_description);
            mActionsRow =
                    (HorizontalGridView) rootView.findViewById(R.id.details_overview_actions);
            mActionsRow.setOnScrollListener(mScrollListener);

            final int fadeLength = rootView.getResources().getDimensionPixelSize(
                    R.dimen.lb_details_overview_actions_fade_size);
            mActionsRow.setFadingRightEdgeLength(fadeLength);
            mActionsRow.setFadingLeftEdgeLength(fadeLength);
        }
    }

    private final Presenter mDetailsPresenter;
    private final ActionPresenterSelector mActionPresenterSelector;
    private final ItemBridgeAdapter mActionBridgeAdapter;
    private int mBackgroundColor = Color.TRANSPARENT;
    private boolean mBackgroundColorSet;
    private boolean mIsStyleLarge = true;

    /**
     * Constructor for a DetailsOverviewRowPresenter.
     *
     * @param detailsPresenter The {@link Presenter} used to render the detailed
     *        description of the row.
     */
    public DetailsOverviewRowPresenter(Presenter detailsPresenter) {
        setHeaderPresenter(null);
        setSelectEffectEnabled(false);
        mDetailsPresenter = detailsPresenter;
        mActionPresenterSelector = new ActionPresenterSelector();
        mActionBridgeAdapter = new ItemBridgeAdapter();
    }

    /**
     * Sets the listener for Action click events.
     */
    public void setOnActionClickedListener(OnActionClickedListener listener) {
        mActionPresenterSelector.setOnActionClickedListener(listener);
    }

    /**
     * Gets the listener for Action click events.
     */
    public OnActionClickedListener getOnActionClickedListener() {
        return mActionPresenterSelector.getOnActionClickedListener();
    }

    /**
     * Sets the background color.  If not set, a default from the theme will be used.
     */
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundColorSet = true;
    }

    /**
     * Returns the background color.  If no background color was set, transparent
     * is returned.
     */
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Sets the layout style to be large or small. This affects the height of
     * the overview, including the text description. The default is large.
     */
    public void setStyleLarge(boolean large) {
        mIsStyleLarge = large;
    }

    /**
     * Returns true if the layout style is large.
     */
    public boolean isStyleLarge() {
        return mIsStyleLarge;
    }

    /**
     * Get overview view with background color.
     */
    public ViewGroup getOverviewView(ViewHolder holder) {
        return holder.mOverviewView;
    }

    private int getDefaultBackgroundColor(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.defaultBrandColor, outValue, true);
        return context.getResources().getColor(outValue.resourceId);
    }

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.lb_details_overview, parent, false);
        ViewHolder vh = new ViewHolder(v);

        vh.mDetailsDescriptionViewHolder =
            mDetailsPresenter.onCreateViewHolder(vh.mDetailsDescriptionFrame);
        vh.mDetailsDescriptionFrame.addView(vh.mDetailsDescriptionViewHolder.view);

        initDetailsOverview(vh);

        return vh;
    }

    private int getCardHeight(Context context) {
        int resId = mIsStyleLarge ? R.dimen.lb_details_overview_height_large :
            R.dimen.lb_details_overview_height_small;
        return context.getResources().getDimensionPixelSize(resId);
    }

    private void initDetailsOverview(ViewHolder vh) {
        View overview = vh.view.findViewById(R.id.details_overview);
        ViewGroup.LayoutParams lp = overview.getLayoutParams();
        lp.height = getCardHeight(overview.getContext());
        overview.setLayoutParams(lp);

        overview.setBackgroundColor(mBackgroundColorSet ?
                mBackgroundColor : getDefaultBackgroundColor(overview.getContext()));
        ShadowHelper.getInstance().setZ(overview, 0f);
    }

    private static int getNonNegativeWidth(Drawable drawable) {
        final int width = (drawable == null) ? 0 : drawable.getIntrinsicWidth();
        return (width > 0 ? width : 0);
    }

    private static int getNonNegativeHeight(Drawable drawable) {
        final int height = (drawable == null) ? 0 : drawable.getIntrinsicHeight();
        return (height > 0 ? height : 0);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        DetailsOverviewRow row = (DetailsOverviewRow) item;
        ViewHolder vh = (ViewHolder) holder;

        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) vh.mImageView.getLayoutParams();
        final int cardHeight = getCardHeight(vh.mImageView.getContext());
        final int verticalMargin = vh.mImageView.getResources().getDimensionPixelSize(
                R.dimen.lb_details_overview_image_margin_vertical);
        final int horizontalMargin = vh.mImageView.getResources().getDimensionPixelSize(
                R.dimen.lb_details_overview_image_margin_horizontal);
        final int drawableWidth = getNonNegativeWidth(row.getImageDrawable());
        final int drawableHeight = getNonNegativeHeight(row.getImageDrawable());

        boolean scaleImage = row.isImageScaleUpAllowed();
        boolean useMargin = false;

        if (row.getImageDrawable() != null) {
            boolean landscape = false;

            // If large style and landscape image we always use margin.
            if (drawableWidth > drawableHeight) {
                landscape = true;
                if (mIsStyleLarge) {
                    useMargin = true;
                }
            }
            // If long dimension bigger than the card height we scale down.
            if ((landscape && drawableWidth > cardHeight) ||
                    (!landscape && drawableHeight > cardHeight)) {
                scaleImage = true;
            }
            // If we're not scaling to fit the card height then we always use margin.
            if (!scaleImage) {
                useMargin = true;
            }
            // If using margin than may need to scale down.
            if (useMargin && !scaleImage) {
                if (landscape && drawableWidth > cardHeight - horizontalMargin) {
                    scaleImage = true;
                } else if (!landscape && drawableHeight > cardHeight - 2 * verticalMargin) {
                    scaleImage = true;
                }
            }
        }

        if (useMargin) {
            layoutParams.leftMargin = horizontalMargin;
            layoutParams.topMargin = layoutParams.bottomMargin = verticalMargin;
        } else {
            layoutParams.leftMargin = layoutParams.topMargin = layoutParams.bottomMargin = 0;
        }
        if (scaleImage) {
            vh.mImageView.setScaleType(ImageView.ScaleType.FIT_START);
            vh.mImageView.setAdjustViewBounds(true);
            vh.mImageView.setMaxWidth(cardHeight);
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            vh.mImageView.setScaleType(ImageView.ScaleType.CENTER);
            vh.mImageView.setAdjustViewBounds(false);
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            // Limit width to the card height
            layoutParams.width = Math.min(cardHeight, drawableWidth);
        }
        vh.mImageView.setLayoutParams(layoutParams);
        vh.mImageView.setImageDrawable(row.getImageDrawable());

        mDetailsPresenter.onBindViewHolder(vh.mDetailsDescriptionViewHolder, row.getItem());

        mActionBridgeAdapter.clear();
        ArrayObjectAdapter aoa = new ArrayObjectAdapter(mActionPresenterSelector);
        aoa.addAll(0, (Collection)row.getActions());

        mActionBridgeAdapter.setAdapter(aoa);
        vh.mActionsRow.setAdapter(mActionBridgeAdapter);

        vh.bind(mActionBridgeAdapter);
    }

    @Override
    protected void onUnbindRowViewHolder(RowPresenter.ViewHolder holder) {
        super.onUnbindRowViewHolder(holder);

        ViewHolder vh = (ViewHolder) holder;
        if (vh.mDetailsDescriptionViewHolder != null) {
            mDetailsPresenter.onUnbindViewHolder(vh.mDetailsDescriptionViewHolder);
        }

        vh.mActionsRow.setAdapter(null);
    }
}
