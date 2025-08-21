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
package androidx.compose.remote.creation.profile;

import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/** Interface representing the constructor for a RemoteComposeWriter (used in {@link Profile}) */
public interface ProfileFactory {
    /**
     * Returns a valid RemoteComposeWriter
     *
     * @param width original width of the document
     * @param height original height of the document
     * @param contentDescription content description
     * @param profile operation profiles used by this document
     * @return a valid RemoteComposeWriter
     */
    @NonNull RemoteComposeWriter create(
            int width, int height, @NonNull String contentDescription, @NonNull Profile profile);
}
