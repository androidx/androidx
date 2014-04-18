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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v17.leanback.R;
import android.support.v17.leanback.graphics.ColorOverlayDimmer;
import android.support.v17.leanback.widget.Presenter.ViewHolder;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

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
    private boolean mShadowEnabled = true;
    private int mBrowseRowsFadingEdgeLength = -1;

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
     *         {@link FocusHighlight#ZOOM_FACTOR_MEDIUM},
     *         {@link FocusHighlight#ZOOM_FACTOR_LARGE}
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

    private ItemBridgeAdapter.Wrapper mCardWrapper = new ItemBridgeAdapter.Wrapper() {
        @Override
        public View createWrapper(View root) {
            ShadowOverlayContainer wrapper = new ShadowOverlayContainer(root.getContext());
            wrapper.setLayoutParams(
                    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            wrapper.initialize(needsDefaultShadow(), needsDefaultListSelectEffect());
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
        if (needsDefaultListSelectEffect() || needsDefaultShadow()) {
            rowViewHolder.mItemBridgeAdapter.setWrapper(mCardWrapper);
        }
        if (needsDefaultListSelectEffect()) {
            ShadowOverlayContainer.prepareParentForShadow(rowViewHolder.mGridView);
            ((ViewGroup) rowViewHolder.view).setClipChildren(false);
            if (rowViewHolder.mContainerViewHolder != null) {
                ((ViewGroup) rowViewHolder.mContainerViewHolder.view).setClipChildren(false);
            }
        }
        FocusHighlightHelper.setupBrowseItemFocusHighlight(rowViewHolder.mItemBridgeAdapter, mZoomFactor);
        rowViewHolder.mGridView.setOnChildSelectedListener(
                new OnChildSelectedListener() {
            @Override
            public void onChildSelected(ViewGroup parent, View view, int position, long id) {
                selectChildView(rowViewHolder, view);
            }
        });
        rowViewHolder.mItemBridgeAdapter.setAdapterListener(
                new ItemBridgeAdapter.AdapterListener() {
            @Override
            public void onCreate(final ItemBridgeAdapter.ViewHolder viewHolder) {
                // Only when having an OnItemClickListner, we will attach the OnClickListener.
                if (getOnItemClickedListener() != null) {
                    viewHolder.mHolder.view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ItemBridgeAdapter.ViewHolder ibh = (ItemBridgeAdapter.ViewHolder)
                                    rowViewHolder.mGridView.getChildViewHolder(viewHolder.itemView);
                            if (getOnItemClickedListener() != null) {
                                getOnItemClickedListener().onItemClicked(ibh.mItem,
                                        (ListRow) rowViewHolder.mRow);
                            }
                        }
                    });
                }
            }

            @Override
            public void onAttachedToWindow(ItemBridgeAdapter.ViewHolder viewHolder) {
                if (viewHolder.itemView instanceof ShadowOverlayContainer) {
                    int dimmedColor = rowViewHolder.mColorDimmer.getPaint().getColor();
                    ((ShadowOverlayContainer) viewHolder.itemView).setOverlayColor(dimmedColor);
                }
            }
        });
    }

    final boolean needsDefaultListSelectEffect() {
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
        ListRowView rowView = new ListRowView(parent.getContext());
        setupFadingEffect(rowView);
        return new ViewHolder(rowView, rowView.getGridView(), this);
    }

    @Override
    protected void onRowViewSelected(RowPresenter.ViewHolder holder, boolean selected) {
        updateFooterViewSwitcher((ViewHolder) holder);
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
                vh.mHoverCardViewSwitcher.clear();
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
        vh.getGridView().setFadingLeftEdge(!expanded);
        updateFooterViewSwitcher(vh);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);
        ViewHolder vh = (ViewHolder) holder;
        ListRow rowItem = (ListRow) item;
        vh.mItemBridgeAdapter.clear();
        vh.mItemBridgeAdapter.setAdapter(rowItem.getAdapter());
        vh.mGridView.setAdapter(vh.mItemBridgeAdapter);
    }

    @Override
    protected void onUnbindRowViewHolder(RowPresenter.ViewHolder holder) {
        ((ViewHolder) holder).mGridView.setAdapter(null);
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
            vh.mColorDimmer.setActiveLevel(holder.mSelectLevel);
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

}
