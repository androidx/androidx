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

/**
 * Simple wrapper class that holds int value.
 * This class can be obtained from either {@link SavedStateProvider}
 * either {@link RetainedStateProvider}
 */
public class IntStateValue extends Saveable {

    private int mValue;

    IntStateValue(int i) {
        mValue = i;
    }

    /**
     * Sets the value.
     *
     * @param i The new value
     */
    public void set(int i) {
        mValue = i;
    }

    /**
     * @return the current value
     */
    public int get() {
        return mValue;
    }

    @Override
    void saveTo(Bundle savedState, String key) {
        savedState.putInt(key, mValue);
    }
}
