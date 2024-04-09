/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.find;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.util.ObservableValue;

/**
 * Callback interface for listening to user actions to find text in a file.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface FindInFileListener {

    /**
     * Called when the query text is changed by the user.
     *
     * @param query The text the user is searching for.
     */
    boolean onQueryTextChange(String query);

    /**
     * The user is attempting to find the next match of the query text.
     *
     * @param query     The text the user is searching for.
     * @param backwards True iff the user is searching for the previous match.
     */
    boolean onFindNextMatch(String query, boolean backwards);

    /**
     * Get an ObservableValue that changes whenever MatchCount data is changed -
     * when more matches are found or the selected match is changed.
     * Can be null if not supported, or if the document is not ready or is destroyed.
     */
    @Nullable
    ObservableValue<MatchCount> matchCount();
}
