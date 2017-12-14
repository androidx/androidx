/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/**
 * Work in progress: go/viewpager2
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ViewPager2 extends ViewGroup {
    // reused in layout(...)
    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();

    private RecyclerView mRecyclerView;

    public ViewPager2(Context context) {
        super(context);
        initialize(context);
    }

    public ViewPager2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPager2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @TargetApi(21)
    public ViewPager2(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // TODO(b/70663531): handle attrs, defStyleAttr, defStyleRes
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        mRecyclerView = new RecyclerView(context);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        // TODO(b/69103581): add support for vertical layout
        // TODO(b/69398856): add support for RTL
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setLayoutParams(
                new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // TODO(b/70666992): add automated test for orientation change
        new PagerSnapHelper().attachToRecyclerView(mRecyclerView);

        attachViewToParent(mRecyclerView, 0, mRecyclerView.getLayoutParams());
    }

    /**
     * TODO(b/70663708): decide on an Adapter class (for now reusing RecyclerView.Adapter)
     *
     * @see RecyclerView#setAdapter(Adapter)
     */
    public <VH extends ViewHolder> void setAdapter(final Adapter<VH> adapter) {
        mRecyclerView.setAdapter(new Adapter<VH>() {
            private final Adapter<VH> mAdapter = adapter;

            @NonNull
            @Override
            public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                VH viewHolder = mAdapter.onCreateViewHolder(parent, viewType);

                LayoutParams layoutParams = viewHolder.itemView.getLayoutParams();
                if ((layoutParams.width | layoutParams.height) != LayoutParams.MATCH_PARENT) {
                    // TODO(b/70666614): decide if throw an exception or wrap in FrameLayout
                    // ourselves; consider accepting exact size equal to parent's exact size
                    throw new IllegalStateException(String.format(
                            "Item's root view must fill the whole %s (use match_parent)",
                            ViewPager2.this.getClass().getSimpleName()));
                }

                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull VH holder, int position) {
                mAdapter.onBindViewHolder(holder, position);
            }

            @Override
            public int getItemCount() {
                return mAdapter.getItemCount();
            }
        });
    }

    @Override
    public void onViewAdded(View child) {
        // TODO(b/70666620): consider adding a support for Decor views
        throw new IllegalStateException(
                getClass().getSimpleName() + " does not support direct child views");
    }

    /** @see RecyclerView#addOnScrollListener(RecyclerView.OnScrollListener) */
    public void addOnScrollListener(RecyclerView.OnScrollListener listener) {
        mRecyclerView.addOnScrollListener(listener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO(b/70666622): consider margin support
        // TODO(b/70666626): consider delegating all this to RecyclerView
        // TODO(b/70666625): write automated tests for this

        measureChild(mRecyclerView, widthMeasureSpec, heightMeasureSpec);
        int width = mRecyclerView.getMeasuredWidth();
        int height = mRecyclerView.getMeasuredHeight();
        int childState = mRecyclerView.getMeasuredState();

        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();

        width = Math.max(width, getSuggestedMinimumWidth());
        height = Math.max(height, getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, childState),
                resolveSizeAndState(height, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = mRecyclerView.getMeasuredWidth();
        int height = mRecyclerView.getMeasuredHeight();

        // TODO(b/70666626): consider delegating padding handling to the RecyclerView to avoid
        // an unnatural page transition effect: http://shortn/_Vnug3yZpQT
        mTmpContainerRect.left = getPaddingLeft();
        mTmpContainerRect.right = r - l - getPaddingRight();
        mTmpContainerRect.top = getPaddingTop();
        mTmpContainerRect.bottom = b - t - getPaddingBottom();

        Gravity.apply(Gravity.TOP | Gravity.START, width, height, mTmpContainerRect, mTmpChildRect);
        mRecyclerView.layout(mTmpChildRect.left, mTmpChildRect.top, mTmpChildRect.right,
                mTmpChildRect.bottom);
    }
}
