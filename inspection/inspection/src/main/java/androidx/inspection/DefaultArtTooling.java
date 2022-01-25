/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.inspection;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.List;

/**
 * Default implementation of [ArtTooling]
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefaultArtTooling implements ArtTooling {
    private String mInspectorId;

    public DefaultArtTooling(@NonNull String inspectorId) {
        mInspectorId = inspectorId;
    }

    @NonNull
    @Override
    public <T> List<T> findInstances(@NonNull Class<T> clazz) {
        return ArtToolingImpl.findInstances(clazz);
    }

    @Override
    public void registerEntryHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull EntryHook entryHook) {
        ArtToolingImpl.addEntryHook(mInspectorId, originClass, originMethod, entryHook);
    }

    @Override
    public <T> void registerExitHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull ExitHook<T> exitHook) {
        ArtToolingImpl.addExitHook(mInspectorId, originClass, originMethod, exitHook);
    }

    /**
     * Remove all hooks registered for this inspector.
     */
    public void unregisterHooks() {
        ArtToolingImpl.unregisterHooks(mInspectorId);
    }
}
