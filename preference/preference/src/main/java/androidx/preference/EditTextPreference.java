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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.TypedArrayUtils;

/**
 * A {@link DialogPreference} that shows a {@link EditText} in the dialog.
 *
 * <p>This preference saves a string value.
 */
public class EditTextPreference extends DialogPreference {
    private String mText;

    @Nullable
    private OnBindEditTextListener mOnBindEditTextListener;

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.EditTextPreference, defStyleAttr, defStyleRes);

        if (TypedArrayUtils.getBoolean(a, R.styleable.EditTextPreference_useSimpleSummaryProvider,
                R.styleable.EditTextPreference_useSimpleSummaryProvider, false)) {
            setSummaryProvider(SimpleSummaryProvider.getInstance());
        }

        a.recycle();
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.editTextPreferenceStyle,
                android.R.attr.editTextPreferenceStyle));
    }

    public EditTextPreference(Context context) {
        this(context, null);
    }

    /**
     * Saves the text to the current data storage.
     *
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mText = text;

        persistString(text);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }

        notifyChanged();
    }

    /**
     * Gets the text from the current data storage.
     *
     * @return The current preference value
     */
    public String getText() {
        return mText;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        setText(getPersistedString((String) defaultValue));
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.mText = getText();
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
        setText(myState.mText);
    }

    /**
     * Set an {@link OnBindEditTextListener} that will be invoked when the corresponding dialog
     * view for this preference is bound. Set {@code null} to remove the existing
     * OnBindEditTextListener.
     *
     * @param onBindEditTextListener The {@link OnBindEditTextListener} that will be invoked when
     *                               the corresponding dialog view for this preference is bound
     * @see OnBindEditTextListener
     */
    public void setOnBindEditTextListener(@Nullable OnBindEditTextListener onBindEditTextListener) {
        mOnBindEditTextListener = onBindEditTextListener;
    }

    /**
     * Returns the {@link OnBindEditTextListener} used to configure the {@link EditText}
     * displayed in the corresponding dialog view for this preference.
     *
     * @return The {@link OnBindEditTextListener} set for this preference, or {@code null} if
     * there is no OnBindEditTextListener set
     * @see OnBindEditTextListener
     */
    @Nullable OnBindEditTextListener getOnBindEditTextListener() {
        return mOnBindEditTextListener;
    }

    /**
     * Interface definition for a callback to be invoked when the corresponding dialog view for
     * this preference is bound. This allows you to customize the {@link EditText} displayed
     * in the dialog, such as setting a max length or a specific input type.
     */
    public interface OnBindEditTextListener {
        /**
         * Called when the dialog view for this preference has been bound, allowing you to
         * customize the {@link EditText} displayed in the dialog.
         *
         * @param editText The {@link EditText} displayed in the dialog
         */
        void onBindEditText(@NonNull EditText editText);
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

        String mText;

        SavedState(Parcel source) {
            super(source);
            mText = source.readString();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(mText);
        }
    }

    /**
     * A simple {@link androidx.preference.Preference.SummaryProvider} implementation for an
     * {@link EditTextPreference}. If no value has been set, the summary displayed will be 'Not
     * set', otherwise the summary displayed will be the value set for this preference.
     */
    public static final class SimpleSummaryProvider implements SummaryProvider<EditTextPreference> {

        private static SimpleSummaryProvider sSimpleSummaryProvider;

        private SimpleSummaryProvider() {}

        /**
         * Retrieve a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation.
         *
         * @return a singleton instance of this simple
         * {@link androidx.preference.Preference.SummaryProvider} implementation
         */
        @NonNull
        public static SimpleSummaryProvider getInstance() {
            if (sSimpleSummaryProvider == null) {
                sSimpleSummaryProvider = new SimpleSummaryProvider();
            }
            return sSimpleSummaryProvider;
        }

        @Override
        public CharSequence provideSummary(@NonNull EditTextPreference preference) {
            if (TextUtils.isEmpty(preference.getText())) {
                return (preference.getContext().getString(R.string.not_set));
            } else {
                return preference.getText();
            }
        }
    }

}
