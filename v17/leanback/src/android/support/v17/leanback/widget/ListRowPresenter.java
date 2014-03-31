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

import java.util.ArrayList;

import android.graphics.Canvas;
import android.support.v17.leanback.graphics.ColorOverlayDimmer;
import android.support.v17.leanback.widget.Presenter.ViewHolder;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * ListRowPresenter renders {@link ListRow} using a
 * {@link HorizontalGridView} hosted in a {@link BrowseRowView}.
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
 */
public class ListRowPresenter extends RowPresenter {

    private static final String TAG = "ListRowPresenter";
    private static final boolean DEBUG = false;

    /**
     * No zoom factor.
     */
    public static final int ZOOM_FACTOR_NONE = FocusHighlightHelper.BrowseItemFocusHighlight.ZOOM_FACTOR_NONE;

    /**
     * A small zoom factor, recommended for large item views.
     */
    public static final int ZOOM_FACTOR_SMALL = FocusHighlightHelper.BrowseItemFocusHighlight.ZOOM_FACTOR_SMALL;

    /**
     * A medium zoom factor, recommended for medium sized item views.
     */
    public static final int ZOOM_FACTOR_MEDIUM = FocusHighlightHelper.BrowseItemFocusHighlight.ZOOM_FACTOR_MEDIUM;

    /**
     * A large zoom factor, recommended for small item views.
     */
    public static final int ZOOM_FACTOR_LARGE = FocusHighlightHelper.BrowseItemFocusHighlight.ZOOM_FACTOR_LARGE;

    public static class ViewHolder extends RowPresenter.ViewHolder {
        final ListRowPresenter mListRowPresenter;
        final HorizontalGridView mGridView;
        final ItemBridgeAdapter mItemBridgeAdapter = new ItemBridgeAdapter();
        final HorizontalHoverCardSwitcher mHoverCardViewSwitcher = new HorizontalHoverCardSwitcher();
        final ColorOverlayDimmer mColorDimmer;

        public ViewHolder(View rootView, HorizontalGridView gridView, ListRowPresenter p) {
            super(rootView);
            mGridView = gridView;
            mListRowPresenter = p;
            mColorDimmer = ColorOverlayDimmer.createDefault(rootView.getContext());
        }

        public final ListRowPresenter getListRowPresenter() {
            return mListRowPresenter;
        }

        public final HorizontalGridView getGridView() {
            return mGridView;
        }
    }

    private PresenterSelector mHoverCardPresenterSelector;
    private int mZoomFactor;

    /**
     * Constructs a ListRowPresenter with defaults.
     * Uses {@link #ZOOM_FACTOR_MEDIUM} for focus zooming.
     */
    public ListRowPresenter() {
        this(ZOOM_FACTOR_MEDIUM);
    }

    /**
     * Constructs a ListRowPresenter with the given parameters.
     *
     * @param zoomFactor Controls the zoom factor used when an item view is focused. One of
     *         {@link #ZOOM_FACTOR_NONE}, {@link #ZOOM_FACTOR_SMALL}, {@link #ZOOM_FACTOR_MEDIUM},
     *         {@link #ZOOM_FACTOR_LARGE}
     */
    public ListRowPresenter(int zoomFactor) {
        mZoomFactor = zoomFactor;
    }

    /**
     * Returns the zoom factor used for focus highlighting.
     */
    public final int getZoomFactor() {
        return mZoomFactor;
    }

