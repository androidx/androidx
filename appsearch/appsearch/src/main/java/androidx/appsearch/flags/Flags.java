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

// @exportToFramework:skipFile()
package androidx.appsearch.flags;


import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;

import java.util.Collection;

/**
 * Flags to control different features.
 *
 * <p>In Jetpack, those values can't be changed during runtime.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Flags {
    private Flags() {
    }

    // The prefix of all the flags defined for AppSearch. The prefix has
    // "com.android.appsearch.flags", aka the package name for generated AppSearch flag classes in
    // the framework, plus an additional trailing '.'.
    private static final String FLAG_PREFIX =
            "com.android.appsearch.flags.";

    // The full string values for flags defined in the framework.
    //
    // The values of the static variables are the names of the flag defined in the framework's
    // aconfig files. E.g. "enable_safe_parcelable", with FLAG_PREFIX as the prefix.
    //
    // The name of the each static variable should be "FLAG_" + capitalized value of the flag.

    /** Enable SafeParcelable related features. */
    public static final String FLAG_ENABLE_SAFE_PARCELABLE_2 =
            FLAG_PREFIX + "enable_safe_parcelable_2";

    /** Enable the "hasProperty" function in list filter query expressions. */
    public static final String FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION =
            FLAG_PREFIX + "enable_list_filter_has_property_function";

    /** Enable the "matchScoreExpression" function in list filter query expressions. */
    public static final String FLAG_ENABLE_LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION =
            FLAG_PREFIX + "enable_list_filter_match_score_expression_function";

    /** Enable Schema Type Grouping related features. */
    public static final String FLAG_ENABLE_GROUPING_TYPE_PER_SCHEMA =
            FLAG_PREFIX + "enable_grouping_type_per_schema";

    /** Enable GenericDocument to take another GenericDocument to copy construct. */
    public static final String FLAG_ENABLE_GENERIC_DOCUMENT_COPY_CONSTRUCTOR =
            FLAG_PREFIX + "enable_generic_document_copy_constructor";

    /**
     * Enable the {@link androidx.appsearch.app.SearchSpec.Builder#addFilterProperties} and
     * {@link androidx.appsearch.app.SearchSuggestionSpec.Builder#addFilterProperties}.
     */
    public static final String FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES =
            FLAG_PREFIX + "enable_search_spec_filter_properties";
    /**
     * Enable the {@link androidx.appsearch.app.SearchSpec.Builder#setSearchSourceLogTag} method.
     */
    public static final String FLAG_ENABLE_SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG =
            FLAG_PREFIX + "enable_search_spec_set_search_source_log_tag";

    /**
     * Enable {@link androidx.appsearch.app.SearchSpec.Builder#addSearchStringParameters} and
     * {@link androidx.appsearch.app.SearchSuggestionSpec.Builder#addSearchStringParameters}
     * methods.
     */
    public static final String FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS =
            FLAG_PREFIX + "enable_search_spec_search_string_parameters";

    /** Enable addTakenActions API in PutDocumentsRequest. */
    public static final String FLAG_ENABLE_PUT_DOCUMENTS_REQUEST_ADD_TAKEN_ACTIONS =
            FLAG_PREFIX + "enable_put_documents_request_add_taken_actions";

    /** Enable setPubliclyVisibleSchema in SetSchemaRequest. */
    public static final String FLAG_ENABLE_SET_PUBLICLY_VISIBLE_SCHEMA = FLAG_PREFIX
            + "enable_set_publicly_visible_schema";

    /**
     * Enable {@link androidx.appsearch.app.GenericDocument.Builder} to use previously hidden
     * methods.
     */
    public static final String FLAG_ENABLE_GENERIC_DOCUMENT_BUILDER_HIDDEN_METHODS = FLAG_PREFIX
            + "enable_generic_document_builder_hidden_methods";

    public static final String FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS = FLAG_PREFIX
            + "enable_set_schema_visible_to_configs";

    /** Enable {@link androidx.appsearch.app.EnterpriseGlobalSearchSession}. */
    public static final String FLAG_ENABLE_ENTERPRISE_GLOBAL_SEARCH_SESSION =
            FLAG_PREFIX + "enable_enterprise_global_search_session";

    /**
     * Enable {@link androidx.appsearch.app.AppSearchSchema#getDescription} and
     * {@link androidx.appsearch.app.AppSearchSchema.PropertyConfig#getDescription} and the related
     * builders.
     */
    public static final String FLAG_ENABLE_SCHEMA_DESCRIPTION =
            FLAG_PREFIX + "enable_schema_description";

    /**
     * Enable {@link androidx.appsearch.app.AppSearchResult#RESULT_DENIED} and
     * {@link androidx.appsearch.app.AppSearchResult#RESULT_RATE_LIMITED} which were previously
     * hidden.
     */
    public static final String FLAG_ENABLE_RESULT_DENIED_AND_RESULT_RATE_LIMITED =
            FLAG_PREFIX + "enable_result_denied_and_result_rate_limited";

    /**
     * Enables {@link AppSearchSchema#getParentTypes()},
     * {@link AppSearchSchema.DocumentPropertyConfig#getIndexableNestedProperties()} and variants of
     * {@link AppSearchSchema.DocumentPropertyConfig.Builder#addIndexableNestedProperties(Collection)}}.
     */
    public static final String FLAG_ENABLE_GET_PARENT_TYPES_AND_INDEXABLE_NESTED_PROPERTIES =
            FLAG_PREFIX + "enable_get_parent_types_and_indexable_nested_properties";

    /** Enables embedding search related APIs. */
    public static final String FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG =
            FLAG_PREFIX + "enable_schema_embedding_property_config";

    /** Enables embedding quantization. */
    public static final String FLAG_ENABLE_SCHEMA_EMBEDDING_QUANTIZATION =
            FLAG_PREFIX + "enable_schema_embedding_quantization";

    /** Enables informational ranking expressions. */
    public static final String FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS =
            FLAG_PREFIX + "enable_informational_ranking_expressions";

    /** Enable {@link androidx.appsearch.app.AppSearchResult#RESULT_ALREADY_EXISTS}. */
    public static final String FLAG_ENABLE_RESULT_ALREADY_EXISTS =
            FLAG_PREFIX + "enable_result_already_exists";

    /** Enable {@link androidx.appsearch.app.AppSearchBlobHandle}. */
    public static final String FLAG_ENABLE_BLOB_STORE =
            FLAG_PREFIX + "enable_blob_store";

    /** Enable {@link androidx.appsearch.app.GenericDocument#writeToParcel}. */
    public static final String FLAG_ENABLE_GENERIC_DOCUMENT_OVER_IPC =
            FLAG_PREFIX + "enable_generic_document_over_ipc";

    /** Enable empty batch result fix for enterprise GetDocuments. */
    public static final String FLAG_ENABLE_ENTERPRISE_EMPTY_BATCH_RESULT_FIX =
            FLAG_PREFIX + "enable_enterprise_empty_batch_result_fix";

    /** Enables abstract syntax trees to be built and used within AppSearch. */
    public static final String FLAG_ENABLE_ABSTRACT_SYNTAX_TREES =
            FLAG_PREFIX + "enable_abstract_syntax_trees";

    /** Enables the feature of scorable property. */
    public static final String FLAG_ENABLE_SCORABLE_PROPERTY =
            FLAG_PREFIX + "enable_scorable_property";

    /**
     * Enable the {@link androidx.appsearch.app.SearchSpec.Builder#addFilterDocumentIds}.
     */
    public static final String FLAG_ENABLE_SEARCH_SPEC_FILTER_DOCUMENT_IDS =
            FLAG_PREFIX + "enable_search_spec_filter_document_ids";

    /**
     * Enables additional builder copy constructors for
     * {@link androidx.appsearch.app.AppSearchSchema},
     * {@link androidx.appsearch.app.SetSchemaRequest}, {@link androidx.appsearch.app.SearchSpec},
     * {@link androidx.appsearch.app.JoinSpec}, {@link androidx.appsearch.app.AppSearchBatchResult},
     * and {@link androidx.appsearch.app.GetSchemaResponse}.
     */
    public static final String FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS =
            FLAG_PREFIX + "enable_additional_builder_copy_constructors";

    /**
     * Enables wrapping the parent types of a document in the corresponding
     * {@link androidx.appsearch.app.SearchResult}, instead of in
     * {@link androidx.appsearch.app.GenericDocument}.
     */
    public static final String FLAG_ENABLE_SEARCH_RESULT_PARENT_TYPES =
            FLAG_PREFIX + "enable_search_result_parent_types";

    /** Enables delete propagation type related APIs. */
    public static final String FLAG_ENABLE_DELETE_PROPAGATION_TYPE =
            FLAG_PREFIX + "enable_delete_propagation_type";

    /** Enables AppSearch to manage blob files. */
    public static final String FLAG_ENABLE_APP_SEARCH_MANAGE_BLOB_FILES =
            FLAG_PREFIX + "enable_app_search_manage_blob_files";

    /**
     * Enables time since last optimize to be calculated by last attempted optimize run time instead
     * of last successful optimize run time.
     */
    public static final String FLAG_ENABLE_CALCULATE_TIME_SINCE_LAST_ATTEMPTED_OPTIMIZE =
            FLAG_PREFIX + "enable_calculate_time_since_last_attempted_optimize";

    /** Enables qualified id join index v3. */
    public static final String FLAG_ENABLE_QUALIFIED_ID_JOIN_INDEX_V3 =
            FLAG_PREFIX + "enable_qualified_id_join_index_v3";

    /** Enables soft index restoration. */
    public static final String FLAG_ENABLE_SOFT_INDEX_RESTORATION =
            FLAG_PREFIX + "enable_soft_index_restoration";

    /** Enables marker file creation for Optimize API. */
    public static final String FLAG_ENABLE_MARKER_FILE_FOR_OPTIMIZE =
            FLAG_PREFIX + "enable_marker_file_for_optimize";

    /**
     * Enables releasing the backup schema file instance in the schema store if the overlay schema
     * instance exists.
     */
    public static final String FLAG_ENABLE_RELEASE_BACKUP_SCHEMA_FILE_IF_OVERLAY_PRESENT =
            FLAG_PREFIX + "enable_release_backup_schema_file_if_overlay_present";

    /** Enables retrieving embedding match snippet information. This affects */
    public static final String FLAG_ENABLE_EMBEDDING_MATCH_INFO =
            FLAG_PREFIX + "enable_embedding_match_info";

    /** Enables to query visibility documents rather than get. */
    public static final String FLAG_ENABLE_QUERY_VISIBILITY_DOCUMENTS =
            FLAG_PREFIX + "enable_query_visibility_documents";

    /** Enables strict byte size enforcement on a result page. */
    public static final String FLAG_ENABLE_STRICT_PAGE_BYTE_SIZE_LIMIT =
            FLAG_PREFIX + "enable_strict_page_byte_size_limit";

    /** Enables compression threshold. */
    public static final String FLAG_ENABLE_COMPRESSION_THRESHOLD =
            FLAG_PREFIX + "enable_compression_threshold";

    /** Enables setting the gzip compression memlevel to 1. */
    public static final String FLAG_ENABLE_COMPRESSION_MEM_LEVEL_ONE =
            FLAG_PREFIX + "enable_compression_mem_level_one";

    /** Enables gzip decompression buffer size memory optimization. */
    public static final String FLAG_ENABLE_SMALLER_DECOMPRESSION_BUFFER_SIZE =
            FLAG_PREFIX + "enable_smaller_decompression_buffer_size";

    /** Enables {@link androidx.appsearch.app.AppSearchResult#RESULT_ABORTED}. */
    public static final String FLAG_ENABLE_RESULT_ABORTED =
            FLAG_PREFIX + "enable_result_aborted";

    /** Enables {@link androidx.appsearch.app.AppSearchResult#RESULT_UNAVAILABLE}. */
    public static final String FLAG_ENABLE_RESULT_UNAVAILABLE =
            FLAG_PREFIX + "enable_result_unavailable";

    /**
     * Enables throwing {@link androidx.appsearch.exceptions.AppSearchException} with code
     * {@link androidx.appsearch.app.AppSearchResult#RESULT_ABORTED} if the search result page token
     * is not found in native.
     */
    public static final String FLAG_ENABLE_THROW_EXCEPTION_FOR_NATIVE_NOT_FOUND_PAGE_TOKEN =
            FLAG_PREFIX + "enable_throw_exception_for_native_not_found_page_token";

    /**
     * Enable database-scoped set and get schema operations for AppSearch internal impl. This
     * allows AppSearchImpl to set and get the schema for a single package-database combo at a time.
     */
    public static final String FLAG_ENABLE_DATABASE_SCOPED_SCHEMA_OPERATIONS =
            FLAG_PREFIX + "enable_database_scoped_schema_operations";

    /** Enables the Eigen library for embedding scoring, if Eigen is compiled in. */
    public static final String FLAG_ENABLE_EIGEN_EMBEDDING_SCORING =
            FLAG_PREFIX + "enable_eigen_embedding_scoring";

    /**
     * Enable retrying the critical section of initialization before resetting as a last resort.
     */
    public static final String FLAG_ENABLE_INITIALIZATION_RETRIES_BEFORE_RESET =
            FLAG_PREFIX + "enable_initialization_retries_before_reset";

    /** Enable reset visibility store during initialization. */
    public static final String FLAG_ENABLE_RESET_VISIBILITY_STORE =
            FLAG_PREFIX + "enable_reset_visibility_store";

    // Whether the features should be enabled.
    //
    // In Jetpack, those should always return true.

    /** Whether SafeParcelable should be enabled. */
    public static boolean enableSafeParcelable() {
        return true;
    }

    /** Whether the "hasProperty" function in list filter query expressions should be enabled. */
    public static boolean enableListFilterHasPropertyFunction() {
        return true;
    }

    /**
     * Whether the "matchScoreExpression" function in list filter query expressions should be
     * enabled.
     */
    public static boolean enableListFilterMatchScoreExpressionFunction() {
        return true;
    }

    /** Whether Schema Type Grouping should be enabled. */
    public static boolean enableGroupingTypePerSchema() {
        return true;
    }

    /** Whether Generic Document Copy Constructing should be enabled. */
    public static boolean enableGenericDocumentCopyConstructor() {
        return true;
    }

    /**
     * Whether the {@link androidx.appsearch.app.SearchSpec.Builder#addFilterProperties} and
     * {@link androidx.appsearch.app.SearchSuggestionSpec.Builder#addFilterProperties} should be
     * enabled.
     */
    public static boolean enableSearchSpecFilterProperties() {
        return true;
    }

    /**
     * Whether the {@link androidx.appsearch.app.SearchSpec.Builder#setSearchSourceLogTag} should
     * be enabled.
     */
    public static boolean enableSearchSpecSetSearchSourceLogTag() {
        return true;
    }

    /** Whether addTakenActions API in PutDocumentsRequest should be enabled. */
    public static boolean enablePutDocumentsRequestAddTakenActions() {
        return true;
    }

    /** Whether setPubliclyVisibleSchema in SetSchemaRequest.Builder should be enabled. */
    public static boolean enableSetPubliclyVisibleSchema() {
        return true;
    }

    /**
     * Whether {@link androidx.appsearch.app.GenericDocument.Builder#setNamespace(String)},
     * {@link androidx.appsearch.app.GenericDocument.Builder#setId(String)},
     * {@link androidx.appsearch.app.GenericDocument.Builder#setSchemaType(String)}, and
     * {@link androidx.appsearch.app.GenericDocument.Builder#clearProperty(String)}
     * should be enabled.
     */
    public static boolean enableGenericDocumentBuilderHiddenMethods() {
        return true;
    }

    /**
     * Whether
     * {@link androidx.appsearch.app.SetSchemaRequest.Builder #setSchemaTypeVisibilityForConfigs}
     * should be enabled.
     */
    public static boolean enableSetSchemaVisibleToConfigs() {
        return true;
    }

    /** Whether {@link androidx.appsearch.app.EnterpriseGlobalSearchSession} should be enabled. */
    public static boolean enableEnterpriseGlobalSearchSession() {
        return true;
    }

    /**
     * Whether {@link androidx.appsearch.app.AppSearchResult#RESULT_DENIED} and
     * {@link androidx.appsearch.app.AppSearchResult#RESULT_RATE_LIMITED} should be enabled.
     */
    public static boolean enableResultDeniedAndResultRateLimited() {
        return true;
    }

    /**
     * Whether {@link AppSearchSchema#getParentTypes()},
     * {@link AppSearchSchema.DocumentPropertyConfig#getIndexableNestedProperties()} and variants of
     * {@link AppSearchSchema.DocumentPropertyConfig.Builder#addIndexableNestedProperties(Collection)}}
     * should be enabled.
     */
    public static boolean enableGetParentTypesAndIndexableNestedProperties() {
        return true;
    }

    /** Whether embedding search related APIs should be enabled. */
    public static boolean enableSchemaEmbeddingPropertyConfig() {
        return true;
    }

    /** Whether embedding quantization is enabled. */
    public static boolean enableSchemaEmbeddingQuantization() {
        return true;
    }

    /** Whether the search parameter APIs should be enabled. */
    public static boolean enableSearchSpecSearchStringParameters() {
        return true;
    }

    /** Whether informational ranking expressions should be enabled. */
    public static boolean enableInformationalRankingExpressions() {
        return true;
    }

    /**
     * Whether {@link androidx.appsearch.app.AppSearchResult#RESULT_ALREADY_EXISTS} should be
     * enabled.
     */
    public static boolean enableResultAlreadyExists() {
        return true;
    }

    /** Whether {@link androidx.appsearch.app.AppSearchBlobHandle} should be enabled. */
    public static boolean enableBlobStore() {
        return true;
    }

    /** Whether AppSearch manages blob files. */
    public static boolean enableAppSearchManageBlobFiles() {
        return true;
    }

    /** Whether empty batch result fix for enterprise GetDocuments should be enabled. */
    public static boolean enableEnterpriseEmptyBatchResultFix() {
        return true;
    }

    /** Whether AppSearch can create and use abstract syntax trees. */
    public static boolean enableAbstractSyntaxTrees() {
        return true;
    }

    /**
     * Whether additional builder copy constructors for
     * {@link androidx.appsearch.app.AppSearchSchema},
     * {@link androidx.appsearch.app.SetSchemaRequest}, {@link androidx.appsearch.app.SearchSpec},
     * {@link androidx.appsearch.app.JoinSpec}, {@link androidx.appsearch.app.AppSearchBatchResult},
     * and {@link androidx.appsearch.app.GetSchemaResponse} should be enabled.
     */
    public static boolean enableAdditionalBuilderCopyConstructors() {
        return true;
    }

    /**
     * Whether or not the AppSearch should keep track of replaces when calculating the document
     * limit or should call into Icing to get the current active document count when the limit is
     * reached.
     */
    public static boolean enableDocumentLimiterReplaceTracking() {
        return true;
    }

    /**
     * Whether the {@link androidx.appsearch.app.SearchSpec.Builder#addFilterDocumentIds} should be
     * enabled.
     */
    public static boolean enableSearchSpecFilterDocumentIds() {
        return true;
    }

    /** Whether the feature of the scorable property should be enabled. */
    public static boolean enableScorableProperty() {
        return true;
    }

    /**
     * Whether to wrap the parent types of a document in the corresponding
     * {@link androidx.appsearch.app.SearchResult}, instead of in
     * {@link androidx.appsearch.app.GenericDocument}.
     */
    public static boolean enableSearchResultParentTypes() {
        return true;
    }

    /**
     * Whether delete propagation related APIs should be enabled.
     *
     * <p>Note: delete propagation depends on qualified id join index v3, so
     * {@link #enableQualifiedIdJoinIndexV3()} should also be true.
     */
    public static boolean enableDeletePropagationType() {
        // TODO(b/384947619): enable this flag once expiry propagation and dependency check are
        //   implemented.
        return false;
    }

    /**
     * Whether to calculate time since last optimize using last attempted optimize run time instead
     * of last successful optimize run time.
     */
    public static boolean enableCalculateTimeSinceLastAttemptedOptimize() {
        return true;
    }

    /** Whether qualified id join index v3 should be enabled. */
    public static boolean enableQualifiedIdJoinIndexV3() {
        return true;
    }

    /** Whether soft index restoration should be enabled. */
    public static boolean enableSoftIndexRestoration() {
        return true;
    }

    /** Whether marker file creation for Optimize API should be enabled. */
    public static boolean enableMarkerFileForOptimize() {
        return true;
    }

    /**
     * Whether to release the backup schema file instance in the schema store if the overlay schema
     * instance exists.
     */
    public static boolean enableReleaseBackupSchemaFileIfOverlayPresent() {
        return true;
    }

    /**
     * Whether to enable retrieving embedding match info during snippetting.
     */
    public static boolean enableEmbeddingMatchInfo() {
        return true;
    }

    /**
     * Whether to query visibility documents rather than get.
     */
    public static boolean enableQueryVisibilityDocuments() {
        return true;
    }

    /** Whether to enforce page byte size limit in a stricter way. */
    public static boolean enableStrictPageByteSizeLimit() {
        return true;
    }

    /**
     * Whether to enable compression threshold.
     */
    public static boolean enableCompressionThreshold() {
        return true;
    }

    /**
     * Whether to use a compression memlevel of 1.
     */
    public static boolean enableCompressionMemLevelOne() {
        return true;
    }

    /**
     * Whether {@link androidx.appsearch.app.AppSearchResult#RESULT_ABORTED} should be
     * enabled.
     */
    public static boolean enableResultAborted() {
        return true;
    }

    /**
     * Whether {@link androidx.appsearch.app.AppSearchResult#RESULT_UNAVAILABLE} should be
     * enabled.
     */
    public static boolean enableResultUnavailable() {
        return true;
    }

    /**
     * Whether {@link androidx.appsearch.exceptions.AppSearchException} with code
     * {@link androidx.appsearch.app.AppSearchResult#RESULT_ABORTED} should be thrown if the search
     * result page token is not found in native.
     */
    public static boolean enableThrowExceptionForNativeNotFoundPageToken() {
        return true;
    }

    /**
     * Whether to batch put visibility documents.
     */
    public static boolean enableBatchPutVisibilityDocuments() {
        return true;
    }

    /**
     * Whether to enable database-scoped set and get schema operations for AppSearch internal impl.
     */
    public static boolean enableDatabaseScopedSchemaOperations() {
        return true;
    }

    /**
     * Whether to enable gzip decompression buffer memory optimization that uses a smaller buffer
     * size.
     */
    public static boolean enableSmallerDecompressionBufferSize() {
        return true;
    }

    /** Whether to enable the Eigen library for embedding scoring */
    public static boolean enableEigenEmbeddingScoring() {
        // The return value does not matter, since Jetpack does not have Eigen compiled in.
        // Set it to false for clarity.
        return false;
    }

    /**
     * Whether to enable retrying the critical section of initialization before resetting as a
     * last resort.
     */
    public static boolean enableInitializationRetriesBeforeReset() {
        return true;
    }

    /**
     * Whether to enable reset visibility store during initialization.
     */
    public static boolean enableResetVisibilityStore() {
        return true;
    }
}
