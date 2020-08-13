/*
 * Copyright 2019 The Android Open Source Project
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

import java.util.List;

/**
 * This interface provides inspector specific utilities, such as
 * managed threads and ARTTI features.
 */
//TODO(b/163335801): remove "extends ArtToolInterface"
// /temporary implements ArtToolInterface to ease drop to Studio.
public interface InspectorEnvironment extends ArtToolInterface {

    // TODO: will be removed once studio and clients are migrated
    /**
     * This interface will be removed
     * @param <T>
     */
    interface ExitHook<T> extends ArtToolInterface.ExitHook<T> {
    }

    /**
     * This interface will be removed
     */
    interface EntryHook extends ArtToolInterface.EntryHook {
    }

    /**
     * Executors provided by App Inspection Platforms. Clients should use it instead of
     * creating their own.
     */
    @NonNull
    default InspectorExecutors executors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Interface that provides ART TI capabilities.
     */
    @NonNull
    default ArtToolInterface artTI() {
        return this;
    }

    // Temporary default implementations (so they can be removed from actual implementation of
    // InspectorEnvironment
    @NonNull
    @Override
    default <T> List<T> findInstances(@NonNull Class<T> clazz) {
        return artTI().findInstances(clazz);
    }

    @Override
    default void registerEntryHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull ArtToolInterface.EntryHook entryHook) {
        artTI().registerEntryHook(originClass, originMethod, entryHook);
    }

    @Override
    default <T> void registerExitHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull ArtToolInterface.ExitHook<T> exitHook) {
        artTI().registerExitHook(originClass, originMethod, exitHook);
    }

    /**
     * Temporary method for backwards compat. TODO(b/163335801)
     */
    default void registerEntryHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull EntryHook entryHook) {
        artTI().registerEntryHook(originClass, originMethod, entryHook);
    }

    /**
     * Temporary method for backwards compat. TODO(b/163335801)
     */
    default <T> void registerExitHook(@NonNull Class<?> originClass, @NonNull String originMethod,
            @NonNull ExitHook<T> exitHook) {
        artTI().registerExitHook(originClass, originMethod, exitHook);
    }

}
