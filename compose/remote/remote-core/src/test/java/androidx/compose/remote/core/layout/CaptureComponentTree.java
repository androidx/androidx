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
import androidx.compose.remote.core.serialization.yaml.YAMLSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CaptureComponentTree extends TestOperation {

    private String mContent;

    @SuppressWarnings("unchecked")
    @Override
    public boolean apply(@NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull TestParameters testParameters,
            @Nullable List<Map<String, Object>> commands) {
        YAMLSerializer serializer = new YAMLSerializer();
        document.serialize(serializer.serializeMap());
        mContent = serializer.toString();
        if (commands != null) {
            Map<String, Object> snapshot = (Map<String, Object>) serializer.toObject();
            commands.add(command("Component Tree", "Capture", snapshot));
        }
        return false;
    }

    public @Nullable String getContent() {
        return mContent;
    }
}
