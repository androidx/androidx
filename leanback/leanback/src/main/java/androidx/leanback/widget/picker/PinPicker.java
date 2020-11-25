/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.leanback.widget.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.core.view.ViewCompat;
import androidx.leanback.R;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Picker} subclass for allowing the user to enter a numerical PIN. The column count can be
 * customized, and defaults to 4.
 *
 * {@link R.attr#columnCount}
 */
public class PinPicker extends Picker {

    private static final int DEFAULT_COLUMN_COUNT = 4;

    public PinPicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.pinPickerStyle);
    }

    @SuppressLint("CustomViewStyleable")
    public PinPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.lbPinPicker, defStyleAttr, 0);
        ViewCompat.saveAttributeDataForStyleable(
                this, context, R.styleable.lbPinPicker, attrs, a, defStyleAttr, 0);
        try {
            setSeparator(" ");
            setNumberOfColumns(a.getInt(R.styleable.lbPinPicker_columnCount, DEFAULT_COLUMN_COUNT));
        } finally {
            a.recycle();
        }
    }

    /**
     * Sets the number of columns for entering the PIN.
     *
     * @param count how many columns to display.
     */
    public void setNumberOfColumns(int count) {
        final List<PickerColumn> columns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final PickerColumn column = new PickerColumn();
            column.setMinValue(0);
            column.setMaxValue(9);
            column.setLabelFormat("%d");
            columns.add(column);
        }
        setColumns(columns);
    }

    @Override
    public boolean performClick() {
        final int column = getSelectedColumn();
        if (column == getColumnsCount() - 1) {
            return super.performClick();
        } else {
            setSelectedColumn(column + 1);
            return false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_UP
                && keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            final int selectedColumn = getSelectedColumn();
            setColumnValue(selectedColumn, keyCode - KeyEvent.KEYCODE_0, false /* animate */);
            performClick();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Returns the PIN that the user has entered.
     *
     * @return Currently entered PIN
     */
    public String getPin() {
        final StringBuilder pin = new StringBuilder();
        final int columnsCount = getColumnsCount();
        for (int i = 0; i < columnsCount; i++) {
            pin.append(Integer.toString(getColumnAt(i).getCurrentValue()));
        }
        return pin.toString();
    }

    /**
     * Resets all columns and selects the first one.
     */
    public void resetPin() {
        final int columnsCount = getColumnsCount();
        for (int i = 0; i < columnsCount; i++) {
            setColumnValue(i, 0, false /* animate */);
        }
        setSelectedColumn(0);
    }
}
