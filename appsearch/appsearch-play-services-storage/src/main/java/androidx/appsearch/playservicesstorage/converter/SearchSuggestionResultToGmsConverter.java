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

package androidx.appsearch.playservicesstorage.converter;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates between Gms and Jetpack versions of {@link SearchSuggestionResult}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SearchSuggestionResultToGmsConverter {
    private SearchSuggestionResultToGmsConverter() {}

    /** Translates from Platform to Jetpack versions of {@linkSearchSuggestionResult}   */
    @NonNull
    public static List<SearchSuggestionResult> toGmsSearchSuggestionResults(
            @NonNull List<com.google.android.gms.appsearch.SearchSuggestionResult>
                    gmsSearchSuggestionResults) {
        Preconditions.checkNotNull(gmsSearchSuggestionResults);
        List<SearchSuggestionResult> jetpackSearchSuggestionResults =
                new ArrayList<>(gmsSearchSuggestionResults.size());
        for (int i = 0; i < gmsSearchSuggestionResults.size(); i++) {
            jetpackSearchSuggestionResults.add(new SearchSuggestionResult.Builder()
                    .setSuggestedResult(gmsSearchSuggestionResults.get(i).getSuggestedResult())
                    .build());
        }
        return jetpackSearchSuggestionResults;
    }
}
