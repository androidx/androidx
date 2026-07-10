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
package androidx.compose.remote.creation.json;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.creation.modifiers.RecordingModifier;

import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

/** Interface for custom layout modifier parsing. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface JsonModifierParser {
    /**
     * Parse a single layout modifier key.
     *
     * @param mod the JSON object containing the modifier parameters
     * @param key the key of the modifier being parsed
     * @param recordingModifier the modifier container to record to
     * @param parser the parser context instance
     * @throws org.json.JSONException if JSON parsing fails
     */
    void parse(
            @NonNull JSONObject mod,
            @NonNull String key,
            @NonNull RecordingModifier recordingModifier,
            @NonNull RemoteComposeJsonParser parser
    );
}
