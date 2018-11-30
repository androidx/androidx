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

package androidx.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.TypedArrayUtils;

/**
 * A {@link Preference} that displays a list of entries as a dialog.
 *
 * <p>This preference saves a string value. This string will be the value from the
 * {@link #setEntryValues(CharSequence[])} array.
 *
 * @attr name android:entries
 * @attr name android:entryValues
 */
public class ListPreference extends DialogPreference {
    private static final String TAG = "ListPreference";
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private String mValue;
    private String mSummary;
    private boolean mValueSet;

    @SuppressLint("RestrictedApi")
    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ListPreference, defStyleAttr, defStyleRes);

        mEntries = TypedArrayUtils.getTextArray(a, R.styleable.ListPreference_entries,
                R.styleable.ListPreference_android_entries);

        mEntryValues = TypedArrayUtils.getTextArray(a, R.styleable.ListPreference_entryValues,
                R.styleable.ListPreference_android_entryValues);

        if (TypedArrayUtils.getBoolean(a, R.styleable.ListPreference_useSimpleSummaryProvider,
                R.styleable.ListPreference_useSimpleSummaryProvider, false)) {
            setSummaryProvider(SimpleSummaryProvider.getInstance());
        }

        a.recycle();

        //Retrieve the Preference summary attribute since it's private in the Preference class.
        a = context.obtainStyledAttributes(attrs,
                R.styleable.Preference, defStyleAttr, defStyleRes);

        mSummary = TypedArrayUtils.getString(a, R.styleable.Preference_summary,
                R.styleable.Preference_android_summary);

        a.recycle();
    }

    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressLint("RestrictedApi")
    public ListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle));
    }

    public ListPreference(Context context) {
        this(context, null);
    }

    /**
     * Sets the human-readable entries to be shown in the list. This will be shown in subsequent
     * dialogs.
     *
     * <p>Each entry must have a corresponding index in {@link #setEntryValues(CharSequence[])}.
     *
     * @param entries The entries
     * @see #setEntryValues(CharSequence[])
     */
    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
    }

    /**
     * @param entriesResId The entries array as a resource
     * @see #setEntries(CharSequence[])
     */
    public void setEntries(@ArrayRes int entriesResId) {
        setEntries(getContext().getResources().getTextArray(entriesResId));
    }

    /**
     * The list of entries to be shown in the list in subsequent dialogs.
     *
     * @return The list as an array
     */
    public CharSequence[] getEntries() {
        return mEntries;
    }

    /**
     * The array to find the value to save for a preference when an entry from entries is
     * selected. If a user clicks on the second item in entries, the second item in this array
     * will be saved to the preference.
     *
     * @param entryValues The array to be used as values to save for the preference
     */
    public void setEntryValues(CharSequence[] entryValues) {
        mEntryValues = entryValues;
    }

    /**
     * @param entryValuesResId The entry values array as a resource
     * @see #setEntryValues(CharSequence[])
     */
    public void setEntryValues(@ArrayRes int entryValuesResId) {
        setEntryValues(getContext().getResources().getTextArray(entryValuesResId));
    }

    /**
     * Returns the array of values to be saved for the preference.
     *
     * @return The array of values
     */
    public CharSequence[] getEntryValues() {
        return mEntryValues;
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        if (summary == null && mSummary != null) {
            mSummary = null;
        } else if (summary != null && !summary.equals(mSummary)) {
            mSummary = summary.toString();
        }
    }

    @Override
    public CharSequence getSummary() {
        if (getSummaryProvider() != null) {
            return getSummaryProvider().provideSummary(this);
        }
        final CharSequence entry = getEntry();
        CharSequence summary = super.getSummary();
        if (mSummary == null) {
            return summary;
        }
        String formattedString = String.format(mSummary, entry == null ? "" : entry);
        if (formattedString.contentEquals(summary)) {
            return summary;
        }
        Log.w(TAG,
                "Setting a summary with a String formatting marker is no longer supported."
                        + " You should use a SummaryProvider instead.");
        return formattedString;
    }

    /**
     * Sets the value of the key. This should be one of the entries in {@link #getEntryValues()}.
     *
     * @param value The value to set for the key
     */
    public void setValue(String value) {
        // Always persist/notify the first time.
        final boolean changed = !TextUtils.equals(mValue, value);
        if (changed || !mValueSet) {
            mValue = value;
            mValueSet = true;
            persistString(value);
            if (changed) {
                notifyChanged();
            }
        }
    }

    /**
     * Returns the value of the key. This should be one of the entries in {@link #getEntryValues()}.
     *
     * @return The value of the key
     */
    public String getValue() {
        return mValue;
    }

    /**
     * Returns the entry corresponding to the current value.
     *
     * @return The entry corresponding to the current value, or {@code null}
     */
    public CharSequence getEntry() {
        int index = getValueIndex();
        return index >= 0 && mEntries != null ? mEntries[index] : null;
    }

    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned
     * @return The index of the value, or -1 if not found
     */
    public int findIndexOfValue(String value) {
        if (value != null && mEntryValues != null) {
            for (int i = mEntryValues.length - 1; i >= 0; i--) {
                if (mEntryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Sets the value to the given index from the entry values.
     *
     * @param index The index of the value to set
     */
    public void setValueIndex(int index) {
        if (mEntryValues != null) {
            setValue(mEntryValues[index].toString());
        }
    }

    private int getValueIndex() {
        return findIndexOfValue(mValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        setValue(getPersistedString((String) defaultValue));
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.mValue = getValue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.mValue);
    }

    private static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        String mValue;

        SavedState(Parcel source) {
            super(source);
            mValue = source.readString();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(mValue);
        }
    }

    /**
     * A simple {@link androidx.preference.Preference.SummaryProvider} implementation for a
     * {@link ListPreference}. If no value has been set, the summary displayed will be 'Not set',
     * otherwise the summary displayed will be the entry set for this preference.
     */
    public static final class SimpleSummaryProvider implements SummaryProvider<ListPreference> {

        private static SimpleSummaryProvider sSimpleSummaryProvider;

        private SimpleSummaryProvider() {}

        /**
         * Retrieve a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation.
         *
         * @return a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation
         */
        public static SimpleSummaryProvider getInstance() {
            if (sSimpleSummaryProvider == null) {
                sSimpleSummaryProvider = new SimpleSummaryProvider();
            }
            return sSimpleSummaryProvider;
        }

        @Override
        public CharSequence provideSummary(ListPreference preference) {
            if (TextUtils.isEmpty(preference.getEntry())) {
                return (preference.getContext().getString(R.string.not_set));
            } else {
                return preference.getEntry();
            }
        }
    }
}
