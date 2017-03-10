/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle.state;

import android.os.Bundle;
import android.os.Parcelable;


/**
 * Simple wrapper class that holds T value.
 * <p>
 * This class can be obtained from {@link SavedStateValue}.
 * @param <T> - type of the object.
 */
@SuppressWarnings("WeakerAccess")
public class SavedStateValue<T extends Parcelable> extends Saveable {

    private T mValue;

    /**
     * Initializes a new SavedStateValue with the given initial value.
     *
     * @param mValue The initial value of the SavedStateValue.
     */
    SavedStateValue(T mValue) {
        this.mValue = mValue;
    }

    /**
     * Sets the value.
     *
     * @param t The new value
     */
    public void set(T t) {
        mValue = t;
    }

    /**
     * @return the current value
     */
    public T get() {
        return mValue;
    }

    @Override
    void saveTo(Bundle savedState, String key) {
        savedState.putParcelable(key, mValue);
    }
}
