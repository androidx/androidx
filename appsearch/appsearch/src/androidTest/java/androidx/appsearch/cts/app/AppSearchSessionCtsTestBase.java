/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.cts.app;

import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_ARGUMENT;
import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_SCHEMA;
import static androidx.appsearch.app.AppSearchResult.RESULT_NOT_FOUND;
import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;
import static androidx.appsearch.testutil.AppSearchTestUtils.doGet;
import static androidx.appsearch.testutil.AppSearchTestUtils.retrieveAllSearchResults;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.cts.app.customer.EmailDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.util.DocumentIdUtil;
import androidx.collection.ArrayMap;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public abstract class AppSearchSessionCtsTestBase {
    static final String DB_NAME_1 = "";
    static final String DB_NAME_2 = "testDb2";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private AppSearchSession mDb1;
    private AppSearchSession mDb2;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName);

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor);

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        mDb2 = createSearchSessionAsync(DB_NAME_2).get();

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        cleanup();
    }

    private void cleanup() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Test
    public void testSetSchema() throws Exception {
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();
    }

    @Test
    public void testSetSchema_Failure() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchSchema emailSchema1 = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .build();

        SetSchemaRequest setSchemaRequest1 =
                new SetSchemaRequest.Builder().addSchemas(emailSchema1).build();
        ExecutionException executionException =
                assertThrows(ExecutionException.class,
                        () -> mDb1.setSchemaAsync(setSchemaRequest1).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_SCHEMA);
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
        assertThat(exception).hasMessageThat().contains("Incompatible types: {builtin:Email}");

        SetSchemaRequest setSchemaRequest2 = new SetSchemaRequest.Builder().build();
        executionException = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(setSchemaRequest2).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_SCHEMA);
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
        assertThat(exception).hasMessageThat().contains("Deleted types: {builtin:Email}");
    }

    @Test
    public void testSetSchema_updateVersion() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(1).build()).get();

        Set<AppSearchSchema> actualSchemaTypes = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actualSchemaTypes).containsExactly(schema);

        // increase version number
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(2).build()).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
        assertThat(getSchemaResponse.getVersion()).isEqualTo(2);
    }

    @Test
    public void testSetSchema_checkVersion() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // set different version number to different database.
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(135).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema)
                .setVersion(246).build()).get();


        // check the version has been set correctly.
        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
        assertThat(getSchemaResponse.getVersion()).isEqualTo(135);

        getSchemaResponse = mDb2.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
        assertThat(getSchemaResponse.getVersion()).isEqualTo(246);
    }

// @exportToFramework:startStrip()

    @Test
    public void testSetSchema_addDocumentClasses() throws Exception {
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();
    }
// @exportToFramework:endStrip()

// @exportToFramework:startStrip()

    @Test
    public void testGetSchema() throws Exception {
        AppSearchSchema emailSchema1 = new AppSearchSchema.Builder("Email1")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema emailSchema2 = new AppSearchSchema.Builder("Email2")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)  // Diff
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)  // Diff
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        SetSchemaRequest request1 = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema1).addDocumentClasses(EmailDocument.class).build();
        SetSchemaRequest request2 = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema2).addDocumentClasses(EmailDocument.class).build();

        mDb1.setSchemaAsync(request1).get();
        mDb2.setSchemaAsync(request2).get();

        Set<AppSearchSchema> actual1 = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual1).hasSize(2);
        assertThat(actual1).isEqualTo(request1.getSchemas());
        Set<AppSearchSchema> actual2 = mDb2.getSchemaAsync().get().getSchemas();
        assertThat(actual2).hasSize(2);
        assertThat(actual2).isEqualTo(request2.getSchemas());
    }
// @exportToFramework:endStrip()

    @Test
    public void testGetSchema_allPropertyTypes() throws Exception {
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("string")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("double")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("boolean")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .build())
                .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "document", AppSearchEmail.SCHEMA_TYPE)
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();

        // Add it to AppSearch and then obtain it again
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(inSchema, AppSearchEmail.SCHEMA).build()).get();
        GetSchemaResponse response = mDb1.getSchemaAsync().get();
        List<AppSearchSchema> schemas = new ArrayList<>(response.getSchemas());
        assertThat(schemas).containsExactly(inSchema, AppSearchEmail.SCHEMA);
        AppSearchSchema outSchema;
        if (schemas.get(0).getSchemaType().equals("Test")) {
            outSchema = schemas.get(0);
        } else {
            outSchema = schemas.get(1);
        }
        assertThat(outSchema.getSchemaType()).isEqualTo("Test");
        assertThat(outSchema).isNotSameInstanceAs(inSchema);

        List<PropertyConfig> properties = outSchema.getProperties();
        assertThat(properties).hasSize(6);

        assertThat(properties.get(0).getName()).isEqualTo("string");
        assertThat(properties.get(0).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(((StringPropertyConfig) properties.get(0)).getIndexingType())
                .isEqualTo(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS);
        assertThat(((StringPropertyConfig) properties.get(0)).getTokenizerType())
                .isEqualTo(StringPropertyConfig.TOKENIZER_TYPE_PLAIN);

        assertThat(properties.get(1).getName()).isEqualTo("long");
        assertThat(properties.get(1).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(properties.get(1)).isInstanceOf(AppSearchSchema.LongPropertyConfig.class);

        assertThat(properties.get(2).getName()).isEqualTo("double");
        assertThat(properties.get(2).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REPEATED);
        assertThat(properties.get(2)).isInstanceOf(AppSearchSchema.DoublePropertyConfig.class);

        assertThat(properties.get(3).getName()).isEqualTo("boolean");
        assertThat(properties.get(3).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REQUIRED);
        assertThat(properties.get(3)).isInstanceOf(AppSearchSchema.BooleanPropertyConfig.class);

        assertThat(properties.get(4).getName()).isEqualTo("bytes");
        assertThat(properties.get(4).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_OPTIONAL);
        assertThat(properties.get(4)).isInstanceOf(AppSearchSchema.BytesPropertyConfig.class);

        assertThat(properties.get(5).getName()).isEqualTo("document");
        assertThat(properties.get(5).getCardinality())
                .isEqualTo(PropertyConfig.CARDINALITY_REPEATED);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(5)).getSchemaType())
                .isEqualTo(AppSearchEmail.SCHEMA_TYPE);
        assertThat(((AppSearchSchema.DocumentPropertyConfig) properties.get(5))
                .shouldIndexNestedProperties()).isEqualTo(true);
    }

    @Test
    public void testGetSchema_visibilitySetting() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email1")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        byte[] shar256Cert1 = new byte[32];
        Arrays.fill(shar256Cert1, (byte) 1);
        byte[] shar256Cert2 = new byte[32];
        Arrays.fill(shar256Cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 =
                new PackageIdentifier("pkgFoo", shar256Cert1);
        PackageIdentifier packageIdentifier2 =
                new PackageIdentifier("pkgBar", shar256Cert2);
        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema)
                .setSchemaTypeDisplayedBySystem("Email1", /*displayed=*/false)
                .setSchemaTypeVisibilityForPackage("Email1", /*visible=*/true,
                        packageIdentifier1)
                .setSchemaTypeVisibilityForPackage("Email1", /*visible=*/true,
                        packageIdentifier2)
                .addRequiredPermissionsForSchemaTypeVisibility("Email1",
                        ImmutableSet.of(SetSchemaRequest.READ_SMS, SetSchemaRequest.READ_CALENDAR))
                .addRequiredPermissionsForSchemaTypeVisibility("Email1",
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                .build();

        mDb1.setSchemaAsync(request).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        Set<AppSearchSchema> actual = getSchemaResponse.getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).isEqualTo(request.getSchemas());
        assertThat(getSchemaResponse.getSchemaTypesNotDisplayedBySystem())
                .containsExactly("Email1");
        assertThat(getSchemaResponse.getSchemaTypesVisibleToPackages())
                .containsExactly("Email1", ImmutableSet.of(
                        packageIdentifier1, packageIdentifier2));
        assertThat(getSchemaResponse.getRequiredPermissionsForSchemaTypeVisibility())
                .containsExactly("Email1", ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                SetSchemaRequest.READ_CALENDAR),
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA)));
    }

    @Test
    public void testGetSchema_visibilitySetting_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email1")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        byte[] shar256Cert1 = new byte[32];
        Arrays.fill(shar256Cert1, (byte) 1);
        byte[] shar256Cert2 = new byte[32];
        Arrays.fill(shar256Cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 =
                new PackageIdentifier("pkgFoo", shar256Cert1);
        PackageIdentifier packageIdentifier2 =
                new PackageIdentifier("pkgBar", shar256Cert2);
        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema)
                .setSchemaTypeDisplayedBySystem("Email1", /*displayed=*/false)
                .setSchemaTypeVisibilityForPackage("Email1", /*visible=*/true,
                        packageIdentifier1)
                .setSchemaTypeVisibilityForPackage("Email1", /*visible=*/true,
                        packageIdentifier2)
                .build();

        mDb1.setSchemaAsync(request).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        Set<AppSearchSchema> actual = getSchemaResponse.getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).isEqualTo(request.getSchemas());
        assertThrows(
                UnsupportedOperationException.class,
                () -> getSchemaResponse.getSchemaTypesNotDisplayedBySystem());
        assertThrows(
                UnsupportedOperationException.class,
                () -> getSchemaResponse.getSchemaTypesVisibleToPackages());
        assertThrows(
                UnsupportedOperationException.class,
                () -> getSchemaResponse.getRequiredPermissionsForSchemaTypeVisibility());
    }

    @Test
    public void testSetSchema_visibilitySettingPermission_notSupported() {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email1").build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(emailSchema)
                .setSchemaTypeDisplayedBySystem("Email1", /*displayed=*/false)
                .addRequiredPermissionsForSchemaTypeVisibility("Email1",
                        ImmutableSet.of(SetSchemaRequest.READ_SMS))
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.setSchemaAsync(request).get());
    }

    @Test
    public void testGetSchema_longPropertyIndexingType() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new LongPropertyConfig.Builder("indexableLong")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_NONE)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testGetSchema_longPropertyIndexingTypeNone_succeeds() throws Exception {
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_NONE)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testGetSchema_longPropertyIndexingTypeRange_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new LongPropertyConfig.Builder("indexableLong")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () ->
                mDb1.setSchemaAsync(request).get());
        assertThat(e.getMessage()).isEqualTo("LongProperty.INDEXING_TYPE_RANGE is not "
                + "supported on this AppSearch implementation.");
    }

    @Test
    public void testGetSchema_joinableValueType() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("normalStr")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("optionalQualifiedIdStr")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("requiredQualifiedIdStr")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testGetSchema_joinableValueTypeNone_succeeds() throws Exception {
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("optionalString")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("requiredString")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("repeatedString")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testGetSchema_joinableValueTypeQualifiedId_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("qualifiedId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () ->
                mDb1.setSchemaAsync(request).get());
        assertThat(e.getMessage()).isEqualTo(
                "StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID is not supported on this "
                        + "AppSearch implementation.");
    }

    @Test
    public void testGetNamespaces() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        assertThat(mDb1.getNamespacesAsync().get()).isEmpty();

        // Index a document
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace1", "id1").build())
                .build()));
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly("namespace1");

        // Index additional data
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(
                        new AppSearchEmail.Builder("namespace2", "id1").build(),
                        new AppSearchEmail.Builder("namespace2", "id2").build(),
                        new AppSearchEmail.Builder("namespace3", "id1").build())
                .build()));
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly(
                "namespace1", "namespace2", "namespace3");

        // Remove namespace2/id2 -- namespace2 should still exist because of namespace2/id1
        checkIsBatchResultSuccess(
                mDb1.removeAsync(new RemoveByDocumentIdRequest.Builder("namespace2").addIds(
                        "id2").build()));
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly(
                "namespace1", "namespace2", "namespace3");

        // Remove namespace2/id1 -- namespace2 should now be gone
        checkIsBatchResultSuccess(
                mDb1.removeAsync(new RemoveByDocumentIdRequest.Builder("namespace2").addIds(
                        "id1").build()));
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly("namespace1", "namespace3");

        // Make sure the list of namespaces is preserved after restart
        mDb1.close();
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        assertThat(mDb1.getNamespacesAsync().get()).containsExactly("namespace1", "namespace3");
    }

    @Test
    public void testGetNamespaces_dbIsolation() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        assertThat(mDb1.getNamespacesAsync().get()).isEmpty();
        assertThat(mDb2.getNamespacesAsync().get()).isEmpty();

        // Index documents
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace1_db1", "id1").build())
                .build()));
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace2_db1", "id1").build())
                .build()));
        checkIsBatchResultSuccess(mDb2.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(new AppSearchEmail.Builder("namespace_db2", "id1").build())
                .build()));
        assertThat(mDb1.getNamespacesAsync().get())
                .containsExactly("namespace1_db1", "namespace2_db1");
        assertThat(mDb2.getNamespacesAsync().get()).containsExactly("namespace_db2");

        // Make sure the list of namespaces is preserved after restart
        mDb1.close();
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        assertThat(mDb1.getNamespacesAsync().get())
                .containsExactly("namespace1_db1", "namespace2_db1");
        assertThat(mDb2.getNamespacesAsync().get()).containsExactly("namespace_db2");
    }

    @Test
    public void testGetSchema_emptyDB() throws Exception {
        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getVersion()).isEqualTo(0);
    }

    @Test
    public void testPutDocuments() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @Test
    public void testPutDocuments_emptyProperties() throws Exception {
        // Schema registration. Due to b/204677124 is fixed in Android T. We have different
        // behaviour when set empty array to bytes and documents between local and platform storage.
        // This test only test String, long, boolean and double, for byte array and Document will be
        // test in backend's specific test.
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new StringPropertyConfig.Builder("string")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("long")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("double")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("boolean")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schema, AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "testSchema")
                .setPropertyBoolean("boolean")
                .setPropertyString("string")
                .setPropertyDouble("double")
                .setPropertyLong("long")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();

        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1")
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);
        assertThat(outDocuments).hasSize(1);
        GenericDocument outDocument = outDocuments.get(0);
        assertThat(outDocument.getPropertyBooleanArray("boolean")).isEmpty();
        assertThat(outDocument.getPropertyStringArray("string")).isEmpty();
        assertThat(outDocument.getPropertyDoubleArray("double")).isEmpty();
        assertThat(outDocument.getPropertyLongArray("long")).isEmpty();
    }

    @Test
    public void testPutLargeDocumentBatch() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Creates a large batch of Documents, since we have max document size in Framework which is
        // 512KiB, we will create 1KiB * 4000 docs = 4MiB total size > 1MiB binder transaction limit
        char[] chars = new char[1024];  // 1KiB
        Arrays.fill(chars, ' ');
        String body = String.valueOf(chars) + "the end.";
        List<GenericDocument> inDocuments = new ArrayList<>();
        GetByDocumentIdRequest.Builder getByDocumentIdRequestBuilder =
                new GetByDocumentIdRequest.Builder("namespace");
        for (int i = 0; i < 4000; i++) {
            GenericDocument inDocument = new GenericDocument.Builder<>(
                    "namespace", "id" + i, "Type")
                    .setPropertyString("body", body)
                    .build();
            inDocuments.add(inDocument);
            getByDocumentIdRequestBuilder.addIds("id" + i);
        }

        // Index documents.
        AppSearchBatchResult<String, Void> result =
                mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(inDocuments)
                        .build()).get();
        assertThat(result.isSuccess()).isTrue();

        // Query those documents and verify they are same with the input. This also verify
        // AppSearchResult could handle large batch.
        SearchResults searchResults = mDb1.search("end", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultCountPerPage(4000)
                .build());
        List<GenericDocument> outDocuments = convertSearchResultsToDocuments(searchResults);

        // Create a map to assert the output is same to the input in O(n).
        // containsExactlyElementsIn will create two iterators and the complexity is O(n^2).
        Map<String, GenericDocument> outMap = new ArrayMap<>(outDocuments.size());
        for (int i = 0; i < outDocuments.size(); i++) {
            outMap.put(outDocuments.get(i).getId(), outDocuments.get(i));
        }
        for (int i = 0; i < inDocuments.size(); i++) {
            GenericDocument inDocument = inDocuments.get(i);
            assertThat(inDocument).isEqualTo(outMap.get(inDocument.getId()));
            outMap.remove(inDocument.getId());
        }
        assertThat(outMap).isEmpty();

        // Get by document ID and verify they are same with the input. This also verify
        // AppSearchBatchResult could handle large batch.
        AppSearchBatchResult<String, GenericDocument> batchResult = mDb1.getByDocumentIdAsync(
                getByDocumentIdRequestBuilder.build()).get();
        assertThat(batchResult.isSuccess()).isTrue();
        for (int i = 0; i < inDocuments.size(); i++) {
            GenericDocument inDocument = inDocuments.get(i);
            assertThat(batchResult.getSuccesses().get(inDocument.getId())).isEqualTo(inDocument);
        }
    }

