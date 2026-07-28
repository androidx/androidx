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

package androidx.appsearch.platformstorage;

import android.annotation.SuppressLint;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter to handle conversion of Jetpack AppSearch APIs that are not yet supported by the
 * platform.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
public interface PlatformConversionAdapter {
    // --- SchemaToPlatformConverter ---

    /**
     * Handles setting the schema description.
     */
    default void setSchemaDescription(
            android.app.appsearch.AppSearchSchema.@NonNull Builder builder,
            @NonNull String description) {
        throw new UnsupportedOperationException(Features.SCHEMA_SET_DESCRIPTION
                + " is not available on this AppSearch implementation.");
    }

    /**
     * Handles setting the property description.
     *
     * @param builder - A builder whose build method will produce an instance that inherits from
     *                  {@link androidx.appsearch.app.AppSearchSchema.PropertyConfig}.
     * @param description - the description to set on the provided builder.
     */
    default void setPropertyDescription(
            @NonNull Object builder,
            @NonNull String description) {
        throw new UnsupportedOperationException(Features.SCHEMA_SET_DESCRIPTION
                + " is not available on this AppSearch implementation.");
    }

    /**
     * Handles setting the string property delete propagation type.
     */
    default void setDeletePropagationType(
            android.app.appsearch.AppSearchSchema.StringPropertyConfig.@NonNull Builder builder,
            @AppSearchSchema.StringPropertyConfig.DeletePropagationType
            int deletePropagationType) {
        throw new UnsupportedOperationException(
                "StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM is not supported"
                        + " on this AppSearch implementation.");
    }

    /**
     * Handles setting the embedding property indexing type.
     */
    default void setAnnIndexingType(
            android.app.appsearch.AppSearchSchema.EmbeddingPropertyConfig.@NonNull Builder builder,
            @AppSearchSchema.EmbeddingPropertyConfig.IndexingType
            int indexingType) {
        throw new UnsupportedOperationException(
                Features.SCHEMA_EMBEDDING_APPROXIMATE_NEAREST_NEIGHBOR
                        + " is not available on this AppSearch implementation.");
    }

    // --- GenericDocumentToPlatformConverter ---

    /**
     * Handles converting a Jetpack EmbeddingVector to a platform one.
     */
    default android.app.appsearch.@NonNull EmbeddingVector convertQuantizedEmbeddingVector(
            @NonNull EmbeddingVector jetpackEmbeddingVector) {
        throw new UnsupportedOperationException(
                Features.SCHEMA_EMBEDDING_PRE_QUANTIZED_DATA
                        + " is not available on this AppSearch implementation.");
    }

    /**
     * Handles converting a platform EmbeddingVector to a Jetpack one.
     *
     * <p>This default implementation does not consider pre-quantized embeddings because this API
     * does not exist in Jetpack. This method is defined in the adapter so it can be overridden
     * when needed and where the platform API is available.
     */
    @SuppressLint("NewApi") // getValues() is incorrectly flagged as needing 34-ext16
    default @NonNull EmbeddingVector toJetpackEmbeddingVector(
            android.app.appsearch.@NonNull EmbeddingVector platformEmbeddingVector) {
        return new EmbeddingVector(
                platformEmbeddingVector.getValues(),
                platformEmbeddingVector.getModelSignature());
    }

    // --- SetSchemaRequestToPlatformConverter ---

    /**
     * Handles setting schema wipeout account property paths.
     */
    default void setSchemasWipeoutAccountPropertyPaths(
            android.app.appsearch.SetSchemaRequest.@NonNull Builder builder,
            @NonNull Map<String, Set<String>> paths) {
        throw new UnsupportedOperationException(
                Features.SET_SCHEMA_REQUEST_SET_WIPEOUT_ACCOUNT
                        + " is not available on this AppSearch implementation.");
    }

    // --- SearchSpecToPlatformConverter ---

    /**
     * Handles setting search string parameters.
     */
    default void setSearchStringParameters(
            android.app.appsearch.SearchSpec.@NonNull Builder builder,
            @NonNull List<String> searchStringParameters) {
        throw new UnsupportedOperationException(
                Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS
                        + " is not available on this AppSearch implementation.");
    }

    /**
     * Handles setting embedding query probe count.
     */
    default void setEmbeddingQueryProbeCount(
            android.app.appsearch.SearchSpec.@NonNull Builder builder,
            int embeddingQueryProbeCount) {
        throw new UnsupportedOperationException(
                Features.SCHEMA_EMBEDDING_APPROXIMATE_NEAREST_NEIGHBOR
                        + " is not available on this AppSearch implementation.");
    }

    // --- SearchSuggestionSpecToPlatformConverter ---

    /**
     * Handles setting search suggestion spec search string parameters.
     */
    default void setSearchStringParameters(
            android.app.appsearch.SearchSuggestionSpec.@NonNull Builder builder,
            @NonNull List<String> searchStringParameters) {
        throw new UnsupportedOperationException(
                Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS
                        + " is not available on this AppSearch implementation.");
    }
}
