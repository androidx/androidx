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

package androidx.appsearch.flags;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class FlagsTest {
    @Test
    public void testFlagValue_enableSafeParcelable2() {
        assertThat(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2).isEqualTo(
                "com.android.appsearch.flags.enable_safe_parcelable_2");
    }

    @Test
    public void testFlagValue_enableListFilterHasPropertyFunction() {
        assertThat(Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION).isEqualTo(
                "com.android.appsearch.flags.enable_list_filter_has_property_function");
    }

    @Test
    public void testFlagValue_enableGroupingTypePerSchema() {
        assertThat(Flags.FLAG_ENABLE_GROUPING_TYPE_PER_SCHEMA).isEqualTo(
                "com.android.appsearch.flags.enable_grouping_type_per_schema");
    }

    @Test
    public void testFlagValue_enableGenericDocumentCopyConstructor() {
        assertThat(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_COPY_CONSTRUCTOR).isEqualTo("com.android"
                + ".appsearch.flags.enable_generic_document_copy_constructor");
    }

    @Test
    public void testFlagValue_enableSearchSpecFilterProperties() {
        assertThat(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES).isEqualTo(
                "com.android.appsearch.flags.enable_search_spec_filter_properties");
    }

    @Test
    public void testFlagValue_enableSearchSpecSetSearchSourceLogTag() {
        assertThat(Flags.FLAG_ENABLE_SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG).isEqualTo(
                "com.android.appsearch.flags.enable_search_spec_set_search_source_log_tag");
    }

    @Test
    public void testFlagValue_enableSetSchemaVisibleToConfigs() {
        assertThat(Flags.FLAG_ENABLE_SET_SCHEMA_VISIBLE_TO_CONFIGS).isEqualTo("com"
                + ".android.appsearch.flags.enable_set_schema_visible_to_configs");
    }

    @Test
    public void testFlagValue_enablePutDocumentsRequestAddTakenActions() {
        assertThat(Flags.FLAG_ENABLE_PUT_DOCUMENTS_REQUEST_ADD_TAKEN_ACTIONS).isEqualTo(
                "com.android.appsearch.flags.enable_put_documents_request_add_taken_actions");
    }

    @Test
    public void testFlagValue_enableGenericDocumentBuilderHiddenMethods() {
        assertThat(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_BUILDER_HIDDEN_METHODS).isEqualTo("com"
                + ".android.appsearch.flags.enable_generic_document_builder_hidden_methods");
    }

    @Test
    public void testFlagValue_enableSetPubliclyVisibleSchema() {
        assertThat(Flags.FLAG_ENABLE_SET_PUBLICLY_VISIBLE_SCHEMA)
                .isEqualTo(
                        "com.android.appsearch.flags.enable_set_publicly_visible_schema");
    }

    @Test
    public void testFlagValue_enableEnterpriseGlobalSearchSession() {
        assertThat(Flags.FLAG_ENABLE_ENTERPRISE_GLOBAL_SEARCH_SESSION)
                .isEqualTo("com.android.appsearch.flags.enable_enterprise_global_search_session");
    }

    @Test
    public void testFlagValue_enableResultDeniedAndResultRateLimited() {
        assertThat(Flags.FLAG_ENABLE_RESULT_DENIED_AND_RESULT_RATE_LIMITED)
                .isEqualTo(
                        "com.android.appsearch.flags.enable_result_denied_and_result_rate_limited");
    }

    @Test
    public void testFlagValue_enableGetParentTypesAndIndexableNestedProperties() {
        assertThat(Flags.FLAG_ENABLE_GET_PARENT_TYPES_AND_INDEXABLE_NESTED_PROPERTIES)
                .isEqualTo(
                        "com.android.appsearch.flags"
                                + ".enable_get_parent_types_and_indexable_nested_properties");
    }

    @Test
    public void testFlagValue_enableSchemaEmbeddingPropertyConfig() {
        assertThat(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
                .isEqualTo("com.android.appsearch.flags.enable_schema_embedding_property_config");
    }

    @Test
    public void testFlagValue_enableSearchSpecSearchStringParameters() {
        assertThat(Flags.FLAG_ENABLE_SEARCH_SPEC_SEARCH_STRING_PARAMETERS)
                .isEqualTo(
                        "com.android.appsearch.flags.enable_search_spec_search_string_parameters");
    }

    @Test
    public void testFlagValue_enableInformationalRankingExpressions() {
        assertThat(Flags.FLAG_ENABLE_INFORMATIONAL_RANKING_EXPRESSIONS)
                .isEqualTo("com.android.appsearch.flags.enable_informational_ranking_expressions");
    }

    @Test
    public void testFlagValue_enableResultAlreadyExists() {
        assertThat(Flags.FLAG_ENABLE_RESULT_ALREADY_EXISTS)
                .isEqualTo("com.android.appsearch.flags.enable_result_already_exists");
    }

    @Test
    public void testFlagValue_enableBlobStore() {
        assertThat(Flags.FLAG_ENABLE_BLOB_STORE)
                .isEqualTo("com.android.appsearch.flags.enable_blob_store");
    }

    @Test
    public void testFlagValue_enableEnterpriseEmptyBatchResultFix() {
        assertThat(Flags.FLAG_ENABLE_ENTERPRISE_EMPTY_BATCH_RESULT_FIX)
                .isEqualTo("com.android.appsearch.flags.enable_enterprise_empty_batch_result_fix");
    }
}
