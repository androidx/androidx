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
import android.content.Context;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresExtension;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.os.BuildCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

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
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static android.app.appsearch.@NonNull SearchSpec toPlatformSearchSpec(
            @NonNull Context context,
            @NonNull SearchSpec jetpackSearchSpec) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(jetpackSearchSpec);

        android.app.appsearch.SearchSpec.Builder platformBuilder =
                new android.app.appsearch.SearchSpec.Builder();

        if (!jetpackSearchSpec.getAdvancedRankingExpression().isEmpty()) {
            if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.U_BASE) {
                throw new UnsupportedOperationException(
                        Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION
                                + " is not available on this AppSearch implementation.");
            }
            ApiHelperForSdkExtensionUBase.setRankingStrategy(
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
            if ((jetpackSearchSpec.getResultGroupingTypeFlags()
                    & SearchSpec.GROUPING_TYPE_PER_SCHEMA) != 0
                    && BuildCompat.T_EXTENSION_INT
                    < AppSearchVersionUtil.TExtensionVersions.V_BASE) {
                throw new UnsupportedOperationException(
                        Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA
                                + " is not available on this AppSearch implementation.");
            }
            platformBuilder.setResultGrouping(
                    jetpackSearchSpec.getResultGroupingTypeFlags(),
                    jetpackSearchSpec.getResultGroupingLimit());
        }

        // Only translate projection for versions after Android U.
        // Projection will be manually applied for earlier versions in SearchResultsImpl.
        // This is a workaround in Jetpack code for a pre-existing projection bug.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                || AppSearchVersionUtil.getAppSearchVersionCode(context)
                >= AppSearchVersionUtil.APPSEARCH_U_BASE_VERSION_CODE) {
            for (Map.Entry<String, List<String>> projection :
                    jetpackSearchSpec.getProjections().entrySet()) {
                platformBuilder.addProjection(projection.getKey(), projection.getValue());
            }
        }

        if (!jetpackSearchSpec.getPropertyWeights().isEmpty()) {
            if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.U_BASE) {
                throw new UnsupportedOperationException(
                        "Property weights are not supported with this backend/Android API level "
                                + "combination.");
            }
            ApiHelperForSdkExtensionUBase.setPropertyWeights(platformBuilder,
                    jetpackSearchSpec.getPropertyWeights());
        }

        if (!jetpackSearchSpec.getEnabledFeatures().isEmpty()) {
            // Copy U-base sdk extension features
            if (jetpackSearchSpec.isNumericSearchEnabled()
                    || jetpackSearchSpec.isVerbatimSearchEnabled()
                    || jetpackSearchSpec.isListFilterQueryLanguageEnabled()) {
                if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.U_BASE) {
                    throw new UnsupportedOperationException(
                            "Advanced query features (NUMERIC_SEARCH, VERBATIM_SEARCH and "
                                    + "LIST_FILTER_QUERY_LANGUAGE) are not supported with this "
                                    + "backend/Android API level combination.");
                }
                ApiHelperForSdkExtensionUBase.copyEnabledFeatures(platformBuilder,
                        jetpackSearchSpec);
            }

            // Copy V-base sdk extension features
            if (jetpackSearchSpec.isListFilterHasPropertyFunctionEnabled()) {
                if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.V_BASE) {
                    throw new UnsupportedOperationException(
                            Features.LIST_FILTER_HAS_PROPERTY_FUNCTION
                                    + " is not available on this AppSearch implementation.");
                }
                ApiHelperForSdkExtensionVBase.copyEnabledFeatures(platformBuilder,
                        jetpackSearchSpec);
            }

            // Copy B-base sdk extension features
            if (jetpackSearchSpec.isListFilterMatchScoreExpressionFunctionEnabled()) {
                if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                    throw new UnsupportedOperationException(
                            Features.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION
                                    + " is not available on this AppSearch implementation.");
                }
                ApiHelperForSdkExtensionBBase.copyEnabledFeatures(platformBuilder,
                        jetpackSearchSpec);
            }
        }
        if (!jetpackSearchSpec.getEmbeddingParameters().isEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
                throw new UnsupportedOperationException(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG
                        + " is not available on this AppSearch implementation.");
            }
            ApiHelperForB.addEmbeddingParameters(platformBuilder,
                    jetpackSearchSpec.getEmbeddingParameters());
            ApiHelperForB.setDefaultEmbeddingSearchMetricType(platformBuilder,
                    jetpackSearchSpec.getDefaultEmbeddingSearchMetricType());
        }
        if (!jetpackSearchSpec.getSearchStringParameters().isEmpty()) {
            // TODO(b/332620561): Remove this once search parameter strings APIs is supported.
            throw new UnsupportedOperationException(Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS
                    + " is not available on this AppSearch implementation.");
        }

        if (jetpackSearchSpec.getJoinSpec() != null) {
            if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.U_BASE) {
                throw new UnsupportedOperationException("JoinSpec is not available on this "
                        + "AppSearch implementation.");
            }
            ApiHelperForSdkExtensionUBase.setJoinSpec(
                    context, platformBuilder, jetpackSearchSpec.getJoinSpec());
        }

        if (!jetpackSearchSpec.getFilterProperties().isEmpty()) {
            if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.V_BASE) {
                throw new UnsupportedOperationException(Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES
                        + " is not available on this AppSearch implementation.");
            }
            ApiHelperForSdkExtensionVBase.addFilterProperties(
                    platformBuilder, jetpackSearchSpec.getFilterProperties());
        }

        if (jetpackSearchSpec.getSearchSourceLogTag() != null) {
            if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.V_BASE) {
                throw new UnsupportedOperationException(
                        Features.SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG
                                + " is not available on this AppSearch implementation.");
            }
            ApiHelperForSdkExtensionVBase.setSearchSourceLogTag(
                    platformBuilder, jetpackSearchSpec.getSearchSourceLogTag());
        }

        if (!jetpackSearchSpec.getInformationalRankingExpressions().isEmpty()) {
            if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                throw new UnsupportedOperationException(
                        Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS
                                + " are not available on this AppSearch implementation.");
            }
            ApiHelperForSdkExtensionBBase.addInformationalRankingExpressions(platformBuilder,
                    jetpackSearchSpec.getInformationalRankingExpressions());
        }

        if (!jetpackSearchSpec.getFilterDocumentIds().isEmpty()) {
            if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                throw new UnsupportedOperationException(
                        Features.SEARCH_SPEC_ADD_FILTER_DOCUMENT_IDS
                                + " is not available on this AppSearch implementation.");
            }
            ApiHelperForSdkExtensionBBase.addFilterDocumentIds(
                    platformBuilder, jetpackSearchSpec.getFilterDocumentIds());
        }

        if (jetpackSearchSpec.isScorablePropertyRankingEnabled()) {
            if (BuildCompat.T_EXTENSION_INT < AppSearchVersionUtil.TExtensionVersions.B_BASE) {
                throw new UnsupportedOperationException(
                        Features.SCHEMA_SCORABLE_PROPERTY_CONFIG
                                + " is not available on this AppSearch implementation.");
            }
            ApiHelperForSdkExtensionBBase.setScorablePropertyRankingEnabled(
                    platformBuilder, jetpackSearchSpec.isScorablePropertyRankingEnabled());
        }
        return platformBuilder.build();
    }

    // TODO(b/412457272): Consider extracting ApiHelpers to allow sharing between converter classes
    @SuppressLint("NewApi")
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU,
            version = AppSearchVersionUtil.TExtensionVersions.U_BASE)
    private static class ApiHelperForSdkExtensionUBase {
        private ApiHelperForSdkExtensionUBase() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setRankingStrategy(android.app.appsearch.SearchSpec.@NonNull Builder builder,
                @NonNull String rankingExpression) {
            builder.setRankingStrategy(rankingExpression);
        }

        @DoNotInline
        static void setJoinSpec(@NonNull Context context,
                android.app.appsearch.SearchSpec.@NonNull Builder builder,
                JoinSpec jetpackJoinSpec) {
            builder.setJoinSpec(JoinSpecToPlatformConverter.toPlatformJoinSpec(context,
                    jetpackJoinSpec));
        }

        @DoNotInline
        static void copyEnabledFeatures(android.app.appsearch.SearchSpec.@NonNull Builder builder,
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
        static void setPropertyWeights(android.app.appsearch.SearchSpec.@NonNull Builder builder,
                @NonNull Map<String, Map<String, Double>> propertyWeightsMap) {
            for (Map.Entry<String, Map<String, Double>> entry : propertyWeightsMap.entrySet()) {
                builder.setPropertyWeights(entry.getKey(), entry.getValue());
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU,
            version = AppSearchVersionUtil.TExtensionVersions.V_BASE)
    private static class ApiHelperForSdkExtensionVBase {
        private ApiHelperForSdkExtensionVBase() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void copyEnabledFeatures(
                android.app.appsearch.SearchSpec.@NonNull Builder platformBuilder,
                @NonNull SearchSpec jetpackSpec) {
            if (jetpackSpec.isListFilterHasPropertyFunctionEnabled()) {
                platformBuilder.setListFilterHasPropertyFunctionEnabled(true);
            }
        }

        @DoNotInline
        static void addFilterProperties(
                android.app.appsearch.SearchSpec.@NonNull Builder platformBuilder,
                Map<String, List<String>> properties) {
            for (Map.Entry<String, List<String>> entry : properties.entrySet()) {
                platformBuilder.addFilterProperties(entry.getKey(), entry.getValue());
            }
        }

        @DoNotInline
        static void setSearchSourceLogTag(
                android.app.appsearch.SearchSpec.Builder platformBuilder,
                String searchSourceLogTag) {
            platformBuilder.setSearchSourceLogTag(searchSourceLogTag);
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU,
            version = AppSearchVersionUtil.TExtensionVersions.B_BASE)
    private static class ApiHelperForSdkExtensionBBase {
        private ApiHelperForSdkExtensionBBase() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setScorablePropertyRankingEnabled(
                android.app.appsearch.SearchSpec.Builder platformBuilder,
                boolean isScorablePropertyRankingEnabled) {
            platformBuilder.setScorablePropertyRankingEnabled(isScorablePropertyRankingEnabled);
        }

        @DoNotInline
        @OptIn(markerClass = ExperimentalAppSearchApi.class)
        static void copyEnabledFeatures(
                android.app.appsearch.SearchSpec.@NonNull Builder platformBuilder,
                @NonNull SearchSpec jetpackSpec) {
            if (jetpackSpec.isListFilterMatchScoreExpressionFunctionEnabled()) {
                platformBuilder.setListFilterMatchScoreExpressionFunctionEnabled(true);
            }
        }

        @DoNotInline
        static void addInformationalRankingExpressions(
                android.app.appsearch.SearchSpec.@NonNull Builder platformBuilder,
                @NonNull List<String> informationalRankingExpressions) {
            platformBuilder.addInformationalRankingExpressions(informationalRankingExpressions);
        }

        @DoNotInline
        static void addFilterDocumentIds(
                android.app.appsearch.SearchSpec.@NonNull Builder platformBuilder,
                @NonNull List<String> documentIds) {
            platformBuilder.addFilterDocumentIds(documentIds);
        }
    }

    @RequiresApi(36)
    @SuppressLint("NewApi") // EmbeddingVector incorrectly flagged as 34-ext16
    private static class ApiHelperForB {
        private ApiHelperForB() {
        }

        @DoNotInline
        static void addEmbeddingParameters(
                android.app.appsearch.SearchSpec.@NonNull Builder platformBuilder,
                @NonNull List<EmbeddingVector> embeddingVectors) {
            android.app.appsearch.EmbeddingVector[] platformEmbeddingVectors =
                    new android.app.appsearch.EmbeddingVector[embeddingVectors.size()];
            for (int i = 0; i < embeddingVectors.size(); i++) {
                platformEmbeddingVectors[i] = new android.app.appsearch.EmbeddingVector(
                        embeddingVectors.get(i).getValues(),
                        embeddingVectors.get(i).getModelSignature());
            }
            platformBuilder.addEmbeddingParameters(platformEmbeddingVectors);
        }

        @DoNotInline
        @SuppressLint("WrongConstant")
        static void setDefaultEmbeddingSearchMetricType(
                android.app.appsearch.SearchSpec.@NonNull Builder platformBuilder,
                @SearchSpec.EmbeddingSearchMetricType int defaultEmbeddingSearchMetricType) {
            platformBuilder.setDefaultEmbeddingSearchMetricType(defaultEmbeddingSearchMetricType);
        }
    }
}
