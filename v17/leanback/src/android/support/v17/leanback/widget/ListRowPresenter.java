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
import android.content.res.TypedArray;
import android.support.v17.leanback.R;
import android.support.v17.leanback.graphics.ColorOverlayDimmer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import java.util.HashMap;

/**
 * ListRowPresenter renders {@link ListRow} using a
 * {@link HorizontalGridView} hosted in a {@link ListRowView}.
 *
 * <h3>Hover card</h3>
 * Optionally, {@link #setHoverCardPresenterSelector(PresenterSelector)} can be used to
 * display a view for the currently focused list item below the rendered
 * list. This view is known as a hover card.
 *
 * <h3>Selection animation</h3>
 * ListRowPresenter disables {@link RowPresenter}'s default dimming effect and draw
 * a dim overlay on top of each individual child items.  Subclass may override and disable
 * {@link #isUsingDefaultListSelectEffect()} and write its own dim effect in
 * {@link #onSelectLevelChanged(RowPresenter.ViewHolder)}.
 *
 * <h3>Shadow</h3>
 * ListRowPresenter applies a default shadow to child of each view.  Call
 * {@link #setShadowEnabled(boolean)} to disable shadow.  Subclass may override and return
 * false in {@link #isUsingDefaultShadow()} and replace with its own shadow implementation.
 */
public class ListRowPresenter extends RowPresenter {

    private static final String TAG = "ListRowPresenter";
    private static final boolean DEBUG = false;

    private static final int DEFAULT_RECYCLED_POOL_SIZE = 24;

    public static class ViewHolder extends RowPresenter.ViewHolder {
        final ListRowPresenter mListRowPresenter;
        final HorizontalGridView mGridView;
        ItemBridgeAdapter mItemBridgeAdapter;
        final HorizontalHoverCardSwitcher mHoverCardViewSwitcher = new HorizontalHoverCardSwitcher();
        final int mPaddingTop;
        final int mPaddingBottom;
        final int mPaddingLeft;
        final int mPaddingRight;

        public ViewHolder(View rootView, HorizontalGridView gridView, ListRowPresenter p) {
            super(rootView);
            mGridView = gridView;
            mListRowPresenter = p;
            mPaddingTop = mGridView.getPaddingTop();
            mPaddingBottom = mGridView.getPaddingBottom();
            mPaddingLeft = mGridView.getPaddingLeft();
            mPaddingRight = mGridView.getPaddingRight();
        }

        public final ListRowPresenter getListRowPresenter() {
            return mListRowPresenter;
        }

        public final HorizontalGridView getGridView() {
            return mGridView;
        }

        public final ItemBridgeAdapter getBridgeAdapter() {
            return mItemBridgeAdapter;
        }
    }

    class ListRowPresenterItemBridgeAdapter extends ItemBridgeAdapter {
        ListRowPresenter.ViewHolder mRowViewHolder;

        ListRowPresenterItemBridgeAdapter(ListRowPresenter.ViewHolder rowViewHolder) {
            mRowViewHolder = rowViewHolder;
        }

