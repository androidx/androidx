/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.layout;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.serialization.yaml.YAMLSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A test operation that advances time and captures the animated state.
 */
public class CaptureAnimatedState extends TestOperation {
    private final long mTimeMs;

    public CaptureAnimatedState(long timeMs) {
        mTimeMs = timeMs;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean apply(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull TestParameters testParameters,
            @Nullable List<Map<String, Object>> commands) {
        context.currentTime += mTimeMs;
        document.paint(context, Theme.UNSPECIFIED);

        YAMLSerializer serializer = new YAMLSerializer();
        document.serialize(serializer.serializeMap());
        Map<String, Object> snapshot = (Map<String, Object>) serializer.toObject();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", "Animated State at " + context.currentTime);
        map.put("command", "Capture");
        map.put("originX", document.getOriginX());
        map.put("originY", document.getOriginY());
        map.put("result", snapshot);
        if (commands != null) {
            commands.add(map);
        }
        return false;
    }
}
