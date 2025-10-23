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

package androidx.compose.remote.core.layout;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class TestOperation {
    /**
     * Apply the operation on the document
     *
     * @param document the document to test
     * @return true if we want to force a repaint
     */
    public abstract boolean apply(@NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull TestParameters testParameters,
            @Nullable List<Map<String, Object>> commands);


    protected Map<String, Object> command(@NonNull String description,
            Map<String, Object> command, Map<String, Object> snapshot) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", description);
        map.put("command", command);
        map.put("result", snapshot);
        return map;
    }

    protected Map<String, Object> command(@NonNull String description,
            @NonNull String command, Map<String, Object> snapshot) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", description);
        map.put("command", command);
        map.put("result", snapshot);
        return map;
    }
}
