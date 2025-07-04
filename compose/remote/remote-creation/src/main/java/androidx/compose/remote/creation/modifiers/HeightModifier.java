/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation.modifiers;

import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/** Height modifier */
public class HeightModifier implements RecordingModifier.Element {
    DimensionModifierOperation.@NonNull Type mType;
    float mValue;

    public HeightModifier(DimensionModifierOperation.@NonNull Type type, float value) {
        mType = type;
        mValue = value;
    }

    public HeightModifier(DimensionModifierOperation.@NonNull Type type) {
        this(type, Float.NaN);
    }

    public HeightModifier(float value) {
        this(DimensionModifierOperation.Type.EXACT, value);
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        HeightModifierOperation.apply(writer.getBuffer().getBuffer(), mType.ordinal(), mValue);
    }

    /**
     * Update the modifier
     *
     * @param type
     * @param value
     */
    public void update(DimensionModifierOperation.@NonNull Type type, float value) {
        mType = type;
        mValue = value;
    }

    public DimensionModifierOperation.@NonNull Type getType() {
        return mType;
    }

    public float getValue() {
        return mValue;
    }
}
