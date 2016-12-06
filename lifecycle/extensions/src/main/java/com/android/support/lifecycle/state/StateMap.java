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
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

class StateMap {
    private static final String TAG = "StateProvider";

    Map<String, Object> mMap = new HashMap<>();
    Bundle mSavedState = null;

    IntStateValue intValue(String key, int defaultValue) {
        IntStateValue intStateValue;
        Object o = mMap.get(key);
        try {
            intStateValue = (IntStateValue) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, IntStateValue.class.getName(), e);
            intStateValue = null;
        }
        if (intStateValue == null) {
            int value = mSavedState != null ? mSavedState.getInt(key, defaultValue)
                    : defaultValue;
            intStateValue = new IntStateValue(value);
            mMap.put(key, intStateValue);
        }
        return intStateValue;
    }

    static void typeWarning(String key, Object value, String className, ClassCastException e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Key ");
        sb.append(key);
        sb.append(" expected ");
        sb.append(className);
        sb.append(" but value was a ");
        sb.append(value.getClass().getName());
        Log.w(TAG, sb.toString());
        Log.w(TAG, "Attempt to cast generated internal exception:", e);
    }
}
