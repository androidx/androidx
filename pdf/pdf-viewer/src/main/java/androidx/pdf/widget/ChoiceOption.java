/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;

/** Represents a single option in a Combobox or Listbox PDF form widget. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ChoiceOption implements Parcelable {

    public static final Creator<ChoiceOption> CREATOR = new Creator<ChoiceOption>() {
        @Override
        public ChoiceOption createFromParcel(Parcel in) {
            return new ChoiceOption(in);
        }

        @Override
        public ChoiceOption[] newArray(int size) {
            return new ChoiceOption[size];
        }
    };
    private int mIndex;
    private String mLabel;
    private boolean mSelected;

    public ChoiceOption(int index, String label, boolean selected) {
        this.mIndex = index;
        this.mLabel = label;
        this.mSelected = selected;
    }

    /** Copy constructor. */
    public ChoiceOption(ChoiceOption option) {
        this(option.mIndex, option.mLabel, option.mSelected);
    }

    protected ChoiceOption(Parcel in) {
        mIndex = in.readInt();
        mLabel = in.readString();
        mSelected = in.readInt() != 0;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        this.mLabel = label;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mIndex);
        dest.writeString(mLabel);
        dest.writeInt(mSelected ? 1 : 0);
    }
}
