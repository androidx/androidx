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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Picker column class used by {@link Picker}, defines a contiguous value ranges and associated
 * labels.  A PickerColumn has a minValue and maxValue to choose between.  The Picker column has
 * a current value.
 * The labels can be dynamically generated from value by {@link #setValueLabelFormat(String)} or
 * a list of static labels set by {@link #setValueStaticLabels(String[])}.
 */
public class PickerColumn implements Parcelable {

    private int mCurrentValue;
    private int mMinValue;
    private int mMaxValue;
    private String[] mStaticLabels;
    private String mValueFormat;

    public PickerColumn() {
    }

    public PickerColumn(Parcel source) {
        mValueFormat = source.readString();
        int count = source.readInt();
        if (count > 0) {
            mStaticLabels = new String[count];
            source.readStringArray(mStaticLabels);
        }
        mCurrentValue = source.readInt();
        mMinValue = source.readInt();
        mMaxValue = source.readInt();
    }

    /**
     * Set string format to display label for value, e.g. "%02d".  The string format is only
     * used when {@link #setValueStaticLabels(String[])} is not called.
     * @param valueFormat String format to display label for value.
     */
    public void setValueLabelFormat(String valueFormat) {
        mValueFormat = valueFormat;
    }

    /**
     * Return string format to display label for value, e.g. "%02d".
     * @return String format to display label for value.
     */
    public String getValueLabelFormat() {
        return mValueFormat;
    }

    /**
     * Set static labels for each value, minValue maps to labels[0], maxValue maps to
     * labels[labels.length - 1].
     * @param labels Static labels for each value between minValue and maxValue.
     */
    public void setValueStaticLabels(String[] labels) {
        mStaticLabels = labels;
    }

    /**
     * Get a label for value.  The label can be static ({@link #setValueStaticLabels(String[])} or
     * dynamically generated (@link {@link #setValueLabelFormat(String)}.
     * @param value Value between minValue and maxValue.
     * @return Label for the value.
     */
    public String getValueLabelAt(int value) {
        if (mStaticLabels == null) {
            return String.format(mValueFormat, value);
        }
        return mStaticLabels[value];
    }

    /**
     * Returns current value of the Column.
     * @return Current value of the Column.
     */
    public int getCurrentValue() {
        return mCurrentValue;
    }

    /**
     * Sets current value of the Column.
     * @return True if current value has changed.
     */
    public boolean setCurrentValue(int value) {
        if (mCurrentValue != value) {
            mCurrentValue = value;
            return true;
        }
        return false;
    }

    /**
     * Get total items count between minValue(inclusive) and maxValue (inclusive).
     * @return Total items count between minValue(inclusive) and maxValue (inclusive).
     */
    public int getItemsCount() {
        return mMaxValue - mMinValue + 1;
    }

    /**
     * Returns minimal value (inclusive) of the Column.
     * @return Minimal value (inclusive) of the Column.
     */
    public int getMinValue() {
        return mMinValue;
    }

    /**
     * Returns maximum value (inclusive) of the Column.
     * @return Maximum value (inclusive) of the Column.
     */
    public int getMaxValue() {
        return mMaxValue;
    }

    /**
     * Sets minimal value (inclusive) of the Column.
     * @param minValue New minimal value to set.
     * @return True if minimal value changes.
     */
    public boolean setMinValue(int minValue) {
        if (minValue != mMinValue) {
            mMinValue = minValue;
            return true;
        }
        return false;
    }

    /**
     * Sets maximum value (inclusive) of the Column.
     * @param maxValue New maximum value to set.
     * @return True if maximum value changes.
     */
    public boolean setMaxValue(int maxValue) {
        if (maxValue != mMaxValue) {
            mMaxValue = maxValue;
            return true;
        }
        return false;
    }

    public static Parcelable.Creator<PickerColumn>
            CREATOR = new Parcelable.Creator<PickerColumn>() {

                @Override
                public PickerColumn createFromParcel(Parcel source) {
                    return new PickerColumn(source);
                }

                @Override
                public PickerColumn[] newArray(int size) {
                    return new PickerColumn[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mValueFormat);
        if (mStaticLabels != null) {
            dest.writeInt(mStaticLabels.length);
            dest.writeStringArray(mStaticLabels);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mCurrentValue);
        dest.writeInt(mMinValue);
        dest.writeInt(mMaxValue);
    }

}
