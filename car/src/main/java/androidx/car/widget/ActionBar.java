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

package androidx.car.widget;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;

import java.lang.annotation.Retention;
import java.util.Locale;

import androidx.car.R;

/**
 * An actions panel with three distinctive zones:
 * <ul>
 * <li>Main control: located in the bottom center it shows a highlighted icon and a circular
 * progress bar.
 * <li>Secondary controls: these are displayed at the left and at the right of the main control.
 * <li>Overflow controls: these are displayed at the left and at the right of the secondary controls
 * (if the space allows) and on the additional space if the panel is expanded.
 * </ul>
 */
public class ActionBar extends RelativeLayout {
    private static final String TAG = "ActionBar";

    // ActionBar container
    private ViewGroup mActionBarWrapper;
    // Rows container
    private ViewGroup mRowsContainer;
    // All slots in this action bar where 0 is the bottom-start corner of the matrix, and
    // mNumColumns * nNumRows - 1 is the top-end corner
    private FrameLayout[] mSlots;
    /** Views to set in particular {@link SlotPosition}s */
    private final SparseArray<View> mFixedViews = new SparseArray<>();
    // View to be used for the expand/collapse action
    private @Nullable View mExpandCollapseView;
    // Default expand/collapse view to use one is not provided.
    private View mDefaultExpandCollapseView;
    // Number of rows in actual use. This is the number of extra rows that will be displayed when
    // the action bar is expanded
    private int mNumExtraRowsInUse;
    // Whether the action bar is expanded or not.
    private boolean mIsExpanded;
    // Views to accomodate in the slots.
    private @Nullable View[] mViews;
    // Number of columns of slots to use.
    private int mNumColumns;
    // Maximum number of rows to use.
    private int mNumRows;

    @Retention(SOURCE)
    @IntDef({SLOT_MAIN, SLOT_LEFT, SLOT_RIGHT, SLOT_EXPAND_COLLAPSE})
    public @interface SlotPosition {}

    /** Slot used for main actions {@link ActionBar}, usually at the bottom center */
    public static final int SLOT_MAIN = 0;
    /** Slot used to host 'move left', 'rewind', 'previous' or similar secondary actions,
     * usually at the left of the main action on the bottom row */
    public static final int SLOT_LEFT = 1;
    /** Slot used to host 'move right', 'fast-forward', 'next' or similar secondary actions,
     * usually at the right of the main action on the bottom row */
    public static final int SLOT_RIGHT = 2;
    /** Slot reserved for the expand/collapse button */
    public static final int SLOT_EXPAND_COLLAPSE = 3;

    // Minimum number of columns supported
    private static final int MIN_COLUMNS = 3;
    // Weight for the spacers used at the start and end of each slots row.
    private static final float SPACERS_WEIGHT = 0.5f;

