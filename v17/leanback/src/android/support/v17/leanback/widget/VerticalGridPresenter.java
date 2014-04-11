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

import android.support.v17.leanback.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.util.Log;

/**
 * A presenter that renders objects in a vertical grid.
 *
 */
public class VerticalGridPresenter extends Presenter {
    private static final String TAG = "GridPresenter";
    private static final boolean DEBUG = false;

    public static class ViewHolder extends Presenter.ViewHolder {
        final ItemBridgeAdapter mItemBridgeAdapter = new ItemBridgeAdapter();
        final VerticalGridView mGridView;
        boolean mInitialized;

        public ViewHolder(VerticalGridView view) {
            super(view);
            mGridView = view;
        }

        public VerticalGridView getGridView() {
            return mGridView;
        }
    }

    private int mNumColumns = -1;
    private int mZoomFactor;
    private boolean mShadowEnabled = true;
    private OnItemSelectedListener mOnItemSelectedListener;
    private OnItemClickedListener mOnItemClickedListener;

    public VerticalGridPresenter() {
        this(FocusHighlight.ZOOM_FACTOR_MEDIUM);
    }

    public VerticalGridPresenter(int zoomFactor) {
        mZoomFactor = zoomFactor;
    }

    /**
     * Sets the number of columns in the vertical grid.
     */
    public void setNumberOfColumns(int numColumns) {
        if (numColumns < 0) {
            throw new IllegalArgumentException("Invalid number of columns");
        }
        if (mNumColumns != numColumns) {
            mNumColumns = numColumns;
        }
    }

    /**
     * Returns the number of columns in the vertical grid.
     */
    public int getNumberOfColumns() {
        return mNumColumns;
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
     * Returns true if opticalBounds is supported (SDK >= 18) so that default shadow
     * is applied to each individual child of {@link VerticalGridView}.
     * Subclass may return false to disable.
     */
    public boolean isUsingDefaultShadow() {
        return ShadowOverlayContainer.supportsShadow();
    }

    final boolean needsDefaultShadow() {
        return isUsingDefaultShadow() && getShadowEnabled();
    }

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {
        ViewHolder vh = createGridViewHolder(parent);
        vh.mInitialized = false;
        initializeGridViewHolder(vh);
        if (!vh.mInitialized) {
            throw new RuntimeException("super.initializeGridViewHolder() must be called");
        }
        return vh;
    }

    /**
     * Subclass may override this to inflate a different layout.
     */
    protected ViewHolder createGridViewHolder(ViewGroup parent) {
        View root = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.lb_vertical_grid, parent, false);
        return new ViewHolder((VerticalGridView) root.findViewById(R.id.browse_grid));
    }

    private ItemBridgeAdapter.Wrapper mWrapper = new ItemBridgeAdapter.Wrapper() {
        @Override
        public View createWrapper(View root) {
            ShadowOverlayContainer wrapper = new ShadowOverlayContainer(root.getContext());
            wrapper.setLayoutParams(
                    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            wrapper.initialize(needsDefaultShadow(), false);
            return wrapper;
        }
        @Override
        public void wrap(View wrapper, View wrapped) {
            ((ShadowOverlayContainer) wrapper).wrap(wrapped);
        }
    };

    protected void initializeGridViewHolder(ViewHolder vh) {
        if (mNumColumns == -1) {
            throw new IllegalStateException("Number of columns must be set");
        }
        if (DEBUG) Log.v(TAG, "mNumColumns " + mNumColumns);
        vh.getGridView().setNumColumns(mNumColumns);
        vh.mInitialized = true;

        if (needsDefaultShadow()) {
            vh.mItemBridgeAdapter.setWrapper(mWrapper);
            ShadowOverlayContainer.prepareParentForShadow(vh.getGridView());
            ((ViewGroup) vh.view).setClipChildren(false);
        }
        FocusHighlightHelper.setupBrowseItemFocusHighlight(vh.mItemBridgeAdapter, mZoomFactor);

        final ViewHolder gridViewHolder = vh;
        vh.getGridView().setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(ViewGroup parent, View view, int position, long id) {
                selectChildView(gridViewHolder, view);
            }
        });

        vh.mItemBridgeAdapter.setAdapterListener(new ItemBridgeAdapter.AdapterListener() {
            @Override
            public void onCreate(final ItemBridgeAdapter.ViewHolder itemViewHolder) {
                // Only when having an OnItemClickListner, we attach the OnClickListener.
                if (getOnItemClickedListener() != null) {
                    final View itemView = itemViewHolder.getViewHolder().view;
                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (getOnItemClickedListener() != null) {
                                // Row is always null
                                getOnItemClickedListener().onItemClicked(itemViewHolder.mItem, null);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (DEBUG) Log.v(TAG, "onBindViewHolder " + item);
        ViewHolder vh = (ViewHolder) viewHolder;
        vh.mItemBridgeAdapter.setAdapter((ObjectAdapter) item);
        vh.getGridView().setAdapter(vh.mItemBridgeAdapter);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        if (DEBUG) Log.v(TAG, "onUnbindViewHolder");
        ViewHolder vh = (ViewHolder) viewHolder;
        vh.mItemBridgeAdapter.setAdapter(null);
        vh.getGridView().setAdapter(null);
    }

    /**
     * Sets the item selected listener.
     * Since this is a grid the row parameter is always null.
     */
    public final void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    /**
     * Returns the item selected listener.
     */
    public final OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    /**
     * Sets the item clicked listener.
     * OnItemClickedListener will override {@link View.OnClickListener} that
     * item presenter sets during {@link Presenter#onCreateViewHolder(ViewGroup)}.
     * So in general, developer should choose one of the listeners but not both.
     */
    public final void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
    }

    /**
     * Returns the item clicked listener.
     */
    public final OnItemClickedListener getOnItemClickedListener() {
        return mOnItemClickedListener;
    }

    private void selectChildView(ViewHolder vh, View view) {
        if (getOnItemSelectedListener() != null) {
            ItemBridgeAdapter.ViewHolder ibh = (view == null) ? null :
                (ItemBridgeAdapter.ViewHolder) vh.getGridView().getChildViewHolder(view);

            getOnItemSelectedListener().onItemSelected(ibh == null ? null : ibh.mItem, null);
        }
    };
}
