/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.serialization;

import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

public class SerializableVariable implements Serializable {

    private final float mId;
    private final float mValue;

    public SerializableVariable(float id, float value) {
        mId = id;
        mValue = value;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        if (Float.isNaN(mId)) {
            serializer.addType("Variable");
            serializer.add("id", Utils.idFromNan(mId));
        } else {
            serializer.addType("Value");
        }
        serializer.add("value", mValue);
    }
}