    public ActionBar(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ActionBar(Context context, AttributeSet attrs, int defStyleAttrs) {
        super(context, attrs, defStyleAttrs);
        init(context, attrs, defStyleAttrs, 0);
    }

    public ActionBar(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttrs, defStyleRes);
        init(context, attrs, defStyleAttrs, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        inflate(context, R.layout.action_bar, this);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ActionBar,
                defStyleAttrs, defStyleRes);
        mNumColumns = Math.max(ta.getInteger(R.styleable.ActionBar_columns, MIN_COLUMNS),
                MIN_COLUMNS);
        ta.recycle();

        mActionBarWrapper = findViewById(R.id.action_bar_wrapper);
        mRowsContainer = findViewById(R.id.rows_container);
        mNumRows = mRowsContainer.getChildCount();
        mSlots = new FrameLayout[mNumColumns * mNumRows];

        for (int i = 0; i < mNumRows; i++) {
            // Slots are reserved in reverse order (first slots are in the bottom row)
            ViewGroup mRow = (ViewGroup) mRowsContainer.getChildAt(mNumRows - i - 1);
            // Inflate space on the left
            Space space = new Space(context);
            mRow.addView(space);
            space.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, SPACERS_WEIGHT));
            // Inflate necessary number of columns
            for (int j = 0; j < mNumColumns; j++) {
                int pos = i * mNumColumns + j;
                mSlots[pos] = (FrameLayout) inflate(context, R.layout.action_bar_slot, null);
                mSlots[pos].setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.MATCH_PARENT, 1f));
                mRow.addView(mSlots[pos]);
            }
            // Inflate space on the right
            space = new Space(context);
            mRow.addView(space);
            space.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, SPACERS_WEIGHT));
        }

        mDefaultExpandCollapseView = createIconButton(context, R.drawable.ic_overflow);
        mDefaultExpandCollapseView.setContentDescription(context.getString(
                R.string.action_bar_expand_collapse_button));
        mDefaultExpandCollapseView.setOnClickListener(v -> onExpandCollapse());
    }

    /**
     * Returns an index in the {@link #mSlots} array, given a well-known slot position.
     */
    private int getSlotIndex(@SlotPosition int slotPosition) {
        switch (slotPosition) {
            case SLOT_MAIN:
                return mNumColumns / 2;
            case SLOT_LEFT:
                return mNumColumns < 3 ? -1 : (mNumColumns / 2) - 1;
            case SLOT_RIGHT:
                return mNumColumns < 2 ? -1 : (mNumColumns / 2) + 1;
            case SLOT_EXPAND_COLLAPSE:
                return mNumColumns - 1;
            default:
                throw new IllegalArgumentException("Unknown position: " + slotPosition);
        }
    }

    /**
     * Sets or clears the view to be displayed at a particular position.
     *
     * @param view view to be displayed, or null to leave the position available.
     * @param slotPosition position to update
     */
    public void setView(@Nullable View view, @SlotPosition int slotPosition) {
        if (view != null) {
            mFixedViews.put(slotPosition, view);
        } else {
            mFixedViews.remove(slotPosition);
        }
        updateViewsLayout();
    }

    /**
     * Sets the view to use for the expand/collapse action. If not provided, a default
     * {@link ImageButton} will be used. The provided {@link View} should be able be able to display
     * changes in the "activated" state appropriately.
     *
     * @param view {@link View} to use for the expand/collapse action.
     */
    public void setExpandCollapseView(@NonNull View view) {
        mExpandCollapseView = view;
        mExpandCollapseView.setOnClickListener(v -> onExpandCollapse());
        updateViewsLayout();
    }

    private View getExpandCollapseView() {
        return mExpandCollapseView != null ? mExpandCollapseView : mDefaultExpandCollapseView;
    }

    private ImageButton createIconButton(Context context, @DrawableRes int iconResId) {
        ImageButton button = (ImageButton) inflate(context, R.layout.action_bar_button, null);
        Drawable icon = context.getDrawable(iconResId);
        button.setImageDrawable(icon);
        return button;
    }

    /**
     * Sets the views to include in each available slot of the action bar. Slots will be filled from
     * start to end (i.e: left to right) and from bottom to top. If more views than available slots
     * are provided, all extra views will be ignored.
     *
     * @param views array of views to include in each available slot.
     */
    public void setViews(@Nullable View[] views) {
        mViews = views;
        updateViewsLayout();
    }

    private void updateViewsLayout() {
        // Prepare an array of positions taken
        int totalSlots = mSlots.length;
        View[] slotViews = new View[totalSlots];

        // Take all known positions
        for (int i = 0; i < mFixedViews.size(); i++) {
            int index = getSlotIndex(mFixedViews.keyAt(i));
            if (index >= 0 && index < slotViews.length) {
                slotViews[index] = mFixedViews.valueAt(i);
            }
        }

        // Set all views using both the fixed and flexible positions
        int expandCollapseIndex = getSlotIndex(SLOT_EXPAND_COLLAPSE);
        int lastUsedIndex = 0;
        int viewsIndex = 0;
        for (int i = 0; i < totalSlots; i++) {
            View viewToUse = null;

            if (slotViews[i] != null) {
                // If there is a view assigned for this slot, use it.
                viewToUse = slotViews[i];
            } else if (i == expandCollapseIndex && mViews != null
                    && viewsIndex < mViews.length - 1) {
                // If this is the expand/collapse slot, use the corresponding view
                viewToUse = getExpandCollapseView();
            } else if (mViews != null && viewsIndex < mViews.length) {
                // Otherwise, if the slot is not reserved, and if we still have views to assign,
                // take one and assign it to this slot.
                viewToUse = mViews[viewsIndex];
                viewsIndex++;
            }
            setView(viewToUse, mSlots[i]);
            if (viewToUse != null) {
                lastUsedIndex = i;
            }
        }

        mNumExtraRowsInUse = lastUsedIndex / mNumColumns;
    }

    private void setView(@Nullable View view, FrameLayout container) {
        container.removeAllViews();
        if (view != null) {
            container.addView(view);
            container.setVisibility(VISIBLE);
            view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        } else {
            container.setVisibility(INVISIBLE);
        }
    }

    private void onExpandCollapse() {
        mIsExpanded = !mIsExpanded;
        mSlots[getSlotIndex(SLOT_EXPAND_COLLAPSE)].setActivated(mIsExpanded);

        int animationDuration = getContext().getResources().getInteger(mIsExpanded
                ? R.integer.car_action_bar_expand_anim_duration
                : R.integer.car_action_bar_collapse_anim_duration);
        TransitionSet set = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new Fade())
                .setDuration(animationDuration)
                .setInterpolator(new FastOutSlowInInterpolator());
        TransitionManager.beginDelayedTransition(mActionBarWrapper, set);
        for (int i = 0; i < mNumExtraRowsInUse; i++) {
            mRowsContainer.getChildAt(i).setVisibility(mIsExpanded ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Returns the view assigned to the given row and column, after layout.
     *
     * @param rowIdx row index from 0 being the top row, and {@link #mNumRows{ -1 being the bottom
     * row.
     * @param colIdx column index from 0 on start (left), to {@link #mNumColumns} on end (right)
     */
    @VisibleForTesting
    @Nullable
    View getViewAt(int rowIdx, int colIdx) {
        if (rowIdx < 0 || rowIdx > mRowsContainer.getChildCount()) {
            throw new IllegalArgumentException(String.format((Locale) null,
                    "Row index out of range (requested: %d, max: %d)",
                    rowIdx, mRowsContainer.getChildCount()));
        }
        if (colIdx < 0 || colIdx > mNumColumns) {
            throw new IllegalArgumentException(String.format((Locale) null,
                    "Column index out of range (requested: %d, max: %d)",
                    colIdx, mNumColumns));
        }
        FrameLayout slot = (FrameLayout) ((LinearLayout) mRowsContainer.getChildAt(rowIdx))
                .getChildAt(colIdx + 1);
        return slot.getChildCount() > 0 ? slot.getChildAt(0) : null;
    }
}