    @Override
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        final ViewHolder rowViewHolder = (ViewHolder) holder;
        if (needsDefaultListItemDecoration()) {
            rowViewHolder.mGridView.addItemDecoration(new ItemDecoration(rowViewHolder));
        }
        FocusHighlightHelper.setupBrowseItemFocusHighlight(rowViewHolder.mItemBridgeAdapter, mZoomFactor);
        rowViewHolder.mGridView.setOnChildSelectedListener(
                new OnChildSelectedListener() {
            @Override
            public void onChildSelected(ViewGroup parent, View view, int position, long id) {
                selectChildView(rowViewHolder, view);
            }
        });
        if (getOnItemClickedListener() != null) {
            // Only when having an OnItemClickListner, we will attach the OnClickListener.
            rowViewHolder.mItemBridgeAdapter.setAdapterListener(
                    new ItemBridgeAdapter.AdapterListener() {
                @Override
                public void onCreate(ItemBridgeAdapter.ViewHolder viewHolder) {
                    viewHolder.mHolder.view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ItemBridgeAdapter.ViewHolder ibh = (ItemBridgeAdapter.ViewHolder)
                                    rowViewHolder.mGridView.getChildViewHolder(v);
                            if (getOnItemClickedListener() != null) {
                                getOnItemClickedListener().onItemClicked(ibh.mItem,
                                        (ListRow) rowViewHolder.mRow);
                            }
                        }
                    });
                }
            });
        }
    }

    private boolean needsDefaultListItemDecoration() {
        return isUsingDefaultListSelectEffect() && getSelectEffectEnabled();
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
        ItemBridgeAdapter.ViewHolder ibh = null;
        if (view != null) {
            ibh = (ItemBridgeAdapter.ViewHolder)
                    rowViewHolder.mGridView.getChildViewHolder(view);
        }
        if (view == null) {
            if (mHoverCardPresenterSelector != null) {
                rowViewHolder.mHoverCardViewSwitcher.unselect();
            }
            if (getOnItemSelectedListener() != null) {
                getOnItemSelectedListener().onItemSelected(null, rowViewHolder.mRow);
            }
        } else if (rowViewHolder.mExpanded && rowViewHolder.mSelected) {
            if (mHoverCardPresenterSelector != null) {
                rowViewHolder.mHoverCardViewSwitcher.select(rowViewHolder.mGridView, view,
                        ibh.mItem);
            }
            if (getOnItemSelectedListener() != null) {
                getOnItemSelectedListener().onItemSelected(ibh.mItem, rowViewHolder.mRow);
            }
        }
    }

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        BrowseRowView rowView = new BrowseRowView(parent.getContext());
        return new ViewHolder(rowView, rowView.getGridView(), this);
    }

    @Override
    protected void onRowViewSelected(RowPresenter.ViewHolder holder, boolean selected) {
        updateFooterViewSwitcher((ViewHolder) holder);
        updateInitialChildSelection((ViewHolder) holder);
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
        } else {
            if (mHoverCardPresenterSelector != null) {
                vh.mHoverCardViewSwitcher.clear();
            }
        }
    }

    /*
     * Make initial child selection when row selection state is changed.
     */
    private void updateInitialChildSelection(ViewHolder vh) {
        if (vh.mExpanded && vh.mSelected) {
            ItemBridgeAdapter.ViewHolder ibh = (ItemBridgeAdapter.ViewHolder)
                    vh.mGridView.findViewHolderForPosition(
                            vh.mGridView.getSelectedPosition());
            selectChildView(vh, ibh == null ? null : ibh.mHolder.view);
        } else {
            selectChildView(vh, null);
        }
    }

    @Override
    protected void onRowViewExpanded(RowPresenter.ViewHolder holder, boolean expanded) {
        super.onRowViewExpanded(holder, expanded);
        ViewHolder vh = (ViewHolder) holder;
        vh.mGridView.setClipToPadding(!expanded);
        vh.mGridView.invalidate();
        updateFooterViewSwitcher(vh);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder holder, Object item) {
        super.onBindViewHolder(holder, item);
        ViewHolder vh = (ViewHolder)holder;
        ListRow rowItem = (ListRow) item;
        vh.mItemBridgeAdapter.clear();
        vh.mItemBridgeAdapter.setAdapter(rowItem.getAdapter());
        vh.mGridView.setAdapter(vh.mItemBridgeAdapter);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder holder) {
        ViewHolder vh = (ViewHolder)holder;
        vh.mGridView.setAdapter(null);
        super.onUnbindViewHolder(holder);
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
        if (needsDefaultListItemDecoration()) {
            ViewHolder vh = (ViewHolder) holder;
            vh.mColorDimmer.setActiveLevel(holder.mSelectLevel);
            vh.mGridView.invalidate();
        }
    }

    private void drawDimSelectionForChildren(ViewHolder vh, Canvas c) {
        final ColorOverlayDimmer dimmer = vh.mColorDimmer;
        if (dimmer.needsDraw()) {
            final HorizontalGridView gridView = vh.mGridView;
            // Clip to padding when not expanded
            if (!vh.mExpanded) {
                c.clipRect(gridView.getPaddingLeft(), gridView.getPaddingTop(),
                        gridView.getWidth() - gridView.getPaddingRight(),
                        gridView.getHeight() - gridView.getPaddingBottom());
            }
            for (int i = 0, count = gridView.getChildCount(); i < count; i++) {
                dimmer.drawColorOverlay(c, gridView.getChildAt(i), true);
            }
        }
    }

    final class ItemDecoration extends RecyclerView.ItemDecoration {
        ViewHolder mViewHolder;
        ItemDecoration(ViewHolder viewHolder) {
            mViewHolder = viewHolder;
        }
        @Override
        public void onDrawOver(Canvas c, RecyclerView parent) {
            drawDimSelectionForChildren(mViewHolder, c);
        }
    }

}
