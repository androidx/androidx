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
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.SearchSpec;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.Map;

/**
 * Translates between Platform and Jetpack versions of {@link SearchSpec}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class SearchSpecToPlatformConverter {
    private SearchSpecToPlatformConverter() {
    }

    /** Translates from Jetpack to Platform version of {@link SearchSpec}. */
    // Most jetpackSearchSpec.get calls cause WrongConstant lint errors because the methods are not
    // defined as returning the same constants as the corresponding setter expects, but they do
    @SuppressLint("WrongConstant")
    @NonNull
    public static android.app.appsearch.SearchSpec toPlatformSearchSpec(
            @NonNull SearchSpec jetpackSearchSpec) {
        Preconditions.checkNotNull(jetpackSearchSpec);

        android.app.appsearch.SearchSpec.Builder platformBuilder =
                new android.app.appsearch.SearchSpec.Builder();

        if (!jetpackSearchSpec.getAdvancedRankingExpression().isEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                throw new UnsupportedOperationException(
                        Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION
                                + " is not available on this AppSearch implementation.");
            }
            ApiHelperForU.setRankingStrategy(
                    platformBuilder, jetpackSearchSpec.getAdvancedRankingExpression());
        } else {
            platformBuilder.setRankingStrategy(jetpackSearchSpec.getRankingStrategy());
        }

        platformBuilder
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
            // TODO(b/258715421): Add Build.VERSION.SDK_INT condition once there is an extservices
            // sdk that includes SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA.
            if (true) {
                if ((jetpackSearchSpec.getResultGroupingTypeFlags()
                        & SearchSpec.GROUPING_TYPE_PER_SCHEMA) != 0) {
                    throw new UnsupportedOperationException(
                        Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA
                            + " is not available on this AppSearch implementation.");
                }
            }
            platformBuilder.setResultGrouping(
                    jetpackSearchSpec.getResultGroupingTypeFlags(),
                    jetpackSearchSpec.getResultGroupingLimit());
        }
        for (Map.Entry<String, List<String>> projection :
                jetpackSearchSpec.getProjections().entrySet()) {
            platformBuilder.addProjection(projection.getKey(), projection.getValue());
        }

        if (!jetpackSearchSpec.getPropertyWeights().isEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                throw new UnsupportedOperationException(
                        "Property weights are not supported with this backend/Android API level "
                                + "combination.");
            }
            ApiHelperForU.setPropertyWeights(platformBuilder,
                    jetpackSearchSpec.getPropertyWeights());
        }

        if (!jetpackSearchSpec.getEnabledFeatures().isEmpty()) {
            if (jetpackSearchSpec.isNumericSearchEnabled()
                    || jetpackSearchSpec.isVerbatimSearchEnabled()
                    || jetpackSearchSpec.isListFilterQueryLanguageEnabled()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    throw new UnsupportedOperationException(
                            "Advanced query features (NUMERIC_SEARCH, VERBATIM_SEARCH and "
                                    + "LIST_FILTER_QUERY_LANGUAGE) are not supported with this "
                                    + "backend/Android API level combination.");
                }
                ApiHelperForU.copyEnabledFeatures(platformBuilder, jetpackSearchSpec);
            }
        }

        if (jetpackSearchSpec.getJoinSpec() != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                throw new UnsupportedOperationException("JoinSpec is not available on this "
                        + "AppSearch implementation.");
            }
            ApiHelperForU.setJoinSpec(platformBuilder, jetpackSearchSpec.getJoinSpec());
        }

        if (!jetpackSearchSpec.getFilterProperties().isEmpty()) {
            // TODO(b/296088047): Remove this once property filters become available.
            throw new UnsupportedOperationException(Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES
                    + " is not available on this AppSearch implementation.");
        }
        return platformBuilder.build();
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private static class ApiHelperForU {
        private ApiHelperForU() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setJoinSpec(@NonNull android.app.appsearch.SearchSpec.Builder builder,
                JoinSpec jetpackJoinSpec) {
            builder.setJoinSpec(JoinSpecToPlatformConverter.toPlatformJoinSpec(jetpackJoinSpec));
        }

        @DoNotInline
        static void setRankingStrategy(@NonNull android.app.appsearch.SearchSpec.Builder builder,
                @NonNull String rankingExpression) {
            builder.setRankingStrategy(rankingExpression);
        }

        @DoNotInline
        static void copyEnabledFeatures(@NonNull android.app.appsearch.SearchSpec.Builder builder,
                @NonNull SearchSpec jetpackSpec) {
            if (jetpackSpec.isNumericSearchEnabled()) {
                builder.setNumericSearchEnabled(true);
            }
            if (jetpackSpec.isVerbatimSearchEnabled()) {
                builder.setVerbatimSearchEnabled(true);
            }
            if (jetpackSpec.isListFilterQueryLanguageEnabled()) {
                builder.setListFilterQueryLanguageEnabled(true);
            }
        }

        @DoNotInline
        static void setPropertyWeights(@NonNull android.app.appsearch.SearchSpec.Builder builder,
                @NonNull Map<String, Map<String, Double>> propertyWeightsMap) {
            for (Map.Entry<String, Map<String, Double>> entry : propertyWeightsMap.entrySet()) {
                builder.setPropertyWeights(entry.getKey(), entry.getValue());
            }
        }
    }
}
