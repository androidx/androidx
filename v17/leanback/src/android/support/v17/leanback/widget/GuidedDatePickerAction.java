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
package android.support.v17.leanback.widget;

import android.content.Context;
import android.support.v17.leanback.widget.picker.DatePicker;

import java.util.Calendar;

/**
 * Subclass of GuidedAction that can choose a date.  The Action is editable by default; to make it
 * read only, call hasEditableActivatorView(false) on the Builder.
 */
public class GuidedDatePickerAction extends GuidedAction {

    /**
     * Base Builder class to build GuidedDatePickerAction.  Subclass this BuilderBase when app needs
     * to subclass GuidedDatePickerAction, implement your build() which should call
     * {@link #applyDatePickerValues(GuidedDatePickerAction)}.  When using GuidedDatePickerAction
     * directly, use {@link Builder}.
     */
    public abstract static class BuilderBase<B extends BuilderBase>
            extends GuidedAction.BuilderBase<B> {

        private String mDatePickerFormat;
        private long mDate;

        public BuilderBase(Context context) {
            super(context);
            Calendar c = Calendar.getInstance();
            mDate = c.getTimeInMillis();
            hasEditableActivatorView(true);
        }

        /**
         * Sets format of date Picker.  When the format is not specified,
         * a default format of current locale will be used.
         * @param format Format of showing Date, e.g. "YMD"
         * @return This Builder object.
         */
        public B datePickerFormat(String format) {
            mDatePickerFormat = format;
            return (B) this;
        }

        /**
         * Sets a Date for date picker, see {@link Calendar#getTimeInMillis()}.
         * @param date See {@link Calendar#getTimeInMillis()}.
         * @return This Builder Object.
         */
        public B date(long date) {
            mDate = date;
            return (B) this;
        }

        /**
         * Apply values to GuidedDatePickerAction.
         * @param action GuidedDatePickerAction to apply values.
         */
        protected final void applyDatePickerValues(GuidedDatePickerAction action) {
            super.applyValues(action);
            action.mDatePickerFormat = mDatePickerFormat;
            action.mDate = mDate;
        }

    }

    /**
     * Builder class to build a GuidedDatePickerAction.
     */
    public final static class Builder extends BuilderBase<Builder> {
        public Builder(Context context) {
            super(context);
        }

        /**
         * Builds the GuidedDatePickerAction corresponding to this Builder.
         * @return The GuidedDatePickerAction as configured through this Builder.
         */
        public GuidedDatePickerAction build() {
            GuidedDatePickerAction action = new GuidedDatePickerAction();
            applyDatePickerValues(action);
            return action;
        }
    }

    private String mDatePickerFormat;
    private long mDate;

    /**
     * Returns format of date Picker or null if not specified.  When the
     * format is not specified, a default format of current locale will be used.
     * @return Format of showing Date, e.g. "YMD".  Returns null if using current locale's default.
     */
    public String getDatePickerFormat() {
        return mDatePickerFormat;
    }

    /**
     * Get current value of DatePicker;
     * @return Current value of DatePicker;
     */
    public long getDate() {
        return mDate;
    }

    /**
     * Sets current value of DatePicker;
     * @param date New value to update current value of DatePicker;
     */
    public void setDate(long date) {
        mDate = date;
    }
}
