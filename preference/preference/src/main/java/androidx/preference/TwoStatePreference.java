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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Common base class for preferences that have two selectable states, save a boolean value, and
 * may have dependent preferences that are enabled/disabled based on the
 * current state.
 */
public abstract class TwoStatePreference extends Preference {

    protected boolean mChecked;
    private CharSequence mSummaryOn;
    private CharSequence mSummaryOff;
    private boolean mCheckedSet;
    private boolean mDisableDependentsState;

    public TwoStatePreference(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public TwoStatePreference(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TwoStatePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwoStatePreference(@NonNull Context context) {
        this(context, null);
    }

    @Override
    protected void onClick() {
        super.onClick();

        final boolean newValue = !isChecked();
        if (callChangeListener(newValue)) {
            setChecked(newValue);
        }
    }

    /**
     * Returns the checked state.
     *
     * @return The checked state
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Sets the checked state and saves it.
     *
     * @param checked The checked state
     */
    public void setChecked(boolean checked) {
        // Always persist/notify the first time; don't assume the field's default of false.
        final boolean changed = mChecked != checked;
        if (changed || !mCheckedSet) {
            mChecked = checked;
            mCheckedSet = true;
            persistBoolean(checked);
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        boolean shouldDisable = mDisableDependentsState ? mChecked : !mChecked;
        return shouldDisable || super.shouldDisableDependents();
    }

    /**
     * Sets the summary to be shown when checked.
     *
     * <p>For more complex configuration of this preference's summary, you should use a
     * {@link Preference.SummaryProvider}
     *
     * @param summary The summary to be shown when checked
     */
    public void setSummaryOn(@Nullable CharSequence summary) {
        mSummaryOn = summary;
        if (isChecked()) {
            notifyChanged();
        }
    }

    /**
     * Returns the summary to be shown when checked.
     *
     * @return The summary
     */
    @Nullable
    public CharSequence getSummaryOn() {
        return mSummaryOn;
    }

    /**
     * @param summaryResId The summary as a resource
     * @see #setSummaryOn(CharSequence)
     */
    public void setSummaryOn(int summaryResId) {
        setSummaryOn(getContext().getString(summaryResId));
    }

    /**
     * Sets the summary to be shown when unchecked.
     *
     * <p>For more complex configuration of this preference's summary, you should use a
     * {@link Preference.SummaryProvider}
     *
     * @param summary The summary to be shown when unchecked
     */
    public void setSummaryOff(@Nullable CharSequence summary) {
        mSummaryOff = summary;
        if (!isChecked()) {
            notifyChanged();
        }
    }

    /**
     * Returns the summary to be shown when unchecked.
     *
     * @return The summary
     */
    @Nullable
    public CharSequence getSummaryOff() {
        return mSummaryOff;
    }

    /**
     * @param summaryResId The summary as a resource
     * @see #setSummaryOff(CharSequence)
     */
    public void setSummaryOff(int summaryResId) {
        setSummaryOff(getContext().getString(summaryResId));
    }

    /**
     * Returns whether dependents are disabled when this preference is on ({@code true})
     * or when this preference is off ({@code false}).
     *
     * @return Whether dependents are disabled when this preference is on ({@code true})
     * or when this preference is off ({@code false}).
     */
    public boolean getDisableDependentsState() {
        return mDisableDependentsState;
    }

    /**
     * Sets whether dependents are disabled when this preference is on ({@code true})
     * or when this preference is off ({@code false}).
     *
     * @param disableDependentsState The preference state that should disable dependents
     */
    public void setDisableDependentsState(boolean disableDependentsState) {
        mDisableDependentsState = disableDependentsState;
    }

    @Override
    protected @Nullable Object onGetDefaultValue(@NonNull TypedArray a, int index) {
        return a.getBoolean(index, false);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue == null) {
            defaultValue = false;
        }
        setChecked(getPersistedBoolean((Boolean) defaultValue));
    }

    /**
     * Sync a summary holder contained within holder's sub-hierarchy with the correct summary text.
     *
     * @param holder A {@link PreferenceViewHolder} which holds a reference to the summary view
     */
    protected void syncSummaryView(@NonNull PreferenceViewHolder holder) {
        // Sync the summary holder
        View view = holder.findViewById(android.R.id.summary);
        syncSummaryView(view);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    protected void syncSummaryView(View view) {
        if (!(view instanceof TextView)) {
            return;
        }
        TextView summaryView = (TextView) view;
        boolean useDefaultSummary = true;
        if (mChecked && !TextUtils.isEmpty(mSummaryOn)) {
            summaryView.setText(mSummaryOn);
            useDefaultSummary = false;
        } else if (!mChecked && !TextUtils.isEmpty(mSummaryOff)) {
            summaryView.setText(mSummaryOff);
            useDefaultSummary = false;
        }
        if (useDefaultSummary) {
            final CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                summaryView.setText(summary);
                useDefaultSummary = false;
            }
        }
        int newVisibility = View.GONE;
        if (!useDefaultSummary) {
            // Someone has written to it
            newVisibility = View.VISIBLE;
        }
        if (newVisibility != summaryView.getVisibility()) {
            summaryView.setVisibility(newVisibility);
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.mChecked = isChecked();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(@Nullable Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setChecked(myState.mChecked);
    }

    static class SavedState extends BaseSavedState {
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

        boolean mChecked;

        SavedState(Parcel source) {
            super(source);
            mChecked = source.readInt() == 1;
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mChecked ? 1 : 0);
        }
    }
}
