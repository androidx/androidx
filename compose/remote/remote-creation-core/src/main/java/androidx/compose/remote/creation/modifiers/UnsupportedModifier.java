/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation.modifiers;

import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/**
 * Unsupported modifier.
 *
 * <p>Indicates that a RemoteModifier was used without support in RemoteCompose.
 */
public class UnsupportedModifier implements RecordingModifier.Element {
    private final @NonNull String mName;

    public UnsupportedModifier(@NonNull String name) {
        this.mName = name;
    }

    @Override
    public @NonNull String toString() {
        return "UnsupportedModifier{" + "name='" + mName + '\'' + '}';
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {}
}
