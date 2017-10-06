/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.preference;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * A version of {@link ListPreference} that presents the options in a
 * drop down menu rather than a dialog.
 */
public class DropDownPreference extends ListPreference {

    private final Context mContext;
    private final ArrayAdapter mAdapter;

    private Spinner mSpinner;

    public DropDownPreference(Context context) {
        this(context, null);
    }

    public DropDownPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dropdownPreferenceStyle);
    }

    public DropDownPreference(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public DropDownPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        mAdapter = createAdapter();

        updateEntries();
    }

    @Override
    protected void onClick() {
        mSpinner.performClick();
    }

    @Override
    public void setEntries(@NonNull CharSequence[] entries) {
        super.setEntries(entries);
        updateEntries();
    }


    /**
     * By default, this class uses a simple {@link android.widget.ArrayAdapter}. But if you need
     * a more complicated {@link android.widget.ArrayAdapter}, this method can be overridden to
     * create a custom one.
     * <p> Note: This method is called from the constructor. So, overridden methods will get called
     * before any subclass initialization.
     *
     * @return The custom {@link android.widget.ArrayAdapter} that needs to be used with this class.
     */
    protected ArrayAdapter createAdapter() {
        return new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_dropdown_item);
    }

    private void updateEntries() {
        mAdapter.clear();
        if (getEntries() != null) {
            for (CharSequence c : getEntries()) {
                mAdapter.add(c.toString());
            }
        }
    }

    @Override
    public void setValueIndex(int index) {
        setValue(getEntryValues()[index].toString());
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public int findSpinnerIndexOfValue(String value) {
        CharSequence[] entryValues = getEntryValues();
        if (value != null && entryValues != null) {
            for (int i = entryValues.length - 1; i >= 0; i--) {
                if (entryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return Spinner.INVALID_POSITION;
    }

    @Override
    protected void notifyChanged() {
        super.notifyChanged();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        mSpinner = (Spinner) view.itemView.findViewById(R.id.spinner);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(mItemSelectedListener);
        mSpinner.setSelection(findSpinnerIndexOfValue(getValue()));
        super.onBindViewHolder(view);
    }

    private final OnItemSelectedListener mItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
            if (position >= 0) {
                String value = getEntryValues()[position].toString();
                if (!value.equals(getValue()) && callChangeListener(value)) {
                    setValue(value);
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // noop
        }
    };
}

