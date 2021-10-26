/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.draganddrop;

import static android.view.DragEvent.ACTION_DRAG_ENDED;
import static android.view.DragEvent.ACTION_DRAG_ENTERED;
import static android.view.DragEvent.ACTION_DRAG_EXITED;
import static android.view.DragEvent.ACTION_DRAG_STARTED;

import static java.lang.Math.max;
import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.content.ClipDescription;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;
import androidx.core.util.Predicate;

import java.util.HashSet;
import java.util.Set;

/** Used for visually indicating a View's affordance as a drop target. */
@RequiresApi(Build.VERSION_CODES.N)
final class DropAffordanceHighlighter {
    static final float FILL_OPACITY_INACTIVE = .2f;
    static final float FILL_OPACITY_ACTIVE = .65f;
    private static final float STROKE_OPACITY_INACTIVE = .4f;
    private static final int STROKE_OPACITY_ACTIVE = 1;
    private static final int STROKE_WIDTH_DP = 3;

    static final int DEFAULT_CORNER_RADIUS_DP = 16;
    private static final @ColorInt int DEFAULT_COLOR = 0xFF1A73E8;

    private static final int DEFAULT_GRAVITY = Gravity.FILL;

    private boolean mDragInProgress = false;

    final View mViewToHighlight;
    private final Predicate<ClipDescription> mEligibilityPredicate;
    /** Highlight for possible targets (light) */
    private final Drawable mHighlightAffordance;
    /** Highlight for the current target that will receive the content if the user lets go (dark) */
    private final Drawable mSelectedAffordance;

    private final Set<View> mViewsWithDragFocus = new HashSet<>();

    @Nullable
    private Drawable mOriginalForeground;
    private int mOriginalForegroundGravity = DEFAULT_GRAVITY;
    @Nullable
    BlendMode mOriginalForegroundTintBlendMode;
    @Nullable
    private ColorStateList mOriginalForegroundTintList;
    @Nullable
    private Mode mOriginalForegroundTintMode;

    DropAffordanceHighlighter(
            View viewToHighlight,
            Predicate<ClipDescription> eligibilityPredicate,
            @ColorInt int highlightColor,
            int cornerRadiusPx) {
        this.mViewToHighlight = viewToHighlight;
        this.mEligibilityPredicate = eligibilityPredicate;

        @ColorInt int  inactiveColor = colorWithOpacity(
                highlightColor, FILL_OPACITY_INACTIVE);
        @ColorInt int  activeColor = colorWithOpacity(
                highlightColor, FILL_OPACITY_ACTIVE);
        @ColorInt int  inactiveStrokeColor = colorWithOpacity(
                highlightColor, STROKE_OPACITY_INACTIVE);
        @ColorInt int  activeStrokeColor = colorWithOpacity(
                highlightColor, STROKE_OPACITY_ACTIVE);
        this.mHighlightAffordance = DropAffordanceHighlighter.makeDrawable(
                mViewToHighlight.getContext(), inactiveColor, inactiveStrokeColor, cornerRadiusPx);
        this.mSelectedAffordance = DropAffordanceHighlighter.makeDrawable(
                mViewToHighlight.getContext(), activeColor, activeStrokeColor, cornerRadiusPx);
    }

    /** Makes a new builder for highlighting the given view. */
    static @NonNull DropAffordanceHighlighter.Builder forView(
            @NonNull View viewToHighlight,
            @NonNull Predicate<ClipDescription> eligibilityPredicate) {
        Preconditions.checkNotNull(viewToHighlight);
        Preconditions.checkNotNull(eligibilityPredicate);
        return new DropAffordanceHighlighter.Builder(viewToHighlight, eligibilityPredicate);
    }

    /** Sets the highlight state based on the drag events. */
    boolean onDrag(@NonNull View reportingView, @NonNull DragEvent dragEvent) {
        int action = dragEvent.getAction();
        // ClipDescription is not present in ACTION_DRAG_ENDED.
        if (action != ACTION_DRAG_ENDED
                && !mEligibilityPredicate.test(dragEvent.getClipDescription())) {
            return false;
        }
        handleDragEvent(reportingView, action);
        // Return true on DRAG_STARTED, so we continue to receive events.
        // @see https://developer.android.com/reference/android/view/DragEvent#ACTION_DRAG_STARTED
        return action == ACTION_DRAG_STARTED;
    }

    private static @ColorInt int colorWithOpacity(@ColorInt int color, float opacity) {
        return (0x00ffffff & color) | (((int) (255 * opacity)) << 24);
    }

