/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import androidx.annotation.NonNull;

/**
 * A host-side interface for reporting to search updates to clients.
 */
public interface SearchListenerWrapper {
    /**
     * Notifies that the search text has changed.
     *
     * @param searchText the up-to-date search text.
     * @param callback   the {@link OnDoneCallback} to trigger when the client finishes handling
     *                   the event.
     */
    void onSearchTextChanged(@NonNull String searchText, @NonNull OnDoneCallback callback);

    /**
     * Notifies that the user has submitted the search.
     *
     * @param searchText the search text that was submitted.
     * @param callback   the {@link OnDoneCallback} to trigger when the client finishes handling
     *                   the event.
     */
    void onSearchSubmitted(@NonNull String searchText, @NonNull OnDoneCallback callback);
}

