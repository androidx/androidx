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
package androidx.compose.remote.serialization.yaml;

import androidx.compose.remote.serialization.AbstractArraySerializer;
import androidx.compose.remote.serialization.AbstractMapSerializer;
import androidx.compose.remote.serialization.AbstractSerializer;
import androidx.compose.remote.serialization.Serializer;

import java.util.LinkedHashMap;
import java.util.Map;

public class YAMLMapSerializer extends AbstractMapSerializer {

    @Override
    public AbstractArraySerializer newArraySerializer() {
        return new YAMLArraySerializer();
    }

    @Override
    public AbstractMapSerializer newMapSerializer() {
        return new YAMLMapSerializer();
    }

    @Override
    public AbstractSerializer newSerializer() {
        return new YAMLSerializer();
    }

    Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (mType != null) {
            map.put("type", mType);
        }
        for (Map.Entry<String, Serializer> entry : mEntries.entrySet()) {
            Serializer serializer = entry.getValue();
            if (serializer instanceof YAMLSerializer) {
                map.put(entry.getKey(), ((YAMLSerializer) serializer).toObject());
            } else {
                throw new RuntimeException("All serializers must be YAML serializers");
            }
        }
        return map;
    }
}
