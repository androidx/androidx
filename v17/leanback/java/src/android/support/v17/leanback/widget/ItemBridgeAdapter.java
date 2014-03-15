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

import android.support.v7.widget.RecyclerView;
import android.support.v17.leanback.R;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Bridge from Presenter to RecyclerView.Adapter. Public to allow use by third
 * party presenters.
 */
public class ItemBridgeAdapter extends RecyclerView.Adapter {
    private static final String TAG = "ItemBridgeAdapter";
    private static final boolean DEBUG = false;

    /**
     * Interface for listening to view holder operations.
     */
    public static class AdapterListener {
        public void onCreate(ViewHolder viewHolder) {
        }
        public void onBind(ViewHolder viewHolder) {
        }
        public void onUnbind(ViewHolder viewHolder) {
        }
        public void onAttachedToWindow(ViewHolder viewHolder) {
        }
        public void onDetachedFromWindow(ViewHolder viewHolder) {
        }
    }

    private ObjectAdapter mAdapter;
    private PresenterSelector mPresenterSelector;
    private FocusHighlight mFocusHighlight;
    private AdapterListener mAdapterListener;
    private ArrayList<Presenter> mPresenters = new ArrayList<Presenter>();

    class OnFocusChangeListener implements View.OnFocusChangeListener {
        View.OnFocusChangeListener mChainedListener;

        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (DEBUG) Log.v(TAG, "onFocusChange " + hasFocus + " " + view + " mFocusHighlight" + mFocusHighlight);
            ViewHolder viewHolder = getChildViewHolder(view);
            if (mFocusHighlight != null) {
                mFocusHighlight.onItemFocused(view, viewHolder.mItem, hasFocus);
            }
            if (mChainedListener != null) {
                mChainedListener.onFocusChange(view, hasFocus);
            }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        Presenter mPresenter;
        Presenter.ViewHolder mHolder;
        Object mItem;
        OnFocusChangeListener mFocusChangeListener = new OnFocusChangeListener();

        public final Presenter getPresenter() {
            return mPresenter;
        }
        public final Presenter.ViewHolder getViewHolder() {
            return mHolder;
        }

        ViewHolder(Presenter presenter, Presenter.ViewHolder holder) {
            super(holder.view);
            mPresenter = presenter;
            mHolder = holder;
        }
    }

    private ObjectAdapter.DataObserver mDataObserver = new ObjectAdapter.DataObserver() {
        @Override
        public void onChanged() {
            ItemBridgeAdapter.this.notifyDataSetChanged();
        }
        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            ItemBridgeAdapter.this.notifyItemRangeChanged(positionStart, itemCount);
        }
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            ItemBridgeAdapter.this.notifyItemRangeInserted(positionStart, itemCount);
        }
        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            ItemBridgeAdapter.this.notifyItemRangeRemoved(positionStart, itemCount);
        }
    };

    public ItemBridgeAdapter(ObjectAdapter adapter, PresenterSelector presenterSelector) {
        setAdapter(adapter);
        mPresenterSelector = presenterSelector;
    }

    public ItemBridgeAdapter() {
    }

    private ViewHolder getChildViewHolder(View view) {
        RecyclerView recyclerView = (RecyclerView) view.getParent();
        return (ViewHolder) recyclerView.getChildViewHolder(view);
    }

    public void setAdapter(ObjectAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterObserver(mDataObserver);
        }
        mAdapter = adapter;
        if (mAdapter == null) {
            return;
        }

        mAdapter.registerObserver(mDataObserver);
        setHasStableIds(mAdapter.hasStableIds());
    }

    void setFocusHighlight(FocusHighlight listener) {
        mFocusHighlight = listener;
        if (DEBUG) Log.v(TAG, "setFocusHighlight " + mFocusHighlight);
    }

    public void clear() {
        setAdapter(null);
    }

    @Override
    public int getItemCount() {
        return mAdapter.size();
    }

    @Override
    public int getItemViewType(int position) {
        PresenterSelector presenterSelector = mPresenterSelector != null ?
                mPresenterSelector : mAdapter.getPresenterSelector();
        Object item = mAdapter.get(position);
        Presenter presenter = presenterSelector.getPresenter(item);
        int type = mPresenters.indexOf(presenter);
        if (type < 0) {
            mPresenters.add(presenter);
            type = mPresenters.indexOf(presenter);
        }
        return type;
    }

    /**
     * {@link View.OnFocusChangeListener} that assigned in
     * {@link Presenter#onCreateViewHolder(ViewGroup)} may be chained, user should never change
     * {@link View.OnFocusChangeListener} after that.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (DEBUG) Log.v(TAG, "onCreateViewHolder viewType " + viewType);
        Presenter presenter = mPresenters.get(viewType);
        ViewHolder viewHolder = new ViewHolder(presenter, presenter.onCreateViewHolder(parent));
        if (mAdapterListener != null) {
            mAdapterListener.onCreate(viewHolder);
        }
        View view = viewHolder.mHolder.view;
        if (view != null) {
            viewHolder.mFocusChangeListener.mChainedListener = view.getOnFocusChangeListener();
            view.setOnFocusChangeListener(viewHolder.mFocusChangeListener);
        }
        return viewHolder;
    }

    public void setAdapterListener(AdapterListener listener) {
        mAdapterListener = listener;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (DEBUG) Log.v(TAG, "onBindViewHolder position " + position);
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.mItem = mAdapter.get(position);

        viewHolder.mPresenter.onBindViewHolder(viewHolder.mHolder, viewHolder.mItem);

        if (mAdapterListener != null) {
            mAdapterListener.onBind(viewHolder);
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.mPresenter.onUnbindViewHolder(viewHolder.mHolder);

        viewHolder.mItem = null;

        if (mAdapterListener != null) {
            mAdapterListener.onUnbind(viewHolder);
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        ViewHolder viewHolder = (ViewHolder) holder;
        if (mAdapterListener != null) {
            mAdapterListener.onAttachedToWindow(viewHolder);
        }
        viewHolder.mPresenter.onViewAttachedToWindow(viewHolder.mHolder);
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.mPresenter.onViewDetachedFromWindow(viewHolder.mHolder);
        if (mAdapterListener != null) {
            mAdapterListener.onDetachedFromWindow(viewHolder);
        }
    }

    @Override
    public long getItemId(int position) {
        return mAdapter.getId(position);
    }

}
