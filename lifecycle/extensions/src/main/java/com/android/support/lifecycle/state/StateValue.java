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

/**
 * Simple wrapper class that holds T value.
 * This class can be obtained from {@link RetainedStateProvider}
 * @param <T> - type of the object.
 */
public class StateValue<T> {

    private T mValue;

    StateValue() {
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
}
