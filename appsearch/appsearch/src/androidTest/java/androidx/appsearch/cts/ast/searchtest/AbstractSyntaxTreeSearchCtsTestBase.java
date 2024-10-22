/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.cts.ast.searchtest;

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.ast.NegationNode;
import androidx.appsearch.ast.TextNode;
import androidx.appsearch.ast.operators.AndNode;
import androidx.appsearch.ast.operators.ComparatorNode;
import androidx.appsearch.ast.operators.OrNode;
import androidx.appsearch.ast.operators.PropertyRestrictNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;
import androidx.appsearch.testutil.AppSearchEmail;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public abstract class AbstractSyntaxTreeSearchCtsTestBase {
    static final String DB_NAME_1 = "";
    private AppSearchSession mDb1;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName) throws Exception;

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
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Test
    public void testTextNode_toString_noFlagsSet() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail inEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("This is the body of the testPut email")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        TextNode body = new TextNode("body");

        SearchResults searchResults = mDb1.search(body.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail);
    }

    @Test
    public void testTextNode_toString_PrefixFlagSet() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .build();

        AppSearchEmail inEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("This is the body of the testPut email")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        TextNode boPrefix = new TextNode("bo");

        // Check that searching the prefix without setting it as a prefix returns nothing.
        SearchResults emptySearchResults = mDb1.search(boPrefix.toString(), searchSpec);
        List<GenericDocument> emptyDocuments = convertSearchResultsToDocuments(emptySearchResults);
        assertThat(emptyDocuments).hasSize(0);

        // Now check that search the prefix with setting it as a prefix returns the document.
        boPrefix.setPrefix(true);
        SearchResults searchResults = mDb1.search(boPrefix.toString(), searchSpec);
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail);
    }

    @Test
    public void testTextNode_toString_VerbatimFlagSet()
            throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("VerbatimSchema")
                .addProperty(new StringPropertyConfig.Builder("verbatimProp")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setVerbatimSearchEnabled(true)
                .build();

        GenericDocument doc = new GenericDocument.Builder<>(
                "namespace", "id1", "VerbatimSchema")
                .setPropertyString("verbatimProp",
                        "Hello, world!")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()));

        // Query for the document
        TextNode verbatimQuery = new TextNode("Hello, world!");

        // Check that searching using the query without setting it as a verbatim returns nothing.
        SearchResults emptySearchResults = mDb1.search(verbatimQuery.toString(), searchSpec);
        List<GenericDocument> emptyDocuments = convertSearchResultsToDocuments(emptySearchResults);
        assertThat(emptyDocuments).hasSize(0);

        // Now check that search using the query with setting it as a verbatim returns the document.
        verbatimQuery.setVerbatim(true);
        SearchResults searchResults = mDb1.search(verbatimQuery.toString(), searchSpec);
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(doc);
    }

    @Test
    public void testTextNode_toString_AllFlagsSet() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("VerbatimSchema")
                .addProperty(new StringPropertyConfig.Builder("verbatimProp")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setVerbatimSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build();

        GenericDocument doc = new GenericDocument.Builder<>(
                "namespace", "id1", "VerbatimSchema")
                .setPropertyString("verbatimProp",
                        "Hello, world!")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()));


        // Query for the document
        TextNode prefixedVerbatimQuery = new TextNode("Hello,");

        // Check that searching using the query without setting it as a verbatim returns nothing.
        SearchResults emptySearchResults = mDb1.search(prefixedVerbatimQuery.toString(),
                searchSpec);
        List<GenericDocument> emptyDocuments = convertSearchResultsToDocuments(emptySearchResults);
        assertThat(emptyDocuments).hasSize(0);

        // Now check that search using the query with setting it as a verbatim returns the document.
        prefixedVerbatimQuery.setVerbatim(true);
        prefixedVerbatimQuery.setPrefix(true);
        SearchResults searchResults = mDb1.search(prefixedVerbatimQuery.toString(), searchSpec);
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(doc);
    }

    @Test
    public void testTextNode_toString_escapesLogicalOperators() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail inEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("NOT you AND me OR them")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        TextNode body = new TextNode("NOT you AND me OR them");

        SearchResults searchResults = mDb1.search(body.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail);
    }

    @Test
    public void testTextNode_toString_escapesSpecialCharacters() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail inEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("(\"foo\"* bar:-baz) (property.path > 0)")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        TextNode body = new TextNode("(\"foo\"* bar:-baz) (property.path > 0)");

        SearchResults searchResults = mDb1.search(body.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail);
    }

    @Test
    public void testTextNode_toString_prefixedMultiTerm() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail inEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("foo barter")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(inEmail).build()));

        // Query for the document
        TextNode body = new TextNode("foo bar");
        body.setPrefix(true);

        SearchResults searchResults = mDb1.search(body.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(inEmail);
    }

    @Test
    public void testTextNode_toString_prefixedTermWithEndWhitespace() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail barEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("bar")
                .build();

        AppSearchEmail fooEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("foo")
                .build();

        AppSearchEmail fooBarterEmail = new AppSearchEmail.Builder("namespace", "id3")
                .setBody("foo barter")
                .build();

        AppSearchEmail fooBarBazEmail = new AppSearchEmail.Builder("namespace", "id4")
                .setBody("foo bar baz")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(
                        barEmail,
                        fooEmail,
                        fooBarterEmail,
                        fooBarBazEmail
                ).build()));

        // Query for the document
        TextNode body = new TextNode("foo ");
        body.setPrefix(true);

        SearchResults searchResults = mDb1.search(body.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooEmail, fooBarterEmail, fooBarBazEmail);
    }

    @Test
    public void testNegationNode_toString_returnsDocumentsWithoutTerm() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail fooEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("foo")
                .build();
        AppSearchEmail barEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("bar")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(fooEmail, barEmail).build()));

        // Query for the document.
        TextNode foo = new TextNode("foo");
        NegationNode notFoo = new NegationNode(foo);

        SearchResults searchResults = mDb1.search(notFoo.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(barEmail);
    }

    @Test
    public void testAndNode_toString_returnsDocumentsWithBothTerms() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail fooEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("foo")
                .build();
        AppSearchEmail barEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("bar")
                .build();
        AppSearchEmail fooBarEmail = new AppSearchEmail.Builder("namespace", "id3")
                .setBody("foo bar")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(fooEmail, barEmail, fooBarEmail).build()));

        // Query for the document
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        AndNode andNode = new AndNode(foo, bar);

        SearchResults searchResults = mDb1.search(andNode.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooBarEmail);
    }

    @Test
    public void testOrNode_toString_returnsDocumentsWithEitherTerms() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail fooEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("foo")
                .build();
        AppSearchEmail barEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("bar")
                .build();
        AppSearchEmail fooBarEmail = new AppSearchEmail.Builder("namespace", "id3")
                .setBody("foo bar")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(fooEmail, barEmail, fooBarEmail).build()));

        // Query for the document
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        OrNode orNode = new OrNode(foo, bar);

        SearchResults searchResults = mDb1.search(orNode.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooEmail, barEmail, fooBarEmail);
    }

    @Test
    public void testAndNodeOrNode_toString_respectsOperatorPrecedence() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();


        AppSearchEmail fooBarEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("foo bar")
                .build();
        AppSearchEmail fooBazEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("foo baz")
                .build();
        AppSearchEmail bazEmail = new AppSearchEmail.Builder("namespace", "id3")
                .setBody("baz")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(bazEmail, fooBarEmail, fooBazEmail).build()));

        // Query for the document
        TextNode foo = new TextNode("foo");
        TextNode bar = new TextNode("bar");
        TextNode baz = new TextNode("baz");
        AndNode andNode = new AndNode(foo, bar);
        OrNode orNode = new OrNode(andNode, baz);

        SearchResults searchResults = mDb1.search(orNode.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooBarEmail, fooBazEmail, bazEmail);
    }

    @Test
    public void testComparatorNode_toString_doesNumericSearch() throws Exception {
        // Schema registration
        AppSearchSchema transactionSchema =
                new AppSearchSchema.Builder("transaction")
                        .addProperty(
                                new LongPropertyConfig.Builder("price")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                                        .build())
                        .addProperty(
                                new LongPropertyConfig.Builder("cost")
                                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)
                                        .build())
                        .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(transactionSchema)
                .build()
            ).get();

        // Index some documents
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "transaction")
                        .setPropertyLong("price", 10)
                        .build();
        GenericDocument doc2 =
                new GenericDocument.Builder<>("namespace", "id2", "transaction")
                        .setPropertyLong("price", 25)
                        .build();
        GenericDocument doc3 =
                new GenericDocument.Builder<>("namespace", "id3", "transaction")
                        .setPropertyLong("cost", 2)
                        .build();
        checkIsBatchResultSuccess(
                mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(doc1, doc2, doc3)
                                .build()));

        // Query for the document.
        PropertyPath pricePath = new PropertyPath("price");
        ComparatorNode comparatorNode = new ComparatorNode(ComparatorNode.LESS_THAN, pricePath, 20);

        SearchResults searchResults = mDb1.search(comparatorNode.toString(),
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setNumericSearchEnabled(true)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(doc1);
    }

    @Test
    public void testPropertyRestrict_toString_restrictsByProperty() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail fooFromEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("foo")
                .setBody("bar")
                .build();
        AppSearchEmail fooBodyEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("foo")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(fooFromEmail, fooBodyEmail)
                        .build()
            )
        );

        // Query for the document.
        TextNode foo = new TextNode("foo");
        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(
                new PropertyPath("body"), foo);

        SearchResults searchResults = mDb1.search(propertyRestrictNode.toString(),
                new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooBodyEmail);
    }
}
