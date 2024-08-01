/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.platformstorage.converter;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.core.util.Preconditions;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Translates between Platform and Jetpack versions of {@link SearchSuggestionSpec}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public final class SearchSuggestionSpecToPlatformConverter {
    private SearchSuggestionSpecToPlatformConverter() {
    }

    /** Translates from Jetpack to Platform version of {@link SearchSuggestionSpec}. */
    // Most jetpackSearchSuggestionSpec.get calls cause WrongConstant lint errors because the
    // methods are not defined as returning the same constants as the corresponding setter
    // expects, but they do
    @SuppressLint("WrongConstant")
    @NonNull
    public static android.app.appsearch.SearchSuggestionSpec toPlatformSearchSuggestionSpec(
            @NonNull SearchSuggestionSpec jetpackSearchSuggestionSpec) {
        Preconditions.checkNotNull(jetpackSearchSuggestionSpec);

        android.app.appsearch.SearchSuggestionSpec.Builder platformBuilder =
                new android.app.appsearch.SearchSuggestionSpec.Builder(
                        jetpackSearchSuggestionSpec.getMaximumResultCount());

        platformBuilder
                .addFilterNamespaces(jetpackSearchSuggestionSpec.getFilterNamespaces())
                .addFilterSchemas(jetpackSearchSuggestionSpec.getFilterSchemas())
                .setRankingStrategy(jetpackSearchSuggestionSpec.getRankingStrategy());
        for (Map.Entry<String, List<String>> documentIdFilters :
                jetpackSearchSuggestionSpec.getFilterDocumentIds().entrySet()) {
            platformBuilder.addFilterDocumentIds(documentIdFilters.getKey(),
                    documentIdFilters.getValue());
        }

        Map<String, List<String>> jetpackFilterProperties =
                jetpackSearchSuggestionSpec.getFilterProperties();
        if (!jetpackFilterProperties.isEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                throw new UnsupportedOperationException(Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES
                        + " is not available on this AppSearch implementation.");
            }
            for (Map.Entry<String, List<String>> entry : jetpackFilterProperties.entrySet()) {
                ApiHelperForV.addFilterProperties(
                        platformBuilder, entry.getKey(), entry.getValue());
            }
        }
        if (!jetpackSearchSuggestionSpec.getSearchStringParameters().isEmpty()) {
            // TODO(b/332620561): Remove this once search parameter strings APIs is supported.
            throw new UnsupportedOperationException(
                    Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS
                            + " is not available on this AppSearch implementation.");
        }
        return platformBuilder.build();
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private static class ApiHelperForV {
        private ApiHelperForV() {}

        @DoNotInline
        static void addFilterProperties(
                android.app.appsearch.SearchSuggestionSpec.Builder platformBuilder,
                String schema,
                Collection<String> propertyPaths) {
            platformBuilder.addFilterProperties(schema, propertyPaths);
        }
    }
}
