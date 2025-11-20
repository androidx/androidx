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

public class SerializeUtils {

    /**
     * Serialize map
     *
     * @param serializer
     * @param map
     * @param <T>
     */
    public static <T> void serializeMap(Serializer serializer, @Nullable Map<String, T> map) {
        if (map == null) {
            return;
        }
        MapSerializer mapSerializer = serializer.serializeMap();
        serializeMap(mapSerializer, map);
    }

    /**
     * Serialize map
     *
     * @param serializer
     * @param map
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public static <T> void serializeMap(MapSerializer serializer, Map<String, T> map) {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            String key = entry.getKey();
            T value = entry.getValue();
            if (value instanceof Serializable) {
                serializer.add(key, (Serializable) value);
            } else if (value instanceof List) {
                serializer.add(key, (List<Object>) value);
            } else if (value instanceof Map) {
                serializer.add(key, (Map<String, Object>) value);
            } else if (value instanceof String) {
                serializer.add(key, (String) value);
            } else if (value instanceof Byte) {
                serializer.add(key, (Byte) value);
            } else if (value instanceof Short) {
                serializer.add(key, (Short) value);
            } else if (value instanceof Integer) {
                serializer.add(key, (Integer) value);
            } else if (value instanceof Long) {
                serializer.add(key, (Long) value);
            } else if (value instanceof Float) {
                serializer.add(key, (Float) value);
            } else if (value instanceof Double) {
                serializer.add(key, (Double) value);
            } else if (value instanceof Boolean) {
                serializer.add(key, (Boolean) value);
            } else if (value == null) {
                serializer.add(key, (String) null);
            }
        }
    }

    /**
     * Serialize array
     *
     * @param serializer
     * @param value
     * @param <T>
     */
    public static <T> void serializeArray(Serializer serializer, @Nullable List<T> value) {
        if (value == null) {
            return;
        }
        ArraySerializer arraySerializer = serializer.serializeArray();
        serializeArray(arraySerializer, value);
    }

    /**
     * Serialize array
     *
     * @param serializer
     * @param value
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public static <T> void serializeArray(ArraySerializer serializer, List<T> value) {
        for (Object element : value) {
            if (element instanceof Serializable) {
                serializer.add((Serializable) element);
            } else if (element instanceof List) {
                serializer.add((List<Object>) element);
            } else if (element instanceof Map) {
                serializer.add((Map<String, Object>) element);
            } else if (element instanceof String) {
                serializer.add((String) element);
            } else if (element instanceof Byte) {
                serializer.add((Byte) element);
            } else if (element instanceof Short) {
                serializer.add((Short) element);
            } else if (element instanceof Integer) {
                serializer.add((Integer) element);
            } else if (element instanceof Long) {
                serializer.add((Long) element);
            } else if (element instanceof Float) {
                serializer.add((Float) element);
            } else if (element instanceof Double) {
                serializer.add((Double) element);
            } else if (element instanceof Boolean) {
                serializer.add((Boolean) element);
            } else if (element == null) {
                serializer.add((String) null);
            }
        }
    }
}
