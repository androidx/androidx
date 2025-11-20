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

package androidx.appsearch.serializers;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.appsearch.app.LongSerializer;

import android.graphics.Color;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Serializer for {@link Color} as a long.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class ColorAsLongSerializer implements LongSerializer<Color> {

    /**
     * Serializes a Color to a long.
     *
     * @param color The Color to serialize.
     * @return The long serialized from the Color.
     */
    @Override
    public long serialize(@NonNull Color color) {
        return color.toArgb();
    }

    /**
     * Deserializes a long to a Color.
     *
     * @param value The long to deserialize.
     * @return The Color deserialized from the long.
     */
    @Override
    public @NonNull Color deserialize(long value) {
        return Color.valueOf(value);
    }
}