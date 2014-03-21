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

import android.support.v17.leanback.graphics.ColorOverlayDimmer;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

/**
 * RowHeaderPresenter provides a default implementation for header using TextView.
 * RowHeaderPresenter receives calls {@link #setHidden(ViewHolder, boolean)} from
 * {@link RowPresenter} when the row takes full screen.
 * If subclass override and creates its own view, subclass must also override
 * {@link #updateViewVisibility(ViewHolder)} and {@link #onSelectLevelChanged(ViewHolder)}.
 */
public class RowHeaderPresenter extends Presenter {

    public static class ViewHolder extends Presenter.ViewHolder {
        boolean mHidden;
        float mSelectLevel;
        int mOriginalTextColor;
        ColorOverlayDimmer mColorDimmer;
        public ViewHolder(View view) {
            super(view);
        }
        public final boolean getHidden() {
            return mHidden;
        }
        public final float getSelectLevel() {
            return mSelectLevel;
        }
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        BrowseRowHeaderView headerView = new BrowseRowHeaderView(parent.getContext());
        ViewHolder viewHolder = new ViewHolder(headerView);
        viewHolder.mOriginalTextColor = headerView.getCurrentTextColor();
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Row rowItem = (Row) item;
        if (rowItem != null) {
            HeaderItem headerItem = rowItem.getHeaderItem();
            if (headerItem != null) {
                String text = headerItem.getName();
                ((BrowseRowHeaderView) viewHolder.view).setText(text);
            }
        }
        updateViewVisibility((ViewHolder) viewHolder);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ((BrowseRowHeaderView) viewHolder.view).setText(null);
    }

    public final void setHidden(ViewHolder holder, boolean hidden) {
        holder.mHidden = hidden;
        updateViewVisibility(holder);
    }

    protected void updateViewVisibility(RowHeaderPresenter.ViewHolder holder) {
        if (!holder.mHidden && !TextUtils.isEmpty(((BrowseRowHeaderView) holder.view)
                        .getText())) {
            holder.view.setVisibility(View.VISIBLE);
        } else {
            holder.view.setVisibility(View.GONE);
        }
    }

    public final void setSelectLevel(ViewHolder holder, float selectLevel) {
        holder.mSelectLevel = selectLevel;
        onSelectLevelChanged(holder);
    }

    protected void onSelectLevelChanged(ViewHolder holder) {
        if (holder.mColorDimmer == null) {
            holder.mColorDimmer = ColorOverlayDimmer.createDefault(holder.view.getContext());
        }
        holder.mColorDimmer.setActiveLevel(holder.mSelectLevel);
        final BrowseRowHeaderView headerView = (BrowseRowHeaderView) holder.view;
        headerView.setTextColor(holder.mColorDimmer.applyToColor(holder.mOriginalTextColor));
    }
}