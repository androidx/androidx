/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.appsearch.app.Features;
import androidx.appsearch.app.SearchSpec;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Map;

/**
 * Translates between Gms and Jetpack versions of {@link SearchSpec}.

 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchSpecToGmsConverter {
    private SearchSpecToGmsConverter() {
    }

    /** Translates from Jetpack to Gms version of {@link SearchSpec}. */
    @NonNull
    public static com.google.android.gms.appsearch.SearchSpec toGmsSearchSpec(
            @NonNull SearchSpec jetpackSearchSpec) {
        Preconditions.checkNotNull(jetpackSearchSpec);
        com.google.android.gms.appsearch.SearchSpec.Builder gmsBuilder =
                new com.google.android.gms.appsearch.SearchSpec.Builder();

        if (!jetpackSearchSpec.getAdvancedRankingExpression().isEmpty()) {
            //TODO(b/274986359) Add GMSCore feature check for Advanced Ranking once available.
            throw new UnsupportedOperationException(
                    Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION
                            + " is not available on this AppSearch implementation.");
        } else {
            gmsBuilder.setRankingStrategy(jetpackSearchSpec.getRankingStrategy());
        }

        gmsBuilder
                .setTermMatch(jetpackSearchSpec.getTermMatch())
                .addFilterSchemas(jetpackSearchSpec.getFilterSchemas())
                .addFilterNamespaces(jetpackSearchSpec.getFilterNamespaces())
                .addFilterPackageNames(jetpackSearchSpec.getFilterPackageNames())
                .setResultCountPerPage(jetpackSearchSpec.getResultCountPerPage())
                .setRankingStrategy(jetpackSearchSpec.getRankingStrategy())
                .setOrder(jetpackSearchSpec.getOrder())
                .setSnippetCount(jetpackSearchSpec.getSnippetCount())
                .setSnippetCountPerProperty(jetpackSearchSpec.getSnippetCountPerProperty())
                .setMaxSnippetSize(jetpackSearchSpec.getMaxSnippetSize());
        if (jetpackSearchSpec.getResultGroupingTypeFlags() != 0) {
            gmsBuilder.setResultGrouping(
                    jetpackSearchSpec.getResultGroupingTypeFlags(),
                    jetpackSearchSpec.getResultGroupingLimit());
        }
        for (Map.Entry<String, List<String>> projection :
                jetpackSearchSpec.getProjections().entrySet()) {
            gmsBuilder.addProjection(projection.getKey(), projection.getValue());
        }

        if (!jetpackSearchSpec.getEnabledFeatures().isEmpty()) {
            if (jetpackSearchSpec.isNumericSearchEnabled()
                    || jetpackSearchSpec.isVerbatimSearchEnabled()
                    || jetpackSearchSpec.isListFilterQueryLanguageEnabled()) {
                //TODO(b/274986359) Add GMSCore feature check for NUMERIC_SEARCH,
                // VERBATIM_SEARCH and LIST_FILTER_QUERY_LANGUAGE once available.
                throw new UnsupportedOperationException(
                        "Advanced query features (NUMERIC_SEARCH, VERBATIM_SEARCH and "
                                + "LIST_FILTER_QUERY_LANGUAGE) are not supported with this "
                                + "backend/Android API level combination.");
            }
        }

        if (!jetpackSearchSpec.getPropertyWeights().isEmpty()) {
            //TODO(b/274986359) Add GMSCore feature check for Property Weights once available.
            throw new UnsupportedOperationException(
                    "Property weights are not supported with this backend/Android API level "
                            + "combination.");
        }

        if (jetpackSearchSpec.getJoinSpec() != null) {
            //TODO(b/274986359) Add GMSCore feature check for Joins once available.
            throw new UnsupportedOperationException("JoinSpec is not available on this "
                    + "AppSearch implementation.");
        }

        if (!jetpackSearchSpec.getFilterProperties().isEmpty()) {
            // TODO(b/296088047): Remove this once property filters become available.
            throw new UnsupportedOperationException(Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES
                    + " is not available on this AppSearch implementation.");
        }

        return gmsBuilder.build();
    }
}
