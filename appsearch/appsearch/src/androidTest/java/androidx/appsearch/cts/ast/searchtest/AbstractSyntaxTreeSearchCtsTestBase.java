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

import static org.junit.Assume.assumeTrue;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.Features;
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
import androidx.appsearch.ast.query.GetSearchStringParameterNode;
import androidx.appsearch.ast.query.HasPropertyNode;
import androidx.appsearch.ast.query.PropertyDefinedNode;
import androidx.appsearch.ast.query.SearchNode;
import androidx.appsearch.ast.query.SemanticSearchNode;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public abstract class AbstractSyntaxTreeSearchCtsTestBase {
    static final String DB_NAME_1 = "";
    private AppSearchSession mDb1;

    private static final EmbeddingVector EMBEDDING_1 =
            new EmbeddingVector(new float[]{1, 1, 1, 1, 2}, "model_v1");
    private static final EmbeddingVector EMBEDDING_2 =
            new EmbeddingVector(new float[]{1, 1, 1, 1, 0}, "model_v1");
    private static final EmbeddingVector EMBEDDING_3 =
            new EmbeddingVector(new float[]{1, 1, 1, 1, -2}, "model_v1");

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

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

    @Test
    public void testPropertyRestrictNode_toString_handlesMultipleTerms() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail fooBarFrom = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("foo bar")
                .build();
        AppSearchEmail fooBodyEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("foo bar")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(fooBarFrom, fooBodyEmail)
                                .build()
                )
        );

        // Query for the document.
        TextNode fooBar = new TextNode("foo bar");
        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(
                new PropertyPath("body"), fooBar);

        SearchResults searchResults = mDb1.search(propertyRestrictNode.toString(),
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooBodyEmail);
    }

    @Test
    public void testPropertyRestrictNode_toString_handlesMultipleTerms_prefixed() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail fooBarFrom = new AppSearchEmail.Builder("namespace", "id1")
                .setFrom("foo bar")
                .build();
        AppSearchEmail fooBodyEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("foo bar")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(fooBarFrom, fooBodyEmail)
                                .build()
                )
        );

        TextNode fooB = new TextNode("foo b");
        PropertyRestrictNode propertyRestrictNode = new PropertyRestrictNode(
                new PropertyPath("body"), fooB);

        // Without prefix match, this will not return the document.
        SearchResults emptySearchResults = mDb1.search(propertyRestrictNode.toString(),
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        List<GenericDocument> emptyDocuments = convertSearchResultsToDocuments(emptySearchResults);
        assertThat(emptyDocuments).isEmpty();

        // With prefix match, this will return the document.
        SearchResults searchResults = mDb1.search(propertyRestrictNode.toString(),
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooBodyEmail);
    }

    @Test
    public void testGetSearchStringParameterNode_toString_retrievesSearchString() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail fooEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("foo")
                .build();

        AppSearchEmail barEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setBody("bar")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(fooEmail, barEmail).build()));

        // Query for the document.
        GetSearchStringParameterNode getSearchStringParameterNode =
                new GetSearchStringParameterNode(0);
        SearchResults searchResults = mDb1.search(getSearchStringParameterNode.toString(),
                new SearchSpec.Builder()
                        .addSearchStringParameters("foo")
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        assertThat(documents).containsExactly(fooEmail);
    }

    @Test
    public void testHasProperty_toString_returnsDocumentsWithProperty() throws Exception {
        // Schema Registration
        AppSearchSchema noBodySchema = new AppSearchSchema.Builder("NoBodySchema")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA, noBodySchema).build()).get();

        GenericDocument noBodyPropertyDoc =
                new GenericDocument.Builder<>("namespace",
                        "genericId1",
                        "NoBodySchema")
                        .build();
        AppSearchEmail emptyBodyEmail = new AppSearchEmail.Builder("namespace", "emailId1")
                .build();
        AppSearchEmail nonEmptyBodyEmail =
                new AppSearchEmail.Builder("namespace", "emailId2")
                        .setBody("bar")
                        .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(noBodyPropertyDoc, emptyBodyEmail, nonEmptyBodyEmail)
                        .build()));

        HasPropertyNode hasPropertyNode = new HasPropertyNode(new PropertyPath("body"));
        SearchResults searchResults = mDb1.search(hasPropertyNode.toString(),
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setListFilterHasPropertyFunctionEnabled(true)
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);

        assertThat(documents).containsExactly(nonEmptyBodyEmail);
    }

    @Test
    public void testPropertyDefined_toString_returnsDocumentsWithPropertyDefined()
            throws Exception {
        // Schema Registration
        AppSearchSchema noBodySchema = new AppSearchSchema.Builder("NoBodySchema")
                .addProperty(new StringPropertyConfig.Builder("subject")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA, noBodySchema).build()).get();

        GenericDocument noBodyPropertyDoc =
                new GenericDocument.Builder<>("namespace",
                        "id1",
                        "NoBodySchema")
                        .build();
        AppSearchEmail emptyBodyEmail = new AppSearchEmail.Builder("namespace", "id2")
                .build();
        AppSearchEmail nonEmptyBodyEmail =
                new AppSearchEmail.Builder("namespace", "id3")
                        .setBody("bar")
                        .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder()
                        .addGenericDocuments(noBodyPropertyDoc, emptyBodyEmail, nonEmptyBodyEmail)
                        .build()));

        // Query for the document.
        PropertyDefinedNode propertyDefinedNode = new PropertyDefinedNode(new PropertyPath("body"));

        SearchResults searchResults = mDb1.search(propertyDefinedNode.toString(),
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setListFilterHasPropertyFunctionEnabled(true)
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(emptyBodyEmail, nonEmptyBodyEmail);
    }

    @Test
    public void testSearchNode_toString_noPropertyRestricts_retrievesSameDocuments()
            throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail barBodyEmail = new AppSearchEmail.Builder("namespace", "id0")
                .setBody("bar")
                .build();

        AppSearchEmail fooBodyEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("foo")
                .build();

        AppSearchEmail fooFromEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setFrom("foo")
                .setTo("baz")
                .setBody("bar")
                .build();
        AppSearchEmail fooToEmail = new AppSearchEmail.Builder("namespace", "id3")
                .setFrom("baz")
                .setTo("foo")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(
                                        barBodyEmail,
                                        fooBodyEmail,
                                        fooFromEmail,
                                        fooToEmail)
                                .build()
                )
        );

        // Query for the document
        TextNode body = new TextNode("foo");
        SearchNode search = new SearchNode(body);

        SearchResults searchResults = mDb1.search(search.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooBodyEmail, fooFromEmail, fooToEmail);
    }

    @Test
    public void testSearchNode_toString_hasPropertyRestricts_retrievesDocumentsWithProperty()
            throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().addSchemas(AppSearchEmail.SCHEMA).build()).get();

        AppSearchEmail fooBodyEmail = new AppSearchEmail.Builder("namespace", "id1")
                .setBody("foo")
                .build();

        AppSearchEmail fooFromEmail = new AppSearchEmail.Builder("namespace", "id2")
                .setFrom("foo")
                .setTo("baz")
                .build();
        AppSearchEmail fooToEmail = new AppSearchEmail.Builder("namespace", "id3")
                .setFrom("baz")
                .setTo("foo")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(fooBodyEmail, fooFromEmail, fooToEmail)
                                .build()
                )
        );

        // Query for the document
        TextNode body = new TextNode("foo");
        List<PropertyPath> properties = List.of(new PropertyPath("from"), new PropertyPath("to"));
        SearchNode search = new SearchNode(body, properties);

        SearchResults searchResults = mDb1.search(search.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(fooFromEmail, fooToEmail);
    }

    @Test
    public void testSearchNode_toString_handlesStringLiterals() throws Exception {
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
                .setListFilterQueryLanguageEnabled(true)
                .build();

        GenericDocument doc = new GenericDocument.Builder<>(
                "namespace", "id1", "VerbatimSchema")
                .setPropertyString("verbatimProp",
                        "Hello, world!")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()));

        // Check that searching using the query without setting it as a verbatim returns nothing.
        TextNode nonVerbatimQuery = new TextNode("Hello, world!");
        SearchNode searchNode = new SearchNode(nonVerbatimQuery);
        SearchResults emptySearchResults = mDb1.search(searchNode.toString(),
                searchSpec);
        List<GenericDocument> emptyDocuments = convertSearchResultsToDocuments(emptySearchResults);
        assertThat(emptyDocuments).isEmpty();

        // Now check that search using the query with setting it as a verbatim returns the document.
        TextNode verbatimQuery = new TextNode("Hello, world!");
        verbatimQuery.setVerbatim(true);
        searchNode.setChild(verbatimQuery);
        SearchResults searchResults = mDb1.search(searchNode.toString(), searchSpec);
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(doc);
    }

    @Test
    public void testSearchNode_toString_handlesPrefixedStringLiterals() throws Exception {
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

        // Check that searching using the verbatim query without setting it as a prefix returns
        // nothing.
        TextNode nonPrefixedVerbatimQuery = new TextNode("Hello, wor");
        nonPrefixedVerbatimQuery.setVerbatim(true);
        SearchNode searchNode = new SearchNode(nonPrefixedVerbatimQuery);
        SearchResults emptySearchResults = mDb1.search(searchNode.toString(),
                searchSpec);
        List<GenericDocument> emptyDocuments = convertSearchResultsToDocuments(emptySearchResults);
        assertThat(emptyDocuments).isEmpty();

        // Check that Prefixed Verbatim Queries returns the document
        TextNode prefixedVerbatimQuery = new TextNode("Hello, wor");
        prefixedVerbatimQuery.setVerbatim(true);
        prefixedVerbatimQuery.setPrefix(true);
        searchNode.setChild(prefixedVerbatimQuery);
        SearchResults prefixedSearchResults = mDb1.search(searchNode.toString(), searchSpec);
        List<GenericDocument> prefixDocuments =
                convertSearchResultsToDocuments(prefixedSearchResults);
        assertThat(prefixDocuments).containsExactly(doc);
    }

    @Test
    public void testSearchNode_toString_handlesNestedSearch() throws Exception {
        // Schema Registration
        AppSearchSchema verbatimSchema = new AppSearchSchema.Builder("VerbatimSchema")
                .addProperty(new StringPropertyConfig.Builder("from")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("to")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .build())
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .build())
                .build();
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder()
                        .addSchemas(AppSearchEmail.SCHEMA, verbatimSchema).build()).get();

        GenericDocument fooBodyEmail = new GenericDocument.Builder<>(
                "namespace",
                "id1",
                "VerbatimSchema")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument fooFromEmail = new GenericDocument.Builder<>(
                "namespace",
                "id2",
                "VerbatimSchema")
                .setPropertyString("from", "foo")
                .setPropertyString("to", "bar")
                .build();
        GenericDocument fooToEmail = new GenericDocument.Builder<>(
                "namespace",
                "id3",
                "VerbatimSchema")
                .setPropertyString("from", "bar")
                .setPropertyString("to", "foo")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(fooBodyEmail, fooFromEmail, fooToEmail)
                                .build()
                )
        );

        // Check that the nested query returns the correct document.
        TextNode body = new TextNode("foo");
        body.setVerbatim(true);
        List<PropertyPath> properties = List.of(new PropertyPath("from"), new PropertyPath("to"));
        SearchNode nestedSearch = new SearchNode(body, properties);
        SearchResults nestedSearchResults = mDb1.search(nestedSearch.toString(),
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .setListFilterQueryLanguageEnabled(true)
                        .setVerbatimSearchEnabled(true)
                        .build());
        List<GenericDocument> nestedDocuments =
                convertSearchResultsToDocuments(nestedSearchResults);
        assertThat(nestedDocuments).containsExactly(fooFromEmail, fooToEmail);

        // Now check that the outer query returns the same documents as the nested query.
        SearchNode searchNode = new SearchNode(nestedSearch);
        SearchResults searchResults = mDb1.search(searchNode.toString(), new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .setListFilterQueryLanguageEnabled(true)
                .setVerbatimSearchEnabled(true)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).isEqualTo(nestedDocuments);
    }

    @Test
    public void testSemanticSearchNode_toString_allDefaults_returnsDocuments() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding1")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding2")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding3")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 = new GenericDocument.Builder<>("namespace", "id0", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .setPropertyEmbedding("embedding2", EMBEDDING_2)
                .setPropertyEmbedding("embedding3", EMBEDDING_3)
                .build();

        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .setPropertyEmbedding("embedding3", EMBEDDING_3)
                .build();

        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .build();

        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Email")
                .build();

        GenericDocument doc4 = new GenericDocument.Builder<>("namespace", "id4", "Email")
                .setPropertyEmbedding("embedding1",
                        new EmbeddingVector(new float[]{1, 2, 3}, "model_v2"))
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(doc0, doc1, doc2, doc3, doc4)
                                .build()
                )
        );
        // Matched embeddings for each doc are:
        // - document 0: -2 (embedding 1), 0 (embedding 2), 2 (embedding 3)
        // - document 1: -2 (embedding 1), 2 (embedding 3)
        // - document 2: -2 (embedding 1)
        // - document 3: (No embedding vectors)
        // - document 4: (No embedding vectors that share the model signature with searchEmbedding)
        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{1, -1, -1, 1, -1}, "model_v1");

        // Matched embeddings for each doc are:
        // - document 0: -2 (embedding 1), 0 (embedding 2), 2 (embedding 3)
        // - document 1: -2 (embedding 1), 2 (embedding 3)
        // - document 2: -2 (embedding 1)
        // - document 3:
        // - document 4:
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding)
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SemanticSearchNode semanticSearchNode = new SemanticSearchNode(0);

        SearchResults searchResults = mDb1.search(semanticSearchNode.toString(), searchSpec);
        List<GenericDocument> results = convertSearchResultsToDocuments(searchResults);

        assertThat(results).containsExactly(doc0, doc1, doc2);
    }

    @Test
    public void testSemanticSearchNode_toString_lowerBoundSet_returnsDocuments() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding1")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding2")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding3")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 = new GenericDocument.Builder<>("namespace", "id0", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .setPropertyEmbedding("embedding2", EMBEDDING_2)
                .setPropertyEmbedding("embedding3", EMBEDDING_3)
                .build();

        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .setPropertyEmbedding("embedding3", EMBEDDING_3)
                .build();

        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(doc0, doc1, doc2)
                                .build()
                )
        );
        // Matched embeddings for each doc are:
        // - document 0: -2 (embedding 1), 0 (embedding 2), 2 (embedding 3)
        // - document 1: -2 (embedding 1), 2 (embedding 3)
        // - document 2: -2 (embedding 1)
        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{1, -1, -1, 1, -1}, "model_v1");

        // Matched embeddings for each doc after filtering are:
        // - document 0: 0 (embedding 2), 2 (embedding 3)
        // - document 1: 2 (embedding 3)
        // - document 2:
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding)
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SemanticSearchNode semanticSearchNode = new SemanticSearchNode(0, -1);

        SearchResults searchResults = mDb1.search(semanticSearchNode.toString(), searchSpec);
        List<GenericDocument> results = convertSearchResultsToDocuments(searchResults);

        assertThat(results).containsExactly(doc0, doc1);
    }

    @Test
    public void testSemanticSearchNode_toString_boundsSet_returnsDocuments() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding1")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding2")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding3")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 = new GenericDocument.Builder<>("namespace", "id0", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .setPropertyEmbedding("embedding2", EMBEDDING_2)
                .setPropertyEmbedding("embedding3", EMBEDDING_3)
                .build();

        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .setPropertyEmbedding("embedding3", EMBEDDING_3)
                .build();

        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(doc0, doc1, doc2)
                                .build()
                )
        );
        // Matched embeddings for each doc are:
        // - document 0: -2 (embedding 1), 0 (embedding 2), 2 (embedding 3)
        // - document 1: -2 (embedding 1), 2 (embedding 3)
        // - document 2: -2 (embedding 1)
        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{1, -1, -1, 1, -1}, "model_v1");

        // Matched embeddings for each doc after filtering are:
        // - document 0: 0 (embedding 2)
        // - document 1:
        // - document 2:
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding)
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SemanticSearchNode semanticSearchNode = new SemanticSearchNode(0, -1, 0.1f);

        SearchResults searchResults = mDb1.search(semanticSearchNode.toString(), searchSpec);
        List<GenericDocument> results = convertSearchResultsToDocuments(searchResults);

        assertThat(results).containsExactly(doc0);
    }

    @Test
    public void testSemanticSearchNode_toString_noDefaults_returnsDocuments() throws Exception {
        assumeTrue(
                mDb1.getFeatures().isFeatureSupported(Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Email")
                .addProperty(new StringPropertyConfig.Builder("body")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding1")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding2")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding3")
                        .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc0 = new GenericDocument.Builder<>("namespace", "id0", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .setPropertyEmbedding("embedding2", EMBEDDING_2)
                .setPropertyEmbedding("embedding3", EMBEDDING_3)
                .build();

        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .setPropertyEmbedding("embedding3", EMBEDDING_3)
                .build();

        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Email")
                .setPropertyEmbedding("embedding1", EMBEDDING_1)
                .build();

        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Email")
                .setPropertyEmbedding("embedding1",
                        new EmbeddingVector(new float[]{1, -1, -1, 1, -1.05f}, "model_v1"))
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                        new PutDocumentsRequest.Builder()
                                .addGenericDocuments(doc0, doc1, doc2, doc3)
                                .build()
                )
        );
        // Matched embeddings for each doc are:
        // - document 0: 3 (embedding 1), 3 (embedding 2), 4.123106 (embedding 3)
        // - document 1: 3 (embedding 1), 4.123106 (embedding 3)
        // - document 2: 3 (embedding 1)
        // - document 3: 0.05 (embedding 3)
        EmbeddingVector searchEmbedding = new EmbeddingVector(
                new float[]{1, -1, -1, 1, -1}, "model_v1");

        // Matched embeddings for each doc are:
        // - document 0:
        // - document 1:
        // - document 2:
        // - document 3: 0.05
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setDefaultEmbeddingSearchMetricType(
                        SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_DOT_PRODUCT)
                .addEmbeddingParameters(searchEmbedding)
                .setListFilterQueryLanguageEnabled(true)
                .build();
        SemanticSearchNode semanticSearchNode = new SemanticSearchNode(0,
                -1, 0.1f, SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_EUCLIDEAN);

        SearchResults searchResults = mDb1.search(semanticSearchNode.toString(), searchSpec);
        List<GenericDocument> results = convertSearchResultsToDocuments(searchResults);

        assertThat(results).containsExactly(doc3);
    }
}
