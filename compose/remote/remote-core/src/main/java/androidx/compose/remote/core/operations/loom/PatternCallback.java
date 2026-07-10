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

package androidx.compose.remote.core.operations.loom;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.RemoteComposeBuffer;

import org.jspecify.annotations.NonNull;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PatternCallback {
    /**
     * Called when a pattern is found in the document
     *
     * @param name the name of the macro
     * @param buffer the buffer containing the macro operations
     */
    void patternFound(@NonNull String name, @NonNull RemoteComposeBuffer buffer);
}
