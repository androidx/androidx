/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.app;

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.testutil.AppSearchEmail;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;


/** This class holds all tests that won't be exported to the framework.  */
public abstract class AppSearchSessionInternalTestBase {

    static final String DB_NAME_1 = "";

    protected AppSearchSession mDb1;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName);

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor);

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();

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
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    // TODO(b/268521214): Move test to cts once deletion propagation is available in framework.
    @Test
    public void testGetSchema_joinableValueType() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        AppSearchSchema inSchema =
                new AppSearchSchema.Builder("Test")
                        .addProperty(
                                new StringPropertyConfig.Builder("normalStr")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("optionalQualifiedIdStr")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("requiredQualifiedIdStr")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setJoinableValueType(
                                                StringPropertyConfig
                                                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                                        .build())
                        .build();

        SetSchemaRequest request = new SetSchemaRequest.Builder().addSchemas(inSchema).build();

        mDb1.setSchemaAsync(request).get();

        Set<AppSearchSchema> actual = mDb1.getSchemaAsync().get().getSchemas();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsIn(request.getSchemas());
    }

    @Test
    public void testQuery_typeFilterWithPolymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        // Schema registration
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist")
                        .addParentType("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema)
                                .addSchemas(artistSchema)
                                .addSchemas(AppSearchEmail.SCHEMA)
                                .build())
                .get();

        // Index some documents
        GenericDocument personDoc =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setPropertyString("name", "Foo")
                        .build();
        GenericDocument artistDoc =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setPropertyString("name", "Foo")
                        .build();
        AppSearchEmail emailDoc =
                new AppSearchEmail.Builder("namespace", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("Foo")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(personDoc, artistDoc, emailDoc)
                                .build()));
        GenericDocument artistDocWithParent =
                new GenericDocument.Builder<>(artistDoc).setParentTypes(
                Collections.singletonList("Person")).build();

        // Query for the documents
        SearchResults searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);
        assertThat(documents).containsExactly(personDoc, artistDocWithParent, emailDoc);

        // Query with a filter for the "Person" type should also include the "Artist" type.
        searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .addFilterSchemas("Person")
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(personDoc, artistDocWithParent);

        // Query with a filters for the "Artist" type should not include the "Person" type.
        searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .addFilterSchemas("Artist")
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(artistDocWithParent);
    }

    @Test
    public void testQuery_projectionWithPolymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        // Schema registration
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("emailAddress")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist")
                        .addParentType("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("emailAddress")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("company")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema)
                                .addSchemas(artistSchema)
                                .build())
                .get();

        // Index two documents
        GenericDocument personDoc =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Person")
                        .setPropertyString("emailAddress", "person@gmail.com")
                        .build();
        GenericDocument artistDoc =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Artist")
                        .setPropertyString("emailAddress", "artist@gmail.com")
                        .setPropertyString("company", "Company")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(personDoc, artistDoc)
                                .build()));

        // Query with type property paths {"Person", ["name"]}, {"Artist", ["emailAddress"]}
        // This will be expanded to paths {"Person", ["name"]}, {"Artist", ["name", "emailAddress"]}
        // via polymorphism.
        SearchResults searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .addProjection("Person", ImmutableList.of("name"))
                                .addProjection("Artist", ImmutableList.of("emailAddress"))
                                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The person document should have been returned with only the "name" property. The artist
        // document should have been returned with all of its properties.
        GenericDocument expectedPerson =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Person")
                        .build();
        GenericDocument expectedArtist =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setParentTypes(Collections.singletonList("Person"))
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Artist")
                        .setPropertyString("emailAddress", "artist@gmail.com")
                        .build();
        assertThat(documents).containsExactly(expectedPerson, expectedArtist);
    }

    @Test
    public void testQuery_projectionWithPolymorphismAndSchemaFilter() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        // Schema registration
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("emailAddress")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist")
                        .addParentType("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("emailAddress")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("company")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema)
                                .addSchemas(artistSchema)
                                .build())
                .get();

        // Index two documents
        GenericDocument personDoc =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Person")
                        .setPropertyString("emailAddress", "person@gmail.com")
                        .build();
        GenericDocument artistDoc =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Artist")
                        .setPropertyString("emailAddress", "artist@gmail.com")
                        .setPropertyString("company", "Company")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(personDoc, artistDoc)
                                .build()));

        // Query with type property paths {"Person", ["name"]} and {"Artist", ["emailAddress"]}, and
        // a schema filter for the "Person".
        // This will be expanded to paths {"Person", ["name"]} and
        // {"Artist", ["name", "emailAddress"]}, and filters for both "Person" and "Artist" via
        // polymorphism.
        SearchResults searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .addFilterSchemas("Person")
                                .addProjection("Person", ImmutableList.of("name"))
                                .addProjection("Artist", ImmutableList.of("emailAddress"))
                                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // The person document should have been returned with only the "name" property. The artist
        // document should have been returned with all of its properties.
        GenericDocument expectedPerson =
                new GenericDocument.Builder<>("namespace", "id1", "Person")
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Person")
                        .build();
        GenericDocument expectedArtist =
                new GenericDocument.Builder<>("namespace", "id2", "Artist")
                        .setParentTypes(Collections.singletonList("Person"))
                        .setCreationTimestampMillis(1000)
                        .setPropertyString("name", "Foo Artist")
                        .setPropertyString("emailAddress", "artist@gmail.com")
                        .build();
        assertThat(documents).containsExactly(expectedPerson, expectedArtist);
    }

    @Test
    public void testQuery_indexBasedOnParentTypePolymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        // Schema registration
        AppSearchSchema personSchema =
                new AppSearchSchema.Builder("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema artistSchema =
                new AppSearchSchema.Builder("Artist")
                        .addParentType("Person")
                        .addProperty(
                                new StringPropertyConfig.Builder("name")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .addProperty(
                                new StringPropertyConfig.Builder("company")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                        .setIndexingType(
                                                StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                        .build())
                        .build();
        AppSearchSchema messageSchema =
                new AppSearchSchema.Builder("Message")
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Person")
                                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                                        .setShouldIndexNestedProperties(true)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(personSchema)
                                .addSchemas(artistSchema)
                                .addSchemas(messageSchema)
                                .build())
                .get();

        // Index some an artistDoc and a messageDoc
        GenericDocument artistDoc =
                new GenericDocument.Builder<>("namespace", "id1", "Artist")
                        .setPropertyString("name", "Foo")
                        .setPropertyString("company", "Bar")
                        .build();
        GenericDocument messageDoc =
                new GenericDocument.Builder<>("namespace", "id2", "Message")
                        // sender is defined as a Person, which accepts an Artist because Artist <:
                        // Person.
                        // However, indexing will be based on what's defined in Person, so the
                        // "company"
                        // property in artistDoc cannot be used to search this messageDoc.
                        .setPropertyDocument("sender", artistDoc)
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(artistDoc, messageDoc)
                                .build()));
        GenericDocument expectedArtistDoc =
                new GenericDocument.Builder<>(artistDoc).setParentTypes(
                Collections.singletonList("Person")).build();
        GenericDocument expectedMessageDoc =
                new GenericDocument.Builder<>(messageDoc).setPropertyDocument("sender",
                expectedArtistDoc).build();

        // Query for the documents
        SearchResults searchResults =
                mDb1.search(
                        "Foo",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents).containsExactly(expectedArtistDoc, expectedMessageDoc);

        // The "company" property in artistDoc cannot be used to search messageDoc.
        searchResults =
                mDb1.search(
                        "Bar",
                        new SearchSpec.Builder()
                                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                                .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents).containsExactly(expectedArtistDoc);
    }

    @Test
    public void testQuery_parentTypeListIsTopologicalOrder() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        // Create the following subtype relation graph, where
        // 1. A's direct parents are B and C.
        // 2. B's direct parent is D.
        // 3. C's direct parent is B and D.
        // DFS order from A: [A, B, D, C]. Not acceptable because B and D appear before C.
        // BFS order from A: [A, B, C, D]. Not acceptable because B appears before C.
        // Topological order (all subtypes appear before supertypes) from A: [A, C, B, D].
        AppSearchSchema schemaA =
                new AppSearchSchema.Builder("A")
                        .addParentType("B")
                        .addParentType("C")
                        .build();
        AppSearchSchema schemaB =
                new AppSearchSchema.Builder("B")
                        .addParentType("D")
                        .build();
        AppSearchSchema schemaC =
                new AppSearchSchema.Builder("C")
                        .addParentType("B")
                        .addParentType("D")
                        .build();
        AppSearchSchema schemaD =
                new AppSearchSchema.Builder("D")
                        .build();
        mDb1.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addSchemas(schemaA)
                                .addSchemas(schemaB)
                                .addSchemas(schemaC)
                                .addSchemas(schemaD)
                                .build())
                .get();

        // Index some documents
        GenericDocument docA =
                new GenericDocument.Builder<>("namespace", "id1", "A")
                        .build();
        GenericDocument docB =
                new GenericDocument.Builder<>("namespace", "id2", "B")
                        .build();
        GenericDocument docC =
                new GenericDocument.Builder<>("namespace", "id3", "C")
                        .build();
        GenericDocument docD =
                new GenericDocument.Builder<>("namespace", "id4", "D")
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(docA, docB, docC, docD)
                                .build()));

        GenericDocument expectedDocA = new GenericDocument.Builder<>(docA).setParentTypes(
                        new ArrayList<>(Arrays.asList("C", "B", "D"))).build();
        GenericDocument expectedDocB =
                new GenericDocument.Builder<>(docB).setParentTypes(
                        Collections.singletonList("D")).build();
        GenericDocument expectedDocC =
                new GenericDocument.Builder<>(docC).setParentTypes(
                        new ArrayList<>(Arrays.asList("B", "D"))).build();
        // Query for the documents
        SearchResults searchResults = mDb1.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(4);
        assertThat(documents).containsExactly(expectedDocA, expectedDocB, expectedDocC, docD);
    }

    // TODO(b/336277840): Move this if setParentTypes becomes public
    @Test
    public void testQuery_wildcardProjection_polymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        AppSearchSchema messageSchema = new AppSearchSchema.Builder("Message")
                .addProperty(new StringPropertyConfig.Builder("sender")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("content")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema textSchema = new AppSearchSchema.Builder("Text")
                .addParentType("Message")
                .addProperty(new StringPropertyConfig.Builder("sender")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("content")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addParentType("Message")
                .addProperty(new StringPropertyConfig.Builder("sender")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("content")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(messageSchema, textSchema, emailSchema).build()).get();

        // Index two child documents
        GenericDocument text = new GenericDocument.Builder<>("namespace", "id1", "Text")
                .setCreationTimestampMillis(1000)
                .setPropertyString("sender", "Some sender")
                .setPropertyString("content", "Some note")
                .build();
        GenericDocument email = new GenericDocument.Builder<>("namespace", "id2", "Email")
                .setCreationTimestampMillis(1000)
                .setPropertyString("sender", "Some sender")
                .setPropertyString("content", "Some note")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(email, text).build()));

        SearchResults searchResults = mDb1.search("Some", new SearchSpec.Builder()
                .addFilterSchemas("Message")
                .addProjection(SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("sender"))
                .addFilterProperties(SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("content"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // We specified the parent document in the filter schemas, but only indexed child documents.
        // As we also specified a wildcard schema type projection, it should apply to the child docs
        // The content property must not appear. Also emailNoContent should not appear as we are
        // filter on the content property
        GenericDocument expectedText = new GenericDocument.Builder<>("namespace", "id1", "Text")
                .setParentTypes(Collections.singletonList("Message"))
                .setCreationTimestampMillis(1000)
                .setPropertyString("sender", "Some sender")
                .build();
        GenericDocument expectedEmail = new GenericDocument.Builder<>("namespace", "id2", "Email")
                .setParentTypes(Collections.singletonList("Message"))
                .setCreationTimestampMillis(1000)
                .setPropertyString("sender", "Some sender")
                .build();
        assertThat(documents).containsExactly(expectedText, expectedEmail);
    }

    // TODO(b/336277840): Move this if setParentTypes becomes public
    @Test
    public void testQuery_wildcardFilterSchema_polymorphism() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));
        AppSearchSchema messageSchema = new AppSearchSchema.Builder("Message")
                .addProperty(new StringPropertyConfig.Builder("content")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema textSchema = new AppSearchSchema.Builder("Text")
                .addParentType("Message")
                .addProperty(new StringPropertyConfig.Builder("content")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("carrier")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addParentType("Message")
                .addProperty(new StringPropertyConfig.Builder("content")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("attachment")
                        .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        // Schema registration
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(messageSchema, textSchema, emailSchema).build()).get();

        // Index two child documents
        GenericDocument text = new GenericDocument.Builder<>("namespace", "id1", "Text")
                .setCreationTimestampMillis(1000)
                .setPropertyString("content", "Some note")
                .setPropertyString("carrier", "Network Inc")
                .build();
        GenericDocument email = new GenericDocument.Builder<>("namespace", "id2", "Email")
                .setCreationTimestampMillis(1000)
                .setPropertyString("content", "Some note")
                .setPropertyString("attachment", "Network report")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(new PutDocumentsRequest.Builder()
                .addGenericDocuments(email, text).build()));

        // Both email and text would match for "Network", but only text should match as it is in the
        // right property
        SearchResults searchResults = mDb1.search("Network", new SearchSpec.Builder()
                .addFilterSchemas("Message")
                .addFilterProperties(SearchSpec.SCHEMA_TYPE_WILDCARD, ImmutableList.of("carrier"))
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        // We specified the parent document in the filter schemas, but only indexed child documents.
        // As we also specified a wildcard schema type projection, it should apply to the child docs
        // The content property must not appear. Also emailNoContent should not appear as we are
        // filter on the content property
        GenericDocument expectedText = new GenericDocument.Builder<>("namespace", "id1", "Text")
                .setParentTypes(Collections.singletonList("Message"))
                .setCreationTimestampMillis(1000)
                .setPropertyString("content", "Some note")
                .setPropertyString("carrier", "Network Inc")
                .build();
        assertThat(documents).containsExactly(expectedText);
    }
}
