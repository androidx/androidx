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

import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Optional helper class for concrete implementations */
public abstract class AbstractMapSerializer implements MapSerializer, SerializeFactory {
    protected Map<String, Serializer> mEntries = new LinkedHashMap<>();
    protected Set<SerializeTags> mTags = new HashSet<>();

    protected @Nullable String mType;

    @Override
    public @NonNull MapSerializer addType(@NonNull String type) {
        mType = type;
        return this;
    }

    @Override
    public @NonNull MapSerializer addFloatExpressionSrc(
            @NonNull String key, float @NonNull [] value) {
        Serializer serializer = newSerializer();
        serializer.serializeFloatExpressionSrc(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer addIntExpressionSrc(
            @NonNull String key, int @NonNull [] value, int mask) {
        Serializer serializer = newSerializer();
        serializer.serializeIntExpressionSrc(value, mask);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer addPath(@NonNull String key, float @NonNull [] path) {
        Serializer serializer = newSerializer();
        serializer.serializePath(path);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer addTags(SerializeTags @NonNull ... value) {
        mTags.addAll(Arrays.asList(value));
        return this;
    }

    @Override
    public <T> @NonNull MapSerializer add(@NonNull String key, @Nullable List<T> value) {
        Serializer serializer = newSerializer();
        if (value != null) {
            ArraySerializer arraySerializer = serializer.serializeArray();
            SerializeUtils.serializeArray(arraySerializer, value);
        }
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public <T> @NonNull MapSerializer add(@NonNull String key, @Nullable Map<String, T> value) {
        Serializer serializer = newSerializer();
        if (value != null) {
            MapSerializer mapSerializer = serializer.serializeMap();
            SerializeUtils.serializeMap(mapSerializer, value);
        }
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable Serializable serializable) {
        Serializer serializer = newSerializer();
        if (serializable != null) {
            serializable.serialize(serializer.serializeMap());
        }
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable String value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, float a, float r, float g, float b) {
        Serializer serializer = newSerializer();
        serializer.serialize(a, r, g, b);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, float id, float value) {
        return add(key, new SerializableVariable(id, value));
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable Byte value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable Short value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable Integer value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable Long value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable Float value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable Double value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public @NonNull MapSerializer add(@NonNull String key, @Nullable Boolean value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }

    @Override
    public <T extends Enum<T>> @NonNull MapSerializer add(
            @NonNull String key, @Nullable Enum<T> value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mEntries.put(key, serializer);
        return this;
    }
}
