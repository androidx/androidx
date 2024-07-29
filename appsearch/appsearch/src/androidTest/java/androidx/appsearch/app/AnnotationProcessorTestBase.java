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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import static androidx.appsearch.app.AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN;
import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.convertSearchResultsToDocuments;
import static androidx.appsearch.testutil.AppSearchTestUtils.doGet;
import static androidx.appsearch.testutil.AppSearchTestUtils.retrieveAllSearchResults;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.builtintypes.PotentialAction;
import androidx.appsearch.builtintypes.Thing;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.appsearch.util.DocumentIdUtil;
import androidx.test.core.app.ApplicationProvider;

import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AnnotationProcessorTestBase {
    private AppSearchSession mSession;
    private static final String TEST_PACKAGE_NAME =
            ApplicationProvider.getApplicationContext().getPackageName();
    private static final String DB_NAME_1 = "";

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName);

    @Before
    public void setUp() throws Exception {
        mSession = createSearchSessionAsync(DB_NAME_1).get();

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
        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .setForceOverride(true).build()).get();
    }

    @Document
    static class Card {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.CreationTimestampMillis
        long mCreationTimestampMillis;

        @Document.StringProperty
                (indexingType = INDEXING_TYPE_PREFIXES, tokenizerType = TOKENIZER_TYPE_PLAIN)
        String mString;        // 3a

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Card)) {
                return false;
            }
            Card otherCard = (Card) other;
            assertThat(otherCard.mId).isEqualTo(this.mId);
            return true;
        }
    }

    @Document
    static class Gift {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.CreationTimestampMillis
        long mCreationTimestampMillis;

        // Collections
        @Document.LongProperty
        Collection<Long> mCollectLong;         // 1a
        @Document.LongProperty
        Collection<Integer> mCollectInteger;   // 1a
        @Document.DoubleProperty
        Collection<Double> mCollectDouble;     // 1a
        @Document.DoubleProperty
        Collection<Float> mCollectFloat;       // 1a
        @Document.BooleanProperty
        Collection<Boolean> mCollectBoolean;   // 1a
        @Document.BytesProperty
        Collection<byte[]> mCollectByteArr;    // 1a
        @Document.StringProperty
        Collection<String> mCollectString;     // 1b
        @Document.DocumentProperty
        Collection<Card> mCollectCard;         // 1c

        // Arrays
        @Document.LongProperty
        Long[] mArrBoxLong;         // 2a
        @Document.LongProperty
        long[] mArrUnboxLong;       // 2b
        @Document.LongProperty
        Integer[] mArrBoxInteger;   // 2a
        @Document.LongProperty
        int[] mArrUnboxInt;         // 2a
        @Document.DoubleProperty
        Double[] mArrBoxDouble;     // 2a
        @Document.DoubleProperty
        double[] mArrUnboxDouble;   // 2b
        @Document.DoubleProperty
        Float[] mArrBoxFloat;       // 2a
        @Document.DoubleProperty
        float[] mArrUnboxFloat;     // 2a
        @Document.BooleanProperty
        Boolean[] mArrBoxBoolean;   // 2a
        @Document.BooleanProperty
        boolean[] mArrUnboxBoolean; // 2b
        @Document.BytesProperty
        byte[][] mArrUnboxByteArr;  // 2b
        @Document.StringProperty
        String[] mArrString;        // 2b
        @Document.DocumentProperty
        Card[] mArrCard;            // 2c

        // Single values
        @Document.StringProperty
        String mString;        // 3a
        @Document.LongProperty
        Long mBoxLong;         // 3a
        @Document.LongProperty
        long mUnboxLong;       // 3b
        @Document.LongProperty
        Integer mBoxInteger;   // 3a
        @Document.LongProperty
        int mUnboxInt;         // 3b
        @Document.DoubleProperty
        Double mBoxDouble;     // 3a
        @Document.DoubleProperty
        double mUnboxDouble;   // 3b
        @Document.DoubleProperty
        Float mBoxFloat;       // 3a
        @Document.DoubleProperty
        float mUnboxFloat;     // 3b
        @Document.BooleanProperty
        Boolean mBoxBoolean;   // 3a
        @Document.BooleanProperty
        boolean mUnboxBoolean; // 3b
        @Document.BytesProperty
        byte[] mUnboxByteArr;  // 3a
        @Document.DocumentProperty
        Card mCard;            // 3c

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Gift)) {
                return false;
            }
            Gift otherGift = (Gift) other;
            assertThat(otherGift.mNamespace).isEqualTo(this.mNamespace);
            assertThat(otherGift.mId).isEqualTo(this.mId);
            assertThat(otherGift.mArrBoxBoolean).isEqualTo(this.mArrBoxBoolean);
            assertThat(otherGift.mArrBoxDouble).isEqualTo(this.mArrBoxDouble);
            assertThat(otherGift.mArrBoxFloat).isEqualTo(this.mArrBoxFloat);
            assertThat(otherGift.mArrBoxLong).isEqualTo(this.mArrBoxLong);
            assertThat(otherGift.mArrBoxInteger).isEqualTo(this.mArrBoxInteger);
            assertThat(otherGift.mArrString).isEqualTo(this.mArrString);
            assertThat(otherGift.mArrUnboxBoolean).isEqualTo(this.mArrUnboxBoolean);
            assertThat(otherGift.mArrUnboxByteArr).isEqualTo(this.mArrUnboxByteArr);
            assertThat(otherGift.mArrUnboxDouble).isEqualTo(this.mArrUnboxDouble);
            assertThat(otherGift.mArrUnboxFloat).isEqualTo(this.mArrUnboxFloat);
            assertThat(otherGift.mArrUnboxLong).isEqualTo(this.mArrUnboxLong);
            assertThat(otherGift.mArrUnboxInt).isEqualTo(this.mArrUnboxInt);
            assertThat(otherGift.mArrCard).isEqualTo(this.mArrCard);

            assertThat(otherGift.mCollectLong).isEqualTo(this.mCollectLong);
            assertThat(otherGift.mCollectInteger).isEqualTo(this.mCollectInteger);
            assertThat(otherGift.mCollectBoolean).isEqualTo(this.mCollectBoolean);
            assertThat(otherGift.mCollectString).isEqualTo(this.mCollectString);
            assertThat(otherGift.mCollectDouble).isEqualTo(this.mCollectDouble);
            assertThat(otherGift.mCollectFloat).isEqualTo(this.mCollectFloat);
            assertThat(otherGift.mCollectCard).isEqualTo(this.mCollectCard);
            checkCollectByteArr(otherGift.mCollectByteArr, this.mCollectByteArr);

            assertThat(otherGift.mString).isEqualTo(this.mString);
            assertThat(otherGift.mBoxLong).isEqualTo(this.mBoxLong);
            assertThat(otherGift.mUnboxLong).isEqualTo(this.mUnboxLong);
            assertThat(otherGift.mBoxInteger).isEqualTo(this.mBoxInteger);
            assertThat(otherGift.mUnboxInt).isEqualTo(this.mUnboxInt);
            assertThat(otherGift.mBoxDouble).isEqualTo(this.mBoxDouble);
            assertThat(otherGift.mUnboxDouble).isEqualTo(this.mUnboxDouble);
            assertThat(otherGift.mBoxFloat).isEqualTo(this.mBoxFloat);
            assertThat(otherGift.mUnboxFloat).isEqualTo(this.mUnboxFloat);
            assertThat(otherGift.mBoxBoolean).isEqualTo(this.mBoxBoolean);
            assertThat(otherGift.mUnboxBoolean).isEqualTo(this.mUnboxBoolean);
            assertThat(otherGift.mUnboxByteArr).isEqualTo(this.mUnboxByteArr);
            assertThat(otherGift.mCard).isEqualTo(this.mCard);
            return true;
        }

        void checkCollectByteArr(Collection<byte[]> first, Collection<byte[]> second) {
            if (first == null && second == null) {
                return;
            }
            assertThat(first).isNotNull();
            assertThat(second).isNotNull();
            assertThat(first.toArray()).isEqualTo(second.toArray());
        }

        public static Gift createPopulatedGift() throws AppSearchException {
            Gift gift = new Gift();
            gift.mNamespace = "gift.namespace";
            gift.mId = "gift.id";

            gift.mArrBoxBoolean = new Boolean[]{true, false};
            gift.mArrBoxDouble = new Double[]{0.0, 1.0};
            gift.mArrBoxFloat = new Float[]{2.0F, 3.0F};
            gift.mArrBoxInteger = new Integer[]{4, 5};
            gift.mArrBoxLong = new Long[]{6L, 7L};
            gift.mArrString = new String[]{"cat", "dog"};
            gift.mArrUnboxBoolean = new boolean[]{false, true};
            gift.mArrUnboxByteArr = new byte[][]{{0, 1}, {2, 3}};
            gift.mArrUnboxDouble = new double[]{1.0, 0.0};
            gift.mArrUnboxFloat = new float[]{3.0f, 2.0f};
            gift.mArrUnboxInt = new int[]{5, 4};
            gift.mArrUnboxLong = new long[]{7, 6};

            Card card1 = new Card();
            card1.mNamespace = "card.namespace";
            card1.mId = "card.id1";
            Card card2 = new Card();
            card2.mNamespace = "card.namespace";
            card2.mId = "card.id2";
            gift.mArrCard = new Card[]{card2, card2};

            gift.mCollectLong = Arrays.asList(gift.mArrBoxLong);
            gift.mCollectInteger = Arrays.asList(gift.mArrBoxInteger);
            gift.mCollectBoolean = Arrays.asList(gift.mArrBoxBoolean);
            gift.mCollectString = Arrays.asList(gift.mArrString);
            gift.mCollectDouble = Arrays.asList(gift.mArrBoxDouble);
            gift.mCollectFloat = Arrays.asList(gift.mArrBoxFloat);
            gift.mCollectByteArr = Arrays.asList(gift.mArrUnboxByteArr);
            gift.mCollectCard = Arrays.asList(card2, card2);

            gift.mString = "String";
            gift.mBoxLong = 1L;
            gift.mUnboxLong = 2L;
            gift.mBoxInteger = 3;
            gift.mUnboxInt = 4;
            gift.mBoxDouble = 5.0;
            gift.mUnboxDouble = 6.0;
            gift.mBoxFloat = 7.0F;
            gift.mUnboxFloat = 8.0f;
            gift.mBoxBoolean = true;
            gift.mUnboxBoolean = false;
            gift.mUnboxByteArr = new byte[]{1, 2, 3};
            gift.mCard = card1;

            return gift;
        }
    }


    @Document
    static class CardAction {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;
        @Document.StringProperty(name = "cardRef",
                joinableValueType = JOINABLE_VALUE_TYPE_QUALIFIED_ID)
        String mCardReference; // 3a
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CardAction)) {
                return false;
            }
            CardAction otherGift = (CardAction) other;
            assertThat(otherGift.mNamespace).isEqualTo(this.mNamespace);
            assertThat(otherGift.mId).isEqualTo(this.mId);
            assertThat(otherGift.mCardReference).isEqualTo(this.mCardReference);
            return true;
        }
    }

    @Document
    static class LongDoc {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.CreationTimestampMillis
        Long mCreationTimestampMillis;

        @Document.Score
        Integer mScore;

        @Document.TtlMillis
        private Long mTtlMillis;

        public Long getTtlMillis() {
            return mTtlMillis;
        }

        public void setTtlMillis(Long ttlMillis) {
            mTtlMillis = ttlMillis;
        }

        @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
        String mString;

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof LongDoc)) {
                return false;
            }
            LongDoc otherDoc = (LongDoc) other;
            assertThat(otherDoc.mId).isEqualTo(this.mId);
            assertThat(otherDoc.mNamespace).isEqualTo(this.mNamespace);
            assertThat(otherDoc.mString).isEqualTo(this.mString);
            return true;
        }
    }

    @Test
    public void testAnnotationProcessor() throws Exception {
        //TODO(b/156296904) add test for int, float, GenericDocument, and class with
        // @Document annotation
        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                        .addDocumentClasses(Card.class, Gift.class)
                        .build())
                .get();

        // Create a Gift object and assign values.
        Gift inputDocument = Gift.createPopulatedGift();

        // Index the Gift document and query it.
        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder().addDocuments(inputDocument).build()));
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);

        // Convert GenericDocument to Gift and check values.
        Gift outputDocument = documents.get(0).toDocumentClass(Gift.class);
        assertThat(outputDocument).isEqualTo(inputDocument);
    }

    @Test
    public void testAnnotationProcessor_queryByType() throws Exception {
        mSession.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addDocumentClasses(Card.class, Gift.class)
                                .addSchemas(AppSearchEmail.SCHEMA).build())
                .get();

        // Create documents and index them
        Gift inputDocument1 = new Gift();
        inputDocument1.mNamespace = "gift.namespace";
        inputDocument1.mId = "gift.id1";
        Gift inputDocument2 = new Gift();
        inputDocument2.mNamespace = "gift.namespace";
        inputDocument2.mId = "gift.id2";
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder()
                        .addDocuments(inputDocument1, inputDocument2)
                        .addGenericDocuments(email1).build()));

        // Query the documents by it's schema type.
        SearchResults searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterSchemas("Gift", AppSearchEmail.SCHEMA_TYPE)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);

        // Query the documents by it's class.
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(Gift.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // Query the documents by schema type and class mix.
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .addFilterDocumentClasses(Gift.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);
    }

    @Test
    public void testAnnotationProcessor_simpleJoin() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        mSession.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addDocumentClasses(Card.class, CardAction.class)
                                .build())
                .get();

        // Index a Card and a Gift referencing it.
        Card peetsCard = new Card();
        peetsCard.mNamespace = "personal";
        peetsCard.mId = "peets1";
        CardAction bdayGift = new CardAction();
        bdayGift.mNamespace = "personal";
        bdayGift.mId = "2023-jan-31";
        bdayGift.mCardReference = DocumentIdUtil.createQualifiedId(TEST_PACKAGE_NAME, DB_NAME_1,
                GenericDocument.fromDocumentClass(peetsCard));
        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder().addDocuments(peetsCard, bdayGift).build()));

        // Retrieve cards with any given gifts.
        SearchSpec innerSpec = new SearchSpec.Builder()
                .addFilterDocumentClasses(CardAction.class)
                .build();
        JoinSpec js = new JoinSpec.Builder("cardRef")
                .setNestedSearch(/*nestedQuery*/ "", innerSpec)
                .build();
        SearchResults resultsIter = mSession.search(/*queryExpression*/ "",
                new SearchSpec.Builder()
                        .addFilterDocumentClasses(Card.class)
                        .setJoinSpec(js)
                        .build());

        // Verify that search results include card(s) joined with gift(s).
        List<SearchResult> results = resultsIter.getNextPageAsync().get();
        assertThat(results).hasSize(1);
        GenericDocument cardResultDoc = results.get(0).getGenericDocument();
        assertThat(cardResultDoc.getId()).isEqualTo(peetsCard.mId);
        List<SearchResult> joinedCardResults = results.get(0).getJoinedResults();
        assertThat(joinedCardResults).hasSize(1);
        GenericDocument giftResultDoc = joinedCardResults.get(0).getGenericDocument();
        assertThat(giftResultDoc.getId()).isEqualTo(bdayGift.mId);
    }

    @Test
    public void testAnnotationProcessor_onTAndBelow_joinNotSupported() throws Exception {
        assumeFalse(mSession.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        Exception e = assertThrows(UnsupportedOperationException.class,
                () -> mSession.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addDocumentClasses(Card.class, CardAction.class)
                                .build()));
    }

    @Test
    public void testAnnotation_unsetNumberClasses() throws Exception {
        // Test for a few kinds of non-primitive Document special properties. This shouldn't
        // cause a NPE.
        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                        .addDocumentClasses(LongDoc.class)
                        .build())
                .get();

        LongDoc doc = new LongDoc();
        doc.mId = "id";
        doc.mNamespace = "ns";
        // Don't set any special fields

        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder().addDocuments(doc).build()));
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder()
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);

        // Convert GenericDocument to Gift and check values.
        LongDoc outputDocument = documents.get(0).toDocumentClass(LongDoc.class);
        assertThat(outputDocument).isEqualTo(doc);
    }

    @Test
    public void testGenericDocumentConversion() throws Exception {
        Gift inGift = Gift.createPopulatedGift();
        GenericDocument genericDocument1 = GenericDocument.fromDocumentClass(inGift);
        GenericDocument genericDocument2 = GenericDocument.fromDocumentClass(inGift);
        Gift outGift = genericDocument2.toDocumentClass(Gift.class);

        assertThat(inGift).isNotSameInstanceAs(outGift);
        assertThat(inGift).isEqualTo(outGift);
        assertThat(genericDocument1).isNotSameInstanceAs(genericDocument2);
        assertThat(genericDocument1).isEqualTo(genericDocument2);
    }

    /**
     * Simple Document to demonstrate use of AutoValue and Document annotations, also nested
     */
    @Document
    @AutoValue
    public abstract static class SampleAutoValue {
        @AutoValue.CopyAnnotations
        @Document.Id
        abstract String id();

        @AutoValue.CopyAnnotations
        @Document.Namespace
        abstract String namespace();

        @AutoValue.CopyAnnotations
        @Document.StringProperty
        abstract String property();

        /** AutoValue constructor */
        public static SampleAutoValue create(String id, String namespace, String property) {
            return new AutoValue_AnnotationProcessorTestBase_SampleAutoValue(id,
                    namespace, property);
        }
    }


    /**
     * Simple Document to demonstrate use of inheritance with Document annotations
     */
    @Document
    static class Pineapple {
        @Document.Namespace String mNamespace;
        @Document.Id String mId;
    }

    @Document
    static class CoolPineapple extends Pineapple {
        @Document.StringProperty String mCool;

        @Document.CreationTimestampMillis long mCreationTimestampMillis;
    }

    @Test
    public void testGenericDocumentConversion_AutoValue() throws Exception {
        SampleAutoValue sampleAutoValue = SampleAutoValue.create("id", "namespace", "property");
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(sampleAutoValue);
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getSchemaType()).isEqualTo("SampleAutoValue");
        assertThat(genericDocument.getPropertyStringArray("property"))
                .asList().containsExactly("property");
    }

    @Test
    public void testGenericDocumentConversion_Superclass() throws Exception {
        CoolPineapple inputDoc = new CoolPineapple();
        inputDoc.mId = "id";
        inputDoc.mNamespace = "namespace";
        inputDoc.mCool = "very cool";
        inputDoc.mCreationTimestampMillis = System.currentTimeMillis();
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(inputDoc);
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getSchemaType()).isEqualTo("Pineapple");
        assertThat(genericDocument.getPropertyStringArray("cool")).asList()
                .containsExactly("very cool");

        //also try inserting and querying
        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(CoolPineapple.class).build()).get();

        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder().addDocuments(inputDoc).build()));

        // Query the documents by it's schema type.
        SearchResults searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterSchemas("Pineapple", AppSearchEmail.SCHEMA_TYPE)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(genericDocument);
    }

    @Test
    public void testActionDocumentPutAndRetrieveHelper() throws Exception {
        String namespace = "namespace";
        String id = "docId";
        String name = "View";
        String uri = "package://view";
        String description = "View action";
        long creationMillis = 300;

        GenericDocument genericDocAction = new GenericDocument.Builder<>(namespace, id,
                "builtin:PotentialAction")
                .setPropertyString("name", name)
                .setPropertyString("uri", uri)
                .setPropertyString("description", description)
                .setCreationTimestampMillis(creationMillis)
                .build();

        mSession.setSchemaAsync(
                new SetSchemaRequest.Builder().addDocumentClasses(PotentialAction.class)
                        .setForceOverride(true).build()).get();
        checkIsBatchResultSuccess(
                mSession.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(
                        genericDocAction).build()));

        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder(namespace)
                .addIds(id)
                .build();
        List<GenericDocument> outDocuments = doGet(mSession, request);
        assertThat(outDocuments).hasSize(1);
        PotentialAction potentialAction =
                outDocuments.get(0).toDocumentClass(PotentialAction.class);

        assertThat(potentialAction.getName()).isEqualTo(name);
        assertThat(potentialAction.getUri()).isEqualTo(uri);
        assertThat(potentialAction.getDescription()).isEqualTo(description);
    }

    @Test
    public void testDependentSchemas() throws Exception {
        // Test that makes sure if you call setSchema on Thing, PotentialAction also goes in.
        String namespace = "namespace";
        String name = "View";
        String uri = "package://view";
        String description = "View action";
        long creationMillis = 300;

        GenericDocument genericDocAction = new GenericDocument.Builder<>(namespace, "actionid",
                "builtin:PotentialAction")
                .setPropertyString("name", name)
                .setPropertyString("uri", uri)
                .setPropertyString("description", description)
                .setCreationTimestampMillis(creationMillis)
                .build();

        Thing thing = new Thing.Builder(namespace, "thingid")
                .setName(name)
                .setCreationTimestampMillis(creationMillis).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder().addDocumentClasses(Thing.class)
                .setForceOverride(true).build();

        // Both Thing and PotentialAction should be set as schemas
        assertThat(request.getSchemas()).hasSize(2);
        mSession.setSchemaAsync(request).get();

        assertThat(mSession.getSchemaAsync().get().getSchemas()).hasSize(2);

        // We should be able to put a PotentialAction as well as a Thing
        checkIsBatchResultSuccess(
                mSession.putAsync(new PutDocumentsRequest.Builder()
                        .addDocuments(thing)
                        .addGenericDocuments(genericDocAction)
                        .build()));

        GetByDocumentIdRequest getDocRequest = new GetByDocumentIdRequest.Builder(namespace)
                .addIds("thingid")
                .build();
        List<GenericDocument> outDocuments = doGet(mSession, getDocRequest);
        assertThat(outDocuments).hasSize(1);
        Thing potentialAction = outDocuments.get(0).toDocumentClass(Thing.class);

        assertThat(potentialAction.getNamespace()).isEqualTo(namespace);
        assertThat(potentialAction.getId()).isEqualTo("thingid");
        assertThat(potentialAction.getName()).isEqualTo(name);
        assertThat(potentialAction.getPotentialActions()).isEmpty();
    }

    @Document
    static class Outer {
        @Document.Id String mId;
        @Document.Namespace String mNamespace;
        @Document.DocumentProperty Middle mMiddle;
    }

    @Document
    static class Middle {
        @Document.Id String mId;
        @Document.Namespace String mNamespace;
        @Document.DocumentProperty Inner mInner;
    }

    @Document
    static class Inner {
        @Document.Id String mId;
        @Document.Namespace String mNamespace;
        @Document.StringProperty String mContents;
    }

    @Test
    public void testMultipleDependentSchemas() throws Exception {
        SetSchemaRequest request = new SetSchemaRequest.Builder().addDocumentClasses(Outer.class)
                .setForceOverride(true).build();

        // Outer, as well as Middle and Inner should be set.
        assertThat(request.getSchemas()).hasSize(3);
        mSession.setSchemaAsync(request).get();
        assertThat(mSession.getSchemaAsync().get().getSchemas()).hasSize(3);
    }

    @Document
    static class Root {
        @Document.Id String mId;
        @Document.Namespace String mNamespace;
    }

    @Document(name = "Email", parent = Root.class)
    static class Email extends Root {
        @Document.StringProperty String mSender;
    }

    @Document(name = "Message", parent = Root.class)
    static class Message extends Root {
        @Document.StringProperty String mContent;
    }

    // EmailMessage can choose any class to "extends" from, since Java's type relationship is
    // independent on AppSearch's. In this case, EmailMessage extends Root to avoid redefining
    // mId and mNamespace, but it still needs to specify mSender and mContent coming from
    // Email and Message.
    @Document(name = "EmailMessage", parent = {Email.class, Message.class})
    static class EmailMessage extends Root {
        @Document.StringProperty String mSender;
        @Document.StringProperty String mContent;
    }

    @Test
    public void testPolymorphism() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                // EmailMessage's dependencies should be automatically added.
                .addDocumentClasses(EmailMessage.class)
                // Add some other class
                .addDocumentClasses(Gift.class)
                .build()).get();

        // Create documents
        Root root = new Root();
        root.mNamespace = "namespace";
        root.mId = "id1";

        Email email = new Email();
        email.mNamespace = "namespace";
        email.mId = "id2";
        email.mSender = "test@test.com";

        Message message = new Message();
        message.mNamespace = "namespace";
        message.mId = "id3";
        message.mContent = "hello";

        EmailMessage emailMessage = new EmailMessage();
        emailMessage.mNamespace = "namespace";
        emailMessage.mId = "id4";
        emailMessage.mSender = "test@test.com";
        emailMessage.mContent = "hello";

        Gift gift = new Gift();
        gift.mNamespace = "namespace";
        gift.mId = "id5";

        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder()
                        .addDocuments(root, email, message, emailMessage, gift)
                        .build()));

        // Query for all documents
        SearchResults searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(5);

        // A query with a filter for the "Root" type should also include "Email", "Message" and
        // "EmailMessage".
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(Root.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(4);

        // A query with a filter for the "Email" type should also include "EmailMessage".
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(Email.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // A query with a filter for the "Message" type should also include "EmailMessage".
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(Message.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // Query with a filter for the "EmailMessage" type.
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(EmailMessage.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
    }

    @Document(name = "Artist", parent = {Root.class})
    static class Artist extends Root {
        @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) String mName;
        @Document.LongProperty(indexingType = INDEXING_TYPE_RANGE) long mAge;
        @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES) String mMostFamousWork;
        @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) String mNationality;
    }


    // Indexed properties for Media type: {"name", "description", "leadActor.name", "leadActor.age"}
    @Document(name = "Media", parent = {Root.class})
    static class Media extends Root {
        @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) String mName;
        @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES) String mDescription;
        @Document.DocumentProperty(indexableNestedPropertiesList = {"name", "age"})
        Artist mLeadActor;
    }

    // Indexed properties for Movie type: {"name", "description", "leadActor.name",
    // "leadActor.nationality"}
    // Movie does not index "leadActor.age" as the java class does not extend from Media
    @Document(name = "Movie", parent = {Media.class})
    static class Movie extends Root {
        @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) String mName;
        @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES) String mDescription;
        @Document.DocumentProperty(
                indexableNestedPropertiesList = {"name", "nationality"},
                inheritIndexableNestedPropertiesFromSuperclass = true)
        Artist mLeadActor;
    }

    // Indexed properties for Documentary type: {"name", "description", "leadActor.name",
    // "leadActor.age", "leadActor.mostFamousWork"}
    // Documentary extends Media, so it should index all nested properties in Media.leadActor, as
    // well as "leadActor.mostFamousWork"
    @Document(name = "Documentary", parent = {Media.class})
    static class Documentary extends Media {
        @Document.DocumentProperty(
                indexableNestedPropertiesList = {"mostFamousWork"},
                inheritIndexableNestedPropertiesFromSuperclass = true)
        Artist mLeadActor;
        @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) String mEvent;
    }

    // Indexed properties for DocumentaryMovie type: {"name", "description", "leadActor.name",
    // "leadActor.age", "leadActor.mostFamousWork"}
    // Documentary extends Media, so it should index all nested properties in Media.leadActor, but
    // should not index "leadActor.nationality" as it does not extend from Movie.
    @Document(name = "DocumentaryMovie", parent = {Documentary.class, Movie.class})
    static class DocumentaryMovie extends Documentary {
        @Document.DocumentProperty(
                indexableNestedPropertiesList = {},
                inheritIndexableNestedPropertiesFromSuperclass = true)
        Artist mLeadActor;
        @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS) String mDate;
    }

    @Test
    public void testIndexableNestedPropertiesList() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(
                Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                // Artist, Media, Movie and Documentary are added automatically as they are
                // DocumentaryMovie's dependencies.
                .addDocumentClasses(DocumentaryMovie.class)
                // Add an unrelated schema type
                .addDocumentClasses(Gift.class)
                .build()).get();


        // Create documents
        Artist actor = new Artist();
        actor.mNamespace = "namespace";
        actor.mId = "id1";
        actor.mName = "actor";
        actor.mAge = 30;
        actor.mMostFamousWork = "famousWork";
        actor.mNationality = "nationality";

        Media media = new Media();
        media.mNamespace = "namespace";
        media.mId = "id2";
        media.mName = "media";
        media.mDescription = "mediaDescription";
        media.mLeadActor = actor;

        Movie movie = new Movie();
        movie.mNamespace = "namespace";
        movie.mId = "id3";
        movie.mName = "movie";
        movie.mDescription = "movieDescription";
        movie.mLeadActor = actor;

        Documentary documentary = new Documentary();
        documentary.mNamespace = "namespace";
        documentary.mId = "id4";
        documentary.mName = "documentary";
        documentary.mDescription = "documentaryDescription";
        documentary.mLeadActor = actor;
        documentary.mEvent = "documentaryEvent";

        DocumentaryMovie documentaryMovie = new DocumentaryMovie();
        documentaryMovie.mNamespace = "namespace";
        documentaryMovie.mId = "id5";
        documentaryMovie.mName = "documentaryMovie";
        documentaryMovie.mDescription = "documentaryMovieDescription";
        documentaryMovie.mLeadActor = actor;
        documentaryMovie.mEvent = "documentaryMovieEvent";
        documentaryMovie.mDate = "2023-10-12";

        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder()
                        .addDocuments(actor, media, movie, documentary, documentaryMovie)
                        .build()));

        // Query for all documents
        SearchResults searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(5);


        // Query for "actor" should retrieve all documents.
        searchResults = mSession.search("actor",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(5);

        // Query for "leadActor.age == 30" should retrieve Media, Documentary and DocumentaryMovie
        searchResults = mSession.search("leadActor.age == 30",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);
        assertThat(documents.get(0).getId()).isEqualTo("id5");  // documentaryMovie
        assertThat(documents.get(1).getId()).isEqualTo("id4");  // documentary
        assertThat(documents.get(2).getId()).isEqualTo("id2");  // media

        // Query for "famous" should retrieve Actor, Documentary and DocumentaryMovie
        searchResults = mSession.search("famous",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);
        assertThat(documents.get(0).getId()).isEqualTo("id5");  // documentaryMovie
        assertThat(documents.get(1).getId()).isEqualTo("id4");  // documentary
        assertThat(documents.get(2).getId()).isEqualTo("id1");  // actor

        // Query for "nationality" should retrieve Actor and Movie.
        searchResults = mSession.search("nationality",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents.get(0).getId()).isEqualTo("id3");  // movie
        assertThat(documents.get(1).getId()).isEqualTo("id1");  // actor
    }

    @Test
    public void testIndexableNestedPropertiesList_notSupported() {
        assumeFalse(mSession.getFeatures().isFeatureSupported(
                Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        assertThrows(UnsupportedOperationException.class,
                () -> mSession.setSchemaAsync(
                        new SetSchemaRequest.Builder()
                                .addDocumentClasses(DocumentaryMovie.class)
                                .build()));
    }

    // A class that some properties are annotated via getters without backing fields.
    @Document
    static class FakeMessage {
        public int mSenderSetCount = 0;
        public String mContent;

        @Document.Id String mId;
        @Document.Namespace String mNamespace;

        @Document.StringProperty
        String getSender() {
            return "fake sender";
        }

        @Document.StringProperty
        String getContent() {
            return mContent;
        }

        @Document.StringProperty String mNote;

        void setSender(String sender) {
            if (sender.equals("fake sender")) {
                mSenderSetCount += 1;
            }
        }

        FakeMessage(String id, String namespace, String content) {
            mId = id;
            mNamespace = namespace;
            mContent = content;
        }
    }

    @Test
    public void testGenericDocumentConversion_AnnotatedGetter() throws Exception {
        // Create a document
        FakeMessage fakeMessage = new FakeMessage("id", "namespace", "fake content");
        fakeMessage.setSender("fake sender");
        fakeMessage.mNote = "fake note";

        // Test the conversion from FakeMessage to GenericDocument
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(fakeMessage);
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getSchemaType()).isEqualTo("FakeMessage");
        assertThat(genericDocument.getPropertyString("sender")).isEqualTo("fake sender");
        assertThat(genericDocument.getPropertyString("content")).isEqualTo("fake content");
        assertThat(genericDocument.getPropertyString("note")).isEqualTo("fake note");


        // Test the conversion from GenericDocument to FakeMessage
        FakeMessage newFakeMessage = genericDocument.toDocumentClass(FakeMessage.class);
        assertThat(newFakeMessage.mId).isEqualTo("id");
        assertThat(newFakeMessage.mNamespace).isEqualTo("namespace");
        assertThat(newFakeMessage.mSenderSetCount).isEqualTo(1);
        assertThat(newFakeMessage.getContent()).isEqualTo("fake content");
        assertThat(newFakeMessage.mNote).isEqualTo("fake note");
    }

    @Document
    interface InterfaceRoot {
        @Document.Id
        String getId();

        @Document.Namespace
        String getNamespace();

        @Document.CreationTimestampMillis
        long getCreationTimestamp();

        static InterfaceRoot create(String id, String namespace, long creationTimestamp) {
            return new InterfaceRootImpl(id, namespace, creationTimestamp);
        }
    }

    static class InterfaceRootImpl implements InterfaceRoot {
        String mId;
        String mNamespace;
        long mCreationTimestamp;

        InterfaceRootImpl(String id, String namespace, long creationTimestamp) {
            mId = id;
            mNamespace = namespace;
            mCreationTimestamp = creationTimestamp;
        }

        public String getId() {
            return mId;
        }

        public String getNamespace() {
            return mNamespace;
        }

        public long getCreationTimestamp() {
            return mCreationTimestamp;
        }
    }

    @Document(name = "DocumentCollection")
    static class DocumentCollection {
        @Document.Id String mId;
        @Document.Namespace String mNamespace;
        @Document.DocumentProperty InterfaceRoot[] mCollection;
    }

    @Document(name = "Place", parent = InterfaceRoot.class)
    interface Place extends InterfaceRoot {
        @Document.StringProperty
        String getLocation();

        static Place createPlace(String id, String namespace, long creationTimestamp,
                String location) {
            return new PlaceImpl(id, namespace, creationTimestamp, location);
        }
    }

    static class PlaceImpl implements Place {
        String mId;
        String mNamespace;
        String mLocation;
        long mCreationTimestamp;

        PlaceImpl(String id, String namespace, long creationTimestamp, String location) {
            mId = id;
            mNamespace = namespace;
            mCreationTimestamp = creationTimestamp;
            mLocation = location;
        }

        public String getId() {
            return mId;
        }

        public String getNamespace() {
            return mNamespace;
        }

        public long getCreationTimestamp() {
            return mCreationTimestamp;
        }

        public String getLocation() {
            return mLocation;
        }
    }

    @Document(name = "Organization", parent = InterfaceRoot.class)
    interface Organization extends InterfaceRoot {
        @Document.StringProperty
        String getOrganizationDescription();

        @Document.BuilderProducer
        static OrganizationBuilder getBuilder() {
            return new OrganizationBuilder();
        }
    }

    static class OrganizationBuilder {
        String mId;
        String mNamespace;
        long mCreationTimestamp;
        String mOrganizationDescription;

        public Organization build() {
            return new OrganizationImpl(mId, mNamespace, mCreationTimestamp,
                    mOrganizationDescription);
        }

        public OrganizationBuilder setId(String id) {
            mId = id;
            return this;
        }

        public OrganizationBuilder setNamespace(String namespace) {
            mNamespace = namespace;
            return this;
        }

        public OrganizationBuilder setCreationTimestamp(long creationTimestamp) {
            mCreationTimestamp = creationTimestamp;
            return this;
        }

        public OrganizationBuilder setOrganizationDescription(String organizationDescription) {
            mOrganizationDescription = organizationDescription;
            return this;
        }
    }

    static class OrganizationImpl implements Organization {
        String mId;
        String mNamespace;
        long mCreationTimestamp;
        String mOrganizationDescription;

        OrganizationImpl(String id, String namespace, long creationTimestamp,
                String organizationDescription) {
            mId = id;
            mNamespace = namespace;
            mCreationTimestamp = creationTimestamp;
            mOrganizationDescription = organizationDescription;
        }

        public String getId() {
            return mId;
        }

        public String getNamespace() {
            return mNamespace;
        }

        public long getCreationTimestamp() {
            return mCreationTimestamp;
        }

        public String getOrganizationDescription() {
            return mOrganizationDescription;
        }
    }

    @Document(name = "Business", parent = {Place.class, Organization.class})
    interface Business extends Place, Organization {
        @Document.StringProperty
        String getBusinessName();

        static Business createBusiness(String id, String namespace, long creationTimestamp,
                String location, String organizationDescription, String businessName) {
            return new BusinessImpl(id, namespace, creationTimestamp, location,
                    organizationDescription, businessName);
        }
    }

    // We have to annotate this class with @Document to generate a factory for it. Otherwise there
    // will be an ambiguity on finding the factory class.
    @Document(name = "BusinessImpl", parent = Business.class)
    static class BusinessImpl extends PlaceImpl implements Business {
        String mOrganizationDescription;
        String mBusinessName;

        BusinessImpl(String id, String namespace, long creationTimestamp, String location,
                String organizationDescription, String businessName) {
            super(id, namespace, creationTimestamp, location);
            mOrganizationDescription = organizationDescription;
            mBusinessName = businessName;
        }

        public String getOrganizationDescription() {
            return mOrganizationDescription;
        }

        public String getBusinessName() {
            return mBusinessName;
        }
    }

    @Test
    public void testGenericDocumentConversion_AnnotatedInterface() throws Exception {
        // Create Place document
        Place place = Place.createPlace("id", "namespace", 1000, "loc");

        // Test the conversion from Place to GenericDocument
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(place);
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(1000);
        assertThat(genericDocument.getSchemaType()).isEqualTo("Place");
        assertThat(genericDocument.getPropertyString("location")).isEqualTo("loc");

        // Test the conversion from GenericDocument to Place
        Place newPlace = genericDocument.toDocumentClass(Place.class);
        assertThat(newPlace.getId()).isEqualTo("id");
        assertThat(newPlace.getNamespace()).isEqualTo("namespace");
        assertThat(newPlace.getCreationTimestamp()).isEqualTo(1000);
        assertThat(newPlace.getLocation()).isEqualTo("loc");


        // Create Business document
        Business business = Business.createBusiness("id", "namespace", 2000, "business_loc",
                "business_dec", "business_name");

        // Test the conversion from Business to GenericDocument
        genericDocument = GenericDocument.fromDocumentClass(business);
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(2000);
        // The schema type of business has to be "BusinessImpl" because
        // GenericDocument.fromDocumentClass is looking up factory classes by runtime types. It will
        // start finding a factory class for "BusinessImpl" first. If the class is not found, it
        // will continue to search the unique parent type (class and interface in Java). In this
        // case, BusinessImpl has more than one parent, so BusinessImpl must also be @Document
        // annotated. Otherwise, no factor class will be found.
        assertThat(genericDocument.getSchemaType()).isEqualTo("BusinessImpl");
        assertThat(genericDocument.getPropertyString("location")).isEqualTo("business_loc");
        assertThat(genericDocument.getPropertyString("organizationDescription")).isEqualTo(
                "business_dec");
        assertThat(genericDocument.getPropertyString("businessName")).isEqualTo("business_name");


        // Test the conversion from GenericDocument to Business
        Business newBusiness = genericDocument.toDocumentClass(Business.class);
        assertThat(newBusiness.getId()).isEqualTo("id");
        assertThat(newBusiness.getNamespace()).isEqualTo("namespace");
        assertThat(newBusiness.getCreationTimestamp()).isEqualTo(2000);
        assertThat(newBusiness.getLocation()).isEqualTo("business_loc");
        assertThat(newBusiness.getOrganizationDescription()).isEqualTo("business_dec");
        assertThat(newBusiness.getBusinessName()).isEqualTo("business_name");
    }

    @Test
    public void testGenericDocumentConversion_AnnotatedBuilder() throws Exception {
        // Create Organization document
        Organization organization = Organization.getBuilder()
                .setId("id")
                .setNamespace("namespace")
                .setCreationTimestamp(3000)
                .setOrganizationDescription("organization_dec")
                .build();

        // Test the conversion from Organization to GenericDocument
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(organization);
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(3000);
        assertThat(genericDocument.getSchemaType()).isEqualTo("Organization");
        assertThat(genericDocument.getPropertyString("organizationDescription")).isEqualTo(
                "organization_dec");

        // Test the conversion from GenericDocument to Organization
        Organization newOrganization = genericDocument.toDocumentClass(Organization.class);
        assertThat(newOrganization.getId()).isEqualTo("id");
        assertThat(newOrganization.getNamespace()).isEqualTo("namespace");
        assertThat(newOrganization.getCreationTimestamp()).isEqualTo(3000);
        assertThat(newOrganization.getOrganizationDescription()).isEqualTo("organization_dec");
    }

    @Document(name = "Person", parent = InterfaceRoot.class)
    interface Person extends InterfaceRoot {
        @Document.StringProperty
        String getFirstName();

        @Document.StringProperty
        String getLastName();

        @Document.BuilderProducer
        class Builder {
            String mId;
            String mNamespace;
            long mCreationTimestamp;
            String mFirstName;
            String mLastName;

            Builder(String id, String namespace) {
                mId = id;
                mNamespace = namespace;
            }

            public Person build() {
                return new PersonImpl(mId, mNamespace, mCreationTimestamp, mFirstName, mLastName);
            }

            public Builder setCreationTimestamp(long creationTimestamp) {
                mCreationTimestamp = creationTimestamp;
                return this;
            }

            public Builder setFirstName(String firstName) {
                mFirstName = firstName;
                return this;
            }

            // Void return type should work.
            public void setLastName(String lastName) {
                mLastName = lastName;
            }
        }
    }

    static class PersonImpl implements Person {
        String mId;
        String mNamespace;
        long mCreationTimestamp;
        String mFirstName;
        String mLastName;

        PersonImpl(String id, String namespace, long creationTimestamp, String firstName,
                String lastName) {
            mId = id;
            mNamespace = namespace;
            mCreationTimestamp = creationTimestamp;
            mFirstName = firstName;
            mLastName = lastName;
        }

        public String getId() {
            return mId;
        }

        public String getNamespace() {
            return mNamespace;
        }

        public long getCreationTimestamp() {
            return mCreationTimestamp;
        }

        public String getFirstName() {
            return mFirstName;
        }

        public String getLastName() {
            return mLastName;
        }
    }

    @Document
    static class Product {
        @NonNull
        @Document.Namespace
        String mNamespace;

        @NonNull
        @Document.Id
        String mId;

        @Nullable
        @Document.LongProperty(serializer = PricePointAsOrdinalSerializer.class)
        PricePoint mPricePoint;

        @Nullable
        @Document.LongProperty(serializer = PricePointAsOrdinalSerializer.class)
        PricePoint[] mPricePointArr;

        @Nullable
        @Document.LongProperty(serializer = PricePointAsOrdinalSerializer.class)
        List<PricePoint> mPricePointList;

        @Nullable
        @Document.StringProperty(serializer = UrlAsStringSerializer.class)
        URL mUrl;

        @Nullable
        @Document.StringProperty(serializer = UrlAsStringSerializer.class)
        URL[] mUrlArr;

        @Nullable
        @Document.StringProperty(serializer = UrlAsStringSerializer.class)
        List<URL> mUrlList;

        // Such naming should not have any collisions with the local vars in the generated code.
        @Document.BooleanProperty
        boolean serializer;

        Product(@NonNull String namespace, @NonNull String id) {
            mId = id;
            mNamespace = namespace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Product product = (Product) o;
            return Objects.equals(mId, product.mId)
                    && Objects.equals(mNamespace, product.mNamespace)
                    && mPricePoint == product.mPricePoint
                    && Arrays.equals(mPricePointArr, product.mPricePointArr)
                    && Objects.equals(mPricePointList, product.mPricePointList)
                    && Objects.equals(mUrl, product.mUrl)
                    && Arrays.equals(mUrlArr, product.mUrlArr)
                    && Objects.equals(mUrlList, product.mUrlList)
                    && serializer == product.serializer;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    mId,
                    mNamespace,
                    mPricePoint,
                    Arrays.hashCode(mPricePointArr),
                    mPricePointList,
                    mUrl,
                    Arrays.hashCode(mUrlArr),
                    mUrlList,
                    serializer);
        }

        enum PricePoint { LOW, MID, HIGH }
        static class PricePointAsOrdinalSerializer implements LongSerializer<PricePoint> {
            @Override
            public long serialize(@NonNull PricePoint pricePoint) {
                return pricePoint.ordinal();
            }

            @Nullable
            @Override
            public PricePoint deserialize(long value) {
                for (PricePoint pricePoint : PricePoint.values()) {
                    if (pricePoint.ordinal() == value) {
                        return pricePoint;
                    }
                }
                return null;
            }
        }

    }

    @Document
    static class DocumentWithPropertyCalledSerializer {
        @Document.Namespace
        String mNamespace;
        @Document.Id
        String mId;

        // Such naming should not cause any issues
        @Document.StringProperty(serializer = UrlAsStringSerializer.class)
        URL serializer;
    }

    static class UrlAsStringSerializer implements StringSerializer<URL> {
        @NonNull
        @Override
        public String serialize(@NonNull URL url) {
            return url.toString();
        }

        @Nullable
        @Override
        public URL deserialize(@NonNull String string) {
            try {
                return new URL(string);
            } catch (MalformedURLException ignore) {
                return null;
            }
        }
    }

    @Test
    public void testGenericDocumentConversion_BuilderConstructor() throws Exception {
        // Create Person document
        Person.Builder personBuilder = new Person.Builder("id", "namespace")
                .setCreationTimestamp(3000)
                .setFirstName("first");
        personBuilder.setLastName("last");
        Person person = personBuilder.build();

        // Test the conversion from person to GenericDocument
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(person);
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(3000);
        assertThat(genericDocument.getSchemaType()).isEqualTo("Person");
        assertThat(genericDocument.getPropertyString("firstName")).isEqualTo("first");
        assertThat(genericDocument.getPropertyString("lastName")).isEqualTo("last");

        // Test the conversion from GenericDocument to person
        Person newPerson = genericDocument.toDocumentClass(Person.class);
        assertThat(newPerson.getId()).isEqualTo("id");
        assertThat(newPerson.getNamespace()).isEqualTo("namespace");
        assertThat(newPerson.getCreationTimestamp()).isEqualTo(3000);
        assertThat(newPerson.getFirstName()).isEqualTo("first");
        assertThat(newPerson.getLastName()).isEqualTo("last");
    }

    @Test
    public void testSerializerSupport() throws Exception {
        Product product = new Product("ns", "id");
        product.mPricePoint = Product.PricePoint.HIGH;
        product.mPricePointArr =
                new Product.PricePoint[]{Product.PricePoint.MID, Product.PricePoint.LOW};
        product.mPricePointList = List.of(Product.PricePoint.HIGH, Product.PricePoint.MID);
        product.mUrl = new URL("https://google.com");
        product.mUrlArr = new URL[]{
                new URL("https://android.com"), new URL("http://gmail.com")};
        product.mUrlList = List.of(
                new URL("https://schema.org"), new URL("https://bard.google.com"));

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(product);
        assertThat(genericDocument.getPropertyLong("pricePoint"))
                .isEqualTo((long) Product.PricePoint.HIGH.ordinal());
        assertThat(genericDocument.getPropertyLongArray("pricePointArr"))
                .asList()
                .containsExactly(
                        (long) Product.PricePoint.MID.ordinal(),
                        (long) Product.PricePoint.LOW.ordinal());
        assertThat(genericDocument.getPropertyLongArray("pricePointList"))
                .asList()
                .containsExactly(
                        (long) Product.PricePoint.HIGH.ordinal(),
                        (long) Product.PricePoint.MID.ordinal());
        assertThat(genericDocument.getPropertyString("url")).isEqualTo("https://google.com");
        assertThat(genericDocument.getPropertyStringArray("urlArr"))
                .asList()
                .containsExactly("https://android.com", "http://gmail.com");
        assertThat(genericDocument.getPropertyStringArray("urlList"))
                .asList()
                .containsExactly("https://schema.org", "https://bard.google.com");

        Product productBack = genericDocument.toDocumentClass(Product.class);
        assertThat(productBack).isEqualTo(product);
    }

    @Test
    public void testSerializerOmitsPropertyUponFailedDeserialization() throws Exception {
        long invalidPricePoint = 999;
        String invalidUrl = "not a valid url";
        GenericDocument genericDocument =
                new GenericDocument.Builder<>("ns", "id", /* schemaType= */"Product")
                        .setPropertyLong("pricePoint", invalidPricePoint)
                        .setPropertyLong("pricePointArr",
                                Product.PricePoint.MID.ordinal(),
                                invalidPricePoint,
                                Product.PricePoint.LOW.ordinal())
                        .setPropertyLong("pricePointList",
                                Product.PricePoint.HIGH.ordinal(),
                                invalidPricePoint,
                                Product.PricePoint.MID.ordinal())
                        .setPropertyString("url", invalidUrl)
                        .setPropertyString("urlArr",
                                "https://android.com", invalidUrl, "http://gmail.com")
                        .setPropertyString("urlList",
                                "https://schema.org", invalidUrl, "https://bard.google.com")
                        .build();

        Product product = genericDocument.toDocumentClass(Product.class);
        assertThat(product).isEqualTo(new Product("ns", "id"));
    }

    @Test
    public void testSerializerSupportWhenFieldIsCalledSerializer() throws Exception {
        DocumentWithPropertyCalledSerializer entity = new DocumentWithPropertyCalledSerializer();
        entity.mNamespace = "ns";
        entity.mId = "id";
        entity.serializer = new URL("https://google.com");

        GenericDocument genericDoc = GenericDocument.fromDocumentClass(entity);
        assertThat(genericDoc.getNamespace()).isEqualTo("ns");
        assertThat(genericDoc.getId()).isEqualTo("id");
        assertThat(genericDoc.getPropertyString("serializer")).isEqualTo("https://google.com");
    }

    @Test
    public void testPolymorphismForInterface() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                // Adding BusinessImpl should be enough to add all the dependency classes.
                .addDocumentClasses(BusinessImpl.class)
                // Add some other class
                .addDocumentClasses(Gift.class)
                .build()).get();

        // Create documents
        InterfaceRoot root = InterfaceRoot.create("id0", "namespace", 1000);
        GenericDocument rootGeneric = GenericDocument.fromDocumentClass(root);
        assertThat(rootGeneric.getId()).isEqualTo("id0");
        assertThat(rootGeneric.getNamespace()).isEqualTo("namespace");
        assertThat(rootGeneric.getCreationTimestampMillis()).isEqualTo(1000);
        assertThat(rootGeneric.getSchemaType()).isEqualTo("InterfaceRoot");

        Place place = Place.createPlace("id1", "namespace", 2000, "place_loc");
        GenericDocument placeGeneric =
                new GenericDocument.Builder<>(GenericDocument.fromDocumentClass(place))
                        .setParentTypes(Collections.singletonList("InterfaceRoot")).build();
        assertThat(placeGeneric.getId()).isEqualTo("id1");
        assertThat(placeGeneric.getNamespace()).isEqualTo("namespace");
        assertThat(placeGeneric.getCreationTimestampMillis()).isEqualTo(2000);
        assertThat(placeGeneric.getSchemaType()).isEqualTo("Place");
        assertThat(placeGeneric.getPropertyString("location")).isEqualTo("place_loc");

        Organization organization = Organization.getBuilder()
                .setId("id2")
                .setNamespace("namespace")
                .setCreationTimestamp(3000)
                .setOrganizationDescription("organization_dec")
                .build();
        GenericDocument organizationGeneric =
                new GenericDocument.Builder<>(GenericDocument.fromDocumentClass(organization))
                        .setParentTypes(Collections.singletonList("InterfaceRoot")).build();
        assertThat(organizationGeneric.getId()).isEqualTo("id2");
        assertThat(organizationGeneric.getNamespace()).isEqualTo("namespace");
        assertThat(organizationGeneric.getCreationTimestampMillis()).isEqualTo(3000);
        assertThat(organizationGeneric.getSchemaType()).isEqualTo("Organization");
        assertThat(organizationGeneric.getPropertyString("organizationDescription")).isEqualTo(
                "organization_dec");

        Business business = Business.createBusiness("id3", "namespace", 4000, "business_loc",
                "business_dec", "business_name");
        // At runtime, business is type of BusinessImpl. As a result, the list of parent types
        // for it should contain Business.
        GenericDocument businessGeneric = new GenericDocument.Builder<>(
                GenericDocument.fromDocumentClass(business)).setParentTypes(new ArrayList<>(
                Arrays.asList("Business", "Place", "Organization", "InterfaceRoot"))).build();
        assertThat(businessGeneric.getId()).isEqualTo("id3");
        assertThat(businessGeneric.getNamespace()).isEqualTo("namespace");
        assertThat(businessGeneric.getCreationTimestampMillis()).isEqualTo(4000);
        // The type of business should be BusinessImpl because it's annotated with @Document.
        assertThat(businessGeneric.getSchemaType()).isEqualTo("BusinessImpl");
        assertThat(businessGeneric.getPropertyString("location")).isEqualTo(
                "business_loc");
        assertThat(businessGeneric.getPropertyString("organizationDescription")).isEqualTo(
                "business_dec");

        Gift gift = new Gift();
        gift.mNamespace = "namespace";
        gift.mId = "id4";
        gift.mCreationTimestampMillis = 5000;
        GenericDocument giftGeneric = GenericDocument.fromDocumentClass(gift);

        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder()
                        .addDocuments(root, place, organization, business, gift)
                        .build()));

        // Query for all documents
        SearchResults searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(rootGeneric, placeGeneric, organizationGeneric,
                businessGeneric, giftGeneric);

        // A query with a filter for the "InterfaceRoot" type should also include "Place",
        // "Organization" and "Business".
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(InterfaceRoot.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(rootGeneric, placeGeneric, organizationGeneric,
                businessGeneric);

        // A query with a filter for the "Place" type should also include "Business".
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(Place.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(placeGeneric, businessGeneric);

        // A query with a filter for the "Organization" type should also include "Business".
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(Organization.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(organizationGeneric, businessGeneric);

        // Query with a filter for the "Business" type.
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(Business.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).containsExactly(businessGeneric);
    }

    @Document(name = "Event", parent = InterfaceRoot.class)
    interface Event extends InterfaceRoot {
        @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS)
        String getName();

        @Document.DocumentProperty(indexableNestedPropertiesList = {"name", "age"})
        Artist getPerformer();

        @Document.BuilderProducer
        class Builder {
            String mId;
            String mNamespace;
            long mCreationTimestamp;
            String mName;
            Artist mPerformer;

            Builder(String id, String namespace) {
                mId = id;
                mNamespace = namespace;
            }

            public Event build() {
                return new EventImpl(mId, mNamespace, mCreationTimestamp, mName, mPerformer);
            }

            public Event.Builder setCreationTimestamp(long creationTimestamp) {
                mCreationTimestamp = creationTimestamp;
                return this;
            }

            public Event.Builder setName(String name) {
                mName = name;
                return this;
            }

            public Event.Builder setPerformer(Artist performer) {
                mPerformer = performer;
                return this;
            }
        }
    }

    @Document(name = "Charity", parent = InterfaceRoot.class)
    interface Charity extends InterfaceRoot {
        @Document.StringProperty(indexingType = INDEXING_TYPE_EXACT_TERMS)
        String getName();

        @Document.DocumentProperty(indexableNestedPropertiesList = {"name", "nationality"})
        Artist getPerformer();

        @Document.BuilderProducer
        class Builder {
            String mId;
            String mNamespace;
            long mCreationTimestamp;
            String mName;
            Artist mPerformer;

            Builder(String id, String namespace) {
                mId = id;
                mNamespace = namespace;
            }

            public Charity build() {
                return new CharityImpl(mId, mNamespace, mCreationTimestamp, mName, mPerformer);
            }

            public Charity.Builder setCreationTimestamp(long creationTimestamp) {
                mCreationTimestamp = creationTimestamp;
                return this;
            }

            public Charity.Builder setName(String name) {
                mName = name;
                return this;
            }

            public Charity.Builder setPerformer(Artist performer) {
                mPerformer = performer;
                return this;
            }
        }
    }

    @Document(name = "CharityEvent", parent = {Event.class, Charity.class})
    interface CharityEvent extends Event, Charity {
        @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
        String getDescription();

        @Document.DocumentProperty(inheritIndexableNestedPropertiesFromSuperclass = true)
        Artist getPerformer();

        @Document.BuilderProducer
        class Builder {
            String mId;
            String mNamespace;
            long mCreationTimestamp;
            String mName;
            String mDescription;
            Artist mPerformer;

            Builder(String id, String namespace) {
                mId = id;
                mNamespace = namespace;
            }

            public CharityEvent build() {
                return new CharityEventImpl(mId, mNamespace, mCreationTimestamp, mName,
                        mDescription, mPerformer);
            }

            public CharityEvent.Builder setCreationTimestamp(long creationTimestamp) {
                mCreationTimestamp = creationTimestamp;
                return this;
            }

            public CharityEvent.Builder setName(String name) {
                mName = name;
                return this;
            }

            public CharityEvent.Builder setDescription(String description) {
                mDescription = description;
                return this;
            }

            public CharityEvent.Builder setPerformer(Artist performer) {
                mPerformer = performer;
                return this;
            }
        }
    }


    static class EventImpl implements Event {
        String mId;
        String mNamespace;
        long mCreationTimestamp;
        String mName;
        Artist mPerformer;

        EventImpl(String id, String namespace, long creationTimestamp, String name,
                Artist performer) {
            mId = id;
            mNamespace = namespace;
            mCreationTimestamp = creationTimestamp;
            mName = name;
            mPerformer = performer;
        }

        @Override
        public String getId() {
            return mId;
        }

        @Override
        public String getNamespace() {
            return mNamespace;
        }

        @Override
        public long getCreationTimestamp() {
            return mCreationTimestamp;
        }

        @Override
        public String getName() {
            return mName;
        }

        @Override
        public Artist getPerformer() {
            return mPerformer;
        }
    }

    static class CharityImpl implements Charity {
        String mId;
        String mNamespace;
        long mCreationTimestamp;
        String mName;
        Artist mPerformer;

        CharityImpl(String id, String namespace, long creationTimestamp, String name,
                Artist performer) {
            mId = id;
            mNamespace = namespace;
            mCreationTimestamp = creationTimestamp;
            mName = name;
            mPerformer = performer;
        }

        @Override
        public String getId() {
            return mId;
        }

        @Override
        public String getNamespace() {
            return mNamespace;
        }

        @Override
        public long getCreationTimestamp() {
            return mCreationTimestamp;
        }

        @Override
        public String getName() {
            return mName;
        }

        @Override
        public Artist getPerformer() {
            return mPerformer;
        }
    }

    // CharityEventImpl.performer's nested properties: {performer.name, performer.age,
    // performer.nationality}
    static class CharityEventImpl implements CharityEvent {
        String mId;
        String mNamespace;
        long mCreationTimestamp;
        String mName;
        String mDescription;
        Artist mPerformer;

        CharityEventImpl(String id, String namespace, long creationTimestamp, String name,
                String description, Artist performer) {
            mId = id;
            mNamespace = namespace;
            mCreationTimestamp = creationTimestamp;
            mName = name;
            mDescription = description;
            mPerformer = performer;
        }

        public String getId() {
            return mId;
        }

        public String getNamespace() {
            return mNamespace;
        }

        public long getCreationTimestamp() {
            return mCreationTimestamp;
        }

        public String getName() {
            return mName;
        }

        public Artist getPerformer() {
            return mPerformer;
        }

        public String getDescription() {
            return null;
        }
    }

    @Test
    public void testIndexableNestedPropertiesListForInterface() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(
                Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(CharityEventImpl.class)
                // Add an unrelated schema type
                .addDocumentClasses(Gift.class)
                .build()).get();


        // Create documents
        Artist performer = new Artist();
        performer.mNamespace = "namespace";
        performer.mId = "id1";
        performer.mName = "performer";
        performer.mAge = 30;
        performer.mMostFamousWork = "famousWork";
        performer.mNationality = "nationality";

        CharityEventImpl charityEvent = new CharityEventImpl("id2", "namespace", 0, "charityEvent",
                "charityEventDescription", performer);

        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder()
                        .addDocuments(performer, charityEvent)
                        .build()));

        // Query for all documents
        SearchResults searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);


        // Query for "performer" should retrieve all documents.
        searchResults = mSession.search("performer",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // Query for "performer.age == 30" should retrieve CharityEvent
        searchResults = mSession.search("performer.age == 30",
                new SearchSpec.Builder()
                        .setNumericSearchEnabled(true)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getId()).isEqualTo("id2");  // charityEvent

        // Query for "famous" should retrieve Performer
        searchResults = mSession.search("famous",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getId()).isEqualTo("id1");  // performer

        // Query for "nationality" should retrieve Performer and CharityEvent.
        searchResults = mSession.search("nationality",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);
        assertThat(documents.get(0).getId()).isEqualTo("id2");  // charityEvent
        assertThat(documents.get(1).getId()).isEqualTo("id1");  // performer
    }

    @Test
    public void testAppSearchDocumentClassMap() throws Exception {
        // Before this test, AppSearch's annotation processor has already generated the maps for
        // each module at compile time for all document classes available in the current JVM
        // environment.
        Map<String, List<String>> expectedDocumentMap = new HashMap<>();
        // The following classes come from androidx.appsearch.builtintypes.
        expectedDocumentMap.put("builtin:StopwatchLap",
                Arrays.asList("androidx.appsearch.builtintypes.StopwatchLap"));
        expectedDocumentMap.put("builtin:Thing",
                Arrays.asList("androidx.appsearch.builtintypes.Thing"));
        expectedDocumentMap.put("builtin:ContactPoint",
                Arrays.asList("androidx.appsearch.builtintypes.ContactPoint"));
        expectedDocumentMap.put("builtin:Person",
                Arrays.asList("androidx.appsearch.builtintypes.Person"));
        expectedDocumentMap.put("builtin:AlarmInstance",
                Arrays.asList("androidx.appsearch.builtintypes.AlarmInstance"));
        expectedDocumentMap.put("Keyword",
                Arrays.asList("androidx.appsearch.builtintypes.properties.Keyword"));
        expectedDocumentMap.put("builtin:Alarm",
                Arrays.asList("androidx.appsearch.builtintypes.Alarm"));
        expectedDocumentMap.put("builtin:Timer",
                Arrays.asList("androidx.appsearch.builtintypes.Timer"));
        expectedDocumentMap.put("builtin:ImageObject",
                Arrays.asList("androidx.appsearch.builtintypes.ImageObject"));
        expectedDocumentMap.put("builtin:PotentialAction",
                Arrays.asList("androidx.appsearch.builtintypes.PotentialAction"));
        expectedDocumentMap.put("builtin:Stopwatch",
                Arrays.asList("androidx.appsearch.builtintypes.Stopwatch"));
        // The following classes come from all test files in androidx.appsearch.cts and
        // androidx.appsearch.app.
        expectedDocumentMap.put("Artist",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Artist"));
        expectedDocumentMap.put("Organization",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Organization",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Organization"));
        expectedDocumentMap.put("Email",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Email",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Email"));
        expectedDocumentMap.put("Message",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Message",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Message"));
        expectedDocumentMap.put("Parent",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Parent"));
        expectedDocumentMap.put("Outer",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Outer",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Outer"));
        expectedDocumentMap.put("BusinessImpl",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$BusinessImpl"));
        expectedDocumentMap.put("Inner",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Inner",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Inner"));
        expectedDocumentMap.put("King",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$King",
                        "androidx.appsearch.cts.app.SearchSpecCtsTest$King",
                        "androidx.appsearch.cts.observer.ObserverSpecCtsTest$King"));
        expectedDocumentMap.put("ArtType",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$ArtType"));
        expectedDocumentMap.put("Pineapple",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Pineapple",
                        "androidx.appsearch.app.AnnotationProcessorTestBase$CoolPineapple"));
        expectedDocumentMap.put("Jack",
                Arrays.asList("androidx.appsearch.cts.observer.ObserverSpecCtsTest$Jack"));
        expectedDocumentMap.put("ClassA",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$ClassA"));
        expectedDocumentMap.put("ClassB",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$ClassB"));
        expectedDocumentMap.put("Thing",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Thing"));
        expectedDocumentMap.put("Business",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Business"));
        expectedDocumentMap.put("Ace",
                Arrays.asList("androidx.appsearch.cts.observer.ObserverSpecCtsTest$Ace"));
        expectedDocumentMap.put("EmailMessage",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$EmailMessage",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$EmailMessage"));
        expectedDocumentMap.put("FakeMessage",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$FakeMessage"));
        expectedDocumentMap.put("Root",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Root"));
        expectedDocumentMap.put("Queen",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Queen",
                        "androidx.appsearch.cts.observer.ObserverSpecCtsTest$Queen"));
        expectedDocumentMap.put("EmailDocument",
                Arrays.asList("androidx.appsearch.cts.app.customer.EmailDocument"));
        expectedDocumentMap.put("Middle",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Middle",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Middle"));
        expectedDocumentMap.put("Common",
                Arrays.asList("androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Common"));
        expectedDocumentMap.put("Card",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Card",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Card",
                        "androidx.appsearch.cts.app.PutDocumentsRequestCtsTest$Card"));
        expectedDocumentMap.put("Gift",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Gift"));
        expectedDocumentMap.put("CardAction",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$CardAction"));
        expectedDocumentMap.put("InterfaceRoot",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$InterfaceRoot"));
        expectedDocumentMap.put("Person",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Person",
                        "androidx.appsearch.cts.app.SetSchemaRequestCtsTest$Person"));
        expectedDocumentMap.put("Place",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$Place"));
        expectedDocumentMap.put("LongDoc",
                Arrays.asList("androidx.appsearch.app.AnnotationProcessorTestBase$LongDoc"));
        expectedDocumentMap.put("SampleAutoValue", Arrays.asList(
                "androidx.appsearch.app.AnnotationProcessorTestBase$SampleAutoValue"));

        Map<String, List<String>> actualDocumentMap = AppSearchDocumentClassMap.getGlobalMap();
        assertThat(actualDocumentMap.keySet()).containsAtLeastElementsIn(
                expectedDocumentMap.keySet());
        for (String key : expectedDocumentMap.keySet()) {
            assertThat(actualDocumentMap.get(key)).containsAtLeastElementsIn(
                    expectedDocumentMap.get(key));
        }
    }

    @Test
    public void testGetAssignableClassBySchemaName() throws Exception {
        // Assignable to InterfaceRoot
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "InterfaceRoot", InterfaceRoot.class))
                .isEqualTo(InterfaceRoot.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Place", InterfaceRoot.class))
                .isEqualTo(Place.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Organization", InterfaceRoot.class))
                .isEqualTo(Organization.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Business", InterfaceRoot.class))
                .isEqualTo(Business.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "BusinessImpl", InterfaceRoot.class))
                .isEqualTo(BusinessImpl.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Person", InterfaceRoot.class))
                .isEqualTo(Person.class);

        // Assignable to Place
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "InterfaceRoot", Place.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Place", Place.class))
                .isEqualTo(Place.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Organization", Place.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Business", Place.class))
                .isEqualTo(Business.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "BusinessImpl", Place.class))
                .isEqualTo(BusinessImpl.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Person", Place.class))
                .isNull();

        // Assignable to Business
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "InterfaceRoot", Business.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Place", Business.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Organization", Business.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Business", Business.class))
                .isEqualTo(Business.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "BusinessImpl", Business.class))
                .isEqualTo(BusinessImpl.class);
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Person", Business.class))
                .isNull();

        // Assignable to Person
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "InterfaceRoot", Person.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Place", Person.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Organization", Person.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Business", Person.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "BusinessImpl", Person.class))
                .isNull();
        assertThat(AppSearchDocumentClassMap.getAssignableClassBySchemaName(
                AppSearchDocumentClassMap.getGlobalMap(),
                "Person", Person.class))
                .isEqualTo(Person.class);
    }

    @Test
    public void testPolymorphicDeserialization_ToOriginalType() throws Exception {
        // Create Person document
        Person.Builder personBuilder = new Person.Builder("id", "namespace")
                .setCreationTimestamp(3000)
                .setFirstName("first");
        personBuilder.setLastName("last");
        Person person = personBuilder.build();

        // Convert person to GenericDocument
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(person);

        // Test that even when deserializing genericDocument to InterfaceRoot, we will get a
        // Person instance, instead of just an InterfaceRoot.
        InterfaceRoot interfaceRoot = genericDocument.toDocumentClass(InterfaceRoot.class,
                AppSearchDocumentClassMap.getGlobalMap());
        assertThat(interfaceRoot).isInstanceOf(Person.class);
        Person newPerson = (Person) interfaceRoot;
        assertThat(newPerson.getId()).isEqualTo("id");
        assertThat(newPerson.getNamespace()).isEqualTo("namespace");
        assertThat(newPerson.getCreationTimestamp()).isEqualTo(3000);
        assertThat(newPerson.getFirstName()).isEqualTo("first");
        assertThat(newPerson.getLastName()).isEqualTo("last");

        // Test that without the document class map provided, the same deserialization will
        // just return an InterfaceRoot instance, instead of a Person.
        interfaceRoot = genericDocument.toDocumentClass(InterfaceRoot.class);
        assertThat(interfaceRoot).isInstanceOf(InterfaceRoot.class);
        assertThat(interfaceRoot).isNotInstanceOf(Person.class);
        assertThat(interfaceRoot.getId()).isEqualTo("id");
        assertThat(interfaceRoot.getNamespace()).isEqualTo("namespace");
        assertThat(interfaceRoot.getCreationTimestamp()).isEqualTo(3000);
    }

    @Test
    public void testPolymorphicDeserialization_ToBestCompatibleType() throws Exception {
        // Create a GenericDocument of unknown type.
        GenericDocument genericDocument =
                new GenericDocument.Builder<>("namespace", "id", "UnknownType")
                        .setCreationTimestampMillis(3000)
                        .setPropertyString("firstName", "first")
                        .setPropertyString("lastName", "last")
                        .build();

        // Without parent information, toDocumentClass() will try to deserialize unknown type to
        // the type that is specified in the parameter.
        InterfaceRoot interfaceRoot = genericDocument.toDocumentClass(InterfaceRoot.class,
                AppSearchDocumentClassMap.getGlobalMap());
        assertThat(interfaceRoot).isNotInstanceOf(Person.class);
        assertThat(interfaceRoot).isInstanceOf(InterfaceRoot.class);
        assertThat(interfaceRoot.getId()).isEqualTo("id");
        assertThat(interfaceRoot.getNamespace()).isEqualTo("namespace");
        assertThat(interfaceRoot.getCreationTimestamp()).isEqualTo(3000);

        // With parent information, toDocumentClass() will try to deserialize unknown type to the
        // nearest known parent type.
        genericDocument = new GenericDocument.Builder<>(genericDocument)
                .setParentTypes(new ArrayList<>(Arrays.asList("Person", "InterfaceRoot")))
                .build();
        interfaceRoot = genericDocument.toDocumentClass(InterfaceRoot.class,
                AppSearchDocumentClassMap.getGlobalMap());
        assertThat(interfaceRoot).isInstanceOf(Person.class);
        Person newPerson = (Person) interfaceRoot;
        assertThat(newPerson.getId()).isEqualTo("id");
        assertThat(newPerson.getNamespace()).isEqualTo("namespace");
        assertThat(newPerson.getCreationTimestamp()).isEqualTo(3000);
        assertThat(newPerson.getFirstName()).isEqualTo("first");
        assertThat(newPerson.getLastName()).isEqualTo("last");
    }

    @Test
    public void testPolymorphicDeserialization_nestedType() throws Exception {
        // Create a Person document
        Person.Builder personBuilder = new Person.Builder("id_person", "namespace")
                .setCreationTimestamp(3000)
                .setFirstName("first");
        personBuilder.setLastName("last");
        Person person = personBuilder.build();
        // Create a Place document
        Place place = Place.createPlace("id_place", "namespace", /* creationTimestamp= */3000,
                "place_loc");

        // Create a DocumentCollection that includes the person and the place
        DocumentCollection documentCollection = new DocumentCollection();
        documentCollection.mId = "id_collection";
        documentCollection.mNamespace = "namespace";
        documentCollection.mCollection = new InterfaceRoot[]{person, place};
        // Convert documentCollection to GenericDocument
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(documentCollection);

        // Test that when deserializing genericDocument, we will get nested Person and Place
        // instances, instead of just nested InterfaceRoot instances.
        DocumentCollection newDocumentCollection = genericDocument.toDocumentClass(
                DocumentCollection.class, AppSearchDocumentClassMap.getGlobalMap());
        assertThat(newDocumentCollection.mId).isEqualTo("id_collection");
        assertThat(newDocumentCollection.mNamespace).isEqualTo("namespace");
        assertThat(newDocumentCollection.mCollection).hasLength(2);
        // Check nested Person
        assertThat(newDocumentCollection.mCollection[0]).isInstanceOf(Person.class);
        Person newPerson = (Person) newDocumentCollection.mCollection[0];
        assertThat(newPerson.getId()).isEqualTo("id_person");
        assertThat(newPerson.getNamespace()).isEqualTo("namespace");
        assertThat(newPerson.getCreationTimestamp()).isEqualTo(3000);
        assertThat(newPerson.getFirstName()).isEqualTo("first");
        assertThat(newPerson.getLastName()).isEqualTo("last");
        // Check nested Place
        assertThat(newDocumentCollection.mCollection[1]).isInstanceOf(Place.class);
        Place newPlace = (Place) newDocumentCollection.mCollection[1];
        assertThat(newPlace.getId()).isEqualTo("id_place");
        assertThat(newPlace.getNamespace()).isEqualTo("namespace");
        assertThat(newPlace.getCreationTimestamp()).isEqualTo(3000);
        assertThat(newPlace.getLocation()).isEqualTo("place_loc");

        // Test that without the document class map provided, the nested properties will only be
        // deserialized to InterfaceRoot instances instead.
        newDocumentCollection = genericDocument.toDocumentClass(DocumentCollection.class);
        assertThat(newDocumentCollection.mId).isEqualTo("id_collection");
        assertThat(newDocumentCollection.mNamespace).isEqualTo("namespace");
        assertThat(newDocumentCollection.mCollection).hasLength(2);
        // Check nested Person
        assertThat(newDocumentCollection.mCollection[0]).isInstanceOf(InterfaceRoot.class);
        assertThat(newDocumentCollection.mCollection[0]).isNotInstanceOf(Person.class);
        assertThat(newDocumentCollection.mCollection[0].getId()).isEqualTo("id_person");
        assertThat(newDocumentCollection.mCollection[0].getNamespace()).isEqualTo("namespace");
        assertThat(newDocumentCollection.mCollection[0].getCreationTimestamp()).isEqualTo(3000);
        // Check nested Place
        assertThat(newDocumentCollection.mCollection[1]).isInstanceOf(InterfaceRoot.class);
        assertThat(newDocumentCollection.mCollection[1]).isNotInstanceOf(Place.class);
        assertThat(newDocumentCollection.mCollection[1].getId()).isEqualTo("id_place");
        assertThat(newDocumentCollection.mCollection[1].getNamespace()).isEqualTo("namespace");
        assertThat(newDocumentCollection.mCollection[1].getCreationTimestamp()).isEqualTo(3000);
    }

    @Test
    public void testPolymorphicDeserialization_Integration() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        // Add an unknown business type this is a subtype of Business.
        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(Business.class)
                .addSchemas(new AppSearchSchema.Builder("UnknownBusiness")
                        .addParentType("Business")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "organizationDescription")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("location")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "businessName")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "unknownProperty")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .build())
                .build()).get();

        // Create and put an UnknownBusiness document.
        GenericDocument genericDoc =
                new GenericDocument.Builder<>("namespace", "id", "UnknownBusiness")
                        .setCreationTimestampMillis(3000)
                        .setPropertyString("location", "business_loc")
                        .setPropertyString("organizationDescription", "business_dec")
                        .setPropertyString("businessName", "business_name")
                        .setPropertyString("unknownProperty", "foo")
                        .build();
        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(genericDoc).build()));

        // Query to get the document back, with parent information added.
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        GenericDocument actualGenericDoc = documents.get(0);
        GenericDocument expectedGenericDoc = new GenericDocument.Builder<>(genericDoc)
                .setParentTypes(new ArrayList<>(Arrays.asList("Business", "Place", "Organization",
                        "InterfaceRoot")))
                .build();
        assertThat(actualGenericDoc).isEqualTo(expectedGenericDoc);

        // Deserializing it to InterfaceRoot will get a Business instance back.
        InterfaceRoot interfaceRoot = actualGenericDoc.toDocumentClass(InterfaceRoot.class,
                AppSearchDocumentClassMap.getGlobalMap());
        assertThat(interfaceRoot).isInstanceOf(Business.class);
        Business business = (Business) interfaceRoot;
        assertThat(business.getId()).isEqualTo("id");
        assertThat(business.getNamespace()).isEqualTo("namespace");
        assertThat(business.getCreationTimestamp()).isEqualTo(3000);
        assertThat(business.getLocation()).isEqualTo("business_loc");
        assertThat(business.getOrganizationDescription()).isEqualTo("business_dec");
        assertThat(business.getBusinessName()).isEqualTo("business_name");
    }

    // InterfaceRoot
    //   |    \
    //   |    Person
    //   |    /
    //   UnknownA
    @Test
    public void testPolymorphicDeserialization_IntegrationDiamondThreeTypes() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("UnknownA")
                        .addParentType("InterfaceRoot")
                        .addParentType("Person")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "firstName")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "lastName")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .build())
                .addDocumentClasses(Person.class)
                .build()).get();

        // Create and put an UnknownA document.
        GenericDocument genericDoc =
                new GenericDocument.Builder<>("namespace", "id", "UnknownA")
                        .setCreationTimestampMillis(3000)
                        .setPropertyString("firstName", "first")
                        .setPropertyString("lastName", "last")
                        .build();
        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(genericDoc).build()));

        // Query to get the document back, with parent information added.
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        GenericDocument actualGenericDoc = documents.get(0);
        GenericDocument expectedGenericDoc = new GenericDocument.Builder<>(genericDoc)
                .setParentTypes(new ArrayList<>(Arrays.asList("Person", "InterfaceRoot")))
                .build();
        assertThat(actualGenericDoc).isEqualTo(expectedGenericDoc);

        // Deserializing it to InterfaceRoot will get a Person instance back.
        InterfaceRoot interfaceRoot = actualGenericDoc.toDocumentClass(InterfaceRoot.class,
                AppSearchDocumentClassMap.getGlobalMap());
        assertThat(interfaceRoot).isInstanceOf(Person.class);
        Person person = (Person) interfaceRoot;
        assertThat(person.getId()).isEqualTo("id");
        assertThat(person.getNamespace()).isEqualTo("namespace");
        assertThat(person.getCreationTimestamp()).isEqualTo(3000);
        assertThat(person.getFirstName()).isEqualTo("first");
        assertThat(person.getLastName()).isEqualTo("last");
    }

    //   InterfaceRoot
    //    /        \
    // UnknownA   Person
    //    \       /
    //    Unknown B
    @Test
    public void testPolymorphicDeserialization_IntegrationDiamondTwoUnknown() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("UnknownA")
                        .addParentType("InterfaceRoot")
                        .build())
                .addSchemas(new AppSearchSchema.Builder("UnknownB")
                        .addParentType("UnknownA")
                        .addParentType("Person")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "firstName")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "lastName")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .build())
                .addDocumentClasses(Person.class)
                .build()).get();

        // Create and put an UnknownB document.
        GenericDocument genericDoc =
                new GenericDocument.Builder<>("namespace", "id", "UnknownB")
                        .setCreationTimestampMillis(3000)
                        .setPropertyString("firstName", "first")
                        .setPropertyString("lastName", "last")
                        .build();
        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(genericDoc).build()));

        // Query to get the document back, with parent information added.
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        GenericDocument actualGenericDoc = documents.get(0);
        GenericDocument expectedGenericDoc = new GenericDocument.Builder<>(genericDoc)
                .setParentTypes(new ArrayList<>(
                        Arrays.asList("UnknownA", "Person", "InterfaceRoot")))
                .build();
        assertThat(actualGenericDoc).isEqualTo(expectedGenericDoc);

        // Deserializing it to InterfaceRoot will get a Person instance back.
        InterfaceRoot interfaceRoot = actualGenericDoc.toDocumentClass(InterfaceRoot.class,
                AppSearchDocumentClassMap.getGlobalMap());
        assertThat(interfaceRoot).isInstanceOf(Person.class);
        Person person = (Person) interfaceRoot;
        assertThat(person.getId()).isEqualTo("id");
        assertThat(person.getNamespace()).isEqualTo("namespace");
        assertThat(person.getCreationTimestamp()).isEqualTo(3000);
        assertThat(person.getFirstName()).isEqualTo("first");
        assertThat(person.getLastName()).isEqualTo("last");
    }

    //   InterfaceRoot
    //    /        \
    // Person   Organization
    //    \        /
    //    Unknown A
    @Test
    public void testPolymorphicDeserialization_IntegrationDiamondOneUnknown() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(Features.SCHEMA_ADD_PARENT_TYPE));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(Person.class)
                .addDocumentClasses(Organization.class)
                .addSchemas(new AppSearchSchema.Builder("UnknownA")
                        .addParentType("Person")
                        .addParentType("Organization")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "firstName")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "lastName")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                                "organizationDescription")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .build())
                        .build())
                .addDocumentClasses(Person.class)
                .build()).get();

        // Create and put an UnknownA document.
        GenericDocument genericDoc =
                new GenericDocument.Builder<>("namespace", "id", "UnknownA")
                        .setCreationTimestampMillis(3000)
                        .setPropertyString("firstName", "first")
                        .setPropertyString("lastName", "last")
                        .setPropertyString("organizationDescription", "person")
                        .build();
        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(genericDoc).build()));

        // Query to get the document back, with parent information added.
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder().build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);
        GenericDocument actualGenericDoc = documents.get(0);
        GenericDocument expectedGenericDoc = new GenericDocument.Builder<>(genericDoc)
                .setParentTypes(new ArrayList<>(
                        Arrays.asList("Person", "Organization", "InterfaceRoot")))
                .build();
        assertThat(actualGenericDoc).isEqualTo(expectedGenericDoc);

        // Deserializing it to InterfaceRoot will get a Person instance back, which is the first
        // known type, instead of an Organization.
        InterfaceRoot interfaceRoot = actualGenericDoc.toDocumentClass(InterfaceRoot.class,
                AppSearchDocumentClassMap.getGlobalMap());
        assertThat(interfaceRoot).isInstanceOf(Person.class);
        assertThat(interfaceRoot).isNotInstanceOf(Organization.class);
        Person person = (Person) interfaceRoot;
        assertThat(person.getId()).isEqualTo("id");
        assertThat(person.getNamespace()).isEqualTo("namespace");
        assertThat(person.getCreationTimestamp()).isEqualTo(3000);
        assertThat(person.getFirstName()).isEqualTo("first");
        assertThat(person.getLastName()).isEqualTo("last");
    }

    @Document
    static class EmailWithEmbedding {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.CreationTimestampMillis
        long mCreationTimestampMillis;

        @Document.StringProperty
        String mSender;

        // Default non-indexable embedding
        @Document.EmbeddingProperty
        EmbeddingVector mSenderEmbedding;

        @Document.EmbeddingProperty(indexingType = 1)
        EmbeddingVector mTitleEmbedding;

        @Document.EmbeddingProperty(indexingType = 1)
        Collection<EmbeddingVector> mReceiverEmbeddings;

        @Document.EmbeddingProperty(indexingType = 1)
        EmbeddingVector[] mBodyEmbeddings;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EmailWithEmbedding email = (EmailWithEmbedding) o;
            return Objects.equals(mNamespace, email.mNamespace) && Objects.equals(mId,
                    email.mId) && Objects.equals(mSender, email.mSender)
                    && Objects.equals(mSenderEmbedding, email.mSenderEmbedding)
                    && Objects.equals(mTitleEmbedding, email.mTitleEmbedding)
                    && Objects.equals(mReceiverEmbeddings, email.mReceiverEmbeddings)
                    && Arrays.equals(mBodyEmbeddings, email.mBodyEmbeddings);
        }

        public static EmailWithEmbedding createSampleDoc() {
            EmbeddingVector embedding1 =
                    new EmbeddingVector(new float[]{1, 2, 3}, "model1");
            EmbeddingVector embedding2 =
                    new EmbeddingVector(new float[]{-1, -2, -3}, "model2");
            EmbeddingVector embedding3 =
                    new EmbeddingVector(new float[]{0.1f, 0.2f, 0.3f, 0.4f}, "model3");
            EmbeddingVector embedding4 =
                    new EmbeddingVector(new float[]{-0.1f, -0.2f, -0.3f, -0.4f}, "model3");
            EmailWithEmbedding email = new EmailWithEmbedding();
            email.mNamespace = "namespace";
            email.mId = "id";
            email.mCreationTimestampMillis = 1000;
            email.mSender = "sender";
            email.mSenderEmbedding = embedding1;
            email.mTitleEmbedding = embedding2;
            email.mReceiverEmbeddings = Collections.singletonList(embedding3);
            email.mBodyEmbeddings = new EmbeddingVector[]{embedding3, embedding4};
            return email;
        }
    }

    @Test
    public void testEmbeddingGenericDocumentConversion() throws Exception {
        EmailWithEmbedding inEmail = EmailWithEmbedding.createSampleDoc();
        GenericDocument genericDocument1 = GenericDocument.fromDocumentClass(inEmail);
        GenericDocument genericDocument2 = GenericDocument.fromDocumentClass(inEmail);
        EmailWithEmbedding outEmail = genericDocument2.toDocumentClass(EmailWithEmbedding.class);

        assertThat(inEmail).isNotSameInstanceAs(outEmail);
        assertThat(inEmail).isEqualTo(outEmail);
        assertThat(genericDocument1).isNotSameInstanceAs(genericDocument2);
        assertThat(genericDocument1).isEqualTo(genericDocument2);
    }

    @Test
    public void testEmbeddingSearch() throws Exception {
        assumeTrue(mSession.getFeatures().isFeatureSupported(
                Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG));

        mSession.setSchemaAsync(new SetSchemaRequest.Builder()
                .addDocumentClasses(EmailWithEmbedding.class)
                .build()).get();

        // Create and add a document
        EmailWithEmbedding email = EmailWithEmbedding.createSampleDoc();
        checkIsBatchResultSuccess(mSession.putAsync(
                new PutDocumentsRequest.Builder()
                        .addDocuments(email)
                        .build()));

        // An empty query should retrieve this document.
        SearchResults searchResults = mSession.search("",
                new SearchSpec.Builder().build());
        List<SearchResult> results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(1);
        // Convert GenericDocument to EmailWithEmbedding and check values.
        EmailWithEmbedding outputDocument = results.get(0).getDocument(EmailWithEmbedding.class);
        assertThat(outputDocument).isEqualTo(email);

        // senderEmbedding is non-indexable, so querying for it will return nothing.
        searchResults = mSession.search("semanticSearch(getEmbeddingParameter(0), 0.9, 1)",
                new SearchSpec.Builder()
                        .setDefaultEmbeddingSearchMetricType(
                                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_COSINE)
                        .addEmbeddingParameters(email.mSenderEmbedding)
                        .setRankingStrategy(
                                "sum(this.matchedSemanticScores(getEmbeddingParameter(0)))")
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        results = retrieveAllSearchResults(searchResults);
        assertThat(results).isEmpty();

        // titleEmbedding is indexable, and querying for it using itself will return a cosine
        // similarity score of 1.
        searchResults = mSession.search("semanticSearch(getEmbeddingParameter(0), 0.9, 1)",
                new SearchSpec.Builder()
                        .setDefaultEmbeddingSearchMetricType(
                                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_COSINE)
                        .addEmbeddingParameters(email.mTitleEmbedding)
                        .setRankingStrategy(
                                "sum(this.matchedSemanticScores(getEmbeddingParameter(0)))")
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRankingSignal()).isWithin(0.00001).of(1);
        // Convert GenericDocument to EmailWithEmbedding and check values.
        outputDocument = results.get(0).getDocument(EmailWithEmbedding.class);
        assertThat(outputDocument).isEqualTo(email);

        // Both receiverEmbeddings and bodyEmbeddings are indexable, and in this specific
        // document, they together hold three embedding vectors with the same signature.
        searchResults = mSession.search("semanticSearch(getEmbeddingParameter(0), -1, 1)",
                new SearchSpec.Builder()
                        .setDefaultEmbeddingSearchMetricType(
                                SearchSpec.EMBEDDING_SEARCH_METRIC_TYPE_COSINE)
                        // Using one of the three vectors to query
                        .addEmbeddingParameters(email.mBodyEmbeddings[0])
                        .setRankingStrategy(
                                // We should get a score of 3 for "len", since there are three
                                // embedding vectors matched.
                                "len(this.matchedSemanticScores(getEmbeddingParameter(0)))")
                        .setListFilterQueryLanguageEnabled(true)
                        .build());
        results = retrieveAllSearchResults(searchResults);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRankingSignal()).isEqualTo(3);
        // Convert GenericDocument to EmailWithEmbedding and check values.
        outputDocument = results.get(0).getDocument(EmailWithEmbedding.class);
        assertThat(outputDocument).isEqualTo(email);
    }
}
