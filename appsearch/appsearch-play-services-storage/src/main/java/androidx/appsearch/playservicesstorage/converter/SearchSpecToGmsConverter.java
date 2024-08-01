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
            gmsBuilder.setRankingStrategy(jetpackSearchSpec.getAdvancedRankingExpression());
        } else {
            gmsBuilder.setRankingStrategy(jetpackSearchSpec.getRankingStrategy());
        }

        gmsBuilder
                .setTermMatch(jetpackSearchSpec.getTermMatch())
                .addFilterSchemas(jetpackSearchSpec.getFilterSchemas())
                .addFilterNamespaces(jetpackSearchSpec.getFilterNamespaces())
                .addFilterPackageNames(jetpackSearchSpec.getFilterPackageNames())
                .setResultCountPerPage(jetpackSearchSpec.getResultCountPerPage())
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

        if (!jetpackSearchSpec.getPropertyWeights().isEmpty()) {
            for (Map.Entry<String, Map<String, Double>> entry :
                    jetpackSearchSpec.getPropertyWeights().entrySet()) {
                gmsBuilder.setPropertyWeights(entry.getKey(), entry.getValue());
            }
        }

        if (!jetpackSearchSpec.getEnabledFeatures().isEmpty()) {
            if (jetpackSearchSpec.isNumericSearchEnabled()) {
                gmsBuilder.setNumericSearchEnabled(true);
            }
            if (jetpackSearchSpec.isVerbatimSearchEnabled()) {
                gmsBuilder.setVerbatimSearchEnabled(true);
            }
            if (jetpackSearchSpec.isListFilterQueryLanguageEnabled()) {
                gmsBuilder.setListFilterQueryLanguageEnabled(true);
            }
            if (jetpackSearchSpec.isListFilterHasPropertyFunctionEnabled()) {
                gmsBuilder.setListFilterHasPropertyFunctionEnabled(true);
            }
        }
        if (!jetpackSearchSpec.getEmbeddingParameters().isEmpty()) {
            // TODO(b/326656531): Remove this once embedding search APIs are available.
            throw new UnsupportedOperationException(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG
                    + " is not available on this AppSearch implementation.");
        }
        if (!jetpackSearchSpec.getSearchStringParameters().isEmpty()) {
            // TODO(b/332620561): Remove this once search parameter strings are supported.
            throw new UnsupportedOperationException(Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS
                    + " is not available on this AppSearch implementation.");
        }

        if (jetpackSearchSpec.getJoinSpec() != null) {
            gmsBuilder.setJoinSpec(JoinSpecToGmsConverter.toGmsJoinSpec(
                    jetpackSearchSpec.getJoinSpec()));
        }

        if (!jetpackSearchSpec.getFilterProperties().isEmpty()) {
            for (Map.Entry<String, List<String>> entry :
                    jetpackSearchSpec.getFilterProperties().entrySet()) {
                gmsBuilder.addFilterProperties(entry.getKey(), entry.getValue());
            }
        }

        if (jetpackSearchSpec.getSearchSourceLogTag() != null) {
            gmsBuilder.setSearchSourceLogTag(jetpackSearchSpec.getSearchSourceLogTag());
        }

        if (!jetpackSearchSpec.getInformationalRankingExpressions().isEmpty()) {
            // TODO(b/332642571): Remove this once informational ranking expressions are available.
            throw new UnsupportedOperationException(
                    Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS
                            + " are not available on this AppSearch implementation.");
        }

        return gmsBuilder.build();
    }
}
