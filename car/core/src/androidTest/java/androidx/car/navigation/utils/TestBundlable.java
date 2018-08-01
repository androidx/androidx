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

import java.util.List;
import java.util.Objects;

/**
 * Reference implementation of a {@link Bundlable}
 */
class TestBundlable implements Bundlable {
    private static final String INT_VALUE_KEY = "intValue";
    private static final String STRING_VALUE_KEY = "stringValue";
    private static final String ENUM_VALUE_KEY = "enumValue";
    private static final String BUNDLABLE_VALUE_KEY = "bundlableValue";
    private static final String BUNDLABLE_LIST_VALUE_KEY = "bundlableListValue";

    enum TestEnum {
        VALUE1,
        VALUE2,
        VALUE3,
    }

    int mIntValue;
    String mStringValue;
    TestEnum mEnumValue;
    TestBundlable mBundableValue;
    List<TestBundlable> mListValue;

    @Override
    public void toBundle(BundleMarshaller out) {
        out.putInt(INT_VALUE_KEY, mIntValue);
        out.putString(STRING_VALUE_KEY, mStringValue);
        out.putEnum(ENUM_VALUE_KEY, mEnumValue);
        out.putBundlable(BUNDLABLE_VALUE_KEY, mBundableValue);
        out.putBundlableList(BUNDLABLE_LIST_VALUE_KEY, mListValue);
    }

    @Override
    public void fromBundle(BundleMarshaller in) {
        mIntValue = in.getInt(INT_VALUE_KEY);
        mStringValue = in.getString(STRING_VALUE_KEY);
        mEnumValue = in.getEnum(ENUM_VALUE_KEY, TestEnum.class);
        mBundableValue = in.getBundlable(BUNDLABLE_VALUE_KEY, mBundableValue, TestBundlable::new);
        mListValue = in.getBundlableList(BUNDLABLE_LIST_VALUE_KEY, mListValue, TestBundlable::new);
    }

    public TestBundlable setInt(int value) {
        mIntValue = value;
        return this;
    }

    public TestBundlable setString(String value) {
        mStringValue = value;
        return this;
    }

    public TestBundlable setEnumValue(TestEnum value) {
        mEnumValue = value;
        return this;
    }

    public TestBundlable setBundlableValue(TestBundlable value) {
        mBundableValue = value;
        return this;
    }

    public TestBundlable setListValue(List<TestBundlable> value) {
        mListValue = value;
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
        TestBundlable that = (TestBundlable) o;
        return mIntValue == that.mIntValue
                && Objects.equals(mStringValue, that.mStringValue)
                && mEnumValue == that.mEnumValue
                && Objects.equals(mBundableValue, that.mBundableValue)
                && Objects.equals(mListValue, that.mListValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIntValue, mStringValue, mEnumValue, mBundableValue, mListValue);
    }
}