// @exportToFramework:startStrip()

    @Test
    public void testPut_addDocumentClasses() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();

        // Index a document
        EmailDocument email = new EmailDocument();
        email.namespace = "namespace";
        email.id = "id1";
        email.subject = "testPut example";
        email.body = "This is the body of the testPut email";

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();
    }
// @exportToFramework:endStrip()

    @Test
    public void testUpdateSchema() throws Exception {
        // Schema registration
        AppSearchSchema oldEmailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema newEmailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema giftSchema = new AppSearchSchema.Builder("Gift")
                .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("price")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(oldEmailSchema).build()).get();

        // Try to index a gift. This should fail as it's not in the schema.
        GenericDocument gift =
                new GenericDocument.Builder<>("namespace", "gift1", "Gift").setPropertyLong("price",
                        5).build();
        AppSearchBatchResult<String, Void> result =
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(gift).build()).get();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailures().get("gift1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Update the schema to include the gift and update email with a new field
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(newEmailSchema, giftSchema).build()).get();

        // Try to index the document again, which should now work
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(gift).build()));

        // Indexing an email with a body should also work
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
    }

    @Test
    public void testRemoveSchema() throws Exception {
        // Schema registration
        AppSearchSchema emailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();

        // Index an email and check it present.
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        List<GenericDocument> outDocuments =
                doGet(mDb1, "namespace", "email1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email);

        // Try to remove the email schema. This should fail as it's an incompatible change.
        SetSchemaRequest setSchemaRequest = new SetSchemaRequest.Builder().build();
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(setSchemaRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException failResult1 = (AppSearchException) executionException.getCause();
        assertThat(failResult1).hasMessageThat().contains("Schema is incompatible");
        assertThat(failResult1).hasMessageThat().contains(
                "Deleted types: {builtin:Email}");

        // Try to remove the email schema again, which should now work as we set forceOverride to
        // be true.
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        // Make sure the indexed email is gone.
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("email1")
                        .build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Try to index an email again. This should fail as the schema has been removed.
        AppSearchEmail email2 = new AppSearchEmail.Builder("namespace", "email2")
                .setSubject("testPut example")
                .build();
        AppSearchBatchResult<String, Void> failResult2 = mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()).get();
        assertThat(failResult2.isSuccess()).isFalse();
        assertThat(failResult2.getFailures().get("email2").getErrorMessage())
                .isEqualTo("Schema type config '" + mContext.getPackageName() + "$" + DB_NAME_1
                        + "/builtin:Email' not found");
    }

    @Test
    public void testRemoveSchema_twoDatabases() throws Exception {
        // Schema registration in mDb1 and mDb2
        AppSearchSchema emailSchema = new AppSearchSchema.Builder(AppSearchEmail.SCHEMA_TYPE)
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(emailSchema).build()).get();

        // Index an email and check it present in database1.
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "email1")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        List<GenericDocument> outDocuments =
                doGet(mDb1, "namespace", "email1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email1);

        // Index an email and check it present in database2.
        AppSearchEmail email2 = new AppSearchEmail.Builder("namespace", "email2")
                .setSubject("testPut example")
                .build();
        checkIsBatchResultSuccess(
                mDb2.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));
        outDocuments = doGet(mDb2, "namespace", "email2");
        assertThat(outDocuments).hasSize(1);
        outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email2);

        // Try to remove the email schema in database1. This should fail as it's an incompatible
        // change.
        SetSchemaRequest setSchemaRequest = new SetSchemaRequest.Builder().build();
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(setSchemaRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException failResult1 = (AppSearchException) executionException.getCause();
        assertThat(failResult1).hasMessageThat().contains("Schema is incompatible");
        assertThat(failResult1).hasMessageThat().contains(
                "Deleted types: {builtin:Email}");

        // Try to remove the email schema again, which should now work as we set forceOverride to
        // be true.
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        // Make sure the indexed email is gone in database 1.
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("email1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("email1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Try to index an email again. This should fail as the schema has been removed.
        AppSearchEmail email3 = new AppSearchEmail.Builder("namespace", "email3")
                .setSubject("testPut example")
                .build();
        AppSearchBatchResult<String, Void> failResult2 = mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email3).build()).get();
        assertThat(failResult2.isSuccess()).isFalse();
        assertThat(failResult2.getFailures().get("email3").getErrorMessage())
                .isEqualTo("Schema type config '" + mContext.getPackageName() + "$" + DB_NAME_1
                        + "/builtin:Email' not found");

        // Make sure email in database 2 still present.
        outDocuments = doGet(mDb2, "namespace", "email2");
        assertThat(outDocuments).hasSize(1);
        outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(email2);

        // Make sure email could still be indexed in database 2.
        checkIsBatchResultSuccess(
                mDb2.putAsync(
                        new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));
    }

    @Test
    public void testGetDocuments() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Get the document
        List<GenericDocument> outDocuments = doGet(mDb1, "namespace", "id1");
        assertThat(outDocuments).hasSize(1);
        AppSearchEmail outEmail = new AppSearchEmail(outDocuments.get(0));
        assertThat(outEmail).isEqualTo(inEmail);

        // Can't get the document in the other instance.
        AppSearchBatchResult<String, GenericDocument> failResult = mDb2.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()).get();
        assertThat(failResult.isSuccess()).isFalse();
        assertThat(failResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

// @exportToFramework:startStrip()

    @Test
    public void testGet_addDocumentClasses() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailDocument.class).build()).get();

        // Index a document
        EmailDocument inEmail = new EmailDocument();
        inEmail.namespace = "namespace";
        inEmail.id = "id1";
        inEmail.subject = "testPut example";
        inEmail.body = "This is the body of the testPut inEmail";
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addDocuments(inEmail).build()));

        // Get the document
        List<GenericDocument> outDocuments = doGet(mDb1, "namespace", "id1");
        assertThat(outDocuments).hasSize(1);
        EmailDocument outEmail = outDocuments.get(0).toDocumentClass(EmailDocument.class);
        assertThat(inEmail.id).isEqualTo(outEmail.id);
        assertThat(inEmail.subject).isEqualTo(outEmail.subject);
        assertThat(inEmail.body).isEqualTo(outEmail.body);
    }
