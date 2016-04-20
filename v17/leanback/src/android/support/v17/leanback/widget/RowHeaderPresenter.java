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

import android.graphics.Paint;
import android.support.v17.leanback.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * RowHeaderPresenter provides a default presentation for {@link HeaderItem} using a
 * {@link RowHeaderView}. If a subclass creates its own view, the subclass must also override
 * {@link #onSelectLevelChanged(ViewHolder)}.
 */
public class RowHeaderPresenter extends Presenter {

    private final int mLayoutResourceId;
    private final Paint mFontMeasurePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean mNullItemVisibilityGone;
    private final boolean mAnimateSelect;

    public RowHeaderPresenter() {
        this(R.layout.lb_row_header);
    }

    /**
     * @hide
     */
    public RowHeaderPresenter(int layoutResourceId) {
        this(layoutResourceId, true);
    }

    /**
     * @hide
     */
    public RowHeaderPresenter(int layoutResourceId, boolean animateSelect) {
        mLayoutResourceId = layoutResourceId;
        mAnimateSelect = animateSelect;
    }

    /**
     * Optionally sets the view visibility to {@link View#GONE} when bound to null.
     */
    public void setNullItemVisibilityGone(boolean nullItemVisibilityGone) {
        mNullItemVisibilityGone = nullItemVisibilityGone;
    }

    /**
     * Returns true if the view visibility is set to {@link View#GONE} when bound to null.
     */
    public boolean isNullItemVisibilityGone() {
        return mNullItemVisibilityGone;
    }

    /**
     * A ViewHolder for the RowHeaderPresenter.
     */
    public static class ViewHolder extends Presenter.ViewHolder {
        float mSelectLevel;
        int mOriginalTextColor;
        float mUnselectAlpha;

        public ViewHolder(View view) {
            super(view);
        }
        public final float getSelectLevel() {
            return mSelectLevel;
        }
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        RowHeaderView headerView = (RowHeaderView) LayoutInflater.from(parent.getContext())
                .inflate(mLayoutResourceId, parent, false);

        ViewHolder viewHolder = new ViewHolder(headerView);
        viewHolder.mOriginalTextColor = headerView.getCurrentTextColor();
        viewHolder.mUnselectAlpha = parent.getResources().getFraction(
                R.fraction.lb_browse_header_unselect_alpha, 1, 1);
        if (mAnimateSelect) {
            setSelectLevel(viewHolder, 0);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        HeaderItem headerItem = item == null ? null : ((Row) item).getHeaderItem();
        if (headerItem == null) {
            ((RowHeaderView) viewHolder.view).setText(null);
            viewHolder.view.setContentDescription(null);
            if (mNullItemVisibilityGone) {
                viewHolder.view.setVisibility(View.GONE);
            }
        } else {
            viewHolder.view.setVisibility(View.VISIBLE);
            ((RowHeaderView) viewHolder.view).setText(headerItem.getName());
            viewHolder.view.setContentDescription(headerItem.getContentDescription());
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ((RowHeaderView) viewHolder.view).setText(null);
        if (mAnimateSelect) {
            setSelectLevel((ViewHolder) viewHolder, 0);
        }
    }

    /**
     * Sets the select level.
     */
    public final void setSelectLevel(ViewHolder holder, float selectLevel) {
        holder.mSelectLevel = selectLevel;
        onSelectLevelChanged(holder);
    }

    /**
     * Called when the select level changes.  The default implementation sets the alpha on the view.
     */
    protected void onSelectLevelChanged(ViewHolder holder) {
        if (mAnimateSelect) {
            holder.view.setAlpha(holder.mUnselectAlpha + holder.mSelectLevel *
                    (1f - holder.mUnselectAlpha));
        }
    }

    /**
     * Returns the space (distance in pixels) below the baseline of the
     * text view, if one exists; otherwise, returns 0.
     */
    public int getSpaceUnderBaseline(ViewHolder holder) {
        int space = holder.view.getPaddingBottom();
        if (holder.view instanceof TextView) {
            space += (int) getFontDescent((TextView) holder.view, mFontMeasurePaint);
        }
        return space;
    }

    protected static float getFontDescent(TextView textView, Paint fontMeasurePaint) {
        if (fontMeasurePaint.getTextSize() != textView.getTextSize()) {
            fontMeasurePaint.setTextSize(textView.getTextSize());
        }
        if (fontMeasurePaint.getTypeface() != textView.getTypeface()) {
            fontMeasurePaint.setTypeface(textView.getTypeface());
        }
        return fontMeasurePaint.descent();
    }
}
