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
import android.view.View;
import android.view.ViewGroup;

/**
 * RowHeaderPresenter provides a default implementation for header using TextView.
 * If subclass override and creates its own view, subclass must also override
 * {@link #onSelectLevelChanged(ViewHolder)}.
 */
public class RowHeaderPresenter extends Presenter {

    public static class ViewHolder extends Presenter.ViewHolder {
        float mSelectLevel;
        int mOriginalTextColor;
        ColorOverlayDimmer mColorDimmer;
        public ViewHolder(View view) {
            super(view);
        }
        public final float getSelectLevel() {
            return mSelectLevel;
        }
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        RowHeaderView headerView = new RowHeaderView(parent.getContext());
        ViewHolder viewHolder = new ViewHolder(headerView);
        viewHolder.mOriginalTextColor = headerView.getCurrentTextColor();
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        setSelectLevel((ViewHolder) viewHolder, 0);
        Row rowItem = (Row) item;
        if (rowItem != null) {
            HeaderItem headerItem = rowItem.getHeaderItem();
            if (headerItem != null) {
                String text = headerItem.getName();
                ((RowHeaderView) viewHolder.view).setText(text);
            }
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ((RowHeaderView) viewHolder.view).setText(null);
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
        final RowHeaderView headerView = (RowHeaderView) holder.view;
        headerView.setTextColor(holder.mColorDimmer.applyToColor(holder.mOriginalTextColor));
    }
}