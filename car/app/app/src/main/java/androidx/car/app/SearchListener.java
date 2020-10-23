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

/** A listener for search updates. */
public interface SearchListener {
    /**
     * Notifies the current {@code searchText}.
     *
     * <p>The host may invoke this callback as the user types a search text. The frequency of these
     * updates is not guaranteed to be after every individual keystroke. The host may decide to wait
     * for several keystrokes before sending a single update.
     *
     * @param searchText the current search text that the user has typed.
     */
    void onSearchTextChanged(@NonNull String searchText);

    /**
     * Notifies that the user has submitted the search and the given {@code searchText} is the final
     * term.
     *
     * @param searchText the search text that the user typed.
     */
    void onSearchSubmitted(@NonNull String searchText);
}