// @exportToFramework:endStrip()


    @Test
    public void testGetDocuments_projection() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1", "id2")
                .addProjection(
                        AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

        // The two email documents should have been returned with only the "subject" and "to"
        // properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_projectionEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace").addIds(
                "id1",
                "id2").addProjection(AppSearchEmail.SCHEMA_TYPE, Collections.emptyList()).build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

        // The two email documents should have been returned without any properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_projectionNonExistentType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1", "id2")
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("subject", "to"))
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

        // The two email documents should have been returned with only the "subject" and "to"
        // properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_wildcardProjection() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1", "id2")
                .addProjection(
                        GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD,
                        ImmutableList.of("subject", "to"))
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

        // The two email documents should have been returned with only the "subject" and "to"
        // properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_wildcardProjectionEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace").addIds(
                "id1",
                "id2").addProjection(GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD,
                Collections.emptyList()).build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

        // The two email documents should have been returned without any properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testGetDocuments_wildcardProjectionNonExistentType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Get with type property paths {"Email", ["subject", "to"]}
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1", "id2")
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(
                        GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD,
                        ImmutableList.of("subject", "to"))
                .build();
        List<GenericDocument> outDocuments = doGet(mDb1, request);

        // The two email documents should have been returned with only the "subject" and "to"
        // properties.
        AppSearchEmail expected1 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        AppSearchEmail expected2 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .build();
        assertThat(outDocuments).containsExactly(expected1, expected2);
    }

    @Test
    public void testQuery() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(inEmail);

        // Multi-term query
        searchResults = mDb1.search("body email", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(inEmail);
    }

    @Test
    public void testQuery_getNextPage() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        Set<AppSearchEmail> emailSet = new HashSet<>();
        PutDocumentsRequest.Builder putDocumentsRequestBuilder = new PutDocumentsRequest.Builder();
        // Index 31 documents
        for (int i = 0; i < 31; i++) {
            AppSearchEmail inEmail =
                    new AppSearchEmail.Builder("namespace", "id" + i)
                            .setFrom("from@example.com")
                            .setTo("to1@example.com", "to2@example.com")
                            .setSubject("testPut example")
                            .setBody("This is the body of the testPut email")
                            .build();
            emailSet.add(inEmail);
            putDocumentsRequestBuilder.addGenericDocuments(inEmail);
        }
        checkIsBatchResultSuccess(mDb1.putAsync(putDocumentsRequestBuilder.build()));

        // Set number of results per page is 7.
        SearchResults searchResults = mDb1.search("body",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setResultCountPerPage(7)
                        .build());
        List<GenericDocument> documents = new ArrayList<>();

        int pageNumber = 0;
        List<SearchResult> results;

        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPageAsync().get();
            ++pageNumber;
            for (SearchResult result : results) {
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);

        // check all document presents
        assertThat(documents).containsExactlyElementsIn(emailSet);
        assertThat(pageNumber).isEqualTo(6); // 5 (upper(31/7)) + 1 (final empty page)
    }

    @Test
    public void testQueryIndexableLongProperty_numericSearchEnabledSucceeds() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));

        // Schema registration
        AppSearchSchema transactionSchema = new AppSearchSchema.Builder("transaction")
                .addProperty(new LongPropertyConfig.Builder("price")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).addProperty(new LongPropertyConfig.Builder("cost")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(transactionSchema).build()).get();

        // Index some documents
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "transaction")
                        .setPropertyLong("price", 10)
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument doc2 =
                new GenericDocument.Builder<>("namespace", "id2", "transaction")
                        .setPropertyLong("price", 25)
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument doc3 =
                new GenericDocument.Builder<>("namespace", "id3", "transaction")
                        .setPropertyLong("cost", 2)
                        .setCreationTimestampMillis(1000)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("price < 20",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(doc1);

        searchResults = mDb1.search("price == 25",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(doc2);

        searchResults = mDb1.search("cost > 2",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        searchResults = mDb1.search("cost >= 2",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isEqualTo(doc3);

        searchResults = mDb1.search("price <= 25",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents.get(0)).isEqualTo(doc2);
        assertThat(documents.get(1)).isEqualTo(doc1);
    }

    @Test
    public void testQueryIndexableLongProperty_numericSearchNotEnabled() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));

        // Schema registration
        AppSearchSchema transactionSchema = new AppSearchSchema.Builder("transaction")
                .addProperty(new LongPropertyConfig.Builder("price")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(transactionSchema).build()).get();

        // Index some documents
        GenericDocument doc =
                new GenericDocument.Builder<>("namespace", "id1", "transaction")
                        .setPropertyLong("price", 10)
                        .setCreationTimestampMillis(1000)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()));

        // Query for the document
        // Use advanced query but disable NUMERIC_SEARCH in the SearchSpec.
        SearchResults searchResults = mDb1.search("price < 20",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(false)
                        .build());

        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.NUMERIC_SEARCH);
    }

    @Test
    public void testQuery_relevanceScoring() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("Mary had a little lamb")
                        .setBody("A little lamb, little lamb")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("I'm a little teapot")
                        .setBody("short and stout. Here is my handle, here is my spout.")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Query for "little". It should match both emails.
        SearchResults searchResults = mDb1.search("little", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);

        // The email1 should be ranked higher because 'little' appears three times in email1 and
        // only once in email2.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(
                results.get(1).getRankingSignal());
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email2);
        assertThat(results.get(1).getRankingSignal()).isGreaterThan(0);

        // Query for "little OR stout". It should match both emails.
        searchResults = mDb1.search("little OR stout", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .build());
        results = retrieveAllSearchResults(searchResults);

        // The email2 should be ranked higher because 'little' appears once and "stout", which is a
        // rarer term, appears once. email1 only has the three 'little' appearances.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email2);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(
                results.get(1).getRankingSignal());
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(1).getRankingSignal()).isGreaterThan(0);
    }

    @Test
    public void testQuery_advancedRanking() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(3)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document, and set an advanced ranking expression that evaluates to 6.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                // "abs(pow(2, 2) - 6)" should be evaluated to 2.
                // "this.documentScore()" should be evaluated to 3.
                .setRankingStrategy("abs(pow(2, 2) - 6) * this.documentScore()")
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(inEmail);
        assertThat(results.get(0).getRankingSignal()).isEqualTo(6);
    }

    @Test
    public void testQuery_invalidAdvancedRanking() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document, but set an invalid advanced ranking expression.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy("sqrt()")
                .build());
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains(
                "Math functions must have at least one argument.");
    }

    @Test
    public void testQuery_unsupportedAdvancedRanking() throws Exception {
        // Assume that advanced ranking has not been supported.
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document, and set a valid advanced ranking expression.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy("sqrt(4)")
                .build();
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.search("body", searchSpec));
        assertThat(e).hasMessageThat().contains(
                Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION + " is not available on this "
                        + "AppSearch implementation.");
    }

    @Test
    public void testQuery_typeFilter() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("foo")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(genericSchema)
                        .build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument inDoc = new GenericDocument.Builder<>("namespace", "id2", "Generic")
                .setPropertyString("foo", "body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail, inDoc).build()));

        // Query for the documents
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(inEmail, inDoc);

        // Query only for Document
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .addFilterSchemas("Generic", "Generic") // duplicate type in filter won't matter.
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inDoc);

        // Query only for non-exist type
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .addFilterSchemas("nonExistType")
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();
    }

    @Test
    public void testQuery_packageFilter() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));

        // Query for the document within our package
        SearchResults searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterPackageNames(ApplicationProvider.getApplicationContext().getPackageName())
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(email);

        // Query for the document in some other package, which won't exist
        searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addFilterPackageNames("some.other.package")
                .build());
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).isEmpty();
    }

    @Test
    public void testQuery_namespaceFilter() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("expectedNamespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail unexpectedEmail =
                new AppSearchEmail.Builder("unexpectedNamespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(expectedEmail, unexpectedEmail).build()));

        // Query for all namespaces
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(expectedEmail, unexpectedEmail);

        // Query only for expectedNamespace
        searchResults = mDb1.search("body",
                new SearchSpec.Builder()
                        .addFilterNamespaces("expectedNamespace")
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(expectedEmail);

        // Query only for non-exist namespace
        searchResults = mDb1.search("body",
                new SearchSpec.Builder()
                        .addFilterNamespaces("nonExistNamespace")
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();
    }

    @Test
    public void testQuery_getPackageName() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> results;
        List<GenericDocument> documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPageAsync().get();
            for (SearchResult result : results) {
                assertThat(result.getGenericDocument()).isEqualTo(inEmail);
                assertThat(result.getPackageName()).isEqualTo(
                        ApplicationProvider.getApplicationContext().getPackageName());
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);
        assertThat(documents).hasSize(1);
    }

    @Test
    public void testQuery_getDatabaseName() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> results;
        List<GenericDocument> documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPageAsync().get();
            for (SearchResult result : results) {
                assertThat(result.getGenericDocument()).isEqualTo(inEmail);
                assertThat(result.getDatabaseName()).isEqualTo(DB_NAME_1);
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);
        assertThat(documents).hasSize(1);

        // Schema registration for another database
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        searchResults = mDb2.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        documents = new ArrayList<>();
        // keep loading next page until it's empty.
        do {
            results = searchResults.getNextPageAsync().get();
            for (SearchResult result : results) {
                assertThat(result.getGenericDocument()).isEqualTo(inEmail);
                assertThat(result.getDatabaseName()).isEqualTo(DB_NAME_2);
                documents.add(result.getGenericDocument());
            }
        } while (results.size() > 0);
        assertThat(documents).hasSize(1);
    }

    @Test
    public void testQuery_projection() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"Email", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("body", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned with only the "body" and "to"
        // properties. The note document should have been returned with all of its properties.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_projectionEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"Email", []}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(AppSearchEmail.SCHEMA_TYPE, Collections.emptyList())
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned without any properties. The note document
        // should have been returned with all of its properties.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_projectionNonExistentType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"NonExistentType", []}, {"Email", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(AppSearchEmail.SCHEMA_TYPE, ImmutableList.of("body", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned with only the "body" and "to" properties.
        // The note document should have been returned with all of its properties.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_wildcardProjection() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN).build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"*", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(
                        SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD, ImmutableList.of("body", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned with only the "body" and "to"
        // properties. The note document should have been returned with only the "body" property.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_wildcardProjectionEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN).build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN).build())
                                .build()).build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"*", []}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection(SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD, Collections.emptyList())
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email and note documents should have been returned without any properties.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000).build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_wildcardProjectionNonExistentType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("Note")
                                .addProperty(new StringPropertyConfig.Builder("title")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .addProperty(new StringPropertyConfig.Builder("body")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                                        .setTokenizerType(
                                                StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .build())
                                .build())
                        .build()).get();

        // Index two documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument note =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("title", "Note title")
                        .setPropertyString("body", "Note body").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email, note).build()));

        // Query with type property paths {"NonExistentType", []}, {"*", ["body", "to"]}
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .addProjection("NonExistentType", Collections.emptyList())
                .addProjection(
                        SearchSpec.PROJECTION_SCHEMA_TYPE_WILDCARD, ImmutableList.of("body", "to"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The email document should have been returned with only the "body" and "to"
        // properties. The note document should have been returned with only the "body" property.
        AppSearchEmail expectedEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setTo("to1@example.com", "to2@example.com")
                        .setBody("This is the body of the testPut email")
                        .build();
        GenericDocument expectedNote =
                new GenericDocument.Builder<>("namespace", "id2", "Note")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("body", "Note body").build();
        assertThat(documents).containsExactly(expectedNote, expectedEmail);
    }

    @Test
    public void testQuery_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));

        // Index a document to instance 2.
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));

        // Query for instance 1.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inEmail1);

        // Query for instance 2.
        searchResults = mDb2.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(inEmail2);
    }

    @Test
    public void testSnippet() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(genericSchema).build()).get();

        // Index a document
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "Generic")
                        .setPropertyString("subject", "A commonly used fake word is foo. "
                                + "Another nonsense word that’s used a lot is bar")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("fo",
                new SearchSpec.Builder()
                        .addFilterSchemas("Generic")
                        .setSnippetCount(1)
                        .setSnippetCountPerProperty(1)
                        .setMaxSnippetSize(10)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(1);

        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatchInfos();
        assertThat(matchInfos).isNotNull();
        assertThat(matchInfos).hasSize(1);
        SearchResult.MatchInfo matchInfo = matchInfos.get(0);
        assertThat(matchInfo.getFullText()).isEqualTo("A commonly used fake word is foo. "
                + "Another nonsense word that’s used a lot is bar");
        assertThat(matchInfo.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/29,  /*upper=*/32));
        assertThat(matchInfo.getExactMatch()).isEqualTo("foo");
        assertThat(matchInfo.getSnippetRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/26,  /*upper=*/33));
        assertThat(matchInfo.getSnippet()).isEqualTo("is foo.");

        if (!mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)) {
            assertThrows(UnsupportedOperationException.class, matchInfo::getSubmatchRange);
            assertThrows(UnsupportedOperationException.class, matchInfo::getSubmatch);
        } else {
            assertThat(matchInfo.getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/29,  /*upper=*/31));
            assertThat(matchInfo.getSubmatch()).isEqualTo("fo");
        }
    }

    @Test
    public void testSetSnippetCount() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(genericSchema).build()).get();

        // Index documents
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(
                        new GenericDocument.Builder<>("namespace", "id1", "Generic")
                                .setPropertyString(
                                        "subject",
                                        "I like cats", "I like dogs", "I like birds", "I like fish")
                                .setScore(10)
                                .build(),
                        new GenericDocument.Builder<>("namespace", "id2", "Generic")
                                .setPropertyString(
                                        "subject",
                                        "I like red",
                                        "I like green",
                                        "I like blue",
                                        "I like yellow")
                                .setScore(20)
                                .build(),
                        new GenericDocument.Builder<>("namespace", "id3", "Generic")
                                .setPropertyString(
                                        "subject",
                                        "I like cupcakes",
                                        "I like donuts",
                                        "I like eclairs",
                                        "I like froyo")
                                .setScore(5)
                                .build())
                .build()));

        // Query for the document
        SearchResults searchResults = mDb1.search(
                "like",
                new SearchSpec.Builder()
                        .addFilterSchemas("Generic")
                        .setSnippetCount(2)
                        .setSnippetCountPerProperty(3)
                        .setMaxSnippetSize(11)
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());

        // Check result 1
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(3);

        assertThat(results.get(0).getGenericDocument().getId()).isEqualTo("id2");
        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatchInfos();
        assertThat(matchInfos).hasSize(3);
        assertThat(matchInfos.get(0).getSnippet()).isEqualTo("I like red");
        assertThat(matchInfos.get(1).getSnippet()).isEqualTo("I like");
        assertThat(matchInfos.get(2).getSnippet()).isEqualTo("I like blue");

        // Check result 2
        assertThat(results.get(1).getGenericDocument().getId()).isEqualTo("id1");
        matchInfos = results.get(1).getMatchInfos();
        assertThat(matchInfos).hasSize(3);
        assertThat(matchInfos.get(0).getSnippet()).isEqualTo("I like cats");
        assertThat(matchInfos.get(1).getSnippet()).isEqualTo("I like dogs");
        assertThat(matchInfos.get(2).getSnippet()).isEqualTo("I like");

        // Check result 2
        assertThat(results.get(2).getGenericDocument().getId()).isEqualTo("id3");
        matchInfos = results.get(2).getMatchInfos();
        assertThat(matchInfos).isEmpty();
    }

    @Test
    public void testCJKSnippet() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(genericSchema).build()).get();

        String japanese =
                "差し出されたのが今日ランドセルでした普通の子であれば満面の笑みで俺を言うでしょうしかし私は赤いランド"
                        + "セルを見て笑うことができませんでしたどうしたのと心配そうな仕事ガラスながら渋い顔する私書いたこと言"
                        + "うんじゃないのカードとなる声を聞きたい私は目から涙をこぼしながらおじいちゃんの近くにかけおり頭をポ"
                        + "ンポンと叩きピンクが良かったんだもん";
        // Index a document
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "Generic")
                        .setPropertyString("subject", japanese)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));

        // Query for the document
        SearchResults searchResults = mDb1.search("は",
                new SearchSpec.Builder()
                        .addFilterSchemas("Generic")
                        .setSnippetCount(1)
                        .setSnippetCountPerProperty(1)
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        List<SearchResult> results = searchResults.getNextPageAsync().get();
        assertThat(results).hasSize(1);

        List<SearchResult.MatchInfo> matchInfos = results.get(0).getMatchInfos();
        assertThat(matchInfos).isNotNull();
        assertThat(matchInfos).hasSize(1);
        SearchResult.MatchInfo matchInfo = matchInfos.get(0);
        assertThat(matchInfo.getFullText()).isEqualTo(japanese);
        assertThat(matchInfo.getExactMatchRange()).isEqualTo(
                new SearchResult.MatchRange(/*lower=*/44,  /*upper=*/45));
        assertThat(matchInfo.getExactMatch()).isEqualTo("は");

        if (!mDb1.getFeatures().isFeatureSupported(
                Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)) {
            assertThrows(UnsupportedOperationException.class, matchInfo::getSubmatchRange);
            assertThrows(UnsupportedOperationException.class, matchInfo::getSubmatch);
        } else {
            assertThat(matchInfo.getSubmatchRange()).isEqualTo(
                    new SearchResult.MatchRange(/*lower=*/44,  /*upper=*/45));
            assertThat(matchInfo.getSubmatch()).isEqualTo("は");
        }
    }

    @Test
    public void testRemove() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1",
                        "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);

        // Test if we delete a nonexistent id.
        AppSearchBatchResult<String, Void> deleteResult = mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()).get();

        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemove_multipleIds() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1", "id2").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                        new GetByDocumentIdRequest.Builder("namespace").addIds("id1",
                                "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByQuery() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("bar")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace", "id2")).hasSize(1);

        // Delete the email 1 by query "foo"
        mDb1.removeAsync("foo",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()).get();
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                        new GetByDocumentIdRequest.Builder("namespace")
                                .addIds("id1", "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);

        // Delete the email 2 by query "bar"
        mDb1.removeAsync("bar",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()).get();
        getResult = mDb1.getByDocumentIdAsync(
                        new GetByDocumentIdRequest.Builder("namespace").addIds("id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }


    @Test
    public void testRemoveByQuery_nonExistNamespace() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace2", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("bar")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace1", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace2", "id2")).hasSize(1);

        // Delete the email by nonExist namespace.
        mDb1.removeAsync("",
                new SearchSpec.Builder().setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterNamespaces("nonExistNamespace").build()).get();
        // None of these emails will be deleted.
        assertThat(doGet(mDb1, "namespace1", "id1")).hasSize(1);
        assertThat(doGet(mDb1, "namespace2", "id2")).hasSize(1);
    }

    @Test
    public void testRemoveByQuery_packageFilter() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("foo")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Try to delete email with query "foo", but restricted to a different package name.
        // Won't work and email will still exist.
        mDb1.removeAsync("foo",
                new SearchSpec.Builder().setTermMatch(
                        SearchSpec.TERM_MATCH_PREFIX).addFilterPackageNames(
                        "some.other.package").build()).get();
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Delete the email by query "foo", restricted to the correct package this time.
        mDb1.removeAsync("foo", new SearchSpec.Builder().setTermMatch(
                SearchSpec.TERM_MATCH_PREFIX).addFilterPackageNames(
                ApplicationProvider.getApplicationContext().getPackageName()).build()).get();
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1", "id2").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemove_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Can't delete in the other instance.
        AppSearchBatchResult<String, Void> deleteResult = mDb2.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Test if we delete a nonexistent id.
        deleteResult = mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(deleteResult.getFailures().get("id1").getResultCode()).isEqualTo(
                AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveByTypes() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic").build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).addSchemas(
                        genericSchema).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id3", "Generic").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1", "id2", "id3")).hasSize(3);

        // Delete the email type
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .build())
                .get();

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1", "id2", "id3").build())
                .get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getSuccesses().get("id3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByTypes_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb2, "namespace", "id2")).hasSize(1);

        // Delete the email type in instance 1
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveByNamespace() throws Exception {
        // Schema registration
        AppSearchSchema genericSchema = new AppSearchSchema.Builder("Generic")
                .addProperty(new StringPropertyConfig.Builder("foo")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build()
                ).build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).addSchemas(
                        genericSchema).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("email", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("email", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        GenericDocument document1 =
                new GenericDocument.Builder<>("document", "id3", "Generic")
                        .setPropertyString("foo", "bar").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2, document1)
                        .build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, /*namespace=*/"email", "id1", "id2")).hasSize(2);
        assertThat(doGet(mDb1, /*namespace=*/"document", "id3")).hasSize(1);

        // Delete the email namespace
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterNamespaces("email")
                        .build())
                .get();

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id1", "id2").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(getResult.getFailures().get("id2").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("document")
                        .addIds("id3").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id3")).isEqualTo(document1);
    }

    @Test
    public void testRemoveByNamespaces_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("email", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("email", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, /*namespace=*/"email", "id1")).hasSize(1);
        assertThat(doGet(mDb2, /*namespace=*/"email", "id2")).hasSize(1);

        // Delete the email namespace in instance 1
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .addFilterNamespaces("email")
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("email")
                        .addIds("id2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveAll_twoInstances() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email2).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);
        assertThat(doGet(mDb2, "namespace", "id2")).hasSize(1);

        // Delete the all document in instance 1
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build())
                .get();

        // Make sure it's really gone in instance 1
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Make sure it's still in instance 2.
        getResult = mDb2.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id2").build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id2")).isEqualTo(email2);
    }

    @Test
    public void testRemoveAll_termMatchType() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 2")
                        .setBody("This is the body of the testPut second email")
                        .build();
        AppSearchEmail email3 =
                new AppSearchEmail.Builder("namespace", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 3")
                        .setBody("This is the body of the testPut second email")
                        .build();
        AppSearchEmail email4 =
                new AppSearchEmail.Builder("namespace", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example 4")
                        .setBody("This is the body of the testPut second email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));
        checkIsBatchResultSuccess(mDb2.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email3, email4).build()));

        // Check the presence of the documents
        SearchResults searchResults = mDb1.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        searchResults = mDb2.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // Delete the all document in instance 1 with TERM_MATCH_PREFIX
        mDb1.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build())
                .get();
        searchResults = mDb1.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();

        // Delete the all document in instance 2 with TERM_MATCH_EXACT_ONLY
        mDb2.removeAsync("", new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build())
                .get();
        searchResults = mDb2.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEmpty();
    }

    @Test
    public void testRemoveAllAfterEmpty() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Check the presence of the documents
        assertThat(doGet(mDb1, "namespace", "id1")).hasSize(1);

        // Remove the document
        checkIsBatchResultSuccess(
                mDb1.removeAsync(new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "id1").build()));

        // Make sure it's really gone
        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // Delete the all documents
        mDb1.removeAsync("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX).build()).get();

        // Make sure it's still gone
        getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace").addIds("id1").build()).get();
        assertThat(getResult.isSuccess()).isFalse();
        assertThat(getResult.getFailures().get("id1").getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testRemoveQueryWithJoinSpecThrowsException() {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> mDb2.removeAsync("", new SearchSpec.Builder()
                        .setJoinSpec(new JoinSpec.Builder("entityId").build())
                        .build()));
        assertThat(e.getMessage()).isEqualTo("JoinSpec not allowed in removeByQuery, "
                + "but JoinSpec was provided.");
    }

    @Test
    public void testCloseAndReopen() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // close and re-open the appSearchSession
        mDb1.close();
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();

        // Query for the document
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail);
    }

    @Test
    public void testCallAfterClose() throws Exception {

        // Create a same-thread database by inject an executor which could help us maintain the
        // execution order of those async tasks.
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession sameThreadDb = createSearchSessionAsync(
                "sameThreadDb", MoreExecutors.newDirectExecutorService()).get();

        try {
            // Schema registration -- just mutate something
            sameThreadDb.setSchemaAsync(
                    new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

            // Close the database. No further call will be allowed.
            sameThreadDb.close();

            // Try to query the closed database
            // We are using the same-thread db here to make sure it has been closed.
            IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                    sameThreadDb.search("query", new SearchSpec.Builder()
                            .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                            .build()));
            assertThat(e).hasMessageThat().contains("SearchSession has already been closed");
        } finally {
            // To clean the data that has been added in the test, need to re-open the session and
            // set an empty schema.
            AppSearchSession reopen = createSearchSessionAsync(
                    "sameThreadDb", MoreExecutors.newDirectExecutorService()).get();
            reopen.setSchemaAsync(new SetSchemaRequest.Builder()
                    .setForceOverride(true).build()).get();
        }
    }

    @Test
    public void testReportUsage() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index two documents.
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1").build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2).build()));

        // Email 1 has more usages, but email 2 has more recent usages.
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(1000).build()).get();
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(2000).build()).get();
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id1")
                .setUsageTimestampMillis(3000).build()).get();
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id2")
                .setUsageTimestampMillis(10000).build()).get();
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id2")
                .setUsageTimestampMillis(20000).build()).get();

        // Query by number of usages
        List<SearchResult> results = retrieveAllSearchResults(
                mDb1.search("", new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_USAGE_COUNT)
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build()));
        // Email 1 has three usages and email 2 has two usages.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email2);
        assertThat(results.get(0).getRankingSignal()).isEqualTo(3);
        assertThat(results.get(1).getRankingSignal()).isEqualTo(2);

        // Query by most recent usage.
        results = retrieveAllSearchResults(
                mDb1.search("", new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_USAGE_LAST_USED_TIMESTAMP)
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build()));
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email2);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(0).getRankingSignal()).isEqualTo(20000);
        assertThat(results.get(1).getRankingSignal()).isEqualTo(3000);
    }

    @Test
    public void testReportUsage_invalidNamespace() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace", "id1").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1).build()));

        // Use the correct namespace; it works
        mDb1.reportUsageAsync(new ReportUsageRequest.Builder("namespace", "id1").build()).get();

        // Use an incorrect namespace; it fails
        ReportUsageRequest reportUsageRequest =
                new ReportUsageRequest.Builder("namespace2", "id1").build();
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> mDb1.reportUsageAsync(reportUsageRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException cause = (AppSearchException) executionException.getCause();
        assertThat(cause.getResultCode()).isEqualTo(RESULT_NOT_FOUND);
    }

    @Test
    public void testGetStorageInfo() throws Exception {
        StorageInfo storageInfo = mDb1.getStorageInfoAsync().get();
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);

        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Still no storage space attributed with just a schema
        storageInfo = mDb1.getStorageInfoAsync().get();
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0);

        // Index two documents.
        AppSearchEmail email1 = new AppSearchEmail.Builder("namespace1", "id1").build();
        AppSearchEmail email2 = new AppSearchEmail.Builder("namespace1", "id2").build();
        AppSearchEmail email3 = new AppSearchEmail.Builder("namespace2", "id1").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email1, email2,
                        email3).build()));

        // Non-zero size now
        storageInfo = mDb1.getStorageInfoAsync().get();
        assertThat(storageInfo.getSizeBytes()).isGreaterThan(0);
        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(3);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(2);
    }

    @Test
    public void testFlush() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        AppSearchEmail email = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("from@example.com")
                .setTo("to1@example.com", "to2@example.com")
                .setSubject("testPut example")
                .setBody("This is the body of the testPut email")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(email).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();

        // The future returned from requestFlush will be set as a void or an Exception on error.
        mDb1.requestFlushAsync().get();
    }

    @Test
    public void testQuery_ResultGroupingLimits() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index four documents.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace1", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));
        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace1", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));
        AppSearchEmail inEmail3 =
                new AppSearchEmail.Builder("namespace2", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail3).build()));
        AppSearchEmail inEmail4 =
                new AppSearchEmail.Builder("namespace2", "id4")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail4).build()));

        // Query with per package result grouping. Only the last document 'email4' should be
        // returned.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*resultLimit=*/ 1)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4);

        // Query with per namespace result grouping. Only the last document in each namespace should
        // be returned ('email4' and 'email2').
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE, /*resultLimit=*/ 1)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4, inEmail2);

        // Query with per package and per namespace result grouping. Only the last document in each
        // namespace should be returned ('email4' and 'email2').
        searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setResultGrouping(SearchSpec.GROUPING_TYPE_PER_NAMESPACE
                        | SearchSpec.GROUPING_TYPE_PER_PACKAGE, /*resultLimit=*/ 1)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail4, inEmail2);
    }

    @Test
    public void testIndexNestedDocuments() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .addSchemas(new AppSearchSchema.Builder("YesNestedIndex")
                                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "prop", AppSearchEmail.SCHEMA_TYPE)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                                .build())
                        .addSchemas(new AppSearchSchema.Builder("NoNestedIndex")
                                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "prop", AppSearchEmail.SCHEMA_TYPE)
                                        .setShouldIndexNestedProperties(false)
                                        .build())
                                .build())
                        .build())
                .get();

        // Index the documents.
        AppSearchEmail email = new AppSearchEmail.Builder("", "")
                .setSubject("This is the body")
                .build();
        GenericDocument yesNestedIndex =
                new GenericDocument.Builder<>("namespace", "yesNestedIndex", "YesNestedIndex")
                        .setPropertyDocument("prop", email)
                        .build();
        GenericDocument noNestedIndex =
                new GenericDocument.Builder<>("namespace", "noNestedIndex", "NoNestedIndex")
                        .setPropertyDocument("prop", email)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(yesNestedIndex, noNestedIndex).build()));

        // Query.
        SearchResults searchResults = mDb1.search("body", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setSnippetCount(10)
                .setSnippetCountPerProperty(10)
                .build());
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument()).isEqualTo(yesNestedIndex);
        List<SearchResult.MatchInfo> matches = page.get(0).getMatchInfos();
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getPropertyPath()).isEqualTo("prop.subject");
        assertThat(matches.get(0).getPropertyPathObject())
                .isEqualTo(new PropertyPath("prop.subject"));
        assertThat(matches.get(0).getFullText()).isEqualTo("This is the body");
        assertThat(matches.get(0).getExactMatch()).isEqualTo("body");
    }

    @Test
    public void testCJKTQuery() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // Index a document to instance 1.
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "uri1")
                        .setBody("他是個男孩 is a boy")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));

        // Query for "他" (He)
        SearchResults searchResults = mDb1.search("他", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail1);

        // Query for "男孩" (boy)
        searchResults = mDb1.search("男孩", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail1);

        // Query for "boy"
        searchResults = mDb1.search("boy", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail1);
    }

    @Test
    public void testSetSchemaWithIncompatibleNestedSchema() throws Exception {
        // 1. Set the original schema. This should succeed without any problems.
        AppSearchSchema originalNestedSchema =
                new AppSearchSchema.Builder("TypeA").addProperty(new StringPropertyConfig.Builder(
                        "prop1").setCardinality(
                        PropertyConfig.CARDINALITY_OPTIONAL).build()).build();
        SetSchemaRequest originalRequest =
                new SetSchemaRequest.Builder().addSchemas(originalNestedSchema).build();
        mDb1.setSchemaAsync(originalRequest).get();

        // 2. Set a new schema with a new type that refers to "TypeA" and an incompatible change to
        // "TypeA". This should fail.
        AppSearchSchema newNestedSchema =
                new AppSearchSchema.Builder("TypeA").addProperty(new StringPropertyConfig.Builder(
                        "prop1").setCardinality(
                        PropertyConfig.CARDINALITY_REQUIRED).build()).build();
        AppSearchSchema newSchema =
                new AppSearchSchema.Builder("TypeB").addProperty(
                        new AppSearchSchema.DocumentPropertyConfig.Builder("prop2",
                                "TypeA").build()).build();
        final SetSchemaRequest newRequest =
                new SetSchemaRequest.Builder().addSchemas(newNestedSchema,
                        newSchema).build();
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> mDb1.setSchemaAsync(newRequest).get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_SCHEMA);
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
        assertThat(exception).hasMessageThat().contains("Incompatible types: {TypeA}");

        // 3. Now set that same set of schemas but with forceOverride=true. This should succeed.
        SetSchemaRequest newRequestForced =
                new SetSchemaRequest.Builder().addSchemas(newNestedSchema,
                        newSchema).setForceOverride(true).build();
        mDb1.setSchemaAsync(newRequestForced).get();
    }

    @Test
    public void testEmojiSnippet() throws Exception {
        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(AppSearchEmail.SCHEMA).build()).get();

        // String:     "Luca Brasi sleeps with the 🐟🐟🐟."
        //              ^    ^     ^      ^    ^   ^ ^  ^ ^
        // UTF8 idx:    0    5     11     18   23 27 3135 39
        // UTF16 idx:   0    5     11     18   23 27 2931 33
        // Breaks into segments: "Luca", "Brasi", "sleeps", "with", "the", "🐟", "🐟"
        // and "🐟".
        // Index a document to instance 1.
        String sicilianMessage = "Luca Brasi sleeps with the 🐟🐟🐟.";
        AppSearchEmail inEmail1 =
                new AppSearchEmail.Builder("namespace", "uri1")
                        .setBody(sicilianMessage)
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail1).build()));

        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "uri2")
                        .setBody("Some other content.")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail2).build()));

        // Query for "🐟"
        SearchResults searchResults = mDb1.search("🐟", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                .setSnippetCount(1)
                .setSnippetCountPerProperty(1)
                .build());
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument()).isEqualTo(inEmail1);
        List<SearchResult.MatchInfo> matches = page.get(0).getMatchInfos();
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getPropertyPath()).isEqualTo("body");
        assertThat(matches.get(0).getFullText()).isEqualTo(sicilianMessage);
        assertThat(matches.get(0).getExactMatch()).isEqualTo("🐟");
        if (mDb1.getFeatures().isFeatureSupported(Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH)) {
            assertThat(matches.get(0).getSubmatch()).isEqualTo("🐟");
        }
    }

    @Test
    public void testRfc822() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.TOKENIZER_TYPE_RFC822));
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("address")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_RFC822)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(emailSchema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>("NS", "alex1", "Email")
                .setPropertyString("address", "Alex Saveliev <alex.sav@google.com>")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        SearchResults sr = mDb1.search("com", new SearchSpec.Builder().build());
        List<SearchResult> page = sr.getNextPageAsync().get();

        // RFC tokenization will produce the following tokens for
        // "Alex Saveliev <alex.sav@google.com>" : ["Alex Saveliev <alex.sav@google.com>", "Alex",
        // "Saveliev", "alex.sav", "alex.sav@google.com", "alex.sav", "google", "com"]. Therefore,
        // a query for "com" should match the document.
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("alex1");

        // Plain tokenizer will not match this
        AppSearchSchema plainEmailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("address")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // Flipping the tokenizer type is a backwards compatible change. The index will be
        // rebuilt with the email doc being tokenized in the new way.
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(plainEmailSchema).build()).get();

        sr = mDb1.search("com", new SearchSpec.Builder().build());

        // Plain tokenization will produce the following tokens for
        // "Alex Saveliev <alex.sav@google.com>" : ["Alex", "Saveliev", "<", "alex.sav",
        // "google.com", ">"]. So "com" will not match any of the tokens produced.
        assertThat(sr.getNextPageAsync().get()).hasSize(0);
    }

    @Test
    public void testRfc822_unsupportedFeature_throwsException() {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.TOKENIZER_TYPE_RFC822));

        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("address")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_RFC822)
                        .build()
                ).build();

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                        .setForceOverride(true).addSchemas(emailSchema).build()).get());
        assertThat(e.getMessage()).isEqualTo("tokenizerType is out of range of [0, 1] (too high)");
    }


    @Test
    public void testQuery_verbatimSearch() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.VERBATIM_SEARCH));
        AppSearchSchema verbatimSchema = new AppSearchSchema.Builder("VerbatimSchema")
                .addProperty(new StringPropertyConfig.Builder("verbatimProp")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(verbatimSchema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>(
                "namespace1", "id1", "VerbatimSchema")
                .setPropertyString("verbatimProp", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        SearchResults sr = mDb1.search("\"Hello, world!\"",
                new SearchSpec.Builder().setVerbatimSearchEnabled(true).build());
        List<SearchResult> page = sr.getNextPageAsync().get();

        // Verbatim tokenization would produce one token 'Hello, world!'.
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");
    }

    @Test
    public void testQuery_verbatimSearchWithoutEnablingFeatureFails() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.VERBATIM_SEARCH));
        AppSearchSchema verbatimSchema = new AppSearchSchema.Builder("VerbatimSchema")
                .addProperty(new StringPropertyConfig.Builder("verbatimProp")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(verbatimSchema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>(
                "namespace1", "id1", "VerbatimSchema")
                .setPropertyString("verbatimProp", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        // ListFilterQueryLanguage is enabled so that EXPERIMENTAL_ICING_ADVANCED_QUERY gets enabled
        // in IcingLib.
        // Disable VERBATIM_SEARCH in the SearchSpec.
        SearchResults searchResults = mDb1.search("\"Hello, world!\"",
                new SearchSpec.Builder()
                        .setVerbatimSearchEnabled(false)
                        .build());
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.VERBATIM_SEARCH);
    }

    @Test
    public void testQuery_listFilterQueryWithEnablingFeatureSucceeds() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema")
                .addProperty(new StringPropertyConfig.Builder("prop")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(schema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>(
                "namespace1", "id1", "Schema")
                .setPropertyString("prop", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(true)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();
        // Support for function calls `search`, `createList` was added in list filters
        SearchResults searchResults = mDb1.search("search(\"hello\", createList(\"prop\"))",
                searchSpec);
        List<SearchResult> page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");

        // Support for prefix operator * was added in list filters.
        searchResults = mDb1.search("wor*", searchSpec);
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");

        // Combining negations with compound statements and property restricts was added in list
        // filters.
        searchResults = mDb1.search("NOT (foo OR otherProp:hello)", searchSpec);
        page = searchResults.getNextPageAsync().get();
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getGenericDocument().getId()).isEqualTo("id1");
    }

    @Test
    public void testQuery_listFilterQueryWithoutEnablingFeatureFails() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema")
                .addProperty(new StringPropertyConfig.Builder("prop")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).addSchemas(schema).build()).get();

        GenericDocument email = new GenericDocument.Builder<>(
                "namespace1", "id1", "Schema")
                .setPropertyString("prop", "Hello, world!")
                .build();
        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(email).build()).get();

        // Disable LIST_FILTER_QUERY_LANGUAGE in the SearchSpec.
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setListFilterQueryLanguageEnabled(false)
                .build();
        SearchResults searchResults = mDb1.search("search(\"hello\", createList(\"prop\"))",
                searchSpec);
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> searchResults.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        AppSearchException exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_QUERY_LANGUAGE);

        SearchResults searchResults2 = mDb1.search("wor*", searchSpec);
        executionException = assertThrows(ExecutionException.class,
                () -> searchResults2.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_QUERY_LANGUAGE);

        SearchResults searchResults3 = mDb1.search("NOT (foo OR otherProp:hello)", searchSpec);
        executionException = assertThrows(ExecutionException.class,
                () -> searchResults3.getNextPageAsync().get());
        assertThat(executionException).hasCauseThat().isInstanceOf(AppSearchException.class);
        exception = (AppSearchException) executionException.getCause();
        assertThat(exception.getResultCode()).isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(exception).hasMessageThat().contains("Attempted use of unenabled feature");
        assertThat(exception).hasMessageThat().contains(Features.LIST_FILTER_QUERY_LANGUAGE);
    }

    @Test
    public void testQuery_listFilterQueryFeatures_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.NUMERIC_SEARCH));
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.VERBATIM_SEARCH));
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.LIST_FILTER_QUERY_LANGUAGE));

        // UnsupportedOperationException will be thrown with these queries so no need to
        // define a schema and index document.
        SearchSpec.Builder builder = new SearchSpec.Builder();
        SearchSpec searchSpec1 = builder.setNumericSearchEnabled(true).build();
        SearchSpec searchSpec2 = builder.setVerbatimSearchEnabled(true).build();
        SearchSpec searchSpec3 = builder.setListFilterQueryLanguageEnabled(true).build();

        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.search("\"Hello, world!\"", searchSpec1));
        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.search("\"Hello, world!\"", searchSpec2));
        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.search("\"Hello, world!\"", searchSpec3));
    }

    @Test
    public void testQuery_propertyWeightsNotSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject",
                        2.0, "body", 0.5))
                .build();
        UnsupportedOperationException exception =
                assertThrows(UnsupportedOperationException.class,
                        () -> mDb1.search("Hello", searchSpec));
        assertThat(exception).hasMessageThat().contains("Property weights are not supported");
    }

    @Test
    public void testQuery_propertyWeights() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Query for "foo". It should match both emails.
        SearchResults searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject",
                        2.0, "body", 0.5))
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);

        // email1 should be ranked higher because "foo" appears in the "subject" property which
        // has higher weight than the "body" property.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(
                results.get(1).getRankingSignal());
        assertThat(results.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(email2);

        // Query for "foo" without property weights.
        SearchSpec searchSpecWithoutWeights = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .build();
        SearchResults searchResultsWithoutWeights = mDb1.search("foo", searchSpecWithoutWeights);
        List<SearchResult> resultsWithoutWeights =
                retrieveAllSearchResults(searchResultsWithoutWeights);

        // email1 should have the same ranking signal as email2 as each contains the term "foo"
        // once.
        assertThat(resultsWithoutWeights).hasSize(2);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isEqualTo(
                resultsWithoutWeights.get(1).getRankingSignal());
    }

    @Test
    public void testQuery_propertyWeightsNestedProperties() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Register a schema with a nested type
        AppSearchSchema schema =
                new AppSearchSchema.Builder("TypeA").addProperty(
                        new AppSearchSchema.DocumentPropertyConfig.Builder("nestedEmail",
                                AppSearchEmail.SCHEMA_TYPE).setShouldIndexNestedProperties(
                                true).build()).build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA,
                schema).build()).get();

        // Index two documents
        AppSearchEmail nestedEmail1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "TypeA").setPropertyDocument(
                        "nestedEmail", nestedEmail1).build();
        AppSearchEmail nestedEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo")
                        .build();
        GenericDocument doc2 =
                new GenericDocument.Builder<>("namespace", "id2", "TypeA").setPropertyDocument(
                        "nestedEmail", nestedEmail2).build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(doc1, doc2).build()));

        // Query for "foo". It should match both emails.
        SearchResults searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights("TypeA", ImmutableMap.of(
                        "nestedEmail.subject",
                        2.0, "nestedEmail.body", 0.5))
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);

        // email1 should be ranked higher because "foo" appears in the "nestedEmail.subject"
        // property which has higher weight than the "nestedEmail.body" property.
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(
                results.get(1).getRankingSignal());
        assertThat(results.get(0).getGenericDocument()).isEqualTo(doc1);
        assertThat(results.get(1).getGenericDocument()).isEqualTo(doc2);

        // Query for "foo" without property weights.
        SearchSpec searchSpecWithoutWeights = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .build();
        SearchResults searchResultsWithoutWeights = mDb1.search("foo", searchSpecWithoutWeights);
        List<SearchResult> resultsWithoutWeights =
                retrieveAllSearchResults(searchResultsWithoutWeights);

        // email1 should have the same ranking signal as email2 as each contains the term "foo"
        // once.
        assertThat(resultsWithoutWeights).hasSize(2);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(resultsWithoutWeights.get(0).getRankingSignal()).isEqualTo(
                resultsWithoutWeights.get(1).getRankingSignal());
    }

    @Test
    public void testQuery_propertyWeightsDefaults() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index two documents
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("foo")
                        .build();
        AppSearchEmail email2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setCreationTimestampMillis(1000)
                        .setBody("foo bar")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1, email2).build()));

        // Query for "foo" without assigning property weights for any path.
        SearchResults searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of())
                .build());
        List<SearchResult> resultsWithoutPropertyWeights = retrieveAllSearchResults(
                searchResults);

        // Query for "foo" with assigning default property weights.
        searchResults = mDb1.search("foo", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject", 1.0,
                        "body", 1.0))
                .build());
        List<SearchResult> expectedResults = retrieveAllSearchResults(searchResults);

        assertThat(resultsWithoutPropertyWeights).hasSize(2);
        assertThat(expectedResults).hasSize(2);

        assertThat(resultsWithoutPropertyWeights.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(resultsWithoutPropertyWeights.get(1).getGenericDocument()).isEqualTo(email2);
        assertThat(expectedResults.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(expectedResults.get(1).getGenericDocument()).isEqualTo(email2);

        // The ranking signal for results with no property path and weights set should be equal
        // to the ranking signal for results with explicitly set default weights.
        assertThat(resultsWithoutPropertyWeights.get(0).getRankingSignal()).isEqualTo(
                expectedResults.get(0).getRankingSignal());
        assertThat(resultsWithoutPropertyWeights.get(1).getRankingSignal()).isEqualTo(
                expectedResults.get(1).getRankingSignal());
    }

    @Test
    public void testQuery_propertyWeightsIgnoresInvalidPropertyPaths() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SPEC_PROPERTY_WEIGHTS));

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA)
                        .build()).get();

        // Index an email
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setCreationTimestampMillis(1000)
                        .setSubject("baz")
                        .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(email1).build()));

        // Query for "baz" with property weight for "subject", a valid property in the schema type.
        SearchResults searchResults = mDb1.search("baz", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject", 2.0))
                .build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);

        // Query for "baz" with property weights, one for valid property "subject" and one for a
        // non-existing property "invalid".
        searchResults = mDb1.search("baz", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                .setOrder(SearchSpec.ORDER_DESCENDING)
                .setPropertyWeights(AppSearchEmail.SCHEMA_TYPE, ImmutableMap.of("subject", 2.0,
                        "invalid", 3.0))
                .build());
        List<SearchResult> resultsWithInvalidPath = retrieveAllSearchResults(searchResults);

        assertThat(results).hasSize(1);
        assertThat(resultsWithInvalidPath).hasSize(1);

        // We expect the ranking signal to be unchanged in the presence of an invalid property
        // weight.
        assertThat(results.get(0).getRankingSignal()).isGreaterThan(0);
        assertThat(resultsWithInvalidPath.get(0).getRankingSignal()).isEqualTo(
                results.get(0).getRankingSignal());

        assertThat(results.get(0).getGenericDocument()).isEqualTo(email1);
        assertThat(resultsWithInvalidPath.get(0).getGenericDocument()).isEqualTo(email1);
    }

    @Test
    public void testSimpleJoin() throws Exception {
        assumeTrue(mDb1.getFeatures()
                .isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        // A full example of how join might be used
        AppSearchSchema actionSchema = new AppSearchSchema.Builder("ViewAction")
                .addProperty(new StringPropertyConfig.Builder("entityId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setJoinableValueType(StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new StringPropertyConfig.Builder("note")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        // Schema registration
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA, actionSchema)
                        .build()).get();

        // Index a document
        // While inEmail2 has a higher document score, we will rank based on the number of joined
        // documents. inEmail1 will have 1 joined document while inEmail2 will have 0 joined
        // documents.
        AppSearchEmail inEmail =
                new AppSearchEmail.Builder("namespace", "id1")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(1)
                        .build();

        AppSearchEmail inEmail2 =
                new AppSearchEmail.Builder("namespace", "id2")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .setScore(10)
                        .build();

        String qualifiedId = DocumentIdUtil.createQualifiedId(mContext.getPackageName(), DB_NAME_1,
                "namespace", "id1");
        GenericDocument viewAction1 = new GenericDocument.Builder<>("NS", "id3", "ViewAction")
                .setScore(1)
                .setPropertyString("entityId", qualifiedId)
                .setPropertyString("note", "Viewed email on Monday").build();
        GenericDocument viewAction2 = new GenericDocument.Builder<>("NS", "id4", "ViewAction")
                .setScore(2)
                .setPropertyString("entityId", qualifiedId)
                .setPropertyString("note", "Viewed email on Tuesday").build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail, inEmail2,
                                viewAction1, viewAction2)
                        .build()));

        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                        .setOrder(SearchSpec.ORDER_ASCENDING)
                        .build();

        JoinSpec js = new JoinSpec.Builder("entityId")
                .setNestedSearch("", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .setMaxJoinedResultCount(1)
                .build();

        SearchResults searchResults = mDb1.search("body email", new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());

        List<SearchResult> sr = searchResults.getNextPageAsync().get();

        // Both email docs are returned, but id1 comes first due to the join
        assertThat(sr).hasSize(2);

        assertThat(sr.get(0).getGenericDocument().getId()).isEqualTo("id1");
        assertThat(sr.get(0).getJoinedResults()).hasSize(1);
        assertThat(sr.get(0).getJoinedResults().get(0).getGenericDocument()).isEqualTo(viewAction1);
        assertThat(sr.get(0).getRankingSignal()).isEqualTo(1.0);

        assertThat(sr.get(1).getGenericDocument().getId()).isEqualTo("id2");
        assertThat(sr.get(1).getRankingSignal()).isEqualTo(0.0);
        assertThat(sr.get(1).getJoinedResults()).isEmpty();
    }

    @Test
    public void testJoin_unsupportedFeature_throwsException() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));

        SearchSpec nestedSearchSpec = new SearchSpec.Builder().build();
        JoinSpec js = new JoinSpec.Builder("entityId").setNestedSearch("", nestedSearchSpec)
                .build();
        Exception e = assertThrows(UnsupportedOperationException.class, () -> mDb1.search(
                /*queryExpression */ "",
                new SearchSpec.Builder()
                        .setJoinSpec(js)
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build()));
        assertThat(e.getMessage()).isEqualTo("JoinSpec is not available on this AppSearch "
                + "implementation.");
    }

    @Test
    public void testSearchSuggestion_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));

        assertThrows(UnsupportedOperationException.class, () ->
                mDb1.searchSuggestionAsync(
                        /*suggestionQueryExpression=*/"t",
                        new SearchSuggestionSpec.Builder(/*totalResultCount=*/2).build()).get());
    }

    @Test
    public void testSearchSuggestion() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "termOne termTwo termThree termFour")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "termOne termTwo termThree")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "termOne termTwo")
                .build();
        GenericDocument doc4 = new GenericDocument.Builder<>("namespace", "id4", "Type")
                .setPropertyString("body", "termOne")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3, doc4)
                        .build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("termthree").build();
        SearchSuggestionResult resultFour =
                new SearchSuggestionResult.Builder().setSuggestedResult("termfour").build();

        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo, resultThree, resultFour)
                .inOrder();

        // Query first 2 suggestions, and they will be ranked.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/2).build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo).inOrder();
    }

    @Test
    public void testSearchSuggestion_namespaceFilter() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace1", "id1", "Type")
                .setPropertyString("body", "fo foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace2", "id2", "Type")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace3", "id3", "Type")
                .setPropertyString("body", "fool")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3).build()));

        SearchSuggestionResult resultFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("fo").build();
        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();

        // namespace1 has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace1").build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFo).inOrder();

        // namespace2 has 1 result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace2").build()).get();
        assertThat(suggestions).containsExactly(resultFoo).inOrder();

        // namespace2 and 3 has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace2", "namespace3")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // non exist namespace has empty result
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("nonExistNamespace").build()).get();
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_documentIdFilter() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace1", "id1", "Type")
                .setPropertyString("body", "termone")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace1", "id2", "Type")
                .setPropertyString("body", "termtwo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace2", "id3", "Type")
                .setPropertyString("body", "termthree")
                .build();
        GenericDocument doc4 = new GenericDocument.Builder<>("namespace2", "id4", "Type")
                .setPropertyString("body", "termfour")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(doc1, doc2, doc3, doc4).build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("termthree").build();
        SearchSuggestionResult resultFour =
                new SearchSuggestionResult.Builder().setSuggestedResult("termfour").build();

        // Only search for namespace1/doc1
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace1")
                        .addFilterDocumentIds("namespace1", "id1")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne);

        // Only search for namespace1/doc1 and namespace1/doc2
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace1")
                        .addFilterDocumentIds("namespace1", ImmutableList.of("id1", "id2"))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo);

        // Only search for namespace1/doc1 and namespace2/doc3
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace1", "namespace2")
                        .addFilterDocumentIds("namespace1", "id1")
                        .addFilterDocumentIds("namespace2", ImmutableList.of("id3"))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultThree);

        // Only search for namespace1/doc1 and everything in namespace2
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterDocumentIds("namespace1", "id1")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultThree, resultFour);
    }

    @Test
    public void testSearchSuggestion_schemaFilter() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schemaType1 = new AppSearchSchema.Builder("Type1").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        AppSearchSchema schemaType2 = new AppSearchSchema.Builder("Type2").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        AppSearchSchema schemaType3 = new AppSearchSchema.Builder("Type3").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schemaType1, schemaType2, schemaType3).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type1")
                .setPropertyString("body", "fo foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type2")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type3")
                .setPropertyString("body", "fool")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3).build()));

        SearchSuggestionResult resultFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("fo").build();
        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();

        // Type1 has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type1").build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFo).inOrder();

        // Type2 has 1 result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type2").build()).get();
        assertThat(suggestions).containsExactly(resultFoo).inOrder();

        // Type2 and 3 has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type2", "Type3")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // non exist type has empty result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("nonExistType").build()).get();
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_differentPrefix() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "fool")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "bar")
                .build();
        GenericDocument doc4 = new GenericDocument.Builder<>("namespace", "id4", "Type")
                .setPropertyString("body", "baz")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3, doc4)
                        .build()));

        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();
        SearchSuggestionResult resultBar =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar").build();
        SearchSuggestionResult resultBaz =
                new SearchSuggestionResult.Builder().setSuggestedResult("baz").build();

        // prefix f has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // prefix b has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"b",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultBar, resultBaz);
    }

    @Test
    public void testSearchSuggestion_differentRankingStrategy() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        // term1 appears 3 times in all 3 docs.
        // term2 appears 4 times in 2 docs.
        // term3 appears 5 times in 1 doc.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "term1 term3 term3 term3 term3 term3")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "term1 term2 term2 term2")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "term1 term2")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3)
                        .build()));

        SearchSuggestionResult result1 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term1").build();
        SearchSuggestionResult result2 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term2").build();
        SearchSuggestionResult result3 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term3").build();


        // rank by NONE, the order should be arbitrary but all terms appear.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_NONE)
                        .build()).get();
        assertThat(suggestions).containsExactly(result2, result1, result3);

        // rank by document count, the order should be term1:3 > term2:2 > term3:1
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT)
                        .build()).get();
        assertThat(suggestions).containsExactly(result1, result2, result3).inOrder();

        // rank by term frequency, the order should be term3:5 > term2:4 > term1:3
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_TERM_FREQUENCY)
                        .build()).get();
        assertThat(suggestions).containsExactly(result3, result2, result1).inOrder();
    }

    @Test
    public void testSearchSuggestion_removeDocument() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument docTwo = new GenericDocument.Builder<>("namespace", "idTwo", "Type")
                .setPropertyString("body", "two")
                .build();
        GenericDocument docThree = new GenericDocument.Builder<>("namespace", "idThree", "Type")
                .setPropertyString("body", "three")
                .build();
        GenericDocument docTart = new GenericDocument.Builder<>("namespace", "idTart", "Type")
                .setPropertyString("body", "tart")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(docTwo, docThree, docTart)
                        .build()));

        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("two").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("three").build();
        SearchSuggestionResult resultTart =
                new SearchSuggestionResult.Builder().setSuggestedResult("tart").build();

        // prefix t has 3 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultTwo, resultThree, resultTart);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "idTwo").build()));

        // now prefix t has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultThree, resultTart);
    }

    @Test
    public void testSearchSuggestion_replacementDocument() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyString("body", "two three tart")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc)
                        .build()));

        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("two").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("three").build();
        SearchSuggestionResult resultTart =
                new SearchSuggestionResult.Builder().setSuggestedResult("tart").build();
        SearchSuggestionResult resultTwist =
                new SearchSuggestionResult.Builder().setSuggestedResult("twist").build();

        // prefix t has 3 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultTwo, resultThree, resultTart);

        // replace the document
        GenericDocument replaceDoc = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyString("body", "twist three")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(replaceDoc)
                        .build()));

        // prefix t has 2 results for now.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultThree, resultTwist);
    }

    @Test
    public void testSearchSuggestion_twoInstances() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();
        mDb2.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents to database 1.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "termOne termTwo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "termOne")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2)
                        .build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();

        // database 1 could get suggestion results
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo).inOrder();

        // database 2 couldn't get suggestion results
        suggestions = mDb2.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_multipleTerms() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "bar fo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "cat foo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "fool")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3)
                        .build()));

        // Search "bar AND f" only document 1 should match the search.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"bar f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        SearchSuggestionResult barFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar fo").build();
        assertThat(suggestions).containsExactly(barFo);

        // Search for "(bar OR cat) AND f" both document1 "bar fo" and document2 "cat foo" could
        // match.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"bar OR cat f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        SearchSuggestionResult barCatFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar OR cat fo").build();
        SearchSuggestionResult barCatFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar OR cat foo").build();
        assertThat(suggestions).containsExactly(barCatFo, barCatFoo);

        // Search for "(bar AND cat) OR f", all documents could match.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"(bar cat) OR f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        SearchSuggestionResult barCatOrFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("(bar cat) OR fo").build();
        SearchSuggestionResult barCatOrFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("(bar cat) OR foo").build();
        SearchSuggestionResult barCatOrFool =
                new SearchSuggestionResult.Builder()
                        .setSuggestedResult("(bar cat) OR fool").build();
        assertThat(suggestions).containsExactly(barCatOrFo, barCatOrFoo, barCatOrFool);

        // Search for "-bar f", document2 "cat foo" could and document3 "fool" could match.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"-bar f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        SearchSuggestionResult noBarFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("-bar foo").build();
        SearchSuggestionResult noBarFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("-bar fool").build();
        assertThat(suggestions).containsExactly(noBarFoo, noBarFool);
    }

    @Test
    public void testSearchSuggestion_PropertyRestriction() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SEARCH_SUGGESTION));
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("subject", "bar fo")
                .setPropertyString("body", "fool")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("subject", "bar cat foo")
                .setPropertyString("body", "fool")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "ide", "Type")
                .setPropertyString("subject", "fool")
                .setPropertyString("body", "fool")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3)
                        .build()));

        // Search for "bar AND subject:f"
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"bar subject:f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        SearchSuggestionResult barSubjectFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar subject:fo").build();
        SearchSuggestionResult barSubjectFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar subject:foo").build();
        assertThat(suggestions).containsExactly(barSubjectFo, barSubjectFoo);
    }
}
