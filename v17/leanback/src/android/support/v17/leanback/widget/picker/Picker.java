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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Picker is a widget showing multiple customized {@link PickerColumn}s.  The PickerColumns are
 * initialized in {@link #setColumns(ArrayList)}.  You could only set columns once and not able to
 * add or remove Column later.  Call {@link #updateAdapter(int)} if the column value range or labels
 * change.  Call {@link #updateValue(int, int, boolean)} to update the current value of
 * PickerColumn.
 * <p>
 * Picker has two states and will change height:
 * <li>{@link #isExpanded()} is true: Picker shows typically three items vertically (see
 * {@link #getVisiblePickerItemsInExpand()}}. Columns other than {@link #getActiveColumn()} still
 * shows one item if the Picker is focused.  On a touch screen device, the Picker will not get
 * focus so it always show three items on all columns.   On a non-touch device (a TV), the Picker
 * will show three items only on currently activated column.  If the Picker has focus, it will
 * intercept DPAD directions and select activated column.
 * <li>{@link #isExpanded()} is false: Picker shows one item vertically (see
 * {@link #getVisiblePickerItems()}) on all columns.  The size of Picker shrinks.
 * <li> The expand mode will be toggled if the Picker has focus and {@link #isToggleExpandOnClick()}
 * is true.
 * Summarize Typically use cases:
 * <li> On a touch screen based device,  the Picker focusableInTouchMode=false.  It won't get focus,
 * it wont toggle expand mode on click or touch, should call {@link #setExpanded(boolean)} with
 * true, so that user always sees three items on all columns.
 * <li> On a TV: the Picker focusable=true.  It will get focus and toggle into expand mode when user
 * clicks on it, toggle can be disabled by {@link #setToggleExpandOnClick(boolean)} with false.
 * Only the activated column shows multiple items and the activated column is selected by DPAD left
 * or right.
 */
public class Picker extends FrameLayout {

    public interface PickerValueListener {
        public void onValueChanged(Picker picker, int column);
    }

    private String mSeparator;
    private ViewGroup mRootView;
    private ViewGroup mPickerView;
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
    private boolean mExpanded;
    private float mVisibleItemsInExpand = 3;
    private float mVisibleItems = 1;
    private int mActivatedColumn = 0;
    private boolean mToggleExpandOnClick = true;

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
     * Creates a Picker widget.
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public Picker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // On TV, Picker is focusable and intercept Click / DPAD direction keys.  We dont want any
        // child to get focus.  On touch screen, Picker is not focusable.
        setFocusable(true);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        // Make it enabled and clickable to receive Click event.
        setEnabled(true);
        setClickable(true);

        mFocusedAlpha = 1f; //getFloat(R.dimen.list_item_selected_title_text_alpha);
        mUnfocusedAlpha = 1f; //getFloat(R.dimen.list_item_unselected_text_alpha);
        mVisibleColumnAlpha = 0.5f; //getFloat(R.dimen.picker_item_visible_column_item_alpha);
        mInvisibleColumnAlpha = 0f; //getFloat(R.dimen.picker_item_invisible_column_item_alpha);

        mAlphaAnimDuration = 200; // mContext.getResources().getInteger(R.integer.dialog_animation_duration);

        mDecelerateInterpolator = new DecelerateInterpolator(2.5F);
        mAccelerateInterpolator = new AccelerateInterpolator(2.5F);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        mRootView = (ViewGroup) inflater.inflate(getRootLayoutId(), this, true);
        mPickerView = (ViewGroup) mRootView.findViewById(getPickerId());

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
            // we dont want VerticalGridView to receive focus.
            columnView.setFocusableInTouchMode(false);
            columnView.setFocusable(false);
            updateColumnSize(columnView);
            // always center aligned, not aligning selected item on top/bottom edge.
            columnView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
            // Width is dynamic, so has fixed size is false.
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

    private void updateColumnAlpha(int colIndex, boolean animate) {
        VerticalGridView column = mColumnViews.get(colIndex);

        int selected = column.getSelectedPosition();
        View item;

        for (int i = 0; i < column.getAdapter().getItemCount(); i++) {
            item = column.getLayoutManager().findViewByPosition(i);
            if (item != null) {
                setOrAnimateAlpha(item, (selected == i), colIndex, animate);
            }
        }
    }

    private void setOrAnimateAlpha(View view, boolean selected, int colIndex,
            boolean animate) {
        boolean columnShownAsActivated = colIndex == mActivatedColumn || !isFocused();
        if (selected) {
            // set alpha for main item (selected) in the column
            if (columnShownAsActivated) {
                setOrAnimateAlpha(view, animate, mFocusedAlpha, -1, mDecelerateInterpolator);
            } else {
                setOrAnimateAlpha(view, animate, mUnfocusedAlpha, -1,  mDecelerateInterpolator);
            }
        } else {
            // set alpha for remaining items in the column
            if (columnShownAsActivated) {
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
                    mColIndex, false);
        }

        public void setData(PickerColumn data) {
            mData = data;
            notifyDataSetChanged();
        }

        public int getItemCount() {
            return mData == null ? 0 : mData.getItemsCount();
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
            updateColumnAlpha(colIndex, true);
            if (child != null) {
                int newValue = mColumns.get(colIndex).getMinValue() + position;
                onColumnValueChange(colIndex, newValue);
            }
        }

    };

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (isExpanded()) {
            final int keyCode = event.getKeyCode();
            switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL?
                            keyCode == KeyEvent.KEYCODE_DPAD_LEFT :
                            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ) {
                        if (mActivatedColumn < getColumnsCount() - 1) {
                            setActiveColumn(mActivatedColumn + 1);
                        }
                    } else {
                        if (mActivatedColumn > 0) {
                            setActiveColumn(mActivatedColumn - 1);
                        }
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (event.getAction() == KeyEvent.ACTION_DOWN && mActivatedColumn >= 0) {
                    VerticalGridView gridView = mColumnViews.get(mActivatedColumn);
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        int newPosition = gridView.getSelectedPosition() - 1;
                        if (newPosition >= 0) {
                            gridView.setSelectedPositionSmooth(newPosition);
                        }
                    } else {
                        int newPosition = gridView.getSelectedPosition() + 1;
                        if (newPosition < gridView.getAdapter().getItemCount()) {
                            gridView.setSelectedPositionSmooth(newPosition);
                        }
                    }
                }
                break;
            default:
                return super.dispatchKeyEvent(event);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Classes extending {@link Picker} can choose to override this method to
     * supply the {@link Picker}'s column's single item height in pixels.
     */
    protected int getPickerItemHeightPixels() {
        return getContext().getResources().getDimensionPixelSize(R.dimen.picker_item_height);
    }

    private void updateColumnSize() {
        for (int i = 0; i < getColumnsCount(); i++) {
            updateColumnSize(mColumnViews.get(i));
        }
    }

    private void updateColumnSize(VerticalGridView columnView) {
        ViewGroup.LayoutParams lp = columnView.getLayoutParams();
        lp.height = (int) (getPickerItemHeightPixels() * (isExpanded() ?
                getVisiblePickerItemsInExpand() : getVisiblePickerItems()));
        columnView.setLayoutParams(lp);
    }

    /**
     * Returns number of visible items showing in a column when it's expanded, it's 3 by default.
     * @return Number of visible items showing in a column when it's expanded.
     */
    public float getVisiblePickerItemsInExpand() {
        return mVisibleItemsInExpand;
    }

    /**
     * Change number of visible items showing in a column when it's expanded.
     * @param visiblePickerItems Number of visible items showing in a column when it's expanded.
     */
    public void setVisiblePickerItemsInExpand(float visiblePickerItems) {
        if (visiblePickerItems <= 0) {
            throw new IllegalArgumentException();
        }
        if (mVisibleItemsInExpand != visiblePickerItems) {
            mVisibleItemsInExpand = visiblePickerItems;
            if (isExpanded()) {
                updateColumnSize();
            }
        }
    }

    /**
     * Returns number of visible items showing in a column when it's not expanded, it's 1 by
     * default.
     * @return Number of visible items showing in a column when it's not expanded.
     */
    public float getVisiblePickerItems() {
        return 1;
    }

    /**
     * Change number of visible items showing in a column when it's not expanded, it's 1 by default.
     * @param pickerItems Number of visible items showing in a column when it's not expanded.
     */
    public void setVisiblePickerItems(float pickerItems) {
        if (pickerItems <= 0) {
            throw new IllegalArgumentException();
        }
        if (mVisibleItems != pickerItems) {
            mVisibleItems = pickerItems;
            if (!isExpanded()) {
                updateColumnSize();
            }
        }
    }

    /**
     * Change expanded state of Picker, the height LayoutParams will be changed.
     * @see #getVisiblePickerItemsInExpand()
     * @see #getVisiblePickerItems()
     * @param expanded New expanded state of Picker.
     */
    public void setExpanded(boolean expanded) {
        if (mExpanded != expanded) {
            mExpanded = expanded;
            updateColumnSize();
        }
    }

    /**
     * Returns true if the Picker is currently expanded, false otherwise.
     * @return True if the Picker is currently expanded, false otherwise.
     */
    public boolean isExpanded() {
        return mExpanded;
    }

    /**
     * Change current activated column.  Shows multiple items on activate column if Picker has
     * focus. Show multiple items on all column if Picker has no focus (e.g. a Touchscreen
     * screen).
     * @param columnIndex Index of column to activate.
     */
    public void setActiveColumn(int columnIndex) {
        if (mActivatedColumn != columnIndex) {
            mActivatedColumn = columnIndex;
            for (int i = 0; i < mColumnViews.size(); i++) {
                updateColumnAlpha(i, true);
            }
        }
    }

    /**
     * Get current activated column index.
     * @return Current activated column index.
     */
    public int getActiveColumn() {
        return mActivatedColumn;
    }

    /**
     * Enable or disable toggle on click when Picker has focus.
     * @param toggleExpandOnClick True to enable toggle on click when Picker has focus, false
     * otherwise.
     */
    public void setToggleExpandOnClick(boolean toggleExpandOnClick) {
        mToggleExpandOnClick = toggleExpandOnClick;
    }

    /**
     * Returns true if toggle on click is enabled when Picker has focus, false otherwise.
     * @return True if toggle on click is enabled when Picker has focus, false otherwise.
     */
    public boolean isToggleExpandOnClick() {
        return mToggleExpandOnClick;
    }

    @Override
    public boolean performClick() {
        if (isFocused() && isToggleExpandOnClick()) {
            setExpanded(!isExpanded());
            super.performClick();
            return true;
        }
        return super.performClick();
    }

}
