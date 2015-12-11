/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v17.leanback.widget.picker;

import android.content.Context;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.OnChildViewHolderSelectedListener;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Picker is a widget showing multiple customized {@link PickerColumn}s.  The PickerColumns are
 * initialized in {@link #setColumns(ArrayList)}.  You could only set columns once and not able to
 * add or remove Column later.  Call {@link #updateAdapter(int)} if the column value range or labels
 * change.  Call {@link #updateValue(int, int, boolean)} to update the current value of
 * PickerColumn.
 */
public class Picker extends FrameLayout {

    public interface PickerValueListener {
        public void onValueChanged(Picker picker, int column);
    }

    private String mSeparator;
    private ViewGroup mRootView;
    private ChildFocusAwareLinearLayout mPickerView;
    private List<VerticalGridView> mColumnViews = new ArrayList<VerticalGridView>();
    private ArrayList<PickerColumn> mColumns;

    private float mUnfocusedAlpha;
    private float mFocusedAlpha;
    private float mVisibleColumnAlpha;
    private float mInvisibleColumnAlpha;
    private int mAlphaAnimDuration;
    private Interpolator mDecelerateInterpolator;
    private Interpolator mAccelerateInterpolator;
    private ArrayList<PickerValueListener> mListeners;

    /**
     * Classes extending {@link Picker} can choose to override this method to
     * supply the separator string
     */
    protected String getSeparator() {
        return mSeparator;
    }

    /**
     * Classes extending {@link Picker} can choose to override this method to
     * supply the {@link Picker}'s root layout id
     */
    protected int getRootLayoutId() {
        return R.layout.lb_picker;
    }

    /**
     * Classes extending {@link Picker} can choose to override this method to
     * supply the {@link Picker}'s id from within the layout provided by
     * {@link Picker#getRootLayoutId()}
     */
    protected int getPickerId() {
        return R.id.picker;
    }

    /**
     * Classes extending {@link Picker} can choose to override this method to
     * supply the {@link Picker}'s separator's layout id
     */
    protected int getPickerSeparatorLayoutId() {
        return R.layout.lb_picker_separator;
    }

    /**
     * Classes extending {@link Picker} can choose to override this method to
     * supply the {@link Picker}'s item's layout id
     */
    protected int getPickerItemLayoutId() {
        return R.layout.lb_picker_item;
    }

    /**
     * Classes extending {@link Picker} can choose to override this method to
     * supply the {@link Picker}'s item's {@link TextView}'s id from within the
     * layout provided by {@link Picker#getPickerItemLayoutId()} or 0 if the
     * layout provided by {@link Picker#getPickerItemLayoutId()} is a {link
     * TextView}.
     */
    protected int getPickerItemTextViewId() {
        return 0;
    }

    /**
     * Classes extending {@link Picker} can choose to override this method to
     * supply the {@link Picker}'s column's height in pixels.
     */
    protected int getPickerColumnHeightPixels() {
        return getContext().getResources().getDimensionPixelSize(R.dimen.picker_column_height);
    }

    /**
     * Creates a Picker widget.
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public Picker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mFocusedAlpha = 1f; //getFloat(R.dimen.list_item_selected_title_text_alpha);
        mUnfocusedAlpha = 1f; //getFloat(R.dimen.list_item_unselected_text_alpha);
        mVisibleColumnAlpha = 0.5f; //getFloat(R.dimen.picker_item_visible_column_item_alpha);
        mInvisibleColumnAlpha = 0f; //getFloat(R.dimen.picker_item_invisible_column_item_alpha);

        mAlphaAnimDuration = 200; // mContext.getResources().getInteger(R.integer.dialog_animation_duration);

        mDecelerateInterpolator = new DecelerateInterpolator(2.5F);
        mAccelerateInterpolator = new AccelerateInterpolator(2.5F);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        mRootView = (ViewGroup) inflater.inflate(getRootLayoutId(), this, true);
        mPickerView = (ChildFocusAwareLinearLayout) mRootView.findViewById(getPickerId());

        mPickerView.setOnChildFocusListener(mColumnGainFocusListener);

    }

    /**
     * Get nth PickerColumn.
     * @param colIndex  Index of PickerColumn.
     * @return PickerColumn at colIndex or null if {@link #setColumns(ArrayList)} is not called yet.
     */
    public PickerColumn getColumnAt(int colIndex) {
        if (mColumns == null) {
            return null;
        }
        return mColumns.get(colIndex);
    }

    /**
     * Get number of PickerColumns.
     * @return Number of PickerColumns or 0 if {@link #setColumns(ArrayList)} is not called yet.
     */
    public int getColumnsCount() {
        if (mColumns == null) {
            return 0;
        }
        return mColumns.size();
    }

    /**
     * Set columns and create Views.  The method is only allowed to be called once.
     * @param columns PickerColumns to be shown in the Picker.
     */
    public void setColumns(ArrayList<PickerColumn> columns) {
        if (mColumns != null) {
            throw new IllegalStateException("columns can only be initialized once");
        }
        mColumns = columns;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int totalCol = getColumnsCount();
        for (int i = 0; i < totalCol; i++) {
            final int colIndex = i;
            final VerticalGridView columnView = (VerticalGridView) inflater.inflate(
                    R.layout.lb_picker_column, mPickerView, false);
            ViewGroup.LayoutParams lp = columnView.getLayoutParams();
            lp.height = getPickerColumnHeightPixels();
            columnView.setLayoutParams(lp);
            columnView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
            columnView.setHasFixedSize(false);
            mColumnViews.add(columnView);

            // add view to root
            mPickerView.addView(columnView);

            // add a separator if not the last element
            if (i != totalCol - 1 && getSeparator() != null) {
                TextView separator = (TextView) inflater.inflate(
                        getPickerSeparatorLayoutId(), mPickerView, false);
                separator.setText(getSeparator());
                mPickerView.addView(separator);
            }

            columnView.setAdapter(new PickerScrollArrayAdapter(getContext(),
                    getPickerItemLayoutId(), getPickerItemTextViewId(), colIndex));
            columnView.setOnChildViewHolderSelectedListener(mColumnChangeListener);
        }
    }

    /**
     * When column labels change or column range changes, call this function to re-populate the
     * selection list.
     * @param columnIndex Index of column to update.
     */
    public void updateAdapter(int columnIndex) {
        VerticalGridView columnView = mColumnViews.get(columnIndex);
        PickerScrollArrayAdapter adapter = (PickerScrollArrayAdapter) columnView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Manually set current value of a column.  The function will update UI and notify listeners.
     * @param columnIndex Index of column to update.
     * @param value New value of the column.
     * @param runAnimation True to scroll to the value or false otherwise.
     */
    public void updateValue(int columnIndex, int value, boolean runAnimation) {
        if (mColumns.get(columnIndex).setCurrentValue(value)) {
            notifyValueChanged(columnIndex);
            VerticalGridView columnView = mColumnViews.get(columnIndex);
            if (columnView != null) {
                int position = value - mColumns.get(columnIndex).getMinValue();
                if (runAnimation) {
                    columnView.setSelectedPositionSmooth(position);
                } else {
                    columnView.setSelectedPosition(position);
                }
            }
        }
    }

    private void notifyValueChanged(int columnIndex) {
        if (mListeners != null) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onValueChanged(this, columnIndex);
            }
        }
    }

    public void addPickerValueListener(PickerValueListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<Picker.PickerValueListener>();
        }
        mListeners.add(listener);
    }

    public void removePickerValueListener(PickerValueListener listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }

    private void updateColumnAlpha(VerticalGridView column, boolean animateAlpha) {
        if (column == null) {
            return;
        }

        int selected = column.getSelectedPosition();
        View item;
        boolean focused = column.hasFocus();

        for (int i = 0; i < column.getAdapter().getItemCount(); i++) {
            item = column.getLayoutManager().findViewByPosition(i);
            if (item != null) {
                setOrAnimateAlpha(item, (selected == i), focused, animateAlpha);
            }
        }
    }

    private void setOrAnimateAlpha(View view, boolean selected, boolean focused, boolean animate) {
        if (selected) {
            // set alpha for main item (selected) in the column
            if (focused) {
                setOrAnimateAlpha(view, animate, mFocusedAlpha, -1, mDecelerateInterpolator);
            } else {
                setOrAnimateAlpha(view, animate, mUnfocusedAlpha, -1,  mDecelerateInterpolator);
            }
        } else {
            // set alpha for remaining items in the column
            if (focused) {
                setOrAnimateAlpha(view, animate, mVisibleColumnAlpha, -1, mDecelerateInterpolator);
            } else {
                setOrAnimateAlpha(view, animate, mInvisibleColumnAlpha, -1,
                        mDecelerateInterpolator);
            }
        }
    }

    private void setOrAnimateAlpha(View view, boolean animate, float destAlpha, float startAlpha,
            Interpolator interpolator) {
        view.animate().cancel();
        if (!animate) {
            view.setAlpha(destAlpha);
        } else {
            if (startAlpha >= 0.0f) {
                // set a start alpha
                view.setAlpha(startAlpha);
            }
            view.animate().alpha(destAlpha)
                    .setDuration(mAlphaAnimDuration).setInterpolator(interpolator)
                    .start();
        }
    }

    /**
     * Classes extending {@link Picker} can override this function to supply the
     * behavior when a list has been scrolled.  Subclass may call {@link #updateValue(int, int,
     * boolean)} and or {@link #updateAdapter(int)}.  Subclass should not directly call
     * {@link PickerColumn#setCurrentValue(int)} which does not update internal state or notify
     * listeners.
     * @param columnIndex index of which column was changed.
     * @param newValue A new value desired to be set on the column.
     */
    public void onColumnValueChange(int columnIndex, int newValue) {
        if (mColumns.get(columnIndex).setCurrentValue(newValue)) {
            notifyValueChanged(columnIndex);
        }
    }

    private float getFloat(int resourceId) {
        TypedValue buffer = new TypedValue();
        getContext().getResources().getValue(resourceId, buffer, true);
        return buffer.getFloat();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        ViewHolder(View v, TextView textView) {
            super(v);
            this.textView = textView;
        }
    }

    class PickerScrollArrayAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final int mResource;
        private final int mColIndex;
        private final int mTextViewResourceId;
        private PickerColumn mData;

        PickerScrollArrayAdapter(Context context, int resource, int textViewResourceId,
                int colIndex) {
            mResource = resource;
            mColIndex = colIndex;
            mTextViewResourceId = textViewResourceId;
            mData = mColumns.get(mColIndex);
        }

        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View v = inflater.inflate(mResource, parent, false);
            TextView textView;
            if (mTextViewResourceId != 0) {
                textView = (TextView) v.findViewById(mTextViewResourceId);
            } else {
                textView = (TextView) v;
            }
            ViewHolder vh = new ViewHolder(v, textView);
            return vh;
        }

        public void onBindViewHolder(ViewHolder holder, int position) {
            if (holder.textView != null && mData != null) {
                holder.textView.setText(mData.getValueLabelAt(mData.getMinValue() + position));
            }
            setOrAnimateAlpha(holder.itemView,
                    (mColumnViews.get(mColIndex).getSelectedPosition() == position),
                    mColumnViews.get(mColIndex).hasFocus(), false);
        }

        public void setData(PickerColumn data) {
            mData = data;
            notifyDataSetChanged();
        }

        public int getItemCount() {
            return mData == null ? 0 : mData.getItemsCount();
        }
    }

    /**
     * Interface for managing child focus in a ChildFocusAwareLinearLayout.
     */
    interface OnChildFocusListener {
        public boolean onPreRequestChildFocus(View child, View focused);
        public void onRequestChildFocus(View child, View focused);
    }

    static class ChildFocusAwareLinearLayout extends LinearLayout {


        private OnChildFocusListener mOnChildFocusListener;

        public void setOnChildFocusListener(OnChildFocusListener listener) {
            mOnChildFocusListener = listener;
        }

        public OnChildFocusListener getOnChildFocusListener() {
            return mOnChildFocusListener;
        }

        public ChildFocusAwareLinearLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void requestChildFocus(View child, View focused) {
            boolean preReturnedTrue = false;
            if (mOnChildFocusListener != null) {
                preReturnedTrue = mOnChildFocusListener.onPreRequestChildFocus(child, focused);
            }
            super.requestChildFocus(child, focused);
            if (preReturnedTrue && mOnChildFocusListener != null) {
                mOnChildFocusListener.onRequestChildFocus(child, focused);
            }
        }
    }

    private final OnChildViewHolderSelectedListener mColumnChangeListener = new
            OnChildViewHolderSelectedListener() {

        @Override
        public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child,
                int position, int subposition) {
            PickerScrollArrayAdapter pickerScrollArrayAdapter = (PickerScrollArrayAdapter) parent
                    .getAdapter();

            int colIndex = mColumnViews.indexOf(parent);
            updateColumnAlpha((VerticalGridView) parent, parent.hasFocus());
            if (child != null) {
                int newValue = mColumns.get(colIndex).getMinValue() + position;
                onColumnValueChange(colIndex, newValue);
            }
        }

    };

    private final OnChildFocusListener mColumnGainFocusListener = new OnChildFocusListener() {
        @Override
        public boolean onPreRequestChildFocus(View child, View focused) {
            return true;
        }

        @Override
        public void onRequestChildFocus(View child, View focused) {
            for (int i = 0; i < mColumnViews.size(); i++) {
                VerticalGridView column = mColumnViews.get(i);
                updateColumnAlpha(column, column.hasFocus());
            }
        }
    };

}
