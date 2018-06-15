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

package androidx.car.navigation.utils;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Objects;

/**
 * Example of a version change. This is a copy of {@link TestBundlable} but with one field
 * deprecated ({@link TestBundlable#mStringValue}) and one field added
 * ({@link TestBundlableNewVersion#mNewValue})
 */
class TestBundlableNewVersion implements Bundlable {
    private static final String INT_VALUE_KEY = "intValue";
    // In this version of TestBundlable, mStringValue field has been deprecated, and its key should
    // not be re-used.
    // private static final String STRING_VALUE_KEY = "stringValue";
    private static final String ENUM_VALUE_KEY = "enumValue";
    private static final String BUNDLABLE_VALUE_KEY = "bundlableValue";
    private static final String BUNDLABLE_LIST_VALUE_KEY = "bundlableListValue";
    private static final String NEW_VALUE_KEY = "newValue";

    enum TestEnum {
        VALUE1,
        VALUE2,
        VALUE3,
    }

    public static final String DEFAULT_NEW_VALUE = "TEST_V2";

    int mIntValue;
    TestEnum mEnumValue;
    // In this version of TestBundlable, we simulate mStringValue being deprecated.
    // String mStringValue;
    TestBundlableNewVersion mBundableValue;
    List<TestBundlableNewVersion> mListValue;
    // In this version of TestBundlable, we simulate the creation of a new mandatory field.
    @NonNull
    String mNewValue;

    TestBundlableNewVersion() {
        mNewValue = DEFAULT_NEW_VALUE;
    }

    @Override
    public void toBundle(@NonNull BundleMarshaller out) {
        out.putInt(INT_VALUE_KEY, mIntValue);
        // dest.putString(STRING_VALUE_KEY, mStringValue) is now deprecated.
        out.putEnum(ENUM_VALUE_KEY, mEnumValue);
        out.putBundlable(BUNDLABLE_VALUE_KEY, mBundableValue);
        out.putBundlableList(BUNDLABLE_LIST_VALUE_KEY, mListValue);
        // new required value
        out.putString(NEW_VALUE_KEY, mNewValue);
    }

    @Override
    public void fromBundle(@NonNull BundleMarshaller in) {
        mIntValue = in.getInt(INT_VALUE_KEY);
        // mStringValue = in.getString(STRING_VALUE_KEY) is now deprecated.
        mEnumValue = in.getEnum(ENUM_VALUE_KEY, TestEnum.class);
        mBundableValue = in.getBundlable(BUNDLABLE_VALUE_KEY, mBundableValue,
                TestBundlableNewVersion::new);
        mListValue = in.getBundlableList(BUNDLABLE_LIST_VALUE_KEY, mListValue,
                TestBundlableNewVersion::new);
        // new required value with a mandatory default
        mNewValue = in.getStringNonNull(NEW_VALUE_KEY, DEFAULT_NEW_VALUE);
    }

    public TestBundlableNewVersion setInt(int value) {
        mIntValue = value;
        return this;
    }

    public TestBundlableNewVersion setEnumValue(TestEnum value) {
        mEnumValue = value;
        return this;
    }

    public TestBundlableNewVersion setBundlableValue(TestBundlableNewVersion value) {
        mBundableValue = value;
        return this;
    }

    public TestBundlableNewVersion setListValue(List<TestBundlableNewVersion> value) {
        mListValue = value;
        return this;
    }

    public TestBundlableNewVersion setNewValue(@NonNull String value) {
        Preconditions.checkNotNull(value);
        mNewValue = value;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestBundlableNewVersion that = (TestBundlableNewVersion) o;
        return mIntValue == that.mIntValue
                && mEnumValue == that.mEnumValue
                && Objects.equals(mBundableValue, that.mBundableValue)
                && Objects.equals(mListValue, that.mListValue)
                && Objects.equals(mNewValue, that.mNewValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIntValue, mEnumValue, mBundableValue, mListValue, mNewValue);
    }
}
