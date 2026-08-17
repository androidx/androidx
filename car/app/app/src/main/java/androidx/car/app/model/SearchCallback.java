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

package androidx.car.app.model;

import org.jspecify.annotations.NonNull;

/**
 * A listener used in a {@link SearchHeader} and a {@link SearchTemplate} to receive the search text
 * updates from a user.
 */
public interface SearchCallback {
    /**
     * Notifies that the current {@code searchText} has changed.
     *
     * <p>The host may invoke this callback as the user types a search text. The frequency of
     * these updates is not guaranteed to be after every individual keystroke. The host may
     * decide to wait for several keystrokes before sending a single update.
     *
     * @param searchText the current search text that the user has typed
     */
    default void onSearchTextChanged(@NonNull String searchText) {
    }

    /**
     * Notifies that the user has submitted the search and the given {@code searchText} is
     * the final term.
     *
     * @param searchText the search text that the user typed
     */
    default void onSearchSubmitted(@NonNull String searchText) {
    }
}
