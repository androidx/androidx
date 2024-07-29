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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Map;

/**
 * Translates between Gms and Jetpack versions of {@link SearchSuggestionSpec}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SearchSuggestionSpecToGmsConverter {
    private SearchSuggestionSpecToGmsConverter() {
    }

    /** Translates from Jetpack to Gms version of {@link SearchSuggestionSpec}. */
    // Most jetpackSearchSuggestionSpec.get calls cause WrongConstant lint errors because the
    // methods are not defined as returning the same constants as the corresponding setter
    // expects, but they do
    @SuppressLint("WrongConstant")
    @NonNull
    public static com.google.android.gms.appsearch.SearchSuggestionSpec toGmsSearchSuggestionSpec(
            @NonNull SearchSuggestionSpec jetpackSearchSuggestionSpec) {
        Preconditions.checkNotNull(jetpackSearchSuggestionSpec);

        com.google.android.gms.appsearch.SearchSuggestionSpec.Builder gmsBuilder =
                new com.google.android.gms.appsearch.SearchSuggestionSpec.Builder(
                        jetpackSearchSuggestionSpec.getMaximumResultCount());

        gmsBuilder
                .addFilterNamespaces(jetpackSearchSuggestionSpec.getFilterNamespaces())
                .addFilterSchemas(jetpackSearchSuggestionSpec.getFilterSchemas())
                .setRankingStrategy(jetpackSearchSuggestionSpec.getRankingStrategy());
        for (Map.Entry<String, List<String>> documentIdFilters :
                jetpackSearchSuggestionSpec.getFilterDocumentIds().entrySet()) {
            gmsBuilder.addFilterDocumentIds(documentIdFilters.getKey(),
                    documentIdFilters.getValue());
        }

        Map<String, List<String>> jetpackFilterProperties =
                jetpackSearchSuggestionSpec.getFilterProperties();
        if (!jetpackFilterProperties.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : jetpackFilterProperties.entrySet()) {
                gmsBuilder.addFilterProperties(entry.getKey(), entry.getValue());
            }
        }

        if (!jetpackSearchSuggestionSpec.getSearchStringParameters().isEmpty()) {
            // TODO(b/332620561): Remove this once search parameter strings are supported.
            throw new UnsupportedOperationException(
                    Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS
                            + " is not available on this AppSearch implementation.");
        }
        return gmsBuilder.build();
    }
}
