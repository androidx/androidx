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

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Vector;

/** Optional helper class for concrete implementations */
public abstract class AbstractArraySerializer implements ArraySerializer, SerializeFactory {

    protected Vector<Serializer> mArray = new Vector<>();

    @Override
    public <T> void add(@Nullable List<T> value) {
        Serializer serializer = newSerializer();
        if (value != null) {
            ArraySerializer arraySerializer = serializer.serializeArray();
            SerializeUtils.serializeArray(arraySerializer, value);
        }
        mArray.add(serializer);
    }

    @Override
    public <T> void add(@Nullable Map<String, T> value) {
        Serializer serializer = newSerializer();
        if (value != null) {
            MapSerializer mapSerializer = serializer.serializeMap();
            SerializeUtils.serializeMap(mapSerializer, value);
        }
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable Serializable value) {
        Serializer serializer = newSerializer();
        if (value != null) {
            value.serialize(serializer.serializeMap());
        }
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable String value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable Byte value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable Short value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable Integer value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable Long value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable Float value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable Double value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }

    @Override
    public void add(@Nullable Boolean value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }

    @Override
    public <T extends Enum<T>> void add(@Nullable Enum<T> value) {
        Serializer serializer = newSerializer();
        serializer.serialize(value);
        mArray.add(serializer);
    }
}
