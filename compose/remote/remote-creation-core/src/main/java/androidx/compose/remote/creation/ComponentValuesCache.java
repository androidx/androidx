/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation;

import androidx.compose.remote.core.operations.Utils;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;

class ComponentValuesCache {
    @NonNull
    HashMap<@NonNull Integer, ComponentValueKeys> mCacheComponentValues = new HashMap<>();
    @NonNull RemoteComposeWriter mWriter;

    ComponentValuesCache(@NonNull RemoteComposeWriter writer) {
        mWriter = writer;
    }

    float addComponentValue(int id, int type) {
        if (!mCacheComponentValues.containsKey(id)) {
            mCacheComponentValues.put(id, new ComponentValueKeys());
        }
        ComponentValueKeys valuesKeys = mCacheComponentValues.get(id);
        float value = valuesKeys.getValue(type);
        if (!Utils.isVariable(value)) {
            value = mWriter.reserveFloatVariable();
            mWriter.mBuffer.addComponentValue(Utils.idFromNan(value), type);
            valuesKeys.setValue(type, value);
        }
        return value;
    }

    public void clear() {
        mCacheComponentValues.clear();
    }
}