    private void handleDragEvent(View reportingView, int action) {
        switch (action) {
            case ACTION_DRAG_STARTED:
                // Multiple views can report DRAG_STARTED events, so we only care about one.
                if (!mDragInProgress) {
                    mDragInProgress = true;
                    backUpOriginalForeground();
                }
                break;
            case ACTION_DRAG_ENDED:
                // Multiple views can report DRAG_ENDED events, so we only care about one.
                if (mDragInProgress) {
                    mDragInProgress = false;
                    restoreOriginalForeground();
                    mViewsWithDragFocus.clear();
                }
                break;
            case ACTION_DRAG_ENTERED:
                mViewsWithDragFocus.add(reportingView);
                break;
            case ACTION_DRAG_EXITED:
                mViewsWithDragFocus.remove(reportingView);
                break;
        }

        if (mDragInProgress) {
            // A drag is in progress. We want the darker "selected" highlight as long as the user's
            // finger is over one of the relevant views, to indicate that they are over an active
            // drop target. Otherwise, we want the lighter highlight. See go/nested-drop for more
            // details.
            if (!mViewsWithDragFocus.isEmpty()) {
                mViewToHighlight.setForeground(mSelectedAffordance);
            } else {
                mViewToHighlight.setForeground(mHighlightAffordance);
            }
        }
    }

    private static GradientDrawable makeDrawable(
            Context context,
            @ColorInt int highlightColor,
            @ColorInt int strokeColor,
            int cornerRadiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(highlightColor);
        drawable.setStroke(dpToPx(context, STROKE_WIDTH_DP), strokeColor);
        drawable.setCornerRadius(cornerRadiusPx);
        return drawable;
    }

    static int dpToPx(Context context, int valueDp) {
        return round(max(0, valueDp) * context.getResources().getDisplayMetrics().density);
    }

    private void backUpOriginalForeground() {
        mOriginalForeground = mViewToHighlight.getForeground();
        mOriginalForegroundGravity = mViewToHighlight.getForegroundGravity();
        mOriginalForegroundTintList = mViewToHighlight.getForegroundTintList();
        mOriginalForegroundTintMode = mViewToHighlight.getForegroundTintMode();
        mViewToHighlight.setForegroundGravity(DEFAULT_GRAVITY);
        mViewToHighlight.setForegroundTintList(null);
        mViewToHighlight.setForegroundTintMode(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29BackUpImpl.backUp(this);
        }
    }

    private void restoreOriginalForeground() {
        mViewToHighlight.setForeground(mOriginalForeground);
        mViewToHighlight.setForegroundGravity(mOriginalForegroundGravity);
        mViewToHighlight.setForegroundTintList(mOriginalForegroundTintList);
        mViewToHighlight.setForegroundTintMode(mOriginalForegroundTintMode);
        mOriginalForeground = null;
        mOriginalForegroundGravity = DEFAULT_GRAVITY;
        mOriginalForegroundTintList = null;
        mOriginalForegroundTintMode = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29RestoreImpl.restore(this);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static class Api29BackUpImpl {
        @DoNotInline
        static void backUp(DropAffordanceHighlighter highlighter) {
            highlighter.mOriginalForegroundTintBlendMode =
                    highlighter.mViewToHighlight.getForegroundTintBlendMode();
            highlighter.mViewToHighlight.setForegroundTintBlendMode(null);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static class Api29RestoreImpl {
        @DoNotInline
        static void restore(DropAffordanceHighlighter highlighter) {
            highlighter.mViewToHighlight.setForegroundTintBlendMode(
                    highlighter.mOriginalForegroundTintBlendMode);
            highlighter.mOriginalForegroundTintBlendMode = null;
        }
    }

    /** Builder for {@link DropAffordanceHighlighter}. */
    @RequiresApi(Build.VERSION_CODES.N)
    static final class Builder {
        private final View mViewToHighlight;
        private final Predicate<ClipDescription> mEligibilityPredicate;
        private int mCornerRadiusPx;
        private @ColorInt int mHighlightColor;
        private boolean mHighlightColorHasBeenSupplied = false;

        Builder(View viewToHighlight, Predicate<ClipDescription> eligibilityPredicate) {
            this.mViewToHighlight = viewToHighlight;
            this.mEligibilityPredicate = eligibilityPredicate;
            mCornerRadiusPx = dpToPx(viewToHighlight.getContext(), DEFAULT_CORNER_RADIUS_DP);
        }

        /** Sets the color of the affordance highlight. */
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull Builder setHighlightColor(@ColorInt int highlightColor) {
            this.mHighlightColor = highlightColor;
            this.mHighlightColorHasBeenSupplied = true;
            return this;
        }

        /** Sets the corner radius (px) of the affordance highlight. */
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull Builder setHighlightCornerRadiusPx(int cornerRadiusPx) {
            this.mCornerRadiusPx = cornerRadiusPx;
            return this;
        }

        /** Creates the {@link androidx.draganddrop.DropAffordanceHighlighter}. */
        @NonNull DropAffordanceHighlighter build() {
            return new DropAffordanceHighlighter(
                    mViewToHighlight, mEligibilityPredicate, getHighlightColor(), mCornerRadiusPx);
        }

        private @ColorInt int getHighlightColor() {
            if (mHighlightColorHasBeenSupplied) {
                return mHighlightColor;
            }

            TypedArray values = mViewToHighlight.getContext().obtainStyledAttributes(
                    new int[]{androidx.appcompat.R.attr.colorAccent});
            try {
                return values.getColor(0, DEFAULT_COLOR);
            } finally {
                values.recycle();
            }
        }
    }
}
