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

import android.support.v17.leanback.widget.Presenter.ViewHolder;
import android.view.View;
import android.view.ViewGroup;

/**
 * Presents a ListRow using a {@link HorizontalGridView} hosted in a {@link BrowseRowView}.
 * <p>
 * Optionally, {@link #setHoverCardPresenterSelector(PresenterSelector)} can be used to
 * display a view for the currently focused list item below the rendered
 * list. This view is known as a hover card.
 * </p>
 * <p>
 * ListRowPresenter has the same capability of {@link #setHeaderPresenter(RowHeaderPresenter)}
 * as parent class {@link RowPresenter}.
 * </p>
 */
public class ListRowPresenter extends RowPresenter {

    private static final String TAG = "ListRowPresenter";
    private static final boolean DEBUG = false;

    public static class ViewHolder extends RowPresenter.ViewHolder {
        final ListRowPresenter mListRowPresenter;
        final HorizontalGridView mGridView;
        final ItemBridgeAdapter mItemBridgeAdapter = new ItemBridgeAdapter();
        final HorizontalHoverCardSwitcher mHoverCardViewSwitcher = new HorizontalHoverCardSwitcher();

        public ViewHolder(View rootView, HorizontalGridView gridView, ListRowPresenter p) {
            super(rootView);
            mGridView = gridView;
            mListRowPresenter = p;
        }

        public final ListRowPresenter getListRowPresenter() {
            return mListRowPresenter;
        }

        public final HorizontalGridView getGridView() {
            return mGridView;
        }
    }

    private PresenterSelector mHoverCardPresenterSelector;

    @Override
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        final ViewHolder rowViewHolder = (ViewHolder) holder;
        FocusHighlightHelper.setupBrowseItemFocusHighlight(rowViewHolder.mItemBridgeAdapter);
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

}