        @Override
        public void onBind(final ItemBridgeAdapter.ViewHolder viewHolder) {
            // Only when having an OnItemClickListner, we will attach the OnClickListener.
            if (getOnItemClickedListener() != null || getOnItemViewClickedListener() != null) {
                viewHolder.mHolder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ItemBridgeAdapter.ViewHolder ibh = (ItemBridgeAdapter.ViewHolder)
                                mRowViewHolder.mGridView.getChildViewHolder(viewHolder.itemView);
                        if (getOnItemClickedListener() != null) {
                            getOnItemClickedListener().onItemClicked(ibh.mItem,
                                    (ListRow) mRowViewHolder.mRow);
                        }
                        if (getOnItemViewClickedListener() != null) {
                            getOnItemViewClickedListener().onItemClicked(viewHolder.mHolder,
                                    ibh.mItem, mRowViewHolder, (ListRow) mRowViewHolder.mRow);
                        }
                    }
                });
            }
        }

        @Override
        public void onUnbind(ItemBridgeAdapter.ViewHolder viewHolder) {
            if (getOnItemClickedListener() != null || getOnItemViewClickedListener() != null) {
                viewHolder.mHolder.view.setOnClickListener(null);
            }
        }

        @Override
        public void onAttachedToWindow(ItemBridgeAdapter.ViewHolder viewHolder) {
            if (viewHolder.itemView instanceof ShadowOverlayContainer) {
                int dimmedColor = mRowViewHolder.mColorDimmer.getPaint().getColor();
                ((ShadowOverlayContainer) viewHolder.itemView).setOverlayColor(dimmedColor);
            }
            mRowViewHolder.syncActivatedStatus(viewHolder.itemView);
        }

        @Override
        public void onAddPresenter(Presenter presenter, int type) {
            mRowViewHolder.getGridView().getRecycledViewPool().setMaxRecycledViews(
                    type, getRecycledPoolSize(presenter));
        }
    }

    private int mRowHeight;
    private int mExpandedRowHeight;
    private PresenterSelector mHoverCardPresenterSelector;
    private int mZoomFactor;
    private boolean mShadowEnabled = true;
    private int mBrowseRowsFadingEdgeLength = -1;
    private boolean mRoundedCornersEnabled = true;
    private HashMap<Presenter, Integer> mRecycledPoolSize = new HashMap<Presenter, Integer>();

    private static int sSelectedRowTopPadding;
    private static int sExpandedSelectedRowTopPadding;
    private static int sExpandedRowNoHovercardBottomPadding;

    /**
     * Constructs a ListRowPresenter with defaults.
     * Uses {@link FocusHighlight#ZOOM_FACTOR_MEDIUM} for focus zooming.
     */
    public ListRowPresenter() {
        this(FocusHighlight.ZOOM_FACTOR_MEDIUM);
    }

    /**
     * Constructs a ListRowPresenter with the given parameters.
     *
     * @param zoomFactor Controls the zoom factor used when an item view is focused. One of
     *         {@link FocusHighlight#ZOOM_FACTOR_NONE},
     *         {@link FocusHighlight#ZOOM_FACTOR_SMALL},
     *         {@link FocusHighlight#ZOOM_FACTOR_XSMALL},
     *         {@link FocusHighlight#ZOOM_FACTOR_MEDIUM},
     *         {@link FocusHighlight#ZOOM_FACTOR_LARGE}
     */
    public ListRowPresenter(int zoomFactor) {
        if (!FocusHighlightHelper.isValidZoomIndex(zoomFactor)) {
            throw new IllegalArgumentException("Unhandled zoom factor");
        }
        mZoomFactor = zoomFactor;
    }

    /**
     * Sets the row height for rows created by this Presenter. Rows
     * created before calling this method will not be updated.
     *
     * @param rowHeight Row height in pixels, or WRAP_CONTENT, or 0
     * to use the default height.
     */
    public void setRowHeight(int rowHeight) {
        mRowHeight = rowHeight;
    }

    /**
     * Returns the row height for list rows created by this Presenter.
     */
    public int getRowHeight() {
        return mRowHeight;
    }

    /**
     * Sets the expanded row height for rows created by this Presenter.
     * If not set, expanded rows have the same height as unexpanded
     * rows.
     *
     * @param rowHeight The row height in to use when the row is expanded,
     *        in pixels, or WRAP_CONTENT, or 0 to use the default.
     */
    public void setExpandedRowHeight(int rowHeight) {
        mExpandedRowHeight = rowHeight;
    }

    /**
     * Returns the expanded row height for rows created by this Presenter.
     */
    public int getExpandedRowHeight() {
        return mExpandedRowHeight != 0 ? mExpandedRowHeight : mRowHeight;
    }

    /**
     * Returns the zoom factor used for focus highlighting.
     */
    public final int getZoomFactor() {
        return mZoomFactor;
    }

    private ItemBridgeAdapter.Wrapper mCardWrapper = new ItemBridgeAdapter.Wrapper() {
        @Override
        public View createWrapper(View root) {
            ShadowOverlayContainer wrapper = new ShadowOverlayContainer(root.getContext());
            wrapper.setLayoutParams(
                    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            wrapper.initialize(needsDefaultShadow(),
                    needsDefaultListSelectEffect(),
                    areChildRoundedCornersEnabled());
            return wrapper;
        }
        @Override
        public void wrap(View wrapper, View wrapped) {
            ((ShadowOverlayContainer) wrapper).wrap(wrapped);
        }
    };

    @Override
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        final ViewHolder rowViewHolder = (ViewHolder) holder;
        rowViewHolder.mItemBridgeAdapter = new ListRowPresenterItemBridgeAdapter(rowViewHolder);
        if (needsDefaultListSelectEffect() || needsDefaultShadow()
                || areChildRoundedCornersEnabled()) {
            rowViewHolder.mItemBridgeAdapter.setWrapper(mCardWrapper);
        }
        if (needsDefaultListSelectEffect()) {
            ShadowOverlayContainer.prepareParentForShadow(rowViewHolder.mGridView);
        }
        FocusHighlightHelper.setupBrowseItemFocusHighlight(rowViewHolder.mItemBridgeAdapter,
                mZoomFactor, false);
        rowViewHolder.mGridView.setFocusDrawingOrderEnabled(!isUsingZOrder());
        rowViewHolder.mGridView.setOnChildSelectedListener(
                new OnChildSelectedListener() {
            @Override
            public void onChildSelected(ViewGroup parent, View view, int position, long id) {
                selectChildView(rowViewHolder, view);
            }
        });
    }

    final boolean needsDefaultListSelectEffect() {
        return isUsingDefaultListSelectEffect() && getSelectEffectEnabled();
    }

    /**
     * Sets the recycled pool size for the given presenter.
     */
    public void setRecycledPoolSize(Presenter presenter, int size) {
        mRecycledPoolSize.put(presenter, size);
    }

    /**
     * Returns the recycled pool size for the given presenter.
     */
    public int getRecycledPoolSize(Presenter presenter) {
        return mRecycledPoolSize.containsKey(presenter) ? mRecycledPoolSize.get(presenter) :
                DEFAULT_RECYCLED_POOL_SIZE;
    }

    /**
     * Set {@link PresenterSelector} used for showing a select object in a hover card.
     */
    public final void setHoverCardPresenterSelector(PresenterSelector selector) {
        mHoverCardPresenterSelector = selector;
    }

    /**
     * Get {@link PresenterSelector} used for showing a select object in a hover card.
     */
    public final PresenterSelector getHoverCardPresenterSelector() {
        return mHoverCardPresenterSelector;
    }

    /*
     * Perform operations when a child of horizontal grid view is selected.
     */
    private void selectChildView(ViewHolder rowViewHolder, View view) {
        if (view != null) {
            if (rowViewHolder.mExpanded && rowViewHolder.mSelected) {
                ItemBridgeAdapter.ViewHolder ibh = (ItemBridgeAdapter.ViewHolder)
                        rowViewHolder.mGridView.getChildViewHolder(view);

                if (mHoverCardPresenterSelector != null) {
                    rowViewHolder.mHoverCardViewSwitcher.select(rowViewHolder.mGridView, view,
                            ibh.mItem);
                }
                if (getOnItemViewSelectedListener() != null) {
                    getOnItemViewSelectedListener().onItemSelected(ibh.mHolder, ibh.mItem,
                            rowViewHolder, rowViewHolder.mRow);
                }
                if (getOnItemSelectedListener() != null) {
                    getOnItemSelectedListener().onItemSelected(ibh.mItem, rowViewHolder.mRow);
                }
            }
        } else {
            if (mHoverCardPresenterSelector != null) {
                rowViewHolder.mHoverCardViewSwitcher.unselect();
            }
            if (getOnItemViewSelectedListener() != null) {
                getOnItemViewSelectedListener().onItemSelected(null, null,
                        rowViewHolder, rowViewHolder.mRow);
            }
            if (getOnItemSelectedListener() != null) {
                getOnItemSelectedListener().onItemSelected(null, rowViewHolder.mRow);
            }
        }
    }

    private static void initStatics(Context context) {
        if (sSelectedRowTopPadding == 0) {
            sSelectedRowTopPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.lb_browse_selected_row_top_padding);
            sExpandedSelectedRowTopPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.lb_browse_expanded_selected_row_top_padding);
            sExpandedRowNoHovercardBottomPadding = context.getResources().getDimensionPixelSize(
                    R.dimen.lb_browse_expanded_row_no_hovercard_bottom_padding);
        }
    }

    private int getSpaceUnderBaseline(ListRowPresenter.ViewHolder vh) {
        RowHeaderPresenter.ViewHolder headerViewHolder = vh.getHeaderViewHolder();
        if (headerViewHolder != null) {
            if (getHeaderPresenter() != null) {
                return getHeaderPresenter().getSpaceUnderBaseline(headerViewHolder);
            }
            return headerViewHolder.view.getPaddingBottom();
        }
        return 0;
    }

    private void setVerticalPadding(ListRowPresenter.ViewHolder vh) {
        int paddingTop, paddingBottom;
        // Note: sufficient bottom padding needed for card shadows.
        if (vh.isExpanded()) {
            int headerSpaceUnderBaseline = getSpaceUnderBaseline(vh);
            if (DEBUG) Log.v(TAG, "headerSpaceUnderBaseline " + headerSpaceUnderBaseline);
            paddingTop = (vh.isSelected() ? sExpandedSelectedRowTopPadding : vh.mPaddingTop) -
                    headerSpaceUnderBaseline;
            paddingBottom = mHoverCardPresenterSelector == null ?
                    sExpandedRowNoHovercardBottomPadding : vh.mPaddingBottom;
        } else if (vh.isSelected()) {
            paddingTop = sSelectedRowTopPadding - vh.mPaddingBottom;
            paddingBottom = sSelectedRowTopPadding;
        } else {
            paddingTop = 0;
            paddingBottom = vh.mPaddingBottom;
        }
        vh.getGridView().setPadding(vh.mPaddingLeft, paddingTop, vh.mPaddingRight,
                paddingBottom);
    }

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        initStatics(parent.getContext());
        ListRowView rowView = new ListRowView(parent.getContext());
        setupFadingEffect(rowView);
        if (mRowHeight != 0) {
            rowView.getGridView().setRowHeight(mRowHeight);
        }
        return new ViewHolder(rowView, rowView.getGridView(), this);
    }

    /**
     * Dispatch item selected event using current selected item in the {@link HorizontalGridView}.
     * The method should only be called from onRowViewSelected().
     */
    @Override
    protected void dispatchItemSelectedListener(RowPresenter.ViewHolder holder, boolean selected) {
        ViewHolder vh = (ViewHolder)holder;
        ItemBridgeAdapter.ViewHolder itemViewHolder = (ItemBridgeAdapter.ViewHolder)
                vh.mGridView.findViewHolderForPosition(vh.mGridView.getSelectedPosition());
        if (itemViewHolder == null) {
            super.dispatchItemSelectedListener(holder, selected);
            return;
        }

        if (selected) {
            if (getOnItemViewSelectedListener() != null) {
                getOnItemViewSelectedListener().onItemSelected(
                        itemViewHolder.getViewHolder(), itemViewHolder.mItem, vh, vh.getRow());
            }
            if (getOnItemSelectedListener() != null) {
                getOnItemSelectedListener().onItemSelected(itemViewHolder.mItem, vh.getRow());
            }
        }
    }

    @Override
    protected void onRowViewSelected(RowPresenter.ViewHolder holder, boolean selected) {
        super.onRowViewSelected(holder, selected);
        ViewHolder vh = (ViewHolder) holder;
        setVerticalPadding(vh);
        updateFooterViewSwitcher(vh);
    }

    /*
     * Show or hide hover card when row selection or expanded state is changed.
     */
    private void updateFooterViewSwitcher(ViewHolder vh) {
        if (vh.mExpanded && vh.mSelected) {
            if (mHoverCardPresenterSelector != null) {
                vh.mHoverCardViewSwitcher.init((ViewGroup) vh.view,
                        mHoverCardPresenterSelector);
            }
            ItemBridgeAdapter.ViewHolder ibh = (ItemBridgeAdapter.ViewHolder)
                    vh.mGridView.findViewHolderForPosition(
                            vh.mGridView.getSelectedPosition());
            selectChildView(vh, ibh == null ? null : ibh.itemView);
        } else {
            if (mHoverCardPresenterSelector != null) {
                vh.mHoverCardViewSwitcher.unselect();
            }
        }
    }

    private void setupFadingEffect(ListRowView rowView) {
        // content is completely faded at 1/2 padding of left, fading length is 1/2 of padding.
        HorizontalGridView gridView = rowView.getGridView();
        if (mBrowseRowsFadingEdgeLength < 0) {
            TypedArray ta = gridView.getContext()
                    .obtainStyledAttributes(R.styleable.LeanbackTheme);
            mBrowseRowsFadingEdgeLength = (int) ta.getDimension(
                    R.styleable.LeanbackTheme_browseRowsFadingEdgeLength, 0);
            ta.recycle();
        }
        gridView.setFadingLeftEdgeLength(mBrowseRowsFadingEdgeLength);
    }

    @Override
    protected void onRowViewExpanded(RowPresenter.ViewHolder holder, boolean expanded) {
        super.onRowViewExpanded(holder, expanded);
        ViewHolder vh = (ViewHolder) holder;
        if (getRowHeight() != getExpandedRowHeight()) {
            int newHeight = expanded ? getExpandedRowHeight() : getRowHeight();
            vh.getGridView().setRowHeight(newHeight);
        }
        setVerticalPadding(vh);
        updateFooterViewSwitcher(vh);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);
        ViewHolder vh = (ViewHolder) holder;
        ListRow rowItem = (ListRow) item;
        vh.mItemBridgeAdapter.setAdapter(rowItem.getAdapter());
        vh.mGridView.setAdapter(vh.mItemBridgeAdapter);
    }

    @Override
    protected void onUnbindRowViewHolder(RowPresenter.ViewHolder holder) {
        ViewHolder vh = (ViewHolder) holder;
        vh.mGridView.setAdapter(null);
        vh.mItemBridgeAdapter.clear();
        super.onUnbindRowViewHolder(holder);
    }

    /**
     * ListRowPresenter overrides the default select effect of {@link RowPresenter}
     * and return false.
     */
    @Override
    public final boolean isUsingDefaultSelectEffect() {
        return false;
    }

    /**
     * Returns true so that default select effect is applied to each individual
     * child of {@link HorizontalGridView}.  Subclass may return false to disable
     * the default implementation.
     * @see #onSelectLevelChanged(RowPresenter.ViewHolder)
     */
    public boolean isUsingDefaultListSelectEffect() {
        return true;
    }

    /**
     * Returns true if SDK >= 18, where default shadow
     * is applied to each individual child of {@link HorizontalGridView}.
     * Subclass may return false to disable.
     */
    public boolean isUsingDefaultShadow() {
        return ShadowOverlayContainer.supportsShadow();
    }

    /**
     * Returns true if SDK >= L, where Z shadow is enabled so that Z order is enabled
     * on each child of horizontal list.   If subclass returns false in isUsingDefaultShadow()
     * and does not use Z-shadow on SDK >= L, it should override isUsingZOrder() return false.
     */
    public boolean isUsingZOrder() {
        return ShadowHelper.getInstance().usesZShadow();
    }

    /**
     * Enable or disable child shadow.
     * This is not only for enable/disable default shadow implementation but also subclass must
     * respect this flag.
     */
    public final void setShadowEnabled(boolean enabled) {
        mShadowEnabled = enabled;
    }

    /**
     * Returns true if child shadow is enabled.
     * This is not only for enable/disable default shadow implementation but also subclass must
     * respect this flag.
     */
    public final boolean getShadowEnabled() {
        return mShadowEnabled;
    }

    /**
     * Enables or disabled rounded corners on children of this row.
     * Supported on Android SDK >= L.
     */
    public final void enableChildRoundedCorners(boolean enable) {
        mRoundedCornersEnabled = enable;
    }

    /**
     * Returns true if rounded corners are enabled for children of this row.
     */
    public final boolean areChildRoundedCornersEnabled() {
        return mRoundedCornersEnabled;
    }

    final boolean needsDefaultShadow() {
        return isUsingDefaultShadow() && getShadowEnabled();
    }

    @Override
    public boolean canDrawOutOfBounds() {
        return needsDefaultShadow();
    }

    /**
     * Applies select level to header and draw a default color dim over each child
     * of {@link HorizontalGridView}.
     * <p>
     * Subclass may override this method.  A subclass
     * needs to call super.onSelectLevelChanged() for applying header select level
     * and optionally applying a default select level to each child view of
     * {@link HorizontalGridView} if {@link #isUsingDefaultListSelectEffect()}
     * is true.  Subclass may override {@link #isUsingDefaultListSelectEffect()} to return
     * false and deal with the individual item select level by itself.
     * </p>
     */
    @Override
    protected void onSelectLevelChanged(RowPresenter.ViewHolder holder) {
        super.onSelectLevelChanged(holder);
        if (needsDefaultListSelectEffect()) {
            ViewHolder vh = (ViewHolder) holder;
            int dimmedColor = vh.mColorDimmer.getPaint().getColor();
            for (int i = 0, count = vh.mGridView.getChildCount(); i < count; i++) {
                ShadowOverlayContainer wrapper = (ShadowOverlayContainer) vh.mGridView.getChildAt(i);
                wrapper.setOverlayColor(dimmedColor);
            }
            if (vh.mGridView.getFadingLeftEdge()) {
                vh.mGridView.invalidate();
            }
        }
    }

    @Override
    public void freeze(RowPresenter.ViewHolder holder, boolean freeze) {
        ViewHolder vh = (ViewHolder) holder;
        vh.mGridView.setScrollEnabled(!freeze);
    }

    @Override
    public void setEntranceTransitionState(RowPresenter.ViewHolder holder,
            boolean afterEntrance) {
        super.setEntranceTransitionState(holder, afterEntrance);
        ((ViewHolder) holder).mGridView.setChildrenVisibility(
                afterEntrance? View.VISIBLE : View.INVISIBLE);
    }
}
